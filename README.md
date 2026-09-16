# Asoul · 枝江日历

一个面向 **A-SOUL** 粉丝的直播周程表日历 App：查看每周直播安排、突击直播自动并入日历、左右划切周回看往期直播录像、按成员/团播（含一期双人组合）过滤、一键写入系统日历、唤起 B 站直播间，并支持 App 内检查更新；同时为「周程表海报 OCR 自动识别」预留了完整的解析管线。

> 应用名：`Asoul` ｜ 包名：`com.example.asoul` ｜ 当前版本：`1.5 (versionCode 6)` ｜ 数据源：同仓库 [`Server/`](Server/README.md) 产出的静态 JSON（阿里云 OSS）

---

## ✨ 核心功能

- **周程表时间线**：以「周」为单位展示直播日程，按日分块；**右划**回看往日周历、**左划**往回切周（本周不可左划，左划一次回退一周，无法超过当前周，带滑动+淡入淡出动画），往日周右下角「回到本周」按钮一键直达当前周。
- **切周手势引导**：内容区上方常驻手势提示条，文案随所处周自适应——本周仅提示右划、上周提示「左划回本周」、更早的往日周提示「左划回上一周」（展示 6 秒后自动淡出）；首次安装弹出一次全屏手势引导浮层（`SharedPreferences` 持久化，全生命周期只弹一次）。
- **直播录像回看**：往日已结束的直播以粉色「录像」标签展示（与团播/节目等标签同款样式），点击标签通过 `bilibili://video/{BV号}` 唤起 B 站客户端播放回放，未安装时自动回退浏览器；无录播上传的日程不显示标签。录像 BV 号优先来自服务端 `latest.json` / `week/{周起始日}.json` 的 `recording_bvid` 字段（服务端录播管道回填；往日周归档在右划进入该周或下拉刷新时按需拉取），周程表整周刷新时会保留已有录像绑定不被冲掉；服务端字段缺失时回退本地 Mock 数据。
- **成员过滤器**：顶部成员头像选择行（原「成员」页整合而来），点击头像即可过滤显示该成员的单播 + 其参与的团播 + 该成员的突击直播（未识别出成员的兜底事件仅在「全部」视图可见）。支持服务端下发的一期双人组合键（`bella_jiaran` / `bella_nailin` / `jiaran_nailin`），双人卡片同时命中两名参与成员。
- **内置成员库**（离线硬编码，无需网络）：
  - 一期：贝拉 Bella、嘉然 Diana、乃琳 Eileen
  - 二期：思诺、心宜
  - 一期双人组合：贝拉&嘉然 / 贝拉&乃琳 / 嘉然&乃琳（由服务端组合 member 键解析，卡片按参与成员展开）
  - 团播分组：Asoul 团播（小剧场/夜谈/游戏室）、心宜思诺团播、枝江综艺（一期+二期共同企划）
- **写入系统日历**：将直播日程写入设备日历并附带提前 30 分钟提醒（`CalendarContract`）；删除日程时同步删除日历事件。
- **一键进入直播间**：日程详情弹窗中通过 `bilibili://live/{roomId}` 唤起 B 站客户端，未安装时自动回退浏览器网页版。
- **突击直播并入日历（v1.5）**：未结束的突击直播不再固定顶部区块，而是按开播时间与周程表条目混排进对应日期段——成员应援色卡片 + 「突击」徽标，展示头像/名字、标题、`HH:mm` 与状态标签（待开播黄 / 直播中红带呼吸动效），`auto_published` 条目额外打「⚠️待确认」徽标；与同成员周程表条目时间差 ≤15 分钟时自动去重，避免同一场直播占两行；点击卡片通过 `bilibili://feed/{动态id}`（直播间兜底事件回退 `bilibili://live/{roomId}`）唤起 B 站客户端，未安装时回退浏览器。后台轮询到新突击直播时 Snackbar 提醒。已结束的突击由服务端并入周程表，以常驻条目回看并可获「录像」标签。
- **App 内更新（v1.4）**：启动后静默拉取 OSS `app_version.json`；当远端 `version_code` 高于本地、用户未「跳过此版本」且当日未提示过时弹更新弹窗——「立即更新」下载 APK（托管于 GitHub Releases）并经 FileProvider 调起系统安装（首次引导授予「安装未知应用」权限）/「跳过此版本」之后不再提示 /「稍后」当日不再打扰。
- **日程标签体系**：来源标签（周程表识别 / API 抓取）+ 形式标签（小剧场 🎭 / 夜谈 🌙 / 游戏室 🎮 / 联动 🤝 / 工商直播 💼）+ 录像标签 + 识别置信度。

