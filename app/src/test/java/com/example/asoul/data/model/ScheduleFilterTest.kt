package com.example.asoul.data.model

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 筛选语义测试：
 * - [ScheduleFilter.MemberFilter]（成员多选）：任一参与即显示（含单播）
 * - [ScheduleFilter.ComboFilter]（组合快捷项）：只显示该组合的**同台多人场**
 *   （双人/团播，且全部参与者都在组合内），不夹杂各自单播
 */
class ScheduleFilterTest {

    private fun item(
        memberId: String? = null,
        participants: List<String> = emptyList(),
        groupType: GroupType = GroupType.NONE,
    ) = LiveSchedule(
        date = LocalDate.of(2026, 9, 20),
        time = LocalTime.of(20, 0),
        memberName = "test",
        memberId = memberId,
        title = "test",
        groupType = groupType,
        participantIds = participants,
    )

    /** 心宜单人直播。 */
    private val solo = item(memberId = "xinyi")

    /** 心宜 & 思诺双人直播（命题KTV 一类的节目场）。 */
    private val duo = item(participants = listOf("xinyi", "sinuo"))

    /** 一期三人团播（Asoul 团综）。 */
    private val asoulGroup = item(groupType = GroupType.ASOUL)

    /** 枝江综艺（全员 5 人）。 */
    private val allHands = item(groupType = GroupType.ZHIJIANG_VARIETY)

    private val xiaoxinsi = ScheduleFilter.ComboFilter(setOf("xinyi", "sinuo"))
    private val jiabei = ScheduleFilter.ComboFilter(setOf("diana", "bella"))
    private val asoulCombo = ScheduleFilter.ComboFilter(setOf("bella", "diana", "eileen"))
    private val zhijiang =
        ScheduleFilter.ComboFilter(setOf("bella", "diana", "eileen", "xinyi", "sinuo"))

    @Test
    fun `成员多选为任一参与（含单播）`() {
        val filter = ScheduleFilter.MemberFilter(setOf("xinyi", "sinuo"))
        assertTrue(solo.matches(filter))
        assertTrue(duo.matches(filter))
        assertTrue(allHands.matches(filter))
        assertFalse(asoulGroup.matches(filter))
    }

    @Test
    fun `组合只显示同台多人场，不夹杂单播`() {
        assertTrue(duo.matches(xiaoxinsi))
        // 修复点：选中「小心思」时，心宜/思诺的单人直播不应被筛出
        assertFalse(solo.matches(xiaoxinsi))
    }

    @Test
    fun `组合要求该场全部参与者都在组合内`() {
        // 团综含乃琳 → 不属于「嘉贝」双人场
        assertFalse(asoulGroup.matches(jiabei))
        assertTrue(asoulGroup.matches(asoulCombo))
        assertTrue(asoulGroup.matches(zhijiang))
        assertFalse(duo.matches(jiabei))
    }

    @Test
    fun `全员组合覆盖多人场但排除单播`() {
        assertTrue(duo.matches(zhijiang))
        assertTrue(asoulGroup.matches(zhijiang))
        assertTrue(allHands.matches(zhijiang))
        assertFalse(solo.matches(zhijiang))
    }
}
