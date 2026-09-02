package com.example.asoul.data.update

import android.content.Context

/**
 * 更新提示的本地记忆（SharedPreferences）：
 * - 用户「跳过此版本」的 version_code（跳过即永久不再提示该版本）
 * - 最近一次提示日期（每日最多提示一次，避免每次冷启动都打扰）
 */
class AppUpdateStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 用户选择「跳过此版本」的 version_code；从未跳过分返回 -1。 */
    fun skippedVersionCode(): Int = prefs.getInt(KEY_SKIPPED_VERSION, -1)

    /** 记录跳过某个版本。 */
    fun markSkipped(versionCode: Int) {
        prefs.edit().putInt(KEY_SKIPPED_VERSION, versionCode).apply()
    }

    /** 最近一次提示更新的日期（yyyy-MM-dd）；从未提示过返回空串。 */
    fun lastPromptDate(): String = prefs.getString(KEY_LAST_PROMPT_DATE, "") ?: ""

    /** 记录「今天已提示过」（配合跳过逻辑实现每日最多一次）。 */
    fun markPrompted(date: String) {
        prefs.edit().putString(KEY_LAST_PROMPT_DATE, date).apply()
    }

    private companion object {
        const val PREFS_NAME = "app_update"
        const val KEY_SKIPPED_VERSION = "skipped_version_code"
        const val KEY_LAST_PROMPT_DATE = "last_prompt_date"
    }
}
