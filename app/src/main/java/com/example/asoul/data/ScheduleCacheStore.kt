package com.example.asoul.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * P6 本地缓存：静态 JSON 的文件缓存 + SharedPreferences 版本号。
 *
 * 数据策略：**缓存优先 → 后台拉新 → 断网兜底**。
 * - `latest_cache.json`：周程表缓存
 * - `flash_cache.json`：突击直播缓存
 * - 版本号存 SharedPreferences，用于与服务端 version 比对
 *
 * 全部 IO 在 [Dispatchers.IO] 上执行；任何读写失败静默返回空，不影响主流程。
 */
class ScheduleCacheStore(context: Context) {

    private val latestFile = File(context.filesDir, LATEST_CACHE_FILE)
    private val flashFile = File(context.filesDir, FLASH_CACHE_FILE)
    // 额外数据源（asoul.love ICS）：缓存原文，重启后解析复用（断网兜底）
    private val asoulLoveFile = File(context.filesDir, ASOUL_LOVE_CACHE_FILE)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 读取缓存的 JSON 原文；不存在或读取失败返回 null。 */
    suspend fun readLatest(): String? = readFile(latestFile)

    /** 读取缓存的突击直播 JSON 原文。 */
    suspend fun readFlash(): String? = readFile(flashFile)

    /** 读取缓存的 asoul.love ICS 原文。 */
    suspend fun readAsoulLove(): String? = readFile(asoulLoveFile)

    /** 写入周程表缓存（覆盖旧文件）。 */
    suspend fun writeLatest(json: String) = writeFile(latestFile, json)

    /** 写入突击直播缓存（覆盖旧文件）。 */
    suspend fun writeFlash(json: String) = writeFile(flashFile, json)

    /** 写入 asoul.love ICS 缓存（覆盖旧文件）。 */
    suspend fun writeAsoulLove(text: String) = writeFile(asoulLoveFile, text)

    /** 已缓存的周程表 version；无缓存返回 0。 */
    fun latestVersion(): Long = prefs.getLong(KEY_LATEST_VERSION, 0L)

    /** 已缓存的突击直播 version；无缓存返回 0。 */
    fun flashVersion(): Long = prefs.getLong(KEY_FLASH_VERSION, 0L)

    /** 更新周程表版本号。 */
    fun saveLatestVersion(version: Long) = prefs.edit().putLong(KEY_LATEST_VERSION, version).apply()

    /** 更新突击直播版本号。 */
    fun saveFlashVersion(version: Long) = prefs.edit().putLong(KEY_FLASH_VERSION, version).apply()

    /** asoul.love ICS 最近一次**成功**拉取时间（epoch 毫秒）；从未拉取返回 0。 */
    fun asoulLoveFetchedAt(): Long = prefs.getLong(KEY_ASOUL_LOVE_FETCHED_AT, 0L)

    /** asoul.love ICS 最近一次**尝试**拉取时间（失败也记录，用于失败重试节流）。 */
    fun asoulLoveAttemptAt(): Long = prefs.getLong(KEY_ASOUL_LOVE_ATTEMPT_AT, 0L)

    /** 记录成功拉取时间。 */
    fun saveAsoulLoveFetchedAt(timestamp: Long) =
        prefs.edit().putLong(KEY_ASOUL_LOVE_FETCHED_AT, timestamp).apply()

    /** 记录拉取尝试时间。 */
    fun saveAsoulLoveAttemptAt(timestamp: Long) =
        prefs.edit().putLong(KEY_ASOUL_LOVE_ATTEMPT_AT, timestamp).apply()

    private suspend fun readFile(file: File): String? = withContext(Dispatchers.IO) {
        runCatching { if (file.exists()) file.readText() else null }.getOrNull()
    }

    private suspend fun writeFile(file: File, json: String) = withContext(Dispatchers.IO) {
        runCatching { file.writeText(json) }
    }

    private companion object {
        const val LATEST_CACHE_FILE = "latest_cache.json"
        const val FLASH_CACHE_FILE = "flash_cache.json"
        const val ASOUL_LOVE_CACHE_FILE = "asoul_love_cache.ics"
        const val PREFS_NAME = "schedule_cache"
        const val KEY_LATEST_VERSION = "latest_version"
        const val KEY_FLASH_VERSION = "flash_version"
        const val KEY_ASOUL_LOVE_FETCHED_AT = "asoul_love_fetched_at"
        const val KEY_ASOUL_LOVE_ATTEMPT_AT = "asoul_love_attempt_at"
    }
}
