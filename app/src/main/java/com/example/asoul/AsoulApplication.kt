package com.example.asoul

import android.app.Application
import com.example.asoul.calendar.CalendarWriter
import com.example.asoul.data.FlashScheduleRepository
import com.example.asoul.data.ScheduleCacheStore
import com.example.asoul.data.ScheduleRepository
import com.example.asoul.data.remote.LatestScheduleFetcher
import com.example.asoul.data.remote.ScheduleApiClient
import com.example.asoul.data.remote.ScheduleSyncManager
import com.example.asoul.ocr.FakeOcrEngine
import com.example.asoul.ocr.OcrEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口，持有单例依赖（P0 无 DI 框架，保持简洁；P2 可迁 Hilt）。
 *
 * P6 新增：网络层（OkHttp）+ 文件缓存 + 突击直播仓库 + 前台轮询调度。
 */
class AsoulApplication : Application() {

    val repository: ScheduleRepository by lazy { ScheduleRepository() }

    /** 当前 OCR 引擎。P0 使用离线演示引擎，P1 切换 MLKit。 */
    var ocrEngine: OcrEngine = FakeOcrEngine()

    val calendarWriter: CalendarWriter by lazy { CalendarWriter(this) }

    // ===== P6：网络层 / 缓存 / 仓库（无 DI，按依赖顺序手写装配） =====

    /** 静态 JSON 文件缓存（filesDir + SharedPreferences 版本号）。 */
    val cacheStore: ScheduleCacheStore by lazy { ScheduleCacheStore(this) }

    /** OkHttp GET 封装（超时 10s，失败静默）。 */
    private val apiClient: ScheduleApiClient by lazy { ScheduleApiClient() }

    /** 突击直播仓库（拉取 → 版本比对 → 更新缓存 → emit）。 */
    val flashRepository: FlashScheduleRepository by lazy {
        FlashScheduleRepository(apiClient, cacheStore)
    }

    /** 周程表拉取器（拉取 → 版本比对 → 按周替换仓库日程）。 */
    val latestFetcher: LatestScheduleFetcher by lazy {
        LatestScheduleFetcher(apiClient, cacheStore, repository)
    }

    /** 前台轮询调度（周程表 1h / 突击直播 5min，回前台即时拉取）。 */
    private val syncManager: ScheduleSyncManager by lazy {
        ScheduleSyncManager(flashRepository, latestFetcher)
    }

    /** 进程级作用域：启动时加载缓存等一次性任务。 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 缓存引导完成信号：[AsoulApplication.onCreate] 中缓存加载完毕后 complete，
     * MainViewModel 等待它再判断「仓库为空 → 注入 Mock」，避免 Mock 抢先覆盖缓存数据。
     */
    val cacheBootstrap = CompletableDeferred<Unit>()

    override fun onCreate() {
        super.onCreate()
        // P6：启动时先用本地缓存填充仓库（缓存优先），供断网兜底；
        // MainViewModel 侧再判断仓库为空时注入 Mock。
        appScope.launch {
            runCatching {
                latestFetcher.loadFromCache()
                flashRepository.loadFromCache()
            }
            cacheBootstrap.complete(Unit)
        }
        // 注册前后台监听，回前台即拉取 + 周期轮询
        syncManager.start()
    }
}
