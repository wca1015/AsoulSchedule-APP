package com.example.asoul.data.model

/**
 * 日历主界面成员过滤器。
 *
 * 原「成员」底部导航页已整合进日历主界面：
 * - [All]：显示全部日程
 * - [MemberFilter]：选中成员头像（支持多选 / 组合快捷项）→ 显示这些成员的单播 + 参与的团播
 * - [GroupFilter]：选中团播头像 → 仅显示对应团播分组的日程
 */
sealed class ScheduleFilter {

    /** 全部日程。 */
    data object All : ScheduleFilter()

    /** 按成员过滤（多选）：任一选中成员参与即显示。 */
    data class MemberFilter(val memberIds: Set<String>) : ScheduleFilter() {
        /** 单人便捷构造（保留旧调用方式）。 */
        constructor(memberId: String) : this(setOf(memberId))
    }

    /** 按团播分组过滤。 */
    data class GroupFilter(val groupType: GroupType) : ScheduleFilter()
}

/** 判断一条日程是否匹配当前过滤器。 */
fun LiveSchedule.matches(filter: ScheduleFilter): Boolean = when (filter) {
    ScheduleFilter.All -> true
    is ScheduleFilter.MemberFilter ->
        // 单播匹配本人；团播 / 双人直播展开参与成员后匹配（多选为任一命中）
        resolvedParticipantIds().any { it in filter.memberIds }
    is ScheduleFilter.GroupFilter -> groupType == filter.groupType
}
