package com.example.asoul.data.model

import java.time.LocalDateTime

/** 突击直播状态（服务端下发）。 */
enum class FlashStatus(val label: String) {
    /** 待开播。 */
    UPCOMING("待开播"),

    /** 直播中。 */
    LIVE("直播中"),

    /** 已结束（默认不展示）。 */
    ENDED("已结束");

    companion object {
        /** 服务端枚举：upcoming / live / ended；未知值按待开播处理。 */
        fun fromRaw(raw: String?): FlashStatus = when (raw?.lowercase()) {
            "live" -> LIVE
            "ended" -> ENDED
            else -> UPCOMING
        }
    }
}

/**
 * P10 突击直播领域模型（由 `/flash.json` 映射而来）。
 *
 * @param id 服务端唯一 id（形如 `flash_20260823_jiaran_1900`）
 * @param member 匹配到内置成员库时填充；`unknown` 成员为 null
 * @param startTime 开播时间（已转换为系统时区的本地时间）
 * @param endTime 下播时间，未下播为 null
 * @param sourceUrl B 站动态/直播源链接（卡片点击跳转）
 * @param status 状态（upcoming / live / ended）
 * @param autoPublished 是否为自动发布（true 时 UI 需展示「⚠️待确认」徽标）
 */
data class FlashLiveEvent(
    val id: String,
    val member: Member?,
    val title: String,
    val desc: String? = null,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val sourceUrl: String? = null,
    val status: FlashStatus = FlashStatus.UPCOMING,
    val autoPublished: Boolean = false,
)
