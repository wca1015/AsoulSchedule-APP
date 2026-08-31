package com.example.asoul.data.model

import androidx.annotation.DrawableRes
import com.example.asoul.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** 日程来源，对应方案中「来源标签」：周程表识别 / API 抓取 / 手动添加。 */
enum class ScheduleSource(val label: String) {
    MANUAL("手动添加"),
    OCR("周程表识别"),
    API("API抓取"),
}

/**
 * 团播分组。
 *
 * 团播分为三大类：
 * - [ASOUL]：Asoul 团播（一期），下分 小剧场 / 夜谈 / 游戏室
 * - [XINYI_SINUO]：心宜思诺团播（二期）
 * - [ZHIJIANG_VARIETY]：枝江综艺（一期 + 二期共同参与的团播）
 */
enum class GroupType(val label: String, @DrawableRes val avatarRes: Int? = null) {
    /** 非团播（单人直播）。 */
    NONE(""),
    /** Asoul 团播。 */
    ASOUL("Asoul团播", R.drawable.avatar_group_asoul),
    /** 心宜 & 思诺 团播。 */
    XINYI_SINUO("心宜思诺团播", R.drawable.avatar_group_xinyi_sinuo),
    /** 枝江综艺：一期 + 二期共同参与的团播企划（暂无专属头像，用枝江 Logo 兜底）。 */
    ZHIJIANG_VARIETY("枝江综艺", R.drawable.logo_zhijiang),
}

/**
 * 直播形式/性质标签。
 *
 * 用于在日程条目上以标签呈现：小剧场、夜谈、游戏室、联动、工商直播等。
 */
enum class StreamFormat(val label: String, val emoji: String) {
    /** 普通直播，不额外打标签。 */
    NORMAL("直播", ""),
    /** Asoul 小剧场。 */
    THEATER("小剧场", "\uD83C\uDFAD"),
    /** Asoul 夜谈。 */
    NIGHT_TALK("夜谈", "\uD83C\uDF19"),
    /** Asoul 游戏室。 */
    GAME_ROOM("游戏室", "\uD83C\uDFAE"),
    /** 联动直播。 */
    COLLAB("联动", "\uD83E\uDD1D"),
    /** 工商直播（商务合作场）。 */
    COMMERCIAL("工商直播", "\uD83D\uDCBC"),
}

/**
 * 一条直播日程。
 *
 * 与产品方案中的 [LiveSchedule] 结构一致，额外携带来源与置信度，
 * 供 UI 展示「来源标签」和识别置信度。团播类型与直播形式以标签呈现。
 */
data class LiveSchedule(
    val id: Long = 0L,
    val date: LocalDate,
    val time: LocalTime,
    val memberName: String,
    /** 匹配到内置成员库时填充成员 id，否则为 null（未识别成员）。 */
    val memberId: String? = null,
    val title: String,
    /** 团播分组：NONE / Asoul团播 / 心宜思诺团播 / 枝江综艺。 */
    val groupType: GroupType = GroupType.NONE,
    /** 直播形式标签：小剧场 / 夜谈 / 游戏室 / 联动 / 工商直播。 */
    val format: StreamFormat = StreamFormat.NORMAL,
    /** 识别置信度 0~1，手动添加恒为 1f。 */
    val confidence: Float = 1f,
    val source: ScheduleSource = ScheduleSource.MANUAL,
    /** 写入系统日历后记录的 event id，便于后续删除/更新。 */
    val calendarEventId: Long? = null,
    /** 直播录像 BV 号：往日已结束的直播在录播上传后填充，供「录像」标签跳转。 */
    val recordingBvid: String? = null,
) {
    /** 兼容旧逻辑：是否为团播。 */
    val isGroupLive: Boolean get() = groupType != GroupType.NONE

    /** 需要以标签呈现的「形式/性质」标签；普通直播返回 null。 */
    val formatTag: StreamFormat? get() = format.takeIf { it != StreamFormat.NORMAL }

    /** 在指定成员库中解析出成员对象；未匹配到则返回 null。 */
    fun member(catalog: List<Member> = MemberCatalog.ALL): Member? =
        memberId?.let { id -> catalog.firstOrNull { it.id == id } }
}

/** 直播是否已结束（往日日程用于判断是否展示「录像」入口）。 */
fun LiveSchedule.isEnded(now: LocalDateTime = Weeks.now()): Boolean =
    date.atTime(time).isBefore(now)

/** 周程表状态：本周周程表是否已识别。 */
data class WeekScheduleStatus(
    val weekStart: LocalDate,
    val recognized: Boolean,
    val recognizedAt: Long? = null,
    val entryCount: Int = 0,
)

/** 以周一为一周起点的工具函数。 */
object Weeks {

    /**
     * 全部日期 / 周界计算的固定时区：北京时间（Asia/Shanghai）。
     *
     * 服务端周程表（week_start / 各日 date）均按北京时间产出；若使用设备系统时区，
     * 其他时区的设备会与服务端对「本周」的边界判定不一致（例如北京时间周一凌晨，
     * 美洲仍是周日），导致拉取到的数据落到客户端判定的「错误的周」，表现为显示空白或错周。
     */
    val APP_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")

    /** 北京时间的「今天」（周界判定 / 日期展示统一使用）。 */
    fun today(): LocalDate = LocalDate.now(APP_ZONE)

    /** 北京时间的当前日期时间（直播是否结束等判定统一使用）。 */
    fun now(): LocalDateTime = LocalDateTime.now(APP_ZONE)

    private val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    fun startOfWeek(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun daysOfWeek(weekStart: LocalDate): List<LocalDate> =
        (0L..6L).map { weekStart.plusDays(it) }

    /** 格式化为「周X」，例如「周一」。 */
    fun weekdayLabel(date: LocalDate): String = WEEKDAY_NAMES[date.dayOfWeek.value - 1]

    /** 格式化为「周X M/D」，例如「周一 8/18」，供日程条目展示具体日期。 */
    fun shortLabel(date: LocalDate): String =
        "${weekdayLabel(date)} ${date.monthValue}/${date.dayOfMonth}"
}
