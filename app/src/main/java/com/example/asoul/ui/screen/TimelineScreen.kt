package com.example.asoul.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asoul.data.model.FlashLiveEvent
import com.example.asoul.data.model.FlashStatus
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.ScheduleFilter
import com.example.asoul.data.model.Weeks
import com.example.asoul.data.model.matches
import com.example.asoul.ui.components.DayEmptyState
import com.example.asoul.ui.components.FlashLiveCard
import com.example.asoul.ui.components.HeaderBanner
import com.example.asoul.ui.components.SwipeHint
import com.example.asoul.ui.components.SwipeTipOverlay
import com.example.asoul.ui.components.LiveEventCard
import com.example.asoul.ui.components.MemberSelectorRow
import com.example.asoul.ui.components.WeekNavigator
import com.example.asoul.ui.dialog.AboutDialog
import com.example.asoul.ui.dialog.ScheduleDetailDialog
import com.example.asoul.util.BilibiliLauncher
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/**
 * 时间线首页（日历主界面）：
 * Header（渐变横幅）→ MemberSelectorRow（成员头像选择器）→ WeekNavigator（横向日期选择器）
 * → DayScheduleList（按日 Section）。
 *
 * 交互：
 * - 点击成员/团播头像 → 过滤显示该成员的单播 + 参与的团播（原「成员」导航页整合至此）
 * - 点击直播卡片 → 弹出直播详情，可唤起 B 站直播间
 * - 右划内容区 → 回看往日周历，往日已结束直播以「录像」标签展示，
 *   点击标签跳转 B 站直播录像；左划 → 往回切周（仅往日周可用，本周不可左划，
 *   不会超过当前周）；「回到本周」按钮快速返回当前周
 * - 全生命周期仅首次进入 → 展示切周手势引导浮层（持久化记录）；内容区手势提示条，
 *   提示用户左右划可切换周历（本周仅提示右划，上周提示「左划回本周」，
 *   更早的往日周提示「左划回上一周」）
 * - 滚动日程列表 → WeekNavigator 同步高亮对应日期
 * - 点击 WeekNavigator 日期 → 平滑滚动到对应 DaySection
 * - 下拉刷新 → 重新发送获取数据请求（周程表 + 突击直播），顶部展示刷新指示器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    weekStart: LocalDate,
    schedules: List<LiveSchedule>,
    filter: ScheduleFilter,
    /** 突击直播列表（含已结束；未结束条目按日期并入日历对应日期段，不再顶部固定展示）。 */
    flashEvents: List<FlashLiveEvent> = emptyList(),
    /** 下拉刷新进行中（顶部展示刷新指示器）。 */
    isRefreshing: Boolean = false,
    /** 最近一次成功应用的周程表 week_start（服务端数据所属周；示例数据注入时为本周）。 */
    latestAppliedWeekStart: LocalDate? = null,
    /** 当前展示的是开发示例数据（无真实周程表时的兑底）。 */
    isMockData: Boolean = false,
    /** 下拉刷新回调：重新发送获取数据请求（周程表 + 突击直播）。 */
    onRefresh: () -> Unit = {},
    onFilterChange: (ScheduleFilter) -> Unit,
    onShiftWeek: (Long) -> Unit,
    onBackToThisWeek: () -> Unit,
    onAddToCalendar: (LiveSchedule) -> Unit,
    onDelete: (LiveSchedule) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 下拉刷新：包裹周内容，拉取期间展示顶部刷新指示器；
        // 数据回流后仓库 Flow 自动驱动 UI 重组（无需手动刷新列表）
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
        ) {
            // 周切换动画：新周从滑动方向进入 + 淡入淡出（300ms easeOut）
            AnimatedContent(
                targetState = weekStart,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300)))
                    } else {
                        (slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(300)))
                    }
                },
                label = "weekSwitch",
            ) { week ->
                WeekContent(
                    weekStart = week,
                    schedules = schedules,
                    filter = filter,
                    flashEvents = flashEvents,
                    latestAppliedWeekStart = latestAppliedWeekStart,
                    isMockData = isMockData,
                    onFilterChange = onFilterChange,
                    onShiftWeek = onShiftWeek,
                    onAddToCalendar = onAddToCalendar,
                    onDelete = onDelete,
                )
            }
        }

        // 「回到本周」按钮：右划回看往日周历后快速返回当前周
        if (weekStart < Weeks.startOfWeek(Weeks.today())) {
            FilledTonalButton(
                onClick = onBackToThisWeek,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 88.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("回到本周", fontWeight = FontWeight.Bold)
            }
        }

        // 切周手势引导浮层：全生命周期仅弹一次（SharedPreferences 持久化）
        val context = LocalContext.current
        val prefs = remember {
            context.getSharedPreferences(SWIPE_TIP_PREFS, android.content.Context.MODE_PRIVATE)
        }
        var tipShown by remember { mutableStateOf(prefs.getBoolean(KEY_SWIPE_TIP_SHOWN, false)) }
        if (!tipShown) {
            SwipeTipOverlay(isThisWeek = weekStart >= Weeks.startOfWeek(Weeks.today())) {
                prefs.edit().putBoolean(KEY_SWIPE_TIP_SHOWN, true).apply()
                tipShown = true
            }
        }
    }
}

