package com.example.asoul.data.model

/**
 * 日历主界面成员过滤器。
 *
 * 原「成员」底部导航页已整合进日历主界面：
 * - [All]：显示全部日程
 * - [MemberFilter]：头像多选——显示这些成员的单播 + 参与的团播（**任一参与即可**，含单播）
 * - [ComboFilter]：组合快捷项——只显示该组合的**多人同台场次**（见下）
 * - [GroupFilter]：选中团播头像 → 仅显示对应团播分组的日程
 */
sealed class ScheduleFilter {

    /** 全部日程。 */
    data object All : ScheduleFilter()

    /** 按成员过滤（多选）：任一选中成员参与即显示（含单播）。 */
    data class MemberFilter(val memberIds: Set<String>) : ScheduleFilter() {
        /** 单人便捷构造（保留旧调用方式）。 */
        constructor(memberId: String) : this(setOf(memberId))
    }

    /**
     * 组合过滤（成员行下方的组合快捷项：枝江 / A-SOUL / 小心思 / 嘉贝 / 乃贝 / 琳嘉）。
     *
     * 与 [MemberFilter] 的区别：这里表达的是「这几位的**同台多人直播**」——
     * 要求该场为多人场，且**全部参与者都在组合内**（该场就是这几位一起的直播），
     * 因此不会把各自单播（如心宜单人、思诺单人）混进来。
     */
    data class ComboFilter(val memberIds: Set<String>) : ScheduleFilter()

    /** 按团播分组过滤。 */
    data class GroupFilter(val groupType: GroupType) : ScheduleFilter()
}

/** 判断一条日程是否匹配当前过滤器。 */
fun LiveSchedule.matches(filter: ScheduleFilter): Boolean = when (filter) {
    ScheduleFilter.All -> true
    is ScheduleFilter.MemberFilter ->
        // 单播匹配本人；团播 / 双人直播展开参与成员后匹配（多选为任一命中）
        resolvedParticipantIds().any { it in filter.memberIds }
    is ScheduleFilter.ComboFilter -> {
        // 多人同台：该场参与者全部属于所选组合（不夹杂各自单播与他人场次）
        val participants = resolvedParticipantIds()
        isMultiLive && participants.isNotEmpty() && filter.memberIds.containsAll(participants)
    }
    is ScheduleFilter.GroupFilter -> groupType == filter.groupType
}
