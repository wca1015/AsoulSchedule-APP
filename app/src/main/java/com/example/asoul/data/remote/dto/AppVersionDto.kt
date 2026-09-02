package com.example.asoul.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * App 版本清单 `/app_version.json` 的 DTO（更新检查）。
 *
 * 由服务端 `Server/scripts/upload_app.py` 在发布新 APK 时生成并上传 OSS，
 * 客户端启动时拉取并与本地 `BuildConfig.VERSION_CODE` 比对。
 */
@Serializable
data class AppVersionDto(
    /** 服务端最新版本号（与 APK 的 versionCode 一致）。 */
    @SerialName("version_code")
    val versionCode: Int,
    /** 服务端最新版本名（如 "1.3"），用于弹窗标题展示。 */
    @SerialName("version_name")
    val versionName: String = "",
    /** APK 下载地址（OSS 公共读，完整 URL）。 */
    @SerialName("apk_url")
    val apkUrl: String = "",
    /** 更新说明（多行文本，`\n` 分隔）。 */
    val notes: String = "",
    /** 发布时间（ISO 时间字符串，仅展示用）。 */
    @SerialName("publish_time")
    val publishTime: String? = null,
)
