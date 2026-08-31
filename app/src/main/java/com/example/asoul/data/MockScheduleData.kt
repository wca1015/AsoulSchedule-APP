package com.example.asoul.data

import com.example.asoul.data.model.GroupType
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.ScheduleSource
import com.example.asoul.data.model.StreamFormat
import com.example.asoul.data.model.Weeks
import java.time.LocalDate
import java.time.LocalTime

/**
 * 开发用 Mock 数据（UI 规格第十节：基于官方 08.17-08.23 周程表海报）。
 *
 * 海报原始日期为 2025-08-17 ~ 08-23；此处按「当前周」的周一为基准映射，
 * 保证打开 App 即可看到完整视觉效果。共 12 个事件（含枝江综艺演示条目）。
 *
 * 直播标题以官方周程表海报（app/img/live.png）为准，例如：
 * 嘉然七夕直播《我们时代的偏爱》、贝拉《爱，很简单》、乃琳《恋爱习题》等。
 *
 * 往日数据：过去两周的日程均附带 B 站直播录像 BV 号（真实公开录像），
 * 用于演示「左划回看往日周历 → 展示录像标签 → 点击跳转 B 站录像」。
 */
object MockScheduleData {

    // ===== 公开可查的 B 站直播录像 BV 号（演示数据） =====

    /** 嘉然直播录像。 */
    private val DIANA_BVIDS = listOf("BV1jnwtz8EqC", "BV1GFNt61Eaf", "BV1jEz7BrEEH", "BV1a3i1BuE1W")

    /** 贝拉直播录像。 */
    private val BELLA_BVIDS = listOf("BV18Qjw6PE4y", "BV1epVt6MEPY")

    /** 乃琳直播录像。 */
    private val EILEEN_BVIDS = listOf("BV1vCzDBeEqX")

    /** Asoul 团播（联动/多人企划）录像。 */
    private val GROUP_BVIDS = listOf("BV1Hf4y1M7w9", "BV1Eb4y1y7bY", "BV1Vp4y1t764")

    /** 生成当前周的 Mock 日程（12 条）；已过日期的事件自动附带录像 BV 号。 */
    fun currentWeekMock(): List<LiveSchedule> {
        val monday = Weeks.startOfWeek(Weeks.today())
        val today = Weeks.today()
        return currentWeekEvents(monday).map { schedule ->
            if (schedule.date < today) {
                schedule.copy(recordingBvid = bvidFor(schedule, weekIndex = 0))
            } else {
                schedule
            }
        }
    }

    /** 生成过去第 [weeksAgo] 周的 Mock 日程（均已结束，全部附带录像 BV 号）。 */
    fun pastWeekMock(weeksAgo: Long): List<LiveSchedule> {
        val monday = Weeks.startOfWeek(Weeks.today()).minusWeeks(weeksAgo)
        return pastWeekEvents(monday).map { schedule ->
            schedule.copy(recordingBvid = bvidFor(schedule, weekIndex = weeksAgo.toInt()))
        }
    }

    /** 当前周模板（官方 08.17-08.23 海报内容映射）。 */
    private fun currentWeekEvents(monday: LocalDate): List<LiveSchedule> {
        fun day(offset: Long): LocalDate = monday.plusDays(offset)

        return listOf(
            // 周一：思诺 2D 直播
            LiveSchedule(
                date = day(0), time = LocalTime.of(17, 0),
                memberName = "思诺", memberId = "sinuo",
                title = "2D直播",
                source = ScheduleSource.MANUAL,
            ),
            // 周二：七夕三连（标题取自官方周程表海报）
            LiveSchedule(
                date = day(1), time = LocalTime.of(19, 0),
                memberName = "嘉然 七夕直播", memberId = "diana",
                title = "我们时代的偏爱",
                format = StreamFormat.NORMAL,
                source = ScheduleSource.MANUAL,
            ),
            LiveSchedule(
                date = day(1), time = LocalTime.of(20, 5),
                memberName = "贝拉 七夕直播", memberId = "bella",
                title = "爱，很简单",
                source = ScheduleSource.MANUAL,
            ),
            LiveSchedule(
                date = day(1), time = LocalTime.of(21, 10),
                memberName = "乃琳 七夕直播", memberId = "eileen",
                title = "恋爱习题",
                source = ScheduleSource.MANUAL,
            ),
            // 周三：心宜 / 思诺 七夕 2D 直播
            LiveSchedule(
                date = day(2), time = LocalTime.of(18, 0),
                memberName = "心宜 七夕2D直播", memberId = "xinyi",
                title = "心动的信号",
                source = ScheduleSource.MANUAL,
            ),
            LiveSchedule(
                date = day(2), time = LocalTime.of(19, 5),
                memberName = "思诺 七夕2D直播", memberId = "sinuo",
                title = "Super Darling!",
                source = ScheduleSource.MANUAL,
            ),
            // 周四：训练时间（无日程 → 休息日空状态）
            // 周五：心宜 2D 直播 + A-SOUL 游戏室
            LiveSchedule(
                date = day(4), time = LocalTime.of(16, 0),
                memberName = "心宜", memberId = "xinyi",
                title = "2D直播",
                source = ScheduleSource.MANUAL,
            ),
            LiveSchedule(
                date = day(4), time = LocalTime.of(20, 0),
                memberName = GroupType.ASOUL.label, memberId = null,
                title = "游戏室",
                groupType = GroupType.ASOUL,
                format = StreamFormat.GAME_ROOM,
                source = ScheduleSource.MANUAL,
            ),
            // 周六：心宜 2D 直播 + A-SOUL 夜谈 + 心宜思诺聊天室
            LiveSchedule(
                date = day(5), time = LocalTime.of(14, 0),
                memberName = "心宜", memberId = "xinyi",
                title = "2D直播",
                source = ScheduleSource.MANUAL,
            ),
            LiveSchedule(
                date = day(5), time = LocalTime.of(18, 50),
                memberName = GroupType.ASOUL.label, memberId = null,
                title = "夜谈",
                groupType = GroupType.ASOUL,
                format = StreamFormat.NIGHT_TALK,
                source = ScheduleSource.MANUAL,
            ),
            LiveSchedule(
                date = day(5), time = LocalTime.of(20, 0),
                memberName = GroupType.XINYI_SINUO.label, memberId = null,
                title = "聊天室",
                groupType = GroupType.XINYI_SINUO,
                format = StreamFormat.NORMAL,
                source = ScheduleSource.MANUAL,
            ),
            // 周日：枝江综艺（一期 + 二期共同参与的团播企划）
            LiveSchedule(
                date = day(6), time = LocalTime.of(19, 30),
                memberName = GroupType.ZHIJIANG_VARIETY.label, memberId = null,
                title = "枝江综艺大派对",
                groupType = GroupType.ZHIJIANG_VARIETY,
                format = StreamFormat.GAME_ROOM,
                source = ScheduleSource.MANUAL,
            ),
        )
    }

