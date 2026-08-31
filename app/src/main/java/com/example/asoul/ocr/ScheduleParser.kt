package com.example.asoul.ocr

import com.example.asoul.data.model.GroupType
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.Member
import com.example.asoul.data.model.MemberCatalog
import com.example.asoul.data.model.ScheduleSource
import com.example.asoul.data.model.StreamFormat
import java.time.LocalDate
import java.time.LocalTime

/**
 * 周程表语义解析引擎。
 *
 * Pipeline 第 ④ 步：OCR 文本块 -> List<LiveSchedule>。
 *
 * 识别容错设计（来自产品方案）：
 * - 成员昵称变体（"贝贝"/"Bella"/"贝拉Bella"）→ 别名映射模糊匹配
 * - 日期格式不统一（"8.18"/"8月18日"/"周一"）→ 多格式正则 + 相对日期推算
 * - 时间格式归一（"20:00"/"20：00"）
 */
object ScheduleParser {

    // ===== 正则定义 =====

    /** 时间：20:00 / 20：00 / 8:30，全角冒号归一化。 */
    private val TIME_REGEX = Regex("""(\d{1,2})\s*[:：]\s*(\d{2})""")

    /** 中文星期：周一~周日/周天，星期一~星期天，礼拜一~礼拜天。 */
    private val WEEKDAY_REGEX = Regex("""(周|星期|礼拜)([一二三四五六日天])""")

    /** 月日：8/18、8.18、8-18、8月18日。 */
    private val MONTH_DAY_REGEX = Regex("""(\d{1,2})\s*[/.\-月]\s*(\d{1,2})\s*日?""")

    // ===== 对外入口 =====

    /**
     * 解析 OCR 文本块为日程列表。
     *
     * @param blocks OCR 输出
     * @param weekStart 周程表所属周的周一（用于「周一」等相对日期推算）
     */
    fun parse(blocks: List<TextBlock>, weekStart: LocalDate): List<LiveSchedule> =
        blocks.mapNotNull { block -> parseLine(block.text.trim(), block.confidence, weekStart) }

    /** 解析单行文本，例如「周一 8/18 20:00 嘉然 歌回」。 */
    fun parseLine(line: String, confidence: Float, weekStart: LocalDate): LiveSchedule? {
        if (line.isBlank()) return null
        // 跳过表头行
        if (line.contains("日期") && line.contains("时间")) return null
        // 显式休息行
        if (isRestRow(line)) return null

        val time = parseTime(line) ?: return null
        val date = parseDate(line, weekStart) ?: return null
        val groupType = detectGroupType(line)
        val format = detectFormat(line)

        return if (groupType != GroupType.NONE) {
            // 团播行：不匹配个人成员，归属到团播分组
            val title = extractTitle(line, member = null, isGroup = true)
            LiveSchedule(
                date = date,
                time = time,
                memberName = groupType.label,
                memberId = null,
                title = title.ifBlank { "直播" },
                groupType = groupType,
                format = format,
                confidence = confidence * 0.85f,
                source = ScheduleSource.OCR,
            )
        } else {
            val matched = matchMember(line)
            // 标题：剥离日期/时间/成员后剩余的文本
            val title = extractTitle(line, member = matched?.first, isGroup = false)
            LiveSchedule(
                date = date,
                time = time,
                memberName = matched?.first?.name ?: extractMemberFallback(line),
                memberId = matched?.first?.id,
                title = title.ifBlank { "直播" },
                format = format,
                confidence = confidence * (matched?.second ?: 0.8f),
                source = ScheduleSource.OCR,
            )
        }
    }

    // ===== 时间解析 =====

    /** "20:00" / "8：30" → LocalTime。 */
    fun parseTime(text: String): LocalTime? {
        val match = TIME_REGEX.find(text) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }

    // ===== 日期解析 =====

    /**
     * 多格式日期解析 + 基于当前周的相对推算：
     * - "周一 8/18" / "8.18" / "8月18日" → 绝对日期
     * - 仅有 "周一" → weekStart + 偏移
     */
    fun parseDate(text: String, weekStart: LocalDate): LocalDate? {
        // 优先取绝对日期 8/18
        MONTH_DAY_REGEX.find(text)?.let { match ->
            val month = match.groupValues[1].toIntOrNull() ?: return null
            val day = match.groupValues[2].toIntOrNull() ?: return null
            if (month in 1..12 && day in 1..31) {
                return try {
                    LocalDate.of(weekStart.year, month, day)
                } catch (_: Exception) {
                    null
                }
            }
        }
        // 相对日期：周一~周日
        WEEKDAY_REGEX.find(text)?.let { match ->
            val offset = weekdayOffset(match.groupValues[2]) ?: return null
            return weekStart.plusDays(offset.toLong())
        }
        return null
    }

    private fun weekdayOffset(char: String): Int? = when (char) {
        "一" -> 0
        "二" -> 1
        "三" -> 2
        "四" -> 3
        "五" -> 4
        "六" -> 5
        "日", "天" -> 6
        else -> null
    }

    // ===== 成员匹配 =====

    private data class Candidate(val member: Member, val conf: Float, val aliasLen: Int)

