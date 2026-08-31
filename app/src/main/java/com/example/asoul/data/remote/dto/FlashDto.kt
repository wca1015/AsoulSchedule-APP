package com.example.asoul.data.remote.dto

import com.example.asoul.data.model.FlashLiveEvent
import com.example.asoul.data.model.FlashStatus
import com.example.asoul.data.model.MemberCatalog
import com.example.asoul.data.model.Weeks
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * 突击直播静态数据源 `/flash.json` 的 DTO（P6）。
 *
 * [version] 每次发布递增，客户端据此判断是否有更新。
 */
@Serializable
data class FlashDto(
    val version: Long,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val events: List<FlashEventDto> = emptyList(),
)

/** 单条突击直播 DTO。 */
@Serializable
data class FlashEventDto(
    val id: String,
    /** bella / jiaran / nailin / xinyi / sinuo / unknown。 */
    val member: String? = null,
    val title: String,
    val desc: String? = null,
    /** ISO8601 带时区，如 `2026-08-23T19:00:00+08:00`。 */
    @SerialName("start_time")
    val startTime: String? = null,
    @SerialName("end_time")
    val endTime: String? = null,
    @SerialName("source_dynamic_id")
    val sourceDynamicId: String? = null,
    @SerialName("source_url")
    val sourceUrl: String? = null,
    /** upcoming / live / ended。 */
    val status: String? = null,
    @SerialName("auto_published")
    val autoPublished: Boolean = false,
    @SerialName("recognized_at")
    val recognizedAt: String? = null,
)

/** ISO8601 带偏移时间解析器（`+08:00` 与 `Z` 均支持）。 */
private val OFFSET_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

/**
 * DTO → 领域模型映射：
 * - member key 经 [MemberCatalog.memberIdFromServerKey] 映射；未知成员为 null
 * - start/end 时间由带时区的 ISO8601 转为系统时区的本地时间
 */
fun FlashDto.toFlashEvents(): List<FlashLiveEvent> = events.mapNotNull { it.toFlashLiveEvent() }

private fun FlashEventDto.toFlashLiveEvent(): FlashLiveEvent? {
    val start = startTime?.let(::parseOffsetDateTime) ?: return null
    return FlashLiveEvent(
        id = id,
        member = MemberCatalog.memberIdFromServerKey(member)
            ?.let { memberId -> MemberCatalog.ALL.firstOrNull { it.id == memberId } },
        title = title,
        desc = desc,
        startTime = start,
        endTime = endTime?.let(::parseOffsetDateTime),
        sourceUrl = sourceUrl,
        status = FlashStatus.fromRaw(status),
        autoPublished = autoPublished,
    )
}

/**
 * 将带时区的 ISO8601 字符串转为北京时间（[Weeks.APP_ZONE]）的 [LocalDateTime]。
 *
 * 服务端时间均为北京时间；若按设备系统时区转换，其他时区设备的展示时间会偏移，
 * 且与「今天」的判定（Weeks.today() 使用北京时间）不一致，故统一按北京时间落地。
 */
private fun parseOffsetDateTime(raw: String): LocalDateTime =
    OffsetDateTime.parse(raw, OFFSET_FORMATTER)
        .atZoneSameInstant(Weeks.APP_ZONE)
        .toLocalDateTime()
