package com.example.asoul.data

import com.example.asoul.data.model.GroupType
import com.example.asoul.data.model.LiveCategory
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.ScheduleSource
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 双数据源合并测试：OSS 周程表为主，asoul.love ICS 只补漏 + 提供类型标签。
 */
class ScheduleMergerTest {

    private val date = LocalDate.of(2026, 9, 16)

    private fun oss(
        time: String,
        memberId: String?,
        title: String,
        recording: String? = null,
        groupType: GroupType = GroupType.NONE,
        participantIds: List<String> = emptyList(),
    ) = LiveSchedule(
        date = date,
        time = LocalTime.parse(time),
        memberName = memberId ?: title,
        memberId = memberId,
        title = title,
        groupType = groupType,
        participantIds = participantIds,
        source = ScheduleSource.API,
        recordingBvid = recording,
        category = LiveCategory.UNKNOWN,
    )

    private fun ics(
        time: String,
        memberId: String?,
        title: String,
        category: LiveCategory,
        participantIds: List<String> = emptyList(),
        groupType: GroupType = GroupType.NONE,
    ) = LiveSchedule(
        date = date,
        time = LocalTime.parse(time),
        memberName = memberId ?: title,
        memberId = memberId,
        title = title,
        groupType = groupType,
        participantIds = participantIds,
        source = ScheduleSource.ASOUL_LOVE,
        category = category,
    )

    @Test
    fun `同场次以 OSS 为准并使用 ICS 的类型标签`() {
        val ossList = listOf(oss("12:00", "bella", "贝拉直播", recording = "BV1xx"))
        val icsList = listOf(ics("12:00", "bella", "一起看假面骑士ZZZ", LiveCategory.FLASH))
        val merged = ScheduleMerger.merge(ossList, icsList)

        assertEquals(1, merged.size)
        val item = merged.first()
        // OSS 字段保留（录播/标题/来源）
        assertEquals("BV1xx", item.recordingBvid)
        assertEquals("贝拉直播", item.title)
        assertEquals(ScheduleSource.API, item.source)
        // ICS 提供类型标签
        assertEquals(LiveCategory.FLASH, item.category)
    }

    @Test
    fun `时间容差 15 分钟内视为同场次`() {
        val ossList = listOf(oss("11:54", "bella", "贝拉直播"))
        val icsList = listOf(ics("12:00", "bella", "一起看假面骑士ZZZ", LiveCategory.FLASH))
        assertEquals(1, ScheduleMerger.merge(ossList, icsList).size)
    }

    @Test
    fun `超过容差则各自保留（ICS 补漏）`() {
        val ossList = listOf(oss("12:00", "bella", "贝拉直播"))
        val icsList = listOf(ics("17:00", "diana", "看完JOJO第三部", LiveCategory.FLASH))
        val merged = ScheduleMerger.merge(ossList, icsList)
        assertEquals(2, merged.size)
        // 排序：按日期 + 时间
        assertEquals("贝拉直播", merged[0].title)
        assertEquals("看完JOJO第三部", merged[1].title)
    }

    @Test
    fun `团播条目按参与成员匹配`() {
        val ossList = listOf(
            oss("21:00", null, "直播出逃计划", groupType = GroupType.ASOUL),
        )
        val icsList = listOf(
            ics(
                "21:00", null, "直播出逃计划", LiveCategory.SHOW,
                participantIds = listOf("bella", "diana", "eileen"),
                groupType = GroupType.ASOUL,
            ),
        )
        val merged = ScheduleMerger.merge(ossList, icsList)
        assertEquals(1, merged.size)
        assertEquals(GroupType.ASOUL, merged.first().groupType)
        assertEquals(LiveCategory.SHOW, merged.first().category)
    }

    @Test
    fun `ICS 独有事件整条追加`() {
        val ossList = listOf(oss("21:00", "eileen", "国风小女子来也！"))
        val icsList = listOf(ics("12:00", "bella", "一起看假面骑士ZZZ", LiveCategory.FLASH))
        val merged = ScheduleMerger.merge(ossList, icsList)
        assertEquals(2, merged.size)
        val added = merged.first { it.source == ScheduleSource.ASOUL_LOVE }
        assertEquals(LiveCategory.FLASH, added.category)
        assertEquals(LocalTime.of(12, 0), added.time)
    }

    @Test
    fun `空 ICS 不改变原列表`() {
        val ossList = listOf(oss("21:00", "eileen", "国风小女子来也！"))
        assertEquals(ossList, ScheduleMerger.merge(ossList, emptyList()))
    }

    @Test
    fun `按周裁剪 ICS 数据`() {
        val weekStart = LocalDate.of(2026, 9, 14)
        val inside = oss("21:00", "eileen", "本周")
        val outside = inside.copy(date = LocalDate.of(2026, 9, 21))
        val filtered = ScheduleMerger.withinWeek(listOf(inside, outside), weekStart)
        assertEquals(listOf("本周"), filtered.map { it.title })
    }
}
