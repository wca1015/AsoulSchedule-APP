package com.example.asoul.data.remote

import java.time.LocalDate

/**
 * P6 静态数据源端点：服务端产出的只读 JSON（托管于 CDN，客户端只读）。
 *
 * - [LATEST_JSON]：周程表，每小时轮询 + 启动拉取
 * - [FLASH_JSON]：突击直播，每 5 分钟轮询 + App 回前台立即拉取
 */
object ApiEndpoints {

    /** 数据源根地址：阿里云 OSS（对象级公共读，由服务端 sync_oss.py 每轮镜像）。 */
    const val BASE_URL = "https://asoul-oss.oss-cn-hangzhou.aliyuncs.com/"

    /** 周程表端点（version 每次发布递增，客户端据此判断是否有更新）。 */
    const val LATEST_JSON = BASE_URL + "latest.json"

    /** 突击直播端点。 */
    const val FLASH_JSON = BASE_URL + "flash.json"

    /**
     * 往日周周程表端点：`week/{week_start}.json`。
     *
     * 服务端将 archive 归档镜像到 OSS（含录播回填），客户端按需拉取，
     * 文件未发布时返回 404（调用方静默处理，不影响现有数据）。
     */
    fun weekUrl(weekStart: LocalDate): String = BASE_URL + "week/$weekStart.json"
}
