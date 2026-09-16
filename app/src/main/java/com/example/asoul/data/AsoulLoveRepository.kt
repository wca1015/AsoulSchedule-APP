package com.example.asoul.data

import android.util.Log
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.remote.IcsParser
import com.example.asoul.data.remote.ScheduleApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 额外数据源仓库：asoul.love 日历订阅（ICS）。
 *
 * 职责：拉取 → 解析 → 缓存 → emit。**严格遵守对方 1 小时拉取间隔**
 * （`calendar.ics` 的 `REFRESH-INTERVAL:PT1H`，且站方明确要求勿高频抓取）：
 * - 自动轮询（前台每 1 小时）与回前台拉取：距上次**成功** ≥ 1 小时才发请求
 * - 下拉刷新（用户主动）：允许提前拉取，但两次**尝试**至少间隔 10 分钟（防抖）
 * - 失败静默（仅日志），保留缓存数据，不影响主流程
 */
class AsoulLoveRepository(
    private val client: ScheduleApiClient,
    private val cache: ScheduleCacheStore,
) {

    private val _events = MutableStateFlow<List<LiveSchedule>>(emptyList())

    /** 解析后的日程列表（全量，UI 层按周裁剪）。 */
    val events: StateFlow<List<LiveSchedule>> = _events.asStateFlow()

    /** 启动时用本地缓存填充（断网兜底）。 */
    suspend fun loadFromCache() = withContext(Dispatchers.IO) {
        val cached = cache.readAsoulLove() ?: return@withContext
        val parsed = runCatching { IcsParser.parseSchedules(cached) }.getOrElse {
            Log.w(TAG, "asoul.love 缓存解析失败: ${it.message}")
            return@withContext
        }
        _events.value = parsed
    }

    /**
     * 按间隔策略拉取 + 解析。
     *
     * @param force 用户主动刷新时为 true（放宽到 [MIN_ATTEMPT_GAP_MS] 的最短间隔）
     * @return 是否真正发起了请求并更新了数据
     */
    suspend fun fetchIfDue(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sinceAttempt = now - cache.asoulLoveAttemptAt()
        val sinceFetch = now - cache.asoulLoveFetchedAt()
        val due = if (force) {
            sinceAttempt >= MIN_ATTEMPT_GAP_MS
        } else {
            sinceFetch >= POLL_INTERVAL_MS && sinceAttempt >= MIN_ATTEMPT_GAP_MS
        }
        if (!due) return@withContext false

        cache.saveAsoulLoveAttemptAt(now)
        val body = client.fetchAsoulLoveIcs() ?: return@withContext false
        val parsed = runCatching { IcsParser.parseSchedules(body) }.getOrElse {
            Log.w(TAG, "asoul.love ICS 解析失败: ${it.message}")
            return@withContext false
        }
        if (parsed.isEmpty()) {
            Log.w(TAG, "asoul.love ICS 无有效事件，保留现有数据")
            return@withContext false
        }
        cache.writeAsoulLove(body)
        cache.saveAsoulLoveFetchedAt(now)
        _events.value = parsed
        true
    }

    private companion object {
        const val TAG = "AsoulLoveRepository"

        /** 常规拉取间隔：1 小时（与对方 REFRESH-INTERVAL / Cache-Control 一致）。 */
        const val POLL_INTERVAL_MS = 60 * 60 * 1000L

        /** 两次尝试的最小间隔（失败重试 / 手动刷新防抖）。 */
        const val MIN_ATTEMPT_GAP_MS = 10 * 60 * 1000L
    }
}
