package com.example.asoul.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asoul.data.model.Weeks
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 切周手势提示条：提示用户左右划可切换周历。文案随当前所处周自适应：
 *
 * - 本周：仅提示「右划看往期」（本周不可左划）
 * - 上周（距今 1 周）：「左划回本周 · 右划看更早」（左划一次即回本周）
 * - 更早的往日周：「左划回上一周 · 右划看更早」
 * - 最早可回看周（距今 [maxPastWeeks] 周）：「左划回上一周」（已到边界，不再提示右划）
 *
 * 提示条展示数秒后自动淡出，避免长期占用视觉空间。
 */
@Composable
fun SwipeHint(
    weekStart: LocalDate,
    modifier: Modifier = Modifier,
    /** 可回看的最大周数（到达边界时不再提示右划）。 */
    maxPastWeeks: Int = 4,
) {
    var visible by remember { mutableStateOf(true) }
    val weeksAgo = ChronoUnit.WEEKS.between(weekStart, Weeks.startOfWeek(Weeks.today()))

    LaunchedEffect(Unit) {
        delay(6_000L)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                // 本周：只能右划看往期（本周不可左划）
                weeksAgo <= 0L -> {
                    Chevron(Icons.Default.ChevronRight)
                    Text(
                        text = "右划回看往期周历，看看之前的直播录像吧",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                // 上周：左划一次即回本周，右划继续看更早（未到边界）
                (weeksAgo == 1L) && (weeksAgo < maxPastWeeks.toLong()) ->
                    BidirectionalHint(leftText = "左划回本周", showRight = true)
                // 最早可回看周：已到边界，只提示左划返回，不再提示右划看更早。
                weeksAgo >= maxPastWeeks.toLong() ->
                    BidirectionalHint(leftText = "左划回上一周", showRight = false)
                // 中间往日周：左划回上一周，右划看更早（尚未到边界）
                else -> BidirectionalHint(leftText = "左划回上一周", showRight = true)
            }
        }
    }
}

/** 双向提示：‹ 左划文案 [· 右划看更早 ›]；[showRight] 为 false 时只保留左划部分。 */
@Composable
private fun BidirectionalHint(leftText: String, showRight: Boolean = true) {
    Chevron(Icons.Default.ChevronLeft)
    Text(
        text = leftText,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary,
    )
    if (!showRight) return
    Spacer(Modifier.width(12.dp))
    Text(
        text = "·",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    )
    Spacer(Modifier.width(12.dp))
    Text(
        text = "右划看更早",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary,
    )
    Chevron(Icons.Default.ChevronRight)
}

/** 小箭头图标（左右各留 2dp 间距）。 */
@Composable
private fun Chevron(icon: ImageVector) {
    Spacer(Modifier.width(2.dp))
    Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp),
    )
    Spacer(Modifier.width(2.dp))
}

/**
 * 首次启动的切周手势引导浮层：
 * 半透明遮罩 + 居中说明卡片，5 秒后自动淡出，点击任意位置立即关闭。
 *
 * 展示一次后即由调用方持久化标记，后续启动不再弹出。
 */
@Composable
fun SwipeTipOverlay(isThisWeek: Boolean, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(5_000L)
        visible = false
        // 淡出动画结束后回调隐藏
        delay(400L)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 28.dp, vertical = 24.dp),
            ) {
                Icon(
                    Icons.Default.Swipe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "左右划动切换周历",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isThisWeek) {
                        "右划回看往期周历，已结束直播会展示可点击的「录像」标签"
                    } else {
                        "右划看更早的周历，左划逐周回到现在，也可以点右下角按钮直达本周"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "轻点任意处关闭",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
