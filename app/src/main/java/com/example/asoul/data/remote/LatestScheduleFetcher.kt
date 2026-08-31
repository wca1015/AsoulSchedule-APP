package com.example.asoul.data.remote

import android.util.Log
import com.example.asoul.data.ScheduleCacheStore
import com.example.asoul.data.ScheduleRepository
import com.example.asoul.data.remote.dto.LatestScheduleDto
import com.example.asoul.data.remote.dto.toSchedules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * P6 周程表拉取器：负责「拉取 `/latest.json` → 版本比对 → 更新缓存 → 按周替换仓库日程」。
 *
 * - version 大于本地缓存版本才视为有更新
 * - 更新成功后按 `week_start` 定位，替换 [ScheduleRepository] 中该周的日程
 * - 网络/解析失败静默（日志 Log.w），仓库保持现状（缓存或 Mock 兜底数据不受影响）
 */
class LatestScheduleFetcher(
    private val client: ScheduleApiClient,
    private val cache: ScheduleCacheStore,
    private val repository: ScheduleRepository,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /** 最近一次成功应用的周程表 version。 */
    var lastLatestVersion: Long = 0L
        private set

    /** 最近一次成功应用到仓库的 week_start（UI 据此提示服务端数据所属周）。 */
    var lastAppliedWeekStart: LocalDate? = null
        private set

    /** 启动时读取本地缓存并注入仓库（断网兜底）；无缓存/解析失败返回 false。 */
    suspend fun loadFromCache(): Boolean = withContext(Dispatchers.IO) {
        // 先用 SharedPreferences 中的版本号恢复基线，避免首拉重复应用同一版本；
        // 缓存文件缺失/损坏时重置基线，保证网络恢复后可重新拉取。
        lastLatestVersion = cache.latestVersion()
        val cached = cache.readLatest()
        if (cached == null) {
            lastLatestVersion = 0L
            return@withContext false
        }
        val dto = runCatching { json.decodeFromString<LatestScheduleDto>(cached) }.getOrElse {
            Log.w(TAG, "latest 缓存解析失败: ${it.message}")
            lastLatestVersion = 0L
            return@withContext false
        }
        // 防火墙：年份异常的缓存（如 VLM 把 2026 认成 2023）视为无缓存，
        // 避免把日程落到用户看不见的周。
        if (!dto.isWeekStartReasonable()) {
            Log.w(TAG, "latest 缓存 week_start 异常（疑似年份错误）: ${dto.weekStart}，忽略缓存")
            lastLatestVersion = 0L
            return@withContext false
        }
        lastLatestVersion = dto.version
        applyToRepository(dto)
        true
    }

    /**
     * 拉取最新周程表；有更新则写缓存并替换对应周。
     *
     * @param force 强制应用（下拉刷新使用）：跳过版本号比对，
     *   避免版本基线异常时客户端永远拉不到数据（用户自救路径）。
     */
    suspend fun fetchAndApply(force: Boolean = false) = withContext(Dispatchers.IO) {
        val body = client.fetchLatest() ?: return@withContext
        val dto = runCatching { json.decodeFromString<LatestScheduleDto>(body) }.getOrElse {
            Log.w(TAG, "latest.json 解析失败: ${it.message}")
            return@withContext
        }
        // 防火墙：年份异常的数据拒绝应用、也不写缓存（服务端校验失守时的客户端兵底）
        if (!dto.isWeekStartReasonable()) {
            Log.w(TAG, "latest.json week_start 异常（疑似年份错误）: ${dto.weekStart}，拒绝应用")
            return@withContext
        }
        // 版本比对：无更新直接返回（无缓存基线时不做限制，照常更新；强制刷新跳过比对）
        if (!force && (lastLatestVersion != 0L) && (dto.version <= lastLatestVersion)) return@withContext
        lastLatestVersion = dto.version
        cache.writeLatest(body)
        cache.saveLatestVersion(dto.version)
        applyToRepository(dto)
    }

    /** 各周最近已应用的 version（往日周按需拉取时按周比对，避免进程内重复替换）。 */
    private val appliedWeekVersions = mutableMapOf<LocalDate, Long>()

    /**
     * 按需拉取往日周周程表 `week/{week_start}.json`（用户右划回看往日周 / 往日周下拉刷新时调用）。
     *
     * - 文件未发布（404）/ 网络失败：静默保持仓库现状，返回 false；
     *   （服务端未同步往日周时，此处始终返回 false，不影响现有数据）
     * - 版本比对按周独立：不复用 [lastLatestVersion]（往日周版本必然低于当前周，
     *   全局比对会被误判为无更新而跳过）
     * - 不写本地缓存：缓存文件只保存当前周，避免往日周污染启动引导流程。
     *
     * @return 是否成功拉取并更新了仓库。
     */
    suspend fun fetchWeekAndApply(weekStart: LocalDate): Boolean = withContext(Dispatchers.IO) {
        val body = client.fetchWeek(ApiEndpoints.weekUrl(weekStart)) ?: return@withContext false
        val dto = runCatching { json.decodeFromString<LatestScheduleDto>(body) }.getOrElse {
            Log.w(TAG, "往日周程表($weekStart) 解析失败: ${it.message}")
            return@withContext false
        }
        val dtoWeekStart = dto.parsedWeekStart()
        if (dtoWeekStart == null) {
            Log.w(TAG, "往日周 week_start 解析失败: ${dto.weekStart}")
            return@withContext false
        }
        if (!dto.isWeekStartReasonable()) {
            Log.w(TAG, "往日周 week_start 异常（疑似年份错误）: ${dto.weekStart}，拒绝应用")
            return@withContext false
        }
        // 按周版本比对：已应用过相同/更高版本则跳过（服务端录播回填后版本递增，可正常拉到）
        val applied = appliedWeekVersions[dtoWeekStart]
        if ((applied != null) && (dto.version <= applied)) return@withContext false
        appliedWeekVersions[dtoWeekStart] = dto.version
        applyToRepository(dto)
        true
    }

    /** 按 week_start 定位，替换仓库中该周日程（解析失败/年份异常/空日程时跳过）。 */
    private fun applyToRepository(dto: LatestScheduleDto) {
        val weekStart = dto.parsedWeekStart()
        if (weekStart == null) {
            Log.w(TAG, "week_start 解析失败: ${dto.weekStart}")
            return
        }
        if (!dto.isWeekStartReasonable()) {
            Log.w(TAG, "week_start 异常（疑似年份错误）: ${dto.weekStart}，拒绝应用")
            return
        }
        val schedules = dto.toSchedules()
        if (schedules.isEmpty()) return
        repository.replaceWeek(weekStart, schedules)
        lastAppliedWeekStart = weekStart
    }

    private companion object {
        const val TAG = "LatestScheduleFetcher"
    }
}
