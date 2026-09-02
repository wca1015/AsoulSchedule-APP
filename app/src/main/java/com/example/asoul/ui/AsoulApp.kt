package com.example.asoul.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.asoul.ui.dialog.AppUpdateDialog
import com.example.asoul.ui.screen.TimelineScreen

/**
 * 应用根组合函数：日历主界面（时间线）+ Snackbar。
 *
 * 原底部导航栏已移除——「成员」页整合为日历主界面顶部的成员头像选择行，
 * 点击头像即可过滤显示对应成员的单播与团播。
 */
@Composable
fun AsoulApp(mainViewModel: MainViewModel = viewModel()) {
    val state by mainViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar 提示
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            mainViewModel.consumeSnackbar()
        }
    }

    // App 更新弹窗：检测到新版本时展示（立即更新 / 跳过此版本 / 稍后）
    state.pendingUpdate?.let { update ->
        AppUpdateDialog(
            update = update,
            downloading = state.updateDownloading,
            onUpdate = mainViewModel::updateNow,
            onSkipThisVersion = mainViewModel::skipThisVersion,
            onDismiss = mainViewModel::dismissUpdate,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TimelineScreen(
                weekStart = state.weekStart,
                schedules = state.weekSchedules,
                filter = state.scheduleFilter,
                flashEvents = state.flashEvents,
                isRefreshing = state.isRefreshing,
                latestAppliedWeekStart = state.latestAppliedWeekStart,
                isMockData = state.isMockData,
                onRefresh = mainViewModel::refreshData,
                onFilterChange = mainViewModel::setScheduleFilter,
                onShiftWeek = mainViewModel::shiftWeek,
                onBackToThisWeek = mainViewModel::backToThisWeek,
                onAddToCalendar = { schedule ->
                    val ok = mainViewModel.writeToCalendar(schedule, reminderMinutes = 30)
                    mainViewModel.showSnackbar(
                        if (ok) "已写入系统日历" else "写入日历失败，请检查日历权限",
                    )
                },
                onDelete = { mainViewModel.removeSchedule(it.id) },
            )
        }
    }
}
