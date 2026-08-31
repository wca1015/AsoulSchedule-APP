package com.example.asoul.data.remote

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.asoul.AsoulApplication
import com.example.asoul.data.FlashScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * P6 轮询调度：应用前台期间的进程内轮询。
 *
 * 设计文档要求：
 * - 周程表 `/latest.json`：每小时轮询一次（[POLL_LATEST_INTERVAL]）
 * - 突击直播 `/flash.json`：每 5 分钟轮询一次（[POLL_FLASH_INTERVAL]）
 * - App 回前台（ProcessLifecycleOwner ON_START）时立即拉取一次 flash，并顺带拉取一次 latest
 * - App 退后台（ON_STOP）时停止轮询，省电省流量
 *
 * 轮询跑在进程级 [CoroutineScope]（[SupervisorJob] + [Dispatchers.IO]），
 * 不依赖任何 Activity/Fragment 生命周期。初始化入口：[AsoulApplication.onCreate]。
 */
class ScheduleSyncManager(
    private val flashRepository: FlashScheduleRepository,
    private val latestFetcher: LatestScheduleFetcher,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var latestPollingJob: Job? = null
    private var flashPollingJob: Job? = null

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> onForeground()
            Lifecycle.Event.ON_STOP -> onBackground()
            else -> Unit
        }
    }

    /** 在 Application.onCreate 中调用：注册前后台监听（回前台即触发拉取与轮询）。 */
    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }

    /** App 回前台：立即拉取 flash + 顺带拉取一次 latest，并启动周期轮询。 */
    private fun onForeground() {
        Log.d(TAG, "回前台：立即拉取 flash + latest")
        scope.launch { runSafely("flash 即时拉取") { flashRepository.fetchLatestFlash() } }
        scope.launch { runSafely("latest 即时拉取") { latestFetcher.fetchAndApply() } }
        startPolling()
    }

    /** App 退后台：停止轮询。 */
    private fun onBackground() {
        latestPollingJob?.cancel()
        flashPollingJob?.cancel()
        latestPollingJob = null
        flashPollingJob = null
    }

    /** 启动两条轮询协程（幂等：已在运行时不重复启动）。 */
    private fun startPolling() {
        if (latestPollingJob?.isActive != true) {
            latestPollingJob = scope.launch {
                pollLoop(POLL_LATEST_INTERVAL) { latestFetcher.fetchAndApply() }
            }
        }
        if (flashPollingJob?.isActive != true) {
            flashPollingJob = scope.launch {
                pollLoop(POLL_FLASH_INTERVAL) { flashRepository.fetchLatestFlash() }
            }
        }
    }

    /** 通用轮询循环：先等一个间隔再拉取（回前台的即时拉取已覆盖首次）。 */
    private suspend fun pollLoop(interval: Duration, block: suspend () -> Unit) {
        while (true) {
            delay(interval)
            runSafely("轮询($interval)") { block() }
        }
    }

    /** 执行挂起块并吞掉异常（轮询/即时拉取不因单次失败中断）。 */
    private suspend fun runSafely(tag: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "$tag 异常: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "ScheduleSyncManager"
    }
}

/** 轮询间隔常量（设计文档：周程表每小时、突击直播每 5 分钟）。 */
internal val POLL_LATEST_INTERVAL: Duration = 1.hours
internal val POLL_FLASH_INTERVAL: Duration = 5.minutes
