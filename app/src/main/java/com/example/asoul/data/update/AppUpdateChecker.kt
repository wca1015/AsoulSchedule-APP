package com.example.asoul.data.update

import android.util.Log
import com.example.asoul.BuildConfig
import com.example.asoul.data.remote.ScheduleApiClient
import com.example.asoul.data.remote.dto.AppVersionDto
import java.time.LocalDate
import kotlinx.serialization.json.Json

/**
 * App 更新检查器：拉取 `/app_version.json`，按三重条件决定是否弹更新提示。
 *
 * 弹窗条件（全部满足才返回版本信息）：
 * 1. 服务端 [AppVersionDto.versionCode] > 本地 [BuildConfig.VERSION_CODE]
 * 2. 该版本未被用户「跳过此版本」
 * 3. 今天尚未提示过（每日最多一次，避免每次冷启动都打扰）
 *
 * 网络 / 解析失败静默（[Log.w]），不影响主流程。
 */
class AppUpdateChecker(
    private val apiClient: ScheduleApiClient,
    private val store: AppUpdateStore,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 检查是否需要提示更新。
     *
     * @param currentVersionCode 本地版本号（默认取 BuildConfig，便于测试注入）
     * @return 需要提示时返回版本信息，否则返回 null
     */
    suspend fun check(currentVersionCode: Int = BuildConfig.VERSION_CODE): AppVersionDto? {
        val body = apiClient.fetchAppVersion() ?: return null
        val remote = runCatching { json.decodeFromString<AppVersionDto>(body) }.getOrElse {
            Log.w(TAG, "app_version.json 解析失败: ${it.message}")
            return null
        }
        // 1. 无新版本
        if (remote.versionCode <= currentVersionCode) return null
        // 没有可下载地址的清单不弹（避免出现无法更新的空弹窗）
        if (remote.apkUrl.isBlank()) return null
        // 2. 用户已跳过该版本
        if (remote.versionCode == store.skippedVersionCode()) return null
        // 3. 今日已提示过
        val today = LocalDate.now().toString()
        if (store.lastPromptDate() == today) return null
        store.markPrompted(today)
        return remote
    }

    private companion object {
        const val TAG = "AppUpdateChecker"
    }
}
