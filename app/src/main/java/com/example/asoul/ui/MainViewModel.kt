package com.example.asoul.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.asoul.AsoulApplication
import com.example.asoul.data.remote.dto.AppVersionDto
import com.example.asoul.data.MockScheduleData
import com.example.asoul.data.model.FlashLiveEvent
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.Member
import com.example.asoul.data.model.MemberCatalog
import com.example.asoul.data.model.ScheduleFilter
import com.example.asoul.data.model.WeekScheduleStatus
import com.example.asoul.data.model.Weeks
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** UI 顶层状态。 */
data class MainUiState(
    val weekStart: LocalDate = Weeks.startOfWeek(Weeks.today()),
    val weekSchedules: List<LiveSchedule> = emptyList(),
    val weekStatus: WeekScheduleStatus? = null,
    val members: List<Member> = MemberCatalog.ALL,
    /** 日历主界面成员/团播过滤器（原「成员」底部导航页整合至此）。 */
    val scheduleFilter: ScheduleFilter = ScheduleFilter.All,
    /** P10：突击直播列表（含已结束；UI 层仅展示未结束条目）。 */
    val flashEvents: List<FlashLiveEvent> = emptyList(),
    val snackbarMessage: String? = null,
    /** 下拉刷新进行中（并发拉取周程表 + 突击直播）。 */
    val isRefreshing: Boolean = false,
    /**
     * 最近一次成功应用的周程表 week_start（服务端数据所属周）。
     * 本周无数据且数据属于往日周时，UI 据此提示「本周周程表尚未发布」而非满屏休息日。
     */
    val latestAppliedWeekStart: LocalDate? = null,
    /** 本周是否已注入开发示例数据（UI 展示「示例数据」徽标，便于与真实数据区分）。 */
    val isMockData: Boolean = false,
    /** 待提示的 App 更新信息（发现新版本时非空，驱动更新弹窗）。 */
    val pendingUpdate: AppVersionDto? = null,
    /** 更新 APK 是否正在下载。 */
    val updateDownloading: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app: AsoulApplication = application as AsoulApplication
    private val repository get() = app.repository

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    /** 已推送过 Snackbar 提醒的突击直播 id（避免重复提醒）。 */
    private val seenFlashIds = mutableSetOf<String>()

    /** 是否为首次收到突击直播数据（首次视为基线，不弹 Snackbar）。 */
    private var flashBaseline = true

    /** 启动引导是否已完成（缓存加载 + 首拉 + 示例数据兑底），防重入。 */
    private var bootstrapStarted = false

    /**
     * 可回看的最早一周：当前周往前 4 周。
     * 与服务端录播回填扫描范围（archive_weeks: 4）对齐，
     * 超出范围的周服务端无归档、也无录播，故客户端直接限制入口。
     */
    private val oldestViewableWeek: LocalDate
        get() = Weeks.startOfWeek(Weeks.today()).minusWeeks(MAX_PAST_WEEKS.toLong())

    init {
        // 订阅仓库，周数据随增删自动刷新
        viewModelScope.launch {
            repository.schedules.collect { refreshWeek() }
        }
        viewModelScope.launch {
            repository.weekStatus.collect { refreshWeek() }
        }
        // P10：订阅突击直播仓库；新出现的突击直播（id 不在已见集合）弹 Snackbar 提醒。
        // 首个非空数据（来自缓存/启动加载）作为基线：只登记不打扰用户，
        // 之后轮询到的新 id 才提示。
        viewModelScope.launch {
            app.flashRepository.events.collect { events ->
                _state.value = _state.value.copy(flashEvents = events)
                if (events.isEmpty()) return@collect
                if (flashBaseline) {
                    flashBaseline = false
                    seenFlashIds += events.map { it.id }
                } else {
                    events.filter { it.id !in seenFlashIds }.forEach { fresh ->
                        seenFlashIds += fresh.id
                        val name = fresh.member?.name ?: "神秘成员"
                        showSnackbar("\uD83D\uDD34 $name 突击直播：${fresh.title}")
                    }
                }
            }
        }
        // P6：启动引导（缓存优先 → 首拉 → 示例数据兜底，全程串行、无竞态）。
        // 旧实现中示例数据注入与网络拉取并发赛跑：两者谁先完成决定用户看到示例假数据还是空白，
        // 不同设备表现不一致。现收拢为单一引导协程，保证结果确定。
        viewModelScope.launch { bootstrap() }
        // App 更新检查：缓存引导完成后静默执行，不阻塞首屏；发现新版本则弹窗提示。
        // 检查器内部已做「版本 > 本地 / 未跳过 / 当日未提示」三重过滤。
        viewModelScope.launch {
            app.cacheBootstrap.await()
            val update = runCatching { app.appUpdateChecker.check() }.getOrNull()
            if (update != null) {
                _state.value = _state.value.copy(pendingUpdate = update)
            }
        }    }

    /**
     * 启动引导：等缓存加载完成 → 首拉一次周程表 → 完全无数据时才注入示例数据。
     *
     * 关键点：
     * - 首拉与示例数据注入串行，彻底消除两者竞态（旧版不同客户端表现不一致的根源）；
     * - 只要存在任何真实数据（缓存或首拉成功），就不注入示例数据：
     *   本周无数据时由 UI 展示「本周周程表尚未发布」提示并引导右划回看往期，
     *   避免把示例假数据混入真实场景（旧版「显示错误」的根源）；
     * - 仅断网且无缓存（仓库整体为空）时注入示例数据兜底，并打上「示例数据」徽标。
     */
    private suspend fun bootstrap() {
        if (bootstrapStarted) return
        bootstrapStarted = true
        // 缓存优先：Application.onCreate 已异步加载本地缓存，等其完成再继续。
        app.cacheBootstrap.await()
        // 拉取最新周程表；失败静默（断网/无网络由后续示例数据兜底）。
        runCatching { app.latestFetcher.fetchAndApply() }
        if (repository.schedules.value.isEmpty()) {
            injectMockData(Weeks.startOfWeek(Weeks.today()))
        }
        refreshWeek()
    }

    /** 注入开发示例数据（断网且无缓存时的兜底，基于官方 08.17-08.23 海报）。 */
    private fun injectMockData(thisWeek: LocalDate) {
        val mock = MockScheduleData.currentWeekMock()
        repository.addAll(mock)
        repository.markWeekRecognized(thisWeek, mock.size)
        // 往日周（近两周）：右划回看时可看到已结束直播及其录像标签。
        for (weeksAgo in 1L..2L) {
            val pastWeek = thisWeek.minusWeeks(weeksAgo)
            val pastMock = MockScheduleData.pastWeekMock(weeksAgo)
            repository.addAll(pastMock)
            repository.markWeekRecognized(pastWeek, pastMock.size)
        }
        _state.value = _state.value.copy(isMockData = true)
    }

    // ===== 周导航 =====

    fun shiftWeek(deltaWeeks: Long) {
        val newStart = _state.value.weekStart.plusWeeks(deltaWeeks)
        // 往日周历最多回看 4 周（与服务端录播扫描范围一致），超界钳制到边界周；
        // 未来方向不限制（周程表提前发布时可预览下周）。
        val clamped = newStart.coerceAtLeast(oldestViewableWeek)
        if (clamped == _state.value.weekStart) {
            if (deltaWeeks < 0) {
                // 已在最早可回看周，继续右划时给用户明确反馈而非无响应。
                showSnackbar("最多回看过去 $MAX_PAST_WEEKS 周的周历")
            }
            return
        }
        _state.value = _state.value.copy(weekStart = clamped)
        refreshWeek()
        loadPastWeekIfNeeded(clamped)
    }

    /** 返回当前周（左划回看往日周历后，通过「回到本周」按钮调用）。 */
    fun backToThisWeek() {
        _state.value = _state.value.copy(weekStart = Weeks.startOfWeek(Weeks.today()))
        refreshWeek()
    }

    /**
     * 进入往日周时按需拉取该周归档 `week/{week_start}.json`（含服务端录播回填），
     * 保证往日周界面无需手动下拉即可展示日程与「录像」标签。
     * 已有更新版本时拉取器内部会按周比对版本自动跳过，重复进入开销很低。
     */
    private fun loadPastWeekIfNeeded(weekStart: LocalDate) {
        if (weekStart >= Weeks.startOfWeek(Weeks.today())) return
        viewModelScope.launch {
            runCatching { app.latestFetcher.fetchWeekAndApply(weekStart) }
        }
    }

    private fun refreshWeek() {
        val weekStart = _state.value.weekStart
        val thisWeek = Weeks.startOfWeek(Weeks.today())
        val realApplied = app.latestFetcher.lastAppliedWeekStart
        // 示例数据标记：注入后持续生效，直到服务端本周真实周程表被应用后自动失效。
        val mockActive = _state.value.isMockData && (realApplied == null || realApplied < thisWeek)
        _state.value = _state.value.copy(
            weekSchedules = repository.schedulesForWeek(weekStart),
            weekStatus = repository.statusForWeek(weekStart),
            // 真实数据所属周优先；仅示例数据兜底（无任何真实数据）时以本周占位，
            // 避免误展示「本周周程表尚未发布」提示。
            latestAppliedWeekStart = realApplied ?: if (mockActive) thisWeek else null,
            isMockData = mockActive,
        )
    }

    // ===== 成员/团播过滤（日历主界面头像选择） =====

    fun setScheduleFilter(filter: ScheduleFilter) {
        _state.value = _state.value.copy(scheduleFilter = filter)
    }

    // ===== 删除日程 =====

    fun removeSchedule(id: Long) {
        viewModelScope.launch {
            // 若已写入系统日历，一并删除
            _state.value.weekSchedules.firstOrNull { it.id == id }
                ?.calendarEventId
                ?.let { app.calendarWriter.deleteEvent(it) }
            repository.removeSchedule(id)
        }
    }

    // ===== 写入系统日历 =====

    /** @return 是否写入成功。 */
    fun writeToCalendar(schedule: LiveSchedule, reminderMinutes: Int): Boolean {
        val eventId = app.calendarWriter.insertEvent(schedule, reminderMinutes = reminderMinutes)
        return if (eventId != null) {
            repository.markCalendarEvent(schedule.id, eventId)
            true
        } else {
            false
        }
    }

    // ===== App 更新 =====

    /** 用户选择「跳过此版本」：记录该版本，之后不再提示。 */
    fun skipThisVersion() {
        val version = _state.value.pendingUpdate?.versionCode ?: return
        app.appUpdateStore.markSkipped(version)
        _state.value = _state.value.copy(pendingUpdate = null)
    }

    /** 关闭更新弹窗（稍后再说）：今日不再打扰，明天重新提示。 */
    fun dismissUpdate() {
        _state.value = _state.value.copy(pendingUpdate = null)
    }

    /** 用户选择「立即更新」：下载 APK 并调起系统安装页。 */
    fun updateNow() {
        val update = _state.value.pendingUpdate ?: return
        val updater = app.appUpdater
        // 先确认「安装未知应用」权限：缺失时引导去系统设置，不进入下载流程
        if (!updater.canInstallUnknownApps()) {
            updater.openInstallPermissionSettings()
            showSnackbar("请在系统设置中允许「安装未知应用」后，再点立即更新")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(updateDownloading = true)
            showSnackbar("正在下载新版本…")
            val file = updater.download(update.apkUrl)
            _state.value = _state.value.copy(updateDownloading = false)
            if (file == null) {
                showSnackbar("下载失败，请检查网络后重试")
                return@launch
            }
            if (updater.install(file)) {
                // 调起系统安装页后关闭弹窗（安装完成由系统引导，App 无法感知）
                _state.value = _state.value.copy(pendingUpdate = null)
            } else {
                // 下载完成但权限在下载期间被撤销等极端情况：再引导一次
                updater.openInstallPermissionSettings()
                showSnackbar("请在系统设置中允许「安装未知应用」后，再点立即更新")
            }
        }
    }

    // ===== 下拉刷新 =====

    /**
     * 下拉刷新：并发重新发送获取数据请求，按当前显示的周分流——
     * - 本周：拉取 `/latest.json`（周程表，强制应用）
     * - 往日周：按需拉取 `week/{week_start}.json` 归档（含服务端录播回填；
     *   服务端未发布该周文件时 404 静默，保持现状）
     * 同时拉取突击直播 `/flash.json`。拉取完成（无论成功或失败）后关闭刷新指示器；
     * 数据若有更新，会经由仓库 Flow（schedules / flashEvents）自动回流到 UI。
     */
    fun refreshData() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            val weekStart = _state.value.weekStart
            val scheduleJob = if (weekStart >= Weeks.startOfWeek(Weeks.today())) {
                // force = true：绕过版本号比对，确保版本基线异常时用户仍可自救。
                async { runCatching { app.latestFetcher.fetchAndApply(force = true) } }
            } else {
                async { runCatching { app.latestFetcher.fetchWeekAndApply(weekStart) } }
            }
            val flashJob = async { runCatching { app.flashRepository.fetchLatestFlash() } }
            scheduleJob.await()
            flashJob.await()
            _state.value = _state.value.copy(isRefreshing = false)
            refreshWeek()
        }
    }

    // ===== Snackbar =====

    fun showSnackbar(message: String) {
        _state.value = _state.value.copy(snackbarMessage = message)
    }

    fun consumeSnackbar() {
        _state.value = _state.value.copy(snackbarMessage = null)
    }

    private companion object {
        /** 往日周历可回看的最大周数（与服务端录播回填扫描范围对齐）。 */
        const val MAX_PAST_WEEKS = 4
    }
}
