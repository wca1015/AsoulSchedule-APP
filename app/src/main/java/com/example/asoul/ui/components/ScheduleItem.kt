package com.example.asoul.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asoul.data.model.GroupType
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.MemberCatalog
import com.example.asoul.data.model.StreamFormat
import com.example.asoul.data.model.isEnded
import com.example.asoul.ui.theme.AsoulColors
import com.example.asoul.util.BilibiliLauncher
import java.time.format.DateTimeFormatter

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/** 卡片左上角 Badge。 */
private data class Badge(
    val label: String,
    val color: Color,
    /** 可点击标签（如「录像」→ 跳转 B 站直播录像）；null 时不可点。 */
    val onClick: (() -> Unit)? = null,
)

/** 从日程推导 Badge 列表（录像 / 团播分组 / 直播形式 / 特别企划）。 */
private fun LiveSchedule.badges(onOpenRecording: () -> Unit): List<Badge> = buildList {
    // 「录像」：往日已结束的直播附带录像链接（点击跳转 B 站回放）
    if (!recordingBvid.isNullOrBlank() && isEnded()) {
        add(Badge("录像", AsoulColors.BadgeRecording, onClick = onOpenRecording))
    }
    if (groupType != GroupType.NONE) add(Badge(groupType.label, AsoulColors.BadgeGroup))
    when (format) {
        StreamFormat.THEATER, StreamFormat.NIGHT_TALK, StreamFormat.GAME_ROOM ->
            add(Badge("节目", AsoulColors.BadgeShow))
        StreamFormat.COLLAB -> add(Badge("联动", AsoulColors.BadgeCollab))
        StreamFormat.COMMERCIAL -> add(Badge("工商", AsoulColors.BadgeCommercial))
        else -> {}
    }
    // 特别企划关键词（七夕/生日/首播等）
    val text = title + memberName
    if (SPECIAL_KEYWORDS.any { text.contains(it) }) {
        add(Badge("特别", AsoulColors.BadgeSpecial))
    }
}

private val SPECIAL_KEYWORDS = listOf("七夕", "生日", "首播", "周年", "新年", "节日", "纪念")

/**
 * 直播事件卡片（UI 规格 3.4，核心组件）：
 * - 圆角 16dp，最小高 88dp
 * - 左侧 4dp 成员应援色条，背景为成员色 8%（暗色模式 15%）
 * - 头像（36dp 圆形）+ Badge 标签 + 标题 + 副标题
 * - 右下角等宽字体时间（成员色）+ 提醒铃铛（写入系统日历）
 * - 点击打开直播详情弹窗（可唤起 B 站直播间）
 * - 长按弹出快捷菜单（添加日历/删除）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveEventCard(
    schedule: LiveSchedule,
    onAddToCalendar: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onCardClick: (LiveSchedule) -> Unit = {},
) {
    val context = LocalContext.current
    val member = schedule.member(MemberCatalog.ALL)
    val accent = when (schedule.groupType) {
        GroupType.XINYI_SINUO ->
            MemberCatalog.SECOND_GEN.firstOrNull { it.id == "xinyi" }?.color ?: Color.Gray
        GroupType.ASOUL -> Color(0xFF8E7CC3)
        GroupType.ZHIJIANG_VARIETY -> AsoulColors.BadgeGroup
        GroupType.NONE -> member?.color ?: Color.Gray
    }
    val isDark = isSystemInDarkTheme()
    val bgAlpha = if (isDark) 0.15f else 0.08f
    var menuVisible by remember { mutableStateOf(false) }
    val reminderSet = schedule.calendarEventId != null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = bgAlpha),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 88.dp)
                    .combinedClickable(
                        onClick = { onCardClick(schedule) },
                        onLongClick = { menuVisible = true },
                    )
                    .padding(start = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 左侧 4dp 成员色条
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .defaultMinSize(minHeight = 88.dp)
                        .background(accent),
                )
                // 头像：单人用成员图片，团播用团播专属头像，其余星标占位
                val avatarRes = schedule.groupType.avatarRes ?: member?.avatarRes
                if (avatarRes != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(avatarRes),
                        contentDescription = "${schedule.memberName}头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.25f)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = member?.emoji?.ifBlank { "\uD83C\uDFA4" } ?: "\uD83C\uDF1F",
                            fontSize = 18.sp,
                        )
                    }
                }
                // 信息区：Badge + 标题 + 副标题
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        schedule.badges(
                            onOpenRecording = {
                                BilibiliLauncher.openRecording(context, schedule.recordingBvid)
                            },
                        ).forEach { badge ->
                            BadgeChip(badge)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = schedule.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    Text(
                        text = schedule.memberName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                // 时间 + 提醒按钮
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Text(
                        text = schedule.time.format(TIME_FORMAT),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accent,
                    )
                    IconButton(
                        onClick = onAddToCalendar,
                        enabled = !reminderSet,
                    ) {
                        Icon(
                            imageVector = if (reminderSet) {
                                Icons.Default.NotificationsActive
                            } else {
                                Icons.Default.Notifications
                            },
                            contentDescription = if (reminderSet) "已设置提醒" else "写入系统日历提醒",
                            tint = if (reminderSet) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 长按快捷菜单（规格六：分享/添加日历/删除）
            DropdownMenu(
                expanded = menuVisible,
                onDismissRequest = { menuVisible = false },
            ) {
                DropdownMenuItem(
                    text = { Text(if (reminderSet) "已写入日历" else "添加到系统日历") },
                    onClick = {
                        menuVisible = false
                        if (!reminderSet) onAddToCalendar()
                    },
                    enabled = !reminderSet,
                )
                DropdownMenuItem(
                    text = { Text("删除日程", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuVisible = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun BadgeChip(badge: Badge) {
    val clickableModifier = badge.onClick?.let { click ->
        Modifier.clickable(
            onClickLabel = "打开直播录像",
            onClick = click,
        )
    } ?: Modifier
    Box(
        modifier = Modifier
            .then(clickableModifier)
            .background(badge.color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = badge.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