/** 手势引导浮层的持久化存储（全生命周期仅展示一次）。 */
private const val SWIPE_TIP_PREFS = "swipe_tip"
private const val KEY_SWIPE_TIP_SHOWN = "swipe_tip_shown"

/** 单周内容：Header + MemberSelectorRow + WeekNavigator + 按日 Section 列表（突击直播并入对应日期）。 */
@Composable
private fun WeekContent(
    weekStart: LocalDate,
    schedules: List<LiveSchedule>,
    filter: ScheduleFilter,
    /** P10：突击直播列表（含已结束，仅展示未结束条目）。 */
    flashEvents: List<FlashLiveEvent>,
    /** 最近一次成功应用的周程表 week_start（服务端数据所属周）。 */
    latestAppliedWeekStart: LocalDate?,
    /** 当前展示的是开发示例数据。 */
    isMockData: Boolean,
    onFilterChange: (ScheduleFilter) -> Unit,
    onShiftWeek: (Long) -> Unit,
    onAddToCalendar: (LiveSchedule) -> Unit,
    onDelete: (LiveSchedule) -> Unit,
) {
    val days = Weeks.daysOfWeek(weekStart)
    val weekEnd = weekStart.plusDays(6)
    val context = LocalContext.current
    // 「关于」弹窗（Header 右上角「i」入口）
    var showAbout by remember { mutableStateOf(false) }
    // 周程表条目与未结束的突击直播按日合并成时间线行（均按成员/团播过滤）：
    // 突击直播不再固定在日历顶部单独展示，而是落在对应日期段、按开播时间混排。
    val dayRows: Map<LocalDate, List<DayRow>> = remember(weekStart, schedules, filter, flashEvents) {
        val weekDays = Weeks.daysOfWeek(weekStart)
        val schedulesByDay = schedules
            .filter { it.matches(filter) }
            .groupBy { it.date }
        val flashCandidates = flashEvents.filter { event ->
            event.status != FlashStatus.ENDED &&
                event.startTime.toLocalDate() in weekDays &&
                event.matchesFlashFilter(filter)
        }
        weekDays.associateWith { date ->
            val scheduleRows = schedulesByDay[date].orEmpty().map { DayRow.Schedule(it) }
            // 与已有周程表条目同成员且时间差 ≤15 分钟的突击不重复展示（防日程内提前开播的兑底事件占两行）
            val flashRows = flashCandidates
                .filter { event ->
                    event.startTime.toLocalDate() == date &&
                        schedulesByDay[date].orEmpty().none { it.overlapsFlash(event) }
                }
                .map { DayRow.Flash(it) }
            (scheduleRows + flashRows).sortedBy { it.time }
        }
    }
    // 直播详情弹窗（点击卡片唤起）
    var detailSchedule by remember { mutableStateOf<LiveSchedule?>(null) }

    var selectedDate by remember(weekStart) {
        val today = Weeks.today()
        mutableStateOf(if (today in days) today else weekStart)
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val accent = MaterialTheme.colorScheme.primary

    // 滚动列表 → 同步高亮 WeekNavigator 对应日期
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index in days.indices) {
                    selectedDate = days[index]
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 右划回看往日周历；左划往回切周（仅往日周可用，本周不可左划，不会超过当前周）
            .pointerInput(weekStart) {
                var dragX by mutableFloatStateOf(0f)
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragX += dragAmount },
                    onDragEnd = {
                        when {
                            dragX > 120f -> onShiftWeek(-1L)
                            dragX < -120f && weekStart < Weeks.startOfWeek(Weeks.today()) ->
                                onShiftWeek(1L)
                        }
                    },
                    onDragCancel = { dragX = 0f },
                )
            },
    ) {
        // ===== Header（规格 3.1） =====
        // 示例数据徽标仅在本周视图展示：右划回看的往日周可能已是真实归档数据。
        HeaderBanner(
            weekStart = weekStart,
            weekEnd = weekEnd,
            showMockBadge = isMockData && weekStart == Weeks.startOfWeek(Weeks.today()),
            onAboutClick = { showAbout = true },
        )

        // ===== 本周无数据提示：服务端周程表属于往日周（本周周程表尚未发布）=====
        // 直接满屏「休息日」会误导用户以为数据错误/丢失；这里明确告知数据状态与查看路径。
        if (
            weekStart == Weeks.startOfWeek(Weeks.today()) &&
            schedules.isEmpty() &&
            latestAppliedWeekStart != null &&
            latestAppliedWeekStart < weekStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(12.dp),
            ) {
                Text(
                    text = "📢 本周周程表尚未发布",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "服务端最新周程表属于 ${latestAppliedWeekStart.monthValue} 月 " +
                        "${latestAppliedWeekStart.dayOfMonth} 日当周，官方发布本周周程表后会自动同步。" +
                        "右划可回看往期周历与直播录像。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // ===== 成员头像选择行（原「成员」导航页整合至此） =====
        MemberSelectorRow(
            filter = filter,
            onFilterChange = onFilterChange,
        )

        // ===== 切周手势提示条（文案随周距自适应：本周 / 上周 / 更早） =====
        SwipeHint(weekStart = weekStart)

        // ===== WeekNavigator（规格 3.2，横向可滚动，避免窄屏挤压） =====
        WeekNavigator(
            weekStart = weekStart,
            selectedDate = selectedDate,
            accentColor = accent,
            onDateSelected = { date ->
                selectedDate = date
                val index = days.indexOf(date)
                if (index >= 0) {
                    scope.launch { listState.animateScrollToItem(index) }
                }
            },
        )

        // ===== DayScheduleList（规格 3.3 / 3.4） =====
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            days.forEachIndexed { index, date ->
                item(key = index) {
                    DaySection(
                        date = date,
                        rows = dayRows[date].orEmpty(),
                        accentColor = accent,
                        isSelected = date == selectedDate,
                        onAddToCalendar = onAddToCalendar,
                        onDelete = onDelete,
                        onCardClick = { detailSchedule = it },
                    )
                }
            }
            // 数据来源鸣谢（额外数据源：枝江站 asoul.love 日历订阅；点击可打开站点）
            item(key = "attribution") {
                Text(
                    text = "特别鸣谢枝江站 https://asoul.love 提供的额外数据支持",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { BilibiliLauncher.openUrl(context, "https://asoul.love") }
                        .padding(top = 12.dp, bottom = 4.dp),
                )
                Spacer(Modifier.height(96.dp))
            }
        }
    }

    // ===== 直播详情弹窗 =====
    detailSchedule?.let { schedule ->
        ScheduleDetailDialog(
            schedule = schedule,
            onDismiss = { detailSchedule = null },
        )
    }

    // ===== 关于弹窗（版本 / 数据来源 / 鸣谢） =====
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

/**
 * 每日区块（UI 规格 3.3）：
 * 日期标题行（竖条 + "M.dd 星期X"）+ 事件卡片列表 / 空状态。
 *
 * [rows] 为周程表条目与未结束突击直播的混合行（已按时间排序）：
 * 周程表条目用 [LiveEventCard]，突击直播用 [FlashLiveCard]（内嵌模式，显示 HH:mm + 状态）。
 */
@Composable
private fun DaySection(
    date: LocalDate,
    rows: List<DayRow>,
    accentColor: Color,
    isSelected: Boolean,
    onAddToCalendar: (LiveSchedule) -> Unit,
    onDelete: (LiveSchedule) -> Unit,
    onCardClick: (LiveSchedule) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        // 日期标题行
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(
                        if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(2.dp),
                    ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${date.monthValue}.${"%02d".format(date.dayOfMonth)}  ${Weeks.weekdayLabel(date)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))

        if (rows.isEmpty()) {
            // 休息日空状态（规格 3.3）
            DayEmptyState(
                emoji = "\uD83D\uDCA4",
                title = "休息日",
                subtitle = "今天没有直播安排哦~",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    when (row) {
                        is DayRow.Schedule -> LiveEventCard(
                            schedule = row.schedule,
                            onAddToCalendar = { onAddToCalendar(row.schedule) },
                            onDelete = { onDelete(row.schedule) },
                            onCardClick = onCardClick,
                        )
                        is DayRow.Flash -> FlashLiveCard(
                            event = row.event,
                            embeddedInCalendar = true,
                        )
                    }
                }
            }
        }
    }
}

