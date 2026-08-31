package com.example.asoul.data.model

/**
 * 日历主界面成员过滤器。
 *
 * 原「成员」底部导航页已整合进日历主界面：
 * - [All]：显示全部日程
 * - [MemberFilter]：选中成员头像 → 显示该成员的单播 + 其参与的团播
 * - [GroupFilter]：选中团播头像 → 仅显示对应团播分组的日程
 */
sealed class ScheduleFilter {

    /** 全部日程。 */
    data object All : ScheduleFilter()

    /** 按成员过滤：单播 + 该成员参与的团播。 */
    data class MemberFilter(val memberId: String) : ScheduleFilter()

    /** 按团播分组过滤。 */
    data class GroupFilter(val groupType: GroupType) : ScheduleFilter()
}

/** 判断一条日程是否匹配当前过滤器。 */
fun LiveSchedule.matches(filter: ScheduleFilter): Boolean = when (filter) {
    ScheduleFilter.All -> true
    is ScheduleFilter.MemberFilter ->
        (memberId == filter.memberId) ||
            (isGroupLive && MemberCatalog.participantsOf(groupType).any { it.id == filter.memberId })
    is ScheduleFilter.GroupFilter -> groupType == filter.groupType
}
