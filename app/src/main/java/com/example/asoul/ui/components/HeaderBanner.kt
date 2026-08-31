package com.example.asoul.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import com.example.asoul.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asoul.ui.theme.AsoulColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val RANGE_FORMAT = DateTimeFormatter.ofPattern("MM.dd")

/**
 * 顶部渐变 Header（UI 规格 3.1）：
 * - 135° 三段渐变：#6C5CE7 → #A29BFE → #FD79A8
 * - "CALENDAR" 48sp w900 白色大标题 + 阴影
 * - 副标题「枝江娱乐直播日历」+ 日期范围（枝江强调黄）
 * - 左上角斜条带装饰 "Z.J. ENTERTAINMENT"
 */
@Composable
fun HeaderBanner(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    modifier: Modifier = Modifier,
    /** 当前展示的是开发示例数据时为 true，头部展示「示例数据」徽标提醒用户。 */
    showMockBadge: Boolean = false,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            AsoulColors.HeaderGradientStart,
            AsoulColors.HeaderGradientMid,
            AsoulColors.HeaderGradientEnd,
        ),
        start = Offset.Zero,
        end = Offset(1000f, 600f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(gradient)
            .drawBehind {
                // 斜条带装饰（左上角，-0.5 rad 旋转的白色半透明条带）
                rotate(degrees = -18f, pivot = Offset(0f, 40f)) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.18f),
                        topLeft = Offset(-60f, 18f),
                        size = androidx.compose.ui.geometry.Size(400f, 22f),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(
                text = "CALENDAR",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(blurRadius = 8f, color = Color.Black.copy(alpha = 0.26f)),
                ),
            )
            Text(
                text = "枝江娱乐直播日历",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(
                text = "${weekStart.format(RANGE_FORMAT)} - ${weekEnd.format(RANGE_FORMAT)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AsoulColors.ZhijiangAccent,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        // 条带文字（跟随条带装饰）
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Z.J. ENTERTAINMENT",
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = Color.White,
            )
            // 示例数据徽标：拉不到真实周程表时的开发兜底数据，明确告知用户所见非真实日程。
            if (showMockBadge) {
                Text(
                    text = "示例数据",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        // 右上角枝江娱乐 Logo
        Image(
            painter = painterResource(R.drawable.logo_zhijiang),
            contentDescription = "枝江娱乐",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 10.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
        )
    }
}
