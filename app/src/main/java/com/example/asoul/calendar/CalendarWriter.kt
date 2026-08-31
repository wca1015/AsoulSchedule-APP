package com.example.asoul.calendar

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.example.asoul.data.model.GroupType
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.MemberCatalog
import java.util.TimeZone

/**
 * 系统日历写入器（模块3：日程写入系统日历）。
 *
 * 需要 [android.Manifest.permission.WRITE_CALENDAR] 权限，
 * 调用方负责在运行时申请权限。
 */
class CalendarWriter(private val context: Context) {

    /**
     * 将一条直播日程写入系统日历。
     *
     * @param calendarId 目标日历账户 id；为 null 时使用设备默认日历（id=1 兜底）。
     * @param reminderMinutes 提前提醒分钟数，<=0 表示不提醒。
     * @return 写入成功的日历事件 id；失败返回 null。
     */
    fun insertEvent(
        schedule: LiveSchedule,
        calendarId: Long? = null,
        reminderMinutes: Int = 30,
    ): Long? {
        val resolver = context.contentResolver
        val zone = TimeZone.getDefault()
        val startMillis = schedule.date.atTime(schedule.time)
            .atZone(zone.toZoneId()).toInstant().toEpochMilli()
        // 直播默认时长 2 小时（方案未指定，可在详情页编辑——P1）
        val endMillis = startMillis + 2 * 60 * 60 * 1000L

        val member = schedule.member(MemberCatalog.ALL)
        val memberText = member?.let { "${it.name}${if (it.subText.isNotBlank()) "（${it.subText}）" else ""}" }
            ?: schedule.memberName

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId ?: queryDefaultCalendarId() ?: 1L)
            put(CalendarContract.Events.TITLE, buildTitle(schedule))
            put(
                CalendarContract.Events.DESCRIPTION,
                buildDescription(schedule, memberText),
            )
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }

        val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
        val eventId = uri.lastPathSegment?.toLongOrNull() ?: return null

        if (reminderMinutes > 0) {
            val reminder = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, reminderMinutes)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder)
        }
        return eventId
    }

    /** 删除已写入的日历事件。 */
    fun deleteEvent(calendarEventId: Long): Int =
        context.contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,
            "${CalendarContract.Events._ID} = ?",
            arrayOf(calendarEventId.toString()),
        )

    private fun buildTitle(schedule: LiveSchedule): String =
        when {
            schedule.groupType == GroupType.ASOUL ->
                "【枝江日历】Asoul团播 · ${schedule.title}"
            schedule.groupType == GroupType.XINYI_SINUO ->
                "【枝江日历】心宜思诺团播 · ${schedule.title}"
            else -> "【枝江日历】${schedule.memberName} · ${schedule.title}"
        }

    private fun buildDescription(schedule: LiveSchedule, memberText: String): String =
        buildString {
            append("成员：$memberText")
            if (schedule.isGroupLive) append("\n类型：${schedule.groupType.label}")
            schedule.formatTag?.let { append("\n形式：${it.label}") }
            append("\n来源：${schedule.source.label}")
        }

    /** 查询设备默认可写日历账户。 */
    private fun queryDefaultCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
        )
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            var fallback: Long? = null
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val isPrimary = cursor.getInt(1) == 1
                if (isPrimary) return id
                if (fallback == null) fallback = id
            }
            return fallback
        }
        return null
    }
}