## 🌐 数据来源

线上真实数据由同仓库服务端（[`Server/`](Server/README.md)）的三条全自动管道产出，镜像到阿里云 OSS 后由客户端只读拉取：

| 端点 | 内容 | 客户端行为 |
| --- | --- | --- |
| `latest.json` | 当前周周程表（含录播 BV 号、已并入的已播突击） | 启动 / 回前台立即拉取 + 每小时轮询 |
| `flash.json` | 突击直播（服务端 48 小时自动清理） | 启动 / 回前台立即拉取 + 每 5 分钟轮询 |
| `week/{周起始日}.json` | 往日周归档（含录播回填） | 右划回看该周 / 下拉刷新时按需拉取 |
| `app_version.json` | App 版本清单（更新检查） | 启动后静默检查一次 |

断网时展示上次缓存；本周无数据时提示「周程表尚未发布」并引导右划回看往期；完全无真实数据（断网且无缓存）时才注入开发示例数据兜底，并展示「示例数据」徽标。

## 🧠 OCR 周程表识别管线（架构已就位）

官方周程表以海报图片形式发布。**线上识别实际由服务端管道（Qwen-VL）完成**（见 [`Server/README.md`](Server/README.md)），App 侧管线为 P1/P2 演进预留（当前 P0 演示阶段）：

```
海报图片 ──► ③ OcrEngine（文本块识别）──► ④ ScheduleParser（语义解析）──► List<LiveSchedule>
```

- `OcrEngine`：引擎抽象接口。当前实现为 `FakeOcrEngine`（离线演示，返回样例周程表文本块），P1 计划接入 Google MLKit，P2 补充云端多模态大模型兜底，UI 层无感知。
- `ScheduleParser`：语义解析引擎，容错处理：
  - 成员昵称变体模糊匹配（"贝贝"/"Bella"/"贝拉Bella" → 贝拉），别名置信度加权
  - 多格式日期解析（`8/18`、`8.18`、`8月18日`、`周一` 相对推算）
  - 时间归一化（`20:00` / `20：00` 全角冒号）
  - 团播分组识别（枝江综艺 > 心宜思诺团播 > Asoul 团播）与直播形式标签识别
  - 自动跳过表头行与休息行

## 🛠 技术栈

| 项 | 版本 / 说明 |
| --- | --- |
| AGP | 9.3.0（内置 Kotlin 支持） |
| Kotlin | 2.4.10（Compose Compiler 插件同版本） |
| Compose BOM | 2026.03.00（Material 3） |
| minSdk | 26（Android 8.0+；`java.time` 原生可用，无需脱糖） |
| target / compileSdk | 37 |
| 架构 | MVVM：`ViewModel` + `StateFlow` + Compose `collectAsStateWithLifecycle` |
| 数据层 | 内存仓库 `ScheduleRepository`（StateFlow 驱动，P1 可平滑替换为 Room） |
| 网络层（P6） | OkHttp GET 静态 JSON（超时 10s，失败静默）+ kotlinx-serialization 解析 + 文件缓存（断网兜底）+ 前台轮询（周程表 1h / 突击直播 5min）+ 往日周归档按需拉取 |
| App 更新 | OkHttp 流式下载 APK → FileProvider 调起系统安装器；「跳过版本 / 当日已提示」持久化于 `SharedPreferences` |
| 依赖注入 | 无框架，`AsoulApplication` 持有单例（P2 可迁 Hilt） |
| 构建脚本 | Gradle Kotlin DSL + Version Catalog（`gradle/libs.versions.toml`） |

