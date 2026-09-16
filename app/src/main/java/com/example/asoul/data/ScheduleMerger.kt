package com.example.asoul.data

import com.example.asoul.data.model.GroupType
import com.example.asoul.data.model.LiveCategory
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.MemberCatalog
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
                adoptIcsParticipants(
                    item.copy(
                        // ICS 提供类型标签（新体系）；OSS 自带的类型（如有）优先保留
                        category = if (item.category != LiveCategory.UNKNOWN) item.category else extra.category,
                        sourceUrl = item.sourceUrl ?: extra.sourceUrl,
                    ),
                    extra,
                )
            }
        }
        val additions = ics.filterIndexed { index, _ -> index !in used }
        return (merged + additions).sortedWith(compareBy({ it.date }, { it.time }))
    }

    /**
     * 用 ICS 的参与成员收窄 OSS 的「分组粗粒度展开」。
     *
     * 服务端对 `member = unknown` 的团播按分组成员**全量展开**——例如「命题KTV」
     * （心宜思诺节目）被标为枝江综艺 → 展开为 5 人；ICS 的成员列表更精确
     * （心宜 思诺），收窄后才能被「小心思」等组合筛选中。
     *
     * 仅在信息不冲突（ICS 成员是 OSS 展开集合的**真子集**且 ≥2 人）时采用；
     * 单播与一期双人（服务端已给精确参与成员）不覆盖，保持「同场次以 OSS 为准」的总原则。
     */
    private fun adoptIcsParticipants(oss: LiveSchedule, ics: LiveSchedule): LiveSchedule {
        if (oss.groupType == GroupType.NONE || oss.memberId != null) return oss
        val icsIds = ics.participantIds
        if (icsIds.size < 2) return oss
        val allowed = oss.resolvedParticipantIds().toSet()
        if (icsIds.toSet() == allowed || !allowed.containsAll(icsIds)) return oss
        val inferred = MemberCatalog.inferGroupType(icsIds)
        val members = icsIds.mapNotNull { id -> MemberCatalog.ALL.firstOrNull { it.id == id } }
        val displayName = when {
            inferred != GroupType.NONE -> inferred.label
            members.size > 1 -> members.joinToString(" & ") { it.name }
            else -> oss.memberName
        }
        return oss.copy(
            participantIds = icsIds,
            groupType = inferred,
            memberName = displayName,
        )
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
