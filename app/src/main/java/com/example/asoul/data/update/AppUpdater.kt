package com.example.asoul.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * APK 下载与安装（更新功能的核心执行器）。
 *
 * - 下载到 `cacheDir/update/asoul-update.apk`（缓存目录，文件被系统回收也不影响数据）
 * - 安装走 FileProvider 授权 + `ACTION_VIEW`（`application/vnd.android.package-archive`）
 * - 本项目 minSdk 26 全覆盖 Android 8.0+ 的「安装未知应用」特殊权限：
 *   未授权时引导用户到系统设置页开启（`ACTION_MANAGE_UNKNOWN_APP_SOURCES`）
 */
class AppUpdater(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // APK 下载可能较慢
        .build()

    private val updateDir: File
        get() = File(context.cacheDir, "update").apply { mkdirs() }

    /** 下载目标文件（固定名，重复更新直接覆盖旧文件）。 */
    fun targetFile(): File = File(updateDir, APK_FILE_NAME)

    /** 是否已授予「安装未知应用」（Android 8.0+；更早版本无需检查）。 */
    fun canInstallUnknownApps(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    /** 跳转系统「安装未知应用」设置页，引导用户放行本应用。 */
    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "打开安装来源设置失败: ${it.message}") }
    }

    /**
     * 下载 APK 到缓存目录（流式写盘，不占内存）。
     *
     * @return 成功返回本地文件；失败返回 null（已清理半成品文件）。
     */
    suspend fun download(url: String): File? = withContext(Dispatchers.IO) {
        val target = targetFile()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "下载失败: HTTP ${resp.code}")
                    return@withContext null
                }
                val body = resp.body
                body.byteStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            target
        } catch (e: Exception) {
            Log.w(TAG, "下载异常: ${e.message}")
            target.delete()
            null
        }
    }

    /**
     * 调起系统安装页（FileProvider 授权）。
     *
     * @return true = 已调起安装；false = 缺少「安装未知应用」权限（调用方引导开启）。
     */
    fun install(apkFile: File): Boolean {
        if (!canInstallUnknownApps()) return false
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrElse {
            Log.w(TAG, "调起安装页失败: ${it.message}")
            false
        }
    }

    private companion object {
        const val TAG = "AppUpdater"
        const val APK_FILE_NAME = "asoul-update.apk"
    }
}