## 📂 项目结构

```
app/src/main/
├── java/com/example/asoul/
│   ├── AsoulApplication.kt    # 应用入口，持有单例依赖（Repository/Fetcher/更新组件/OcrEngine/CalendarWriter）
│   ├── MainActivity.kt        # 唯一 Activity：申请日历权限 + setContent
│   ├── calendar/
│   │   └── CalendarWriter.kt  # 模块3：系统日历写入（事件+提醒，默认日历查询，删除）
│   ├── data/
│   │   ├── MockScheduleData.kt     # P0 演示数据（缓存与网络都不可用时的兜底）
│   │   ├── ScheduleRepository.kt   # 内存仓库 + StateFlow + 周状态（replaceWeek 整周替换）
│   │   ├── ScheduleCacheStore.kt   # P6：静态 JSON 文件缓存 + SharedPreferences 版本号（IO 线程）
│   │   ├── FlashScheduleRepository.kt  # P6/P10：突击直播仓库（拉取→版本比对→更新缓存→emit）
│   │   ├── model/
│   │   │   ├── LiveSchedule.kt     # 日程模型 / 来源 / 团播分组 / 形式标签 / 录像BV号 / 参与成员 / 周工具
│   │   │   ├── Member.kt           # 成员模型 + MemberCatalog（服务端 member key → 内置 id、 双人组合键解析）
│   │   │   ├── FlashLiveEvent.kt   # P10：突击直播领域模型 + 状态枚举（upcoming/live/ended）
│   │   │   └── ScheduleFilter.kt   # 主界面过滤器（全部 / 成员 / 团播）
│   │   ├── remote/                 # P6：网络层（静态 JSON 只读，OSS 托管）
│   │   │   ├── ApiEndpoints.kt     # BASE_URL + latest / flash / week / app_version 端点
│   │   │   ├── ScheduleApiClient.kt      # OkHttp GET 封装（10s 超时，异常静默）
│   │   │   ├── LatestScheduleFetcher.kt  # 周程表拉取：版本比对 → 缓存 → 按周替换；往日周按需拉取
│   │   │   ├── ScheduleSyncManager.kt    # 前台轮询（周程表 1h / 突击 5min）+ 回前台即时拉取
│   │   │   └── dto/                # LatestScheduleDto / FlashDto / AppVersionDto + 领域映射
│   │   └── update/                 # v1.4 App 更新：AppUpdateStore（跳过/提示节流）
│   │                               # + AppUpdateChecker（版本比对） + AppUpdater（下载+安装）
│   ├── ocr/
│   │   ├── OcrEngine.kt       # OCR 引擎抽象 + FakeOcrEngine 演示实现
│   │   └── ScheduleParser.kt  # 周程表语义解析引擎
│   ├── ui/
│   │   ├── AsoulApp.kt        # 根组合：日历主界面 + Snackbar + 更新弹窗
│   │   ├── MainViewModel.kt   # 顶层 UI 状态与交互（周导航/过滤/删除/写日历/更新处理）
│   │   ├── screen/            # TimelineScreen 时间线主界面（切周手势/引导浮层/突击并入日历）
│   │   ├── components/        # HeaderBanner、MemberSelectorRow、WeekNavigator、SwipeHint
│   │   │                      # （手势提示条/引导浮层）、ScheduleItem/LiveEventCard、DayEmptyState、
│   │   │                      # FlashLiveCard（突击卡片，日历内嵌模式）
│   │   ├── dialog/            # ScheduleDetailDialog（直播详情弹窗）+ AppUpdateDialog（v1.4 更新弹窗）
│   │   └── theme/             # AsoulTheme + 成员主题色
│   └── util/
│       └── BilibiliLauncher.kt  # B 站直播间 / 直播录像 / 动态唤起（scheme 优先，网页兜底）
├── keepRules/rules.keep       # R8 keep 规则（kotlinx-serialization）
└── res/xml/file_paths.xml     # v1.4：FileProvider 暴露缓存 APK 供系统安装器读取
```

