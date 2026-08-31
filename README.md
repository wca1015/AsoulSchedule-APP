# Asoul · 枝江日历

一个面向 **A-SOUL** 粉丝的直播周程表日历 App：查看每周直播安排、左右划切周回看往期直播录像、按成员/团播过滤、一键写入系统日历、唤起 B 站直播间，并为「周程表海报 OCR 自动识别」预留了完整的解析管线。

> 应用名：`Asoul` ｜ 包名：`com.example.asoul` ｜ 当前版本：`1.0 (versionCode 1)`

---

## ✨ 核心功能

- **周程表时间线**：以「周」为单位展示直播日程，按日分块；**右划**回看往日周历、**左划**往回切周（本周不可左划，左划一次回退一周，无法超过当前周，带滑动+淡入淡出动画），往日周右下角「回到本周」按钮一键直达当前周。
- **切周手势引导**：内容区上方常驻手势提示条，文案随所处周自适应——本周仅提示右划、上周提示「左划回本周」、更早的往日周提示「左划回上一周」（展示 6 秒后自动淡出）；首次安装弹出一次全屏手势引导浮层（`SharedPreferences` 持久化，全生命周期只弹一次）。
- **直播录像回看**：往日已结束的直播以粉色「录像」标签展示（与团播/节目等标签同款样式），点击标签通过 `bilibili://video/{BV号}` 唤起 B 站客户端播放回放，未安装时自动回退浏览器；无录播上传的日程不显示标签。录像 BV 号优先来自服务端 `latest.json` 的 `recording_bvid` 字段（服务端录播管道回填），周程表整周刷新时会保留已有录像绑定不被冲掉；服务端字段缺失时回退本地 Mock 数据。
- **成员过滤器**：顶部成员头像选择行（原「成员」页整合而来），点击头像即可过滤显示该成员的单播 + 其参与的团播。
- **内置成员库**（离线硬编码，无需网络）：
  - 一期：贝拉 Bella、嘉然 Diana、乃琳 Eileen
  - 二期：思诺、心宜
  - 团播分组：Asoul 团播（小剧场/夜谈/游戏室）、心宜思诺团播、枝江综艺（一期+二期共同企划）
- **写入系统日历**：将直播日程写入设备日历并附带提前 30 分钟提醒（`CalendarContract`）；删除日程时同步删除日历事件。
- **一键进入直播间**：日程详情弹窗中通过 `bilibili://live/{roomId}` 唤起 B 站客户端，未安装时自动回退浏览器网页版。
- **突击直播卡片（P10）**：日历主界面 Header 下方新增「突击直播」区块，仅展示未结束条目——成员应援色卡片 + 「突击」徽标，展示头像/名字、标题、开播时间（今天显示「今晚 19:00」）、状态标签（待开播黄 / 直播中红带呼吸动效 / 已结束默认隐藏），自动发布条目额外打「⚠️待确认」徽标；点击卡片通过 `bilibili://feed/{动态id}` 唤起 B 站客户端打开源动态，未安装时回退浏览器。后台轮询到新突击直播时 Snackbar 提醒。
- **日程标签体系**：来源标签（周程表识别 / API 抓取）+ 形式标签（小剧场 🎭 / 夜谈 🌙 / 游戏室 🎮 / 联动 🤝 / 工商直播 💼）+ 录像标签 + 识别置信度。

## 🧠 OCR 周程表识别管线（架构已就位）

官方周程表以海报图片形式发布，App 设计了完整的识别管线（当前处于 P0 演示阶段）：

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
| minSdk | 35（`java.time` API 原生可用） |
| target / compileSdk | 37 |
| 架构 | MVVM：`ViewModel` + `StateFlow` + Compose `collectAsStateWithLifecycle` |
| 数据层 | 内存仓库 `ScheduleRepository`（StateFlow 驱动，P1 可平滑替换为 Room） |
| 网络层（P6） | OkHttp GET 静态 JSON（超时 10s，失败静默）+ kotlinx-serialization 解析 + 文件缓存（断网兜底）+ 前台轮询（周程表 1h / 突击直播 5min） |
| 依赖注入 | 无框架，`AsoulApplication` 持有单例（P2 可迁 Hilt） |
| 构建脚本 | Gradle Kotlin DSL + Version Catalog（`gradle/libs.versions.toml`） |

## 📂 项目结构

