package com.example.asoul.data

import com.example.asoul.data.model.GroupType
import com.example.asoul.data.model.LiveCategory
import com.example.asoul.data.model.ScheduleSource
import com.example.asoul.data.remote.IcsParser
import com.example.asoul.data.remote.toLiveSchedule
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * asoul.love ICS 解析与映射测试（离线，样本取自真实 `calendar.ics` 内容）。
 *
 * 覆盖：折叠行还原、UTC→北京时间换算、类型前缀映射、标题切分、
 * 单/多成员解析、来源动态链接提取。
 */
class IcsParserTest {

    /** 真实样本（含 75 字节折叠行：直播间 URL 被折成两行）。 */
    private val sample = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//asoul.love//Calendar//ZH
        BEGIN:VEVENT
        UID:20260916-1200-bella@asoul.love
        DTSTAMP:20260915T160000Z
        DTSTART:20260916T040000Z
        SUMMARY:【突击】贝拉突击: 【突击】一起看假面骑士ZZZ
        DESCRIPTION:突击 | 贝拉\n\n直播间：https://live.bilibili.com/226324
         24\n\n动态: https://www.bilibili.com/opus/1247763084158697475
        STATUS:CONFIRMED
        DURATION:PT1H
        URL:https://live.bilibili.com/22632424
        END:VEVENT
        BEGIN:VEVENT
        UID:20260916-1400-gladys@asoul.love
        DTSTART:20260916T060000Z
        SUMMARY:【2D】思诺直播
        DESCRIPTION:2D | 思诺\n\n直播间：https://live.bilibili.com/30858592\n
         \n动态: https://www.bilibili.com/opus/1247773901044318217
        URL:https://live.bilibili.com/30858592
        END:VEVENT
        BEGIN:VEVENT
        UID:20260916-1700-fiona@asoul.love
        DTSTART:20260916T090000Z
        SUMMARY:【突击】心宜突击: 【2D】《闪耀吧！噜咪》开启
        DESCRIPTION:突击 | 心宜\n\n直播间：https://live.bilibili.com/30849777
        URL:https://live.bilibili.com/30849777
        END:VEVENT
        BEGIN:VEVENT
        UID:20260919-2100-bella@asoul.love
        DTSTART:20260919T130000Z
        SUMMARY:【节目】A-SOUL团综: 直播出逃计划
        DESCRIPTION:节目 | 贝拉 嘉然 乃琳\n\n直播间：https://live.bilibili.com/22632424
        \n\n动态: https://www.bilibili.com/opus/1247780861019947044
        URL:https://live.bilibili.com/22632424
        END:VEVENT
        BEGIN:VEVENT
        UID:20260920-2000-fiona@asoul.love
        DTSTART:20260920T120000Z
        SUMMARY:【节目】有点宜思的世界: 命题KTV
        DESCRIPTION:节目 | 心宜 思诺\n\n直播间：https://live.bilibili.com/30849777
        URL:https://live.bilibili.com/30849777
        END:VEVENT
        END:VCALENDAR
    """.trimIndent()

    @Test
    fun `解析事件数量与折叠行还原`() {
        val events = IcsParser.parse(sample)
        assertEquals(5, events.size)
        // 折叠行（"226324" + "24"）应还原成完整直播间地址
        assert(events[0].description.contains("https://live.bilibili.com/22632424"))
    }

    @Test
    fun `UTC 时间换算为北京时间`() {
        val events = IcsParser.parse(sample)
        assertEquals(LocalDate.of(2026, 9, 16), events[0].start.toLocalDate())
        assertEquals(LocalTime.of(12, 0), events[0].start.toLocalTime())
        assertEquals(LocalTime.of(14, 0), events[1].start.toLocalTime())
        assertEquals(LocalTime.of(21, 0), events[3].start.toLocalTime())
    }

    @Test
    fun `突击事件：类型标签、标题剥离嵌套前缀、来源动态`() {
        val schedule = IcsParser.parseSchedules(sample).first()
        assertEquals(LiveCategory.FLASH, schedule.category)
        assertEquals("一起看假面骑士ZZZ", schedule.title)
        assertEquals("bella", schedule.memberId)
        assertEquals(LocalTime.of(12, 0), schedule.time)
        assertEquals(ScheduleSource.ASOUL_LOVE, schedule.source)
        // 来源动态链接（opus）被提取
        assertEquals(
            "https://www.bilibili.com/opus/1247763084158697475",
            schedule.sourceUrl,
        )
    }

    @Test
    fun `2D 事件映射`() {
        val schedule = IcsParser.parseSchedules(sample)[1]
        assertEquals(LiveCategory.TWO_D, schedule.category)
        assertEquals("思诺直播", schedule.title)
        assertEquals("sinuo", schedule.memberId)
    }

    @Test
    fun `含二级前缀的突击标题`() {
        val schedule = IcsParser.parseSchedules(sample)[2]
        assertEquals("xinyi", schedule.memberId)
        assertEquals("《闪耀吧！噜咪》开启", schedule.title)
    }

    @Test
    fun `多人节目：参与成员与团播分组推断`() {
        val schedule = IcsParser.parseSchedules(sample)[3]
        assertEquals(LiveCategory.SHOW, schedule.category)
        assertEquals("直播出逃计划", schedule.title)
        assertEquals(GroupType.ASOUL, schedule.groupType)
        assertEquals(listOf("bella", "diana", "eileen"), schedule.participantIds)
        assertEquals("Asoul团播", schedule.memberName)
        assertNull(schedule.memberId)
    }

    @Test
    fun `心宜思诺节目推断为二期团播`() {
        val schedule = IcsParser.parseSchedules(sample)[4]
        assertEquals(GroupType.XINYI_SINUO, schedule.groupType)
        assertEquals("命题KTV", schedule.title)
        assertNotNull(schedule.participantIds)
    }

    @Test
    fun `无前缀摘要与非法内容不产生日程`() {
        val bad = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:20260916-9999-bella@asoul.love
            DTSTART:20260916T040000Z
            SUMMARY:没有前缀的摘要
            END:VEVENT
            BEGIN:VEVENT
            UID:20260916-8888-bella@asoul.love
            SUMMARY:【日常】缺时间
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        assertEquals(0, IcsParser.parseSchedules(bad).size)
    }
}
