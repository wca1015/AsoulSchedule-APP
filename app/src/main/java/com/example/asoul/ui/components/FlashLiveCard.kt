package com.example.asoul.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asoul.data.model.FlashLiveEvent
import com.example.asoul.data.model.FlashStatus
import com.example.asoul.data.model.Weeks
import com.example.asoul.util.BilibiliLauncher
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 突击直播状态标签配色（规格：待开播黄 / 直播中红 / 已结束灰）。 */
private val STATUS_UPCOMING = Color(0xFFF59E0B)
private val STATUS_LIVE = Color(0xFFEF4444)
private val STATUS_ENDED = Color(0xFF9CA3AF)

/**
 * P10 突击直播卡片：成员应援色描边 + 「突击」徽标。
 *
 * 展示：成员头像/名字、标题、开播时间（"今晚 19:00" 或 "M月d日 HH:mm"）、
 * 状态标签（待开播/直播中/已结束，直播中带呼吸动效）。
 * `auto_published == true` 时额外展示「⚠️待确认」徽标（设计文档明确要求）。
 *
 * 点击卡片 → 打开 source_url（B 站客户端 scheme 优先，网页兜底）。
 */
@Composable
fun FlashLiveCard(
    event: FlashLiveEvent,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val accent = event.member?.color ?: Color(0xFF8E7CC3)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .clickable { BilibiliLauncher.openDynamic(context, event.sourceUrl) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 成员头像（默认 56dp；未识别成员时用「⚡」占位）
        val member = event.member
        if (member != null) {
            MemberAvatar(member = member)
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("\u26A1", fontSize = 20.sp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member?.name ?: "未知成员",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = accent,
                )
                Spacer(Modifier.width(6.dp))
                Badge(text = "突击", background = accent)
                if (event.autoPublished) {
                    Spacer(Modifier.width(4.dp))
                    // 自动发布条目需人工确认（设计文档明确要求展示）
                    Badge(text = "⚠️待确认", background = Color(0xFFDC2626))
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = event.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!event.desc.isNullOrBlank()) {
                Text(
                    text = event.desc,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatStartTime(event.startTime),
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(status = event.status)
            }
        }
    }
}

/** 状态徽标：文字 + 底色小标签。 */
@Composable
private fun Badge(text: String, background: Color) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** 状态标签（直播中带呼吸动效）。 */
@Composable
private fun StatusChip(status: FlashStatus) {
    val (color, label) = when (status) {
        FlashStatus.UPCOMING -> STATUS_UPCOMING to FlashStatus.UPCOMING.label
        FlashStatus.LIVE -> STATUS_LIVE to FlashStatus.LIVE.label
        FlashStatus.ENDED -> STATUS_ENDED to FlashStatus.ENDED.label
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        // 直播中：红点呼吸动效
        if (status == FlashStatus.LIVE) {
            val transition = rememberInfiniteTransition(label = "flashBreath")
            val alpha by transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "flashBreathAlpha",
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 开播时间文案：今天 → "今晚 19:00"；其他日期 → "8月23日 19:00"。 */
private fun formatStartTime(start: LocalDateTime): String =
    if (start.toLocalDate() == Weeks.today()) {
        "今晚 ${start.format(TIME_FORMAT)}"
    } else {
        "${start.monthValue}月${start.dayOfMonth}日 ${start.format(TIME_FORMAT)}"
    }