```
app/src/main/java/com/example/asoul/
├── AsoulApplication.kt        # 应用入口，持有单例依赖（Repository/OcrEngine/CalendarWriter）
├── MainActivity.kt            # 唯一 Activity：申请日历权限 + setContent
├── calendar/
│   └── CalendarWriter.kt      # 模块3：系统日历写入（事件+提醒，默认日历查询，删除）
├── data/
│   ├── MockScheduleData.kt    # P0 演示数据（缓存与网络都不可用时的兜底）
│   ├── ScheduleRepository.kt  # 内存仓库 + StateFlow + 周状态（replaceWeek 整周替换）
│   ├── ScheduleCacheStore.kt  # P6：静态 JSON 文件缓存 + SharedPreferences 版本号（IO 线程）
│   ├── FlashScheduleRepository.kt  # P6/P10：突击直播仓库（拉取→版本比对→更新缓存→emit）
│   ├── model/
│   │   ├── LiveSchedule.kt    # 日程模型 / 来源 / 团播分组 / 形式标签 / 录像BV号 / 周工具
│   │   ├── Member.kt          # 成员模型 + MemberCatalog（含服务端 member key → 内置 id 映射）
│   │   ├── FlashLiveEvent.kt  # P10：突击直播领域模型 + 状态枚举（upcoming/live/ended）
│   │   └── ScheduleFilter.kt  # 主界面过滤器（全部 / 成员 / 团播）
│   └── remote/                # P6：网络层（静态 JSON 只读，CDN 托管）
│       ├── ApiEndpoints.kt    # BASE_URL + latest.json / flash.json 端点（上线前替换真实地址）
│       ├── ScheduleApiClient.kt      # OkHttp GET 封装（10s 超时，异常静默）
│       ├── LatestScheduleFetcher.kt  # 周程表拉取：版本比对 → 缓存 → 按周替换仓库
│       ├── ScheduleSyncManager.kt    # 前台轮询（周程表 1h / 突击 5min）+ 回前台即时拉取
│       └── dto/               # LatestScheduleDto / FlashDto（@Serializable）+ 领域映射
├── ocr/
│   ├── OcrEngine.kt           # OCR 引擎抽象 + FakeOcrEngine 演示实现
│   └── ScheduleParser.kt      # 周程表语义解析引擎
├── ui/
│   ├── AsoulApp.kt            # 根组合：日历主界面 + Snackbar
│   ├── MainViewModel.kt       # 顶层 UI 状态与交互（周导航/过滤/删除/写日历）
│   ├── screen/                # TimelineScreen 时间线主界面（切周手势/引导浮层/突击直播区块）
│   ├── components/            # HeaderBanner、MemberSelectorRow、WeekNavigator、SwipeHint
│   │                          # （手势提示条/引导浮层）、ScheduleItem、EmptyState、FlashLiveCard（P10 突击直播卡片）
│   ├── dialog/                # ScheduleDetailDialog（直播详情弹窗）
│   └── theme/                 # AsoulTheme + 成员主题色
└── util/
    └── BilibiliLauncher.kt    # B 站直播间 / 直播录像 / 动态（P10 openDynamic）唤起（scheme 优先，网页兜底）
```

## 🔐 权限说明

| 权限 | 用途 |
| --- | --- |
| `INTERNET` | 拉取静态周程表 / 突击直播 JSON（CDN 托管，只读，P6） |
| `READ_CALENDAR` / `WRITE_CALENDAR` | 将直播日程写入系统日历并设置提醒（运行时申请） |

`AndroidManifest` 中声明了 `<queries>` 以支持 Android 11+ 包可见性要求，用于探测可处理 `bilibili://` scheme 的 B 站客户端。

## 🚀 构建与运行

环境要求：Android Studio（支持 AGP 9 / compileSdk 37 的版本）、JDK 11+ 工具链（已配置 foojay toolchain resolver）。

```bash
# Debug 构建
./gradlew :app:assembleDebug

# 安装运行（连接设备后）
./gradlew :app:installDebug

# 单元测试（测试源集目前为空，依赖已就位：JUnit4 / Espresso / Compose UI Test）
./gradlew :app:testDebugUnitTest
```

> [!NOTE]
> Release 开启 R8 收缩 + 资源压缩；kotlinx-serialization 的 keep 规则见 `app/src/main/keepRules/rules.keep`。

## 🗺 演进路线（代码注释中的分期规划）

- **P0**：内存仓库 + Mock 数据 + FakeOcrEngine，跑通「展示 → 过滤 → 往日录像回看 → 写入日历」全链路（手动添加日程功能已下线，日程以周程表识别/服务下发为主）。✅ 已完成
- **P1**：接入 Google MLKit 端侧 OCR；Room 替换内存仓库；日程详情支持编辑时长。
- **P2**：云端多模态大模型识别兜底；成员配置云端热更新；Hilt 依赖注入；成员数据（二期 UID 等）补全。
- **P6 客户端对接**：✅ 已完成 —— OkHttp 拉取静态 JSON、kotlinx-serialization 解析、文件缓存 + 版本比对、断网兜底、前台轮询（周程表 1h / 突击直播 5min）。（真实数据源地址待配置：`data/remote/ApiEndpoints.kt` 中 `BASE_URL` 上线前替换为阿里云 OSS+CDN 地址。）
- **P10 突击直播 UI 卡片**：✅ 已完成 —— 时间线主界面突击直播区块（状态标签 / 呼吸动效 / 待确认徽标 / 点击打开源动态 / 新条目 Snackbar 提醒）。

---

> [!IMPORTANT]
> 本项目为粉丝自制工具，仅用于个人日程整理与学习交流；A-SOUL 成员信息与周程表内容版权归官方所有。内置的直播间跳转仅指向成员公开直播间。
