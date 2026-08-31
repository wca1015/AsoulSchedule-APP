package com.example.asoul.data

import android.util.Log
import com.example.asoul.data.model.FlashLiveEvent
import com.example.asoul.data.remote.ScheduleApiClient
import com.example.asoul.data.remote.dto.FlashDto
import com.example.asoul.data.remote.dto.toFlashEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * P6/P10 突击直播仓库：负责「拉取 → 版本比对 → 更新缓存 → emit」。
 *
 * 策略：缓存优先 → 后台拉新 → 断网兜底
 * - 启动 / 回前台：先用本地缓存填充（有数据即可展示，断网可用）
 * - 拉取成功后比对 version：更新才写缓存并 emit，避免无谓刷新
 *
 * 网络与文件 IO 均在 [Dispatchers.IO] 上，失败静默（日志 Log.w），不抛异常。
 */
class FlashScheduleRepository(
    private val client: ScheduleApiClient,
    private val cache: ScheduleCacheStore,
) {

    private val _events = MutableStateFlow<List<FlashLiveEvent>>(emptyList())
    /** 当前突击直播列表（含已结束，UI 层负责过滤）。 */
    val events: StateFlow<List<FlashLiveEvent>> = _events.asStateFlow()

    /** 最近一次成功拉取/缓存加载到的服务端 version。 */
    var lastFlashVersion: Long = 0L
        private set

    private val json = Json { ignoreUnknownKeys = true }

    /** 启动时用本地缓存填充（断网兜底）；无缓存时列表为空。 */
    suspend fun loadFromCache() = withContext(Dispatchers.IO) {
        // 先用 SharedPreferences 中的版本号恢复基线，避免首拉重复写入/重复提醒；
        // 缓存文件缺失/损坏时重置基线，保证网络恢复后可重新拉取。
        lastFlashVersion = cache.flashVersion()
        val cached = cache.readFlash()
        if (cached == null) {
            lastFlashVersion = 0L
            return@withContext
        }
        val dto = runCatching { json.decodeFromString<FlashDto>(cached) }.getOrElse {
            Log.w(TAG, "flash 缓存解析失败: ${it.message}")
            lastFlashVersion = 0L
            return@withContext
        }
        lastFlashVersion = dto.version
        _events.value = dto.toFlashEvents()
    }

    /**
     * 拉取 `/flash.json`：version 大于本地才更新（否则跳过）。
     * 网络失败时保持现有数据（已由缓存/上次拉取填充）。
     */
    suspend fun fetchLatestFlash() = withContext(Dispatchers.IO) {
        val body = client.fetchFlash() ?: return@withContext
        val dto = runCatching { json.decodeFromString<FlashDto>(body) }.getOrElse {
            Log.w(TAG, "flash.json 解析失败: ${it.message}")
            return@withContext
        }
        // 版本比对：无更新直接返回（无缓存基线时不做限制，照常更新）
        if ((lastFlashVersion != 0L) && (dto.version <= lastFlashVersion)) return@withContext
        lastFlashVersion = dto.version
        cache.writeFlash(body)
        cache.saveFlashVersion(dto.version)
        _events.value = dto.toFlashEvents()
    }

    private companion object {
        const val TAG = "FlashScheduleRepository"
    }
}