    /**
     * 别名模糊匹配：在内置成员库中查找别名出现在行内的成员。
     * 置信度随别名长度提升；同分取更长别名。
     */
    fun matchMember(line: String): Pair<Member, Float>? {
        var best: Candidate? = null
        for (member in MemberCatalog.ALL) {
            for (alias in member.aliases) {
                if (alias.isBlank() || !line.contains(alias)) continue
                val conf = 0.7f + 0.3f * (alias.length.coerceAtMost(6) / 6f)
                val current = best
                if (current == null ||
                    conf > current.conf ||
                    (conf == current.conf && alias.length > current.aliasLen)
                ) {
                    best = Candidate(member, conf, alias.length)
                }
            }
        }
        return best?.let { it.member to it.conf }
    }

    // ===== 团播分组与直播形式识别 =====

    /**
     * 团播分组识别：
     * - 含「枝江综艺」的团播行 → [GroupType.ZHIJIANG_VARIETY]（一期 + 二期共同参与，
     *   行内可能同时出现多位成员名字，因此优先于个人成员判断）
     * - 含「心宜」「思诺」的团播行 → [GroupType.XINYI_SINUO]（心宜思诺团播）
     * - 其余团播行 → [GroupType.ASOUL]（Asoul 团播）
     */
    fun detectGroupType(line: String): GroupType {
        // 枝江综艺优先识别（一期 + 二期共同企划）
        if (line.contains("枝江综艺") || (line.contains("枝江") && line.contains("综艺"))) {
            return GroupType.ZHIJIANG_VARIETY
        }
        val isGroupLine = MemberCatalog.GROUP.aliases.any {
            it.isNotBlank() && line.contains(it, ignoreCase = true)
        }
        if (!isGroupLine) return GroupType.NONE
        return if (line.contains("心宜") || line.contains("思诺")) {
            GroupType.XINYI_SINUO
        } else {
            GroupType.ASOUL
        }
    }

    /**
     * 直播形式标签识别（联动 / 工商直播对团播与单人直播均适用；
     * 小剧场 / 夜谈 / 游戏室主要对应团播企划）。
     */
    fun detectFormat(line: String): StreamFormat = when {
        line.contains("小剧场") || line.contains("剧场") -> StreamFormat.THEATER
        line.contains("夜谈") -> StreamFormat.NIGHT_TALK
        line.contains("游戏室") -> StreamFormat.GAME_ROOM
        line.contains("联动") || line.contains("连麦") -> StreamFormat.COLLAB
        line.contains("工商") || line.contains("商务") -> StreamFormat.COMMERCIAL
        else -> StreamFormat.NORMAL
    }

    // ===== 辅助 =====

    /** 团播行标题剥离用的关键词。 */
    private val GROUP_KEYWORDS = listOf(
        "团播", "心宜思诺", "心宜", "思诺", "A-SOUL", "ASOUL", "全员", "枝江综艺", "枝江",
        "综艺", "特别企划",
    )

    /** 直播形式关键词（从标题中剥离，避免与标签重复）。 */
    private val FORMAT_KEYWORDS = listOf(
        "小剧场", "剧场", "夜谈", "游戏室", "联动", "连麦", "工商直播", "工商", "商务",
    )

    private fun isRestRow(line: String): Boolean {
        val stripped = line.replace(WEEKDAY_REGEX, "")
            .replace(MONTH_DAY_REGEX, "")
            .replace(TIME_REGEX, "")
            .replace(Regex("""[\s—\-·|]"""), "")
        return stripped.isEmpty() || stripped == "休息" || stripped == "休" || stripped == "无"
    }

    private fun extractMemberFallback(line: String): String {
        // 未匹配到成员库时，取第一个非日期/非时间词作为成员名（尽力而为）
        val tokens = line.split(Regex("""\s+"""))
        return tokens.firstOrNull { token ->
            !TIME_REGEX.matches(token) &&
                !WEEKDAY_REGEX.containsMatchIn(token) &&
                !MONTH_DAY_REGEX.matches(token) &&
                token != "—" && token != "-"
        } ?: "未知成员"
    }

    private fun extractTitle(line: String, member: Member?, isGroup: Boolean): String {
        var rest = line
        // 依次剥离 日期、时间
        rest = MONTH_DAY_REGEX.replace(rest, "")
        rest = WEEKDAY_REGEX.replace(rest, "")
        rest = TIME_REGEX.replace(rest, "")
        if (isGroup) {
            // 团播行：剥离团播分组关键词（如「团播」「心宜思诺」「Asoul」）
            GROUP_KEYWORDS.forEach { kw -> rest = rest.replace(kw, "", ignoreCase = true) }
        } else {
            // 再剥离命中的成员别名（仅第一个命中的最长别名）
            member?.aliases
                ?.filter { it.isNotBlank() && rest.contains(it) }
                ?.maxByOrNull { it.length }
                ?.let { rest = rest.replaceFirst(it, "") }
        }
        // 剥离直播形式关键词（已由标签呈现，避免与标题重复）
        FORMAT_KEYWORDS.forEach { kw -> rest = rest.replace(kw, "") }
        return rest.replace(Regex("""[\s|·—\-]+"""), "").trim()
    }
}