/** 日历某日的一行：周程表条目或突击直播条目（按开播时间混排）。 */
private sealed interface DayRow {
    /** 行内排序用的开播时间。 */
    val time: LocalTime

    data class Schedule(val schedule: LiveSchedule) : DayRow {
        override val time: LocalTime get() = schedule.time
    }

    data class Flash(val event: FlashLiveEvent) : DayRow {
        override val time: LocalTime get() = event.startTime.toLocalTime()
    }
}

/** 突击直播是否匹配当前筛选（突击为单人场：仅「全部」或选中该成员时展示）。 */
private fun FlashLiveEvent.matchesFlashFilter(filter: ScheduleFilter): Boolean = when (filter) {
    ScheduleFilter.All -> true
    is ScheduleFilter.MemberFilter -> member?.id?.let { it in filter.memberIds } == true
    // 组合筛选针对「同台多人场」，突击（单人）不属于任何组合场次
    is ScheduleFilter.ComboFilter -> false
    is ScheduleFilter.GroupFilter -> false
}

/**
 * 周程表条目是否与突击直播重叠（同成员且开播时间差 ≤15 分钟视为同一场）。
 * 未知成员（直播间兜底）事件与团播条目时段重叠时也视为重复，避免双行。
 */
private fun LiveSchedule.overlapsFlash(event: FlashLiveEvent): Boolean {
    val diffMinutes = Duration.between(event.startTime.toLocalTime(), time).abs().toMinutes()
    if (diffMinutes > 15) return false
    val flashMemberId = event.member?.id
    return if (flashMemberId != null) {
        resolvedParticipantIds().contains(flashMemberId)
    } else {
        isMultiLive
    }
}
