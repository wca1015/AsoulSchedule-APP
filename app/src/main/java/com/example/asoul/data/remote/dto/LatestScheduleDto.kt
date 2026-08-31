package com.example.asoul.data.remote.dto

import com.example.asoul.data.model.GroupType
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.MemberCatalog
import com.example.asoul.data.model.ScheduleSource
import com.example.asoul.data.model.StreamFormat
import com.example.asoul.data.model.Weeks
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

/**
 * 周程表静态数据源 `/latest.json` 的 DTO（P6）。
 *
 * 契约要点：
 * - [version] 每次发布递增，客户端据此判断是否有更新
 * - [days] 按日分组的日程条目
 * - member 枚举：bella / jiaran / nailin / xinyi / sinuo / unknown
 * - tag 枚举：live / show / special / rest（rest 行不入日程）
 * - [EventDto.recordingBvid]：直播结束后服务端录播管道回填的 B 站回放 BV 号（可选）
 * - [EventDto.groupType]：团播分组 none / asoul / xinyi_sinuo / zhijiang_variety（可选，缺省 none）
 * - [EventDto.format]：直播形式 normal / theater / night_talk / game_room / collab / commercial（可选，缺省 normal）
 */
@Serializable
data class LatestScheduleDto(
    val version: Long,
    @SerialName("week_start")
    val weekStart: String,
    @SerialName("week_end")
    val weekEnd: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    /** auto / manual。 */
    val source: String? = null,
    val days: List<DayDto> = emptyList(),
) {
    /** 解析 [weekStart]；格式异常时返回 null。 */
    fun parsedWeekStart(): LocalDate? = runCatching { LocalDate.parse(weekStart) }.getOrNull()

    /**
     * week_start 合理性校验（客户端防火墙）：拒绝年份异常的数据。
     *
     * 周程表海报通常只印月日不印年份，服务端 VLM 识别可能把年份认错
     * （如 2026 认成 2023）。年份错误的数据一旦被应用，日程会落到
     * 用户划不到、也看不见的周，表现为「数据正确但显示不了」。
     * 服务端 validate.py 有等价校验，此处为客户端防御层。
     *
     * 合理窗口：[now - 8 周, now + 2 周]（覆盖可回看 4 周 + 提前发布 1~2 周）。
     */
    fun isWeekStartReasonable(now: LocalDate = Weeks.today()): Boolean {
        val weekStart = parsedWeekStart() ?: return false
        return weekStart in now.minusWeeks(8)..now.plusWeeks(2)
    }
}

/** 单日日程组。 */
@Serializable
data class DayDto(
    val date: String,
    val weekday: String? = null,
    val events: List<EventDto> = emptyList(),
)

/** 单条日程。 */
@Serializable
data class EventDto(
    /** HH:mm。 */
    val time: String,
    /** bella / jiaran / nailin / xinyi / sinuo / unknown。 */
    val member: String,
    val title: String,
    val desc: String? = null,
    /** live / show / special / rest。 */
    val tag: String,
    /** 直播录像 BV 号：服务端录播管道在直播结束后回填，未上传时为 null。 */
    @SerialName("recording_bvid")
    val recordingBvid: String? = null,
    /** 团播分组：none / asoul / xinyi_sinuo / zhijiang_variety；缺省/未知按 none。 */
    @SerialName("group_type")
    val groupType: String? = null,
    /** 直播形式：normal / theater / night_talk / game_room / collab / commercial；缺省/未知按 normal。 */
    val format: String? = null,
)

/** 服务端 group_type 枚举 → [GroupType]（unknown/缺省回退 NONE）。 */
private val GROUP_TYPE_KEYS = mapOf(
    "asoul" to GroupType.ASOUL,
    "xinyi_sinuo" to GroupType.XINYI_SINUO,
    "zhijiang_variety" to GroupType.ZHIJIANG_VARIETY,
)

/** 服务端 format 枚举 → [StreamFormat]（unknown/缺省回退 NORMAL）。 */
private val FORMAT_KEYS = mapOf(
    "theater" to StreamFormat.THEATER,
    "night_talk" to StreamFormat.NIGHT_TALK,
    "game_room" to StreamFormat.GAME_ROOM,
    "collab" to StreamFormat.COLLAB,
    "commercial" to StreamFormat.COMMERCIAL,
)

/**
 * DTO → 领域模型映射：
 * - `tag = rest` 的行不入日程（跳过）
 * - member key 经 [MemberCatalog.memberIdFromServerKey] 映射到内置成员库
 * - 来源统一标记 [ScheduleSource.API]
 * - groupType / format 由服务端新增字段映射（缺省 NONE / NORMAL，向后兼容旧格式）
 * - 团播条目的 memberName 使用分组标签（如「Asoul团播」），与 Mock 数据展示一致；
 *   成员过滤时由 `matches` 经 [MemberCatalog.participantsOf] 展开参与成员，不受影响。
 */
fun LatestScheduleDto.toSchedules(): List<LiveSchedule> = days.flatMap { day ->
    val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@flatMap emptyList()
    day.events.mapNotNull { event -> event.toLiveSchedule(date) }
}

private fun EventDto.toLiveSchedule(date: LocalDate): LiveSchedule? {
    // rest 行不入日程
    if (tag == "rest") return null
    val time = runCatching { LocalTime.parse(time) }.getOrNull() ?: return null
    val memberId = MemberCatalog.memberIdFromServerKey(member)
    val groupType = groupType?.lowercase()?.trim()?.let(GROUP_TYPE_KEYS::get) ?: GroupType.NONE
    val format = format?.lowercase()?.trim()?.let(FORMAT_KEYS::get) ?: StreamFormat.NORMAL
    // 团播条目：副标题用分组标签；其余：成员名，unknown 回退标题/「未知成员」
    val displayName = when {
        groupType != GroupType.NONE -> groupType.label
        else -> memberId?.let { id -> MemberCatalog.ALL.firstOrNull { it.id == id }?.name }
            ?: title.ifBlank { "未知成员" }
    }
    return LiveSchedule(
        date = date,
        time = time,
        memberName = displayName,
        memberId = if (groupType != GroupType.NONE) null else memberId,
        title = title,
        groupType = groupType,
        format = format,
        source = ScheduleSource.API,
        recordingBvid = recordingBvid?.takeIf { it.isNotBlank() },
    )
}