## 🔐 权限说明

| 权限 | 用途 |
| --- | --- |
| `INTERNET` | 拉取周程表 / 突击直播 / 往日周归档 / 版本清单 JSON（OSS 托管，只读） |
| `READ_CALENDAR` / `WRITE_CALENDAR` | 将直播日程写入系统日历并设置提醒（运行时申请） |
| `REQUEST_INSTALL_PACKAGES` | App 内更新：Android 8.0+ 安装下载的 APK 需「安装未知应用」授权（缺失时跳转系统设置页引导） |

`AndroidManifest` 中声明了 `<queries>` 以支持 Android 11+ 包可见性要求，用于探测可处理 `bilibili://` scheme 的 B 站客户端与 `https` 浏览器兜底；更新安装经 FileProvider（`res/xml/file_paths.xml`）授权系统安装器读取缓存 APK。

## 🚀 构建与运行

环境要求：Android Studio（支持 AGP 9 / compileSdk 37 的版本）、JDK 11+ 工具链（已配置 foojay toolchain resolver；仓库内 VS Code 构建任务使用 Android Studio 内置 JBR）。

```bash
# Debug 构建
./gradlew :app:assembleDebug

# 安装运行（连接设备后）
./gradlew :app:installDebug

# 单元测试（测试源集仅含占位用例，依赖已就位：JUnit4 / Espresso / Compose UI Test）
./gradlew :app:testDebugUnitTest
```

> [!NOTE]
> Release 开启 R8 收缩 + 资源压缩（kotlinx-serialization keep 规则见 `app/src/main/keepRules/rules.keep`），签名配置从 `keystore/keystore.properties` 读取（`keystore/` 已加入 `.gitignore`，缺失时无法打出可分发的 Release 包）。

### 📦 发布新版本

1. 更新 `app/build.gradle.kts` 的 `versionCode` / `versionName`，执行 `assembleRelease`（产物 `app/build/outputs/apk/release/app-release.apk`）
2. 创建 GitHub Release 并上传 APK（资产名固定为 `app-release-{versionName}.apk`）
3. 运行服务端发版流程更新 OSS 版本清单（`upload_app.py` / `upload_app.yml`，详见 [`Server/README.md`](Server/README.md)）

客户端启动后拉取 `app_version.json`，发现更高版本即弹更新提示。

## 🗺 演进路线（代码注释中的分期规划）

- **P0**：内存仓库 + Mock 数据 + FakeOcrEngine，跑通「展示 → 过滤 → 往日录像回看 → 写入日历」全链路（手动添加日程功能已下线，日程以周程表识别/服务下发为主）。✅ 已完成
- **P1**：接入 Google MLKit 端侧 OCR；Room 替换内存仓库；日程详情支持编辑时长。
- **P2**：云端多模态大模型识别兜底；成员配置云端热更新；Hilt 依赖注入。
- **P6 客户端对接**：✅ 已完成 —— OkHttp 拉取静态 JSON、kotlinx-serialization 解析、文件缓存 + 版本比对、断网兜底、前台轮询（周程表 1h / 突击直播 5min）、往日周归档按需拉取。（`data/remote/ApiEndpoints.kt` 中 `BASE_URL` 已指向阿里云 OSS。）
- **P10 突击直播**：✅ 已完成 —— 突击直播按开播时间并入日历（状态标签 / 呼吸动效 / 待确认徽标 / 点击打开来源 / 新条目 Snackbar 提醒），v1.5 起替代原顶部固定区块。
- **App 内更新（v1.4）**：✅ 已完成 —— 启动静默检查 OSS 版本清单，弹窗支持「立即更新 / 跳过此版本 / 稍后」。

---

> [!IMPORTANT]
> 本项目为粉丝自制工具，仅用于个人日程整理与学习交流；A-SOUL 成员信息与周程表内容版权归官方所有。内置的直播间跳转仅指向成员公开直播间。