    /** 往日周模板：以有公开录像的一期成员 + Asoul 团播为主。 */
    private fun pastWeekEvents(monday: LocalDate): List<LiveSchedule> {
        fun day(offset: Long): LocalDate = monday.plusDays(offset)

        return listOf(
            // 周一：贝拉舞蹈练习
            LiveSchedule(
                date = day(0), time = LocalTime.of(20, 0),
                memberName = "贝拉", memberId = "bella",
                title = "舞蹈练习室",
                source = ScheduleSource.MANUAL,
            ),
            // 周二：嘉然歌回 + 乃琳深夜电台
            LiveSchedule(
                date = day(1), time = LocalTime.of(19, 30),
                memberName = "嘉然", memberId = "diana",
                title = "歌回",
                source = ScheduleSource.MANUAL,
            ),
            LiveSchedule(
                date = day(1), time = LocalTime.of(21, 0),
                memberName = "乃琳", memberId = "eileen",
                title = "深夜电台",
                source = ScheduleSource.MANUAL,
            ),
            // 周三：嘉然游戏回
            LiveSchedule(
                date = day(2), time = LocalTime.of(20, 0),
                memberName = "嘉然", memberId = "diana",
                title = "游戏回",
                source = ScheduleSource.MANUAL,
            ),
            // 周四：训练时间（无日程 → 休息日空状态）
            // 周五：Asoul 团播游戏室
            LiveSchedule(
                date = day(4), time = LocalTime.of(20, 0),
                memberName = GroupType.ASOUL.label, memberId = null,
                title = "游戏室",
                groupType = GroupType.ASOUL,
                format = StreamFormat.GAME_ROOM,
                source = ScheduleSource.MANUAL,
            ),
            // 周六：乃琳联动 + Asoul 团播夜谈
            LiveSchedule(
                date = day(5), time = LocalTime.of(19, 0),
                memberName = "乃琳", memberId = "eileen",
                title = "联动直播",
                format = StreamFormat.COLLAB,
                source = ScheduleSource.MANUAL,
            ),
            LiveSchedule(
                date = day(5), time = LocalTime.of(20, 30),
                memberName = GroupType.ASOUL.label, memberId = null,
                title = "夜谈",
                groupType = GroupType.ASOUL,
                format = StreamFormat.NIGHT_TALK,
                source = ScheduleSource.MANUAL,
            ),
            // 周日：贝拉工商直播
            LiveSchedule(
                date = day(6), time = LocalTime.of(15, 0),
                memberName = "贝拉", memberId = "bella",
                title = "工商直播",
                format = StreamFormat.COMMERCIAL,
                source = ScheduleSource.MANUAL,
            ),
        )
    }

    /** 按成员/团播分组挑选录像 BV 号（不同周轮换，避免重复）；无公开录像时返回 null。 */
    private fun bvidFor(schedule: LiveSchedule, weekIndex: Int): String? {
        val pool = when {
            schedule.memberId == "diana" -> DIANA_BVIDS
            schedule.memberId == "bella" -> BELLA_BVIDS
            schedule.memberId == "eileen" -> EILEEN_BVIDS
            schedule.groupType == GroupType.ASOUL -> GROUP_BVIDS
            // 二期成员 / 心宜思诺团播 / 枝江综艺暂无公开录像
            else -> return null
        }
        return pool[Math.floorMod(weekIndex, pool.size)]
    }
}
