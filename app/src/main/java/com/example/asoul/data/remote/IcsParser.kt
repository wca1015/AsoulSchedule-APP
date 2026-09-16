package com.example.asoul.data.remote

import com.example.asoul.data.model.GroupType
import com.example.asoul.data.model.LiveCategory
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.MemberCatalog
import com.example.asoul.data.model.ScheduleSource
import com.example.asoul.data.model.Weeks
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** ICS 单条事件（原始字段，尚未映射到领域模型）。 */
data class IcsEvent(
    /** 事件唯一 id，形如 `20260916-1200-bella@asoul.love`。 */
    val uid: String,
    /** 摘要，形如 `【突击】贝拉突击: 【突击】一起看假面骑士ZZZ`。 */
    val summary: String,
    /** 描述：`类别 | 成员列表` + 直播间 / 动态链接（\n 已反转义）。 */
    val description: String,
    /** 事件链接（直播间地址）。 */
    val url: String?,
    /** 开播时间（已由 UTC 换算为北京时间）。 */
    val start: LocalDateTime,
)

/**
 * asoul.love 日历订阅（`calendar.ics`）解析器。
 *
 * 纯 Kotlin 实现（无 Android 依赖），可直接在 JVM 单测中验证。支持：
 * - 75 字节折叠行（续行以空格/Tab 开头）还原
 * - `\n` `\,` `\;` `\\` 转义反转义
 * - `DTSTART` 两种形态：UTC（`20260916T040000Z`，换算为北京时间）与本时区（无后缀）
 * - 我方关心的字段：UID / SUMMARY / DESCRIPTION / URL / DTSTART
 *
 * 摘要与描述由 [toLiveSchedule] 映射为 App 领域模型：
 * - 摘要前缀 `【日常】/【节目】/【突击】/【2D】` → [LiveCategory]（新类型标签体系）
 * - 描述首行 `类别 | 贝拉 嘉然 乃琳` → 参与成员；缺失时回退 UID 尾部的成员键
 * - 描述中的 `.../opus/{id}` 链接 → [LiveSchedule.sourceUrl]（详情弹窗可跳转源动态）
 */
object IcsParser {

    private val ICS_LOCAL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    /** ICS 文本转义：`\n` `\N` `\,` `\;` `\\`。 */
    private val ESCAPE_RE = Regex("""\\([nN,;\\])""")

    /** 解析 ICS 文本为事件列表；无有效事件返回空列表。 */
    fun parse(text: String): List<IcsEvent> {
        val result = mutableListOf<IcsEvent>()
        var inEvent = false
        var uid: String? = null
        var summary: String? = null
        var description: String? = null
        var url: String? = null
        var start: LocalDateTime? = null

        for (line in unfold(text)) {
            if (line.equals("BEGIN:VEVENT", ignoreCase = true)) {
                inEvent = true
                uid = null
                summary = null
                description = null
                url = null
                start = null
                continue
            }
            if (line.equals("END:VEVENT", ignoreCase = true)) {
                if (inEvent && start != null && summary != null) {
                    result += IcsEvent(
                        uid = uid.orEmpty(),
                        summary = summary,
                        description = description.orEmpty(),
                        url = url,
                        start = start,
                    )
                }
                inEvent = false
                continue
            }
            if (!inEvent) continue
            val sep = line.indexOf(':')
            if (sep <= 0) continue
            val key = line.substring(0, sep).substringBefore(';').trim().uppercase()
            val value = unescape(line.substring(sep + 1))
            when (key) {
                "UID" -> uid = value
                "SUMMARY" -> summary = value
                "DESCRIPTION" -> description = value
                "URL" -> url = value
                "DTSTART" -> start = parseStart(value)
            }
        }
        return result
    }

    /** 解析并映射为 App 日程列表（跳过无法识别的条目）。 */
    fun parseSchedules(text: String): List<LiveSchedule> =
        parse(text).mapNotNull { it.toLiveSchedule() }

    /** 还原折叠行（续行以空格 / Tab 开头）。 */
    private fun unfold(raw: String): List<String> {
        val lines = mutableListOf<String>()
        for (rawLine in raw.split('\n')) {
            val line = rawLine.trimEnd('\r')
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (lines.isNotEmpty()) lines[lines.size - 1] += line.substring(1)
            } else {
                lines += line
            }
        }
        return lines
    }

    private fun unescape(value: String): String = ESCAPE_RE.replace(value) { match ->
        when (match.groupValues[1]) {
            "n", "N" -> "\n"
            else -> match.groupValues[1]
        }
    }

    /** 解析 `DTSTART`：UTC（带 Z）换算为北京时间，其余按字面本地时间。 */
    private fun parseStart(value: String): LocalDateTime? {
        val v = value.trim()
        return runCatching {
            when {
                v.endsWith("Z") -> LocalDateTime
                    .parse(v.dropLast(1), ICS_LOCAL)
                    .toInstant(ZoneOffset.UTC)
                    .atZone(Weeks.APP_ZONE)
                    .toLocalDateTime()
                v.length == 15 -> LocalDateTime.parse(v, ICS_LOCAL)
                else -> null
            }
        }.getOrNull()
    }
}

