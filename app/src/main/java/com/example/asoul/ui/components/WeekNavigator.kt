package com.example.asoul.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asoul.data.model.Weeks
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMAT = DateTimeFormatter.ofPattern("MM.dd")

/**
 * 横向日期选择器（UI 规格 3.2）：
 * 72dp 高，单项 56dp，选中态成员色底 + 白色文字 + 圆角 12。
 * 上行 "MM.dd"（14sp bold），下行 "星期X"（11sp）。
 *
 * 窄屏适配：使用 LazyRow 横向滚动，避免 7 个日期被挤压形变。
 * 选中日期变化时自动滚动到可见区域。
 */
@Composable
fun WeekNavigator(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    /** 选中日对应的强调色（默认成员/主题色）。 */
    accentColor: Color,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = Weeks.daysOfWeek(weekStart)
    val selectedIndex = days.indexOf(selectedDate)
    val listState = rememberLazyListState()

    // 选中项自动滚到居中可见
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(days.size) { index ->
                val date = days[index]
                val isSelected = date == selectedDate
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor else Color.Transparent,
                    animationSpec = tween(200),
                    label = "navBg",
                )
                val fgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(200),
                    label = "navFg",
                )
                Column(
                    modifier = Modifier
                        .width(56.dp)
                        .height(64.dp)
                        .background(bgColor, RoundedCornerShape(12.dp))
                        .clickable { onDateSelected(date) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = date.format(DATE_FORMAT),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = fgColor,
                    )
                    Text(
                        text = Weeks.weekdayLabel(date),
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.7f) else fgColor,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
