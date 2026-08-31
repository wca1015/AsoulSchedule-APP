package com.example.asoul.data

import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.WeekScheduleStatus
import com.example.asoul.data.model.Weeks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

/**
 * P0 阶段使用内存仓库 + StateFlow，保证离线可用、无数据库依赖。
 * P1 阶段可平滑替换为 Room 实现（接口不变）。
 */
class ScheduleRepository {

    private val idGenerator = AtomicLong(1L)

    private val _schedules = MutableStateFlow<List<LiveSchedule>>(emptyList())
    val schedules: StateFlow<List<LiveSchedule>> = _schedules.asStateFlow()

    /** 周程表识别状态（按周一起始日索引）。 */
    private val _weekStatus = MutableStateFlow<Map<LocalDate, WeekScheduleStatus>>(emptyMap())
    val weekStatus: StateFlow<Map<LocalDate, WeekScheduleStatus>> = _weekStatus.asStateFlow()

    fun addSchedule(schedule: LiveSchedule): LiveSchedule {
        val saved = schedule.copy(id = idGenerator.getAndIncrement())
        _schedules.update { it + saved }
        return saved
    }

    fun addAll(schedules: List<LiveSchedule>): List<LiveSchedule> =
        schedules.map { addSchedule(it) }

    fun removeSchedule(id: Long) {
        _schedules.update { list -> list.filterNot { it.id == id } }
    }

    /**
     * 替换指定周的日程（P6：周程表拉取成功后按 week_start 定位整周替换）。
     *
     * - 移除该周内的现有日程（其他周保持不变）
     * - 写入新日程（重新分配 id）
     * - 按「日期 + 时间 + 成员」匹配旧条目，保留已回填的录像 BV 号与系统日历绑定，
     *   避免录播晚于周程表上传时被整周替换冲掉（新条目自身带 bvid 时优先用新值）
     * - 标记该周已识别（来源为 API）
     */
    fun replaceWeek(weekStart: LocalDate, schedules: List<LiveSchedule>) {
        val weekDays = Weeks.daysOfWeek(weekStart).toSet()
        // 旧条目的录像/日历绑定索引：日期+时间+成员 → (recordingBvid, calendarEventId)
        val preserved = _schedules.value
            .filter { it.date in weekDays && (it.recordingBvid != null || it.calendarEventId != null) }
            .associateBy({ Triple(it.date, it.time, it.memberId ?: it.memberName) },
                { it.recordingBvid to it.calendarEventId })
        val merged = schedules.map { schedule ->
            val old = preserved[Triple(schedule.date, schedule.time, schedule.memberId ?: schedule.memberName)]
            schedule.copy(
                recordingBvid = schedule.recordingBvid ?: old?.first,
                calendarEventId = schedule.calendarEventId ?: old?.second,
            )
        }
        _schedules.update { list -> list.filterNot { it.date in weekDays } }
        addAll(merged)
        markWeekRecognized(weekStart, merged.size)
    }

    /** 记录日历事件 id，用于后续删除系统日历条目。 */
    fun markCalendarEvent(id: Long, calendarEventId: Long) {
        _schedules.update { list ->
            list.map { if (it.id == id) it.copy(calendarEventId = calendarEventId) else it }
        }
    }

    /** 标记某周周程表已识别。 */
    fun markWeekRecognized(weekStart: LocalDate, entryCount: Int) {
        _weekStatus.update { map ->
            map + (weekStart to WeekScheduleStatus(
                weekStart = weekStart,
                recognized = true,
                recognizedAt = System.currentTimeMillis(),
                entryCount = entryCount,
            ))
        }
    }

    fun schedulesForWeek(weekStart: LocalDate): List<LiveSchedule> {
        val days = Weeks.daysOfWeek(weekStart).toSet()
        return _schedules.value.filter { it.date in days }.sortedWith(compareBy({ it.date }, { it.time }))
    }

    fun statusForWeek(weekStart: LocalDate): WeekScheduleStatus =
        _weekStatus.value[weekStart] ?: WeekScheduleStatus(weekStart, recognized = false)
}