// ===== ICS → 领域模型映射（文件级常量供 object 与扩展函数共用） =====

/** 摘要前缀：`【类型】` + 其余正文。 */
private val SUMMARY_PREFIX_RE = Regex("""^【([^】]{1,6})】\s*(.*)$""")

/** 嵌套前缀（如 `【突击】标题` 中的二次前缀），展示前剥离。 */
private val INNER_PREFIX_RE = Regex("""^【[^】]{1,6}】\s*""")

/** 摘要 `名称: 标题` 形态的冒号切分（兼容全角）。 */
private val COLON_RE = Regex("""^([^:：]{1,20})[:：]\s*(.+)$""")

private const val OPUS_MARKER = "/opus/"

/** asoul.love 使用的成员键 → App 内置成员 id。 */
private val MEMBER_KEYS = mapOf(
    "bella" to "bella",
    "diana" to "diana",
    "eileen" to "eileen",
    "fiona" to "xinyi",
    "gladys" to "sinuo",
)

/** 描述中的中文成员名 → App 内置成员 id。 */
private val MEMBER_NAMES = mapOf(
    "贝拉" to "bella",
    "嘉然" to "diana",
    "乃琳" to "eileen",
    "心宜" to "xinyi",
    "思诺" to "sinuo",
)

/** ICS 事件 → App 日程（类型标签 / 参与成员 / 来源链接全部就位）。 */
fun IcsEvent.toLiveSchedule(): LiveSchedule? {
    val prefix = SUMMARY_PREFIX_RE.find(summary.trim()) ?: return null
    val category = LiveCategory.fromIcsLabel(prefix.groupValues[1])
    val body = prefix.groupValues[2].trim()

    // 摘要两种形态：
    //   "思诺直播"                   → 无独立标题，直接用整段
    //   "乃琳直播: 国风小女子来也！"   → 冒号后为标题
    val colon = COLON_RE.find(body)
    val title = (colon?.groupValues?.get(2) ?: body)
        .replace(INNER_PREFIX_RE, "")
        .trim()
        .ifBlank { body }

    // 参与成员：优先描述首行「类别 | 成员列表」，缺失时回退 UID 尾部的成员键
    val descFirstLine = description.lineSequence().firstOrNull().orEmpty()
    val names = descFirstLine.substringAfter('|', "").trim().split(Regex("\\s+"))
    val idsFromNames = names.mapNotNull { MEMBER_NAMES[it] }
    val uidKey = uid.substringBefore('@').substringAfterLast('-').lowercase()
    val memberIds = (idsFromNames.ifEmpty { listOfNotNull(MEMBER_KEYS[uidKey]) }).distinct()
    val members = memberIds.mapNotNull { id -> MemberCatalog.ALL.firstOrNull { it.id == id } }

    // 团播分组推断（对齐服务端语义）：一期三人 / 心宜思诺 / 全员
    val groupType = when {
        memberIds.toSet() == setOf("bella", "diana", "eileen") -> GroupType.ASOUL
        memberIds.toSet() == setOf("xinyi", "sinuo") -> GroupType.XINYI_SINUO
        memberIds.size >= 5 -> GroupType.ZHIJIANG_VARIETY
        else -> GroupType.NONE
    }

    val displayName = when {
        groupType != GroupType.NONE -> groupType.label
        members.size > 1 -> members.joinToString(" & ") { it.name }
        members.size == 1 -> members.first().name
        else -> title
    }

    val sourceUrl = Regex("""https?://\S+""").findAll(description)
        .map { it.value }
        .firstOrNull { it.contains(OPUS_MARKER) }

    return LiveSchedule(
        date = start.toLocalDate(),
        time = start.toLocalTime(),
        memberName = displayName,
        memberId = members.singleOrNull()?.id,
        title = title,
        groupType = groupType,
        participantIds = if (memberIds.size > 1) memberIds else emptyList(),
        category = category,
        source = ScheduleSource.ASOUL_LOVE,
        sourceUrl = sourceUrl,
    )
}
