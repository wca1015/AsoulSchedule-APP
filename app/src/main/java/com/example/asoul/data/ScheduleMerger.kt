package com.example.asoul.data

import com.example.asoul.data.model.LiveCategory
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.Weeks
import java.time.Duration
import java.time.LocalDate

/**
 * 合并「OSS 周程表」与「asoul.love ICS」两份数据（纯函数，便于离线单测）。
 *
 * 合并策略（与需求确认一致）：
 * - **同场次以 OSS 为准**：匹配到的事件保留 OSS 条目（录播 BV、系统日历绑定、
 *   团播分组等更完整），仅用 ICS 补两类信息——类型标签（节目/日常/突击/2D）
 *   与来源动态链接。
 * - **ICS 只补漏**：OSS 没有的场次（典型如「纯文字预告」的突击直播）整体追加。
 *
 * 匹配规则：同日期 + 开播时间差 ≤ [MATCH_WINDOW_MINUTES] 分钟 +
 * 参与成员有交集；双方都无法解析参与成员时退回成员名比较。
 */
object ScheduleMerger {

    /** 同场次判定的时间容差（分钟）：成员可能提前/推迟开播。 */
    private const val MATCH_WINDOW_MINUTES = 15L

    /** 取某周的条目（用于把 ICS 全量数据裁剪到当前展示周）。 */
    fun withinWeek(events: List<LiveSchedule>, weekStart: LocalDate): List<LiveSchedule> {
        val days = Weeks.daysOfWeek(weekStart).toSet()
        return events.filter { it.date in days }
    }

    /** 合并两份日程：OSS 条目 + ICS 补充（类型/来源链接）与新增条目。 */
    fun merge(oss: List<LiveSchedule>, ics: List<LiveSchedule>): List<LiveSchedule> {
        if (ics.isEmpty()) return oss
        val used = mutableSetOf<Int>()
        val merged = oss.map { item ->
            val index = ics.indices.firstOrNull { i -> i !in used && matches(item, ics[i]) }
            if (index == null) {
                item
            } else {
                used += index
                val extra = ics[index]
                item.copy(
                    // ICS 提供类型标签（新体系）；OSS 自带的类型（如有）优先保留
                    category = if (item.category != LiveCategory.UNKNOWN) item.category else extra.category,
                    sourceUrl = item.sourceUrl ?: extra.sourceUrl,
                )
            }
        }
        val additions = ics.filterIndexed { index, _ -> index !in used }
        return (merged + additions).sortedWith(compareBy({ it.date }, { it.time }))
    }

    private fun matches(a: LiveSchedule, b: LiveSchedule): Boolean {
        if (a.date != b.date) return false
        val diffMinutes = Duration.between(a.time, b.time).abs().toMinutes()
        if (diffMinutes > MATCH_WINDOW_MINUTES) return false
        val participantsA = a.resolvedParticipantIds().toSet()
        val participantsB = b.resolvedParticipantIds().toSet()
        if (participantsA.isEmpty() || participantsB.isEmpty()) {
            return a.memberName == b.memberName
        }
        return participantsA.intersect(participantsB).isNotEmpty()
    }
}
