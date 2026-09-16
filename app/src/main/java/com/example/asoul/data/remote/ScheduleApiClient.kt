package com.example.asoul.data.remote

import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * P6 网络层：OkHttp GET 封装（仅两个静态 JSON 端点，无需 Retrofit）。
 *
 * - 超时 10s（连接 / 读 / 写）
 * - 异常静默：任何失败返回 null 并记 [Log.w]，由上层走缓存/兜底策略
 */
class ScheduleApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /** 拉取周程表 JSON 原文；失败返回 null。 */
    suspend fun fetchLatest(): String? = get(ApiEndpoints.LATEST_JSON)

    /** 拉取突击直播 JSON 原文；失败返回 null。 */
    suspend fun fetchFlash(): String? = get(ApiEndpoints.FLASH_JSON)

    /**
     * 拉取指定往日周的周程表 JSON 原文；文件未发布（404）/ 失败均返回 null。
     */
    suspend fun fetchWeek(weekUrl: String): String? = get(weekUrl)

    /** 拉取 App 版本清单 JSON 原文；失败返回 null。 */
    suspend fun fetchAppVersion(): String? = get(ApiEndpoints.APP_VERSION_JSON)

    /**
     * 拉取 asoul.love 日历订阅（ICS 文本）；失败返回 null。
     *
     * 必须携带浏览器 UA：该站点由 Cloudflare 防护，非浏览器 UA 会被 403 挑战拦截。
     * 调用方（[com.example.asoul.data.AsoulLoveRepository]）负责 1 小时拉取间隔。
     */
    suspend fun fetchAsoulLoveIcs(): String? =
        get(ApiEndpoints.ASOUL_LOVE_ICS, userAgent = BROWSER_UA)

    /** GET 请求，挂起等待响应；非 2xx / IO 异常均返回 null。 */
    private suspend fun get(url: String, userAgent: String? = null): String? =
        suspendCancellableCoroutine { cont ->
            val request = Request.Builder().url(url).apply {
                if (userAgent != null) header("User-Agent", userAgent)
            }.build()
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val body = response.use { resp ->
                            if (resp.isSuccessful) resp.body.string() else null
                        }
                        if (body == null) {
                            Log.w(TAG, "GET $url 失败: HTTP ${response.code}")
                        }
                        cont.resume(body)
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        if (cont.isCancelled) return
                        Log.w(TAG, "GET $url 异常: ${e.message}")
                        cont.resume(null)
                    }
                },
            )
        }

    private companion object {
        const val TAG = "ScheduleApiClient"

        /** 浏览器 UA（访问 asoul.love 必需，否则被 Cloudflare 403）。 */
        const val BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}
