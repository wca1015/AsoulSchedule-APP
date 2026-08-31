以下是可直接交给 Agent 执行的完整规格文件。复制保存为 `UI_SPEC.md` 放入项目根目录即可。

---

# 📋 枝江日历 - UI实现规格书 (Agent执行用)

> **目标**：基于官方周程表视觉风格，实现Flutter手机端日程展示模块  
> **设计基准**：官方发布的 `08.17-08.23` 周程表海报  
> **适配目标**：Android/iOS 竖屏手机，Material 3 设计语言

---

## 一、成员配置（数据源）

```dart
// lib/config/members.dart
enum Member {
  bella('贝拉', Color(0xFFDB7D74), 'assets/avatars/bella.png'),
  diana('嘉然', Color(0xFFFCB97C), 'assets/avatars/diana.png'),
  eileen('乃琳', Color(0xFF57A9A7), 'assets/avatars/eileen.png'),
  xinyi('心宜', Color(0xFFF2A7C3), 'assets/avatars/xinyi.png'),
  sinuo('思诺', Color(0xFFA8D8EA), 'assets/avatars/sinuo.png');

  final String displayName;
  final Color accentColor;
  final String avatarAsset;
  
  const Member(this.displayName, this.accentColor, this.avatarAsset);
}
```

⚠️ **注意**：心宜/思诺色值为参考值，上线前需与社区确认官方应援色。

---

## 二、页面结构（竖屏流式布局）

```
┌─────────────────────────┐
│  Header: CALENDAR        │  ← 固定顶部，渐变背景
│  枝江娱乐直播日历         │
│  08.17 - 08.23           │
├─────────────────────────┤
│  WeekNavigator           │  ← 横向日期选择器，可滑动
│  [17][18][19][20]...     │
├─────────────────────────┤
│                         │
│  DayScheduleList         │  ← 纵向滚动，每日一个Section
│                         │
│  ┌─── 08.17 星期一 ───┐ │
│  │ 💤 休息日            │ │  ← 空状态卡片
│  └────────────────────┘ │
│                         │
│  ┌─── 08.18 星期二 ───┐ │
│  │ 🏋️ 训练时间          │ │
│  │ [思诺直播 17:00]     │ │  ← 事件卡片
│  └────────────────────┘ │
│                         │
│  ┌─── 08.19 星期三 ───┐ │
│  │ [嘉然七夕 19:00]     │ │
│  │ [贝拉七夕 20:05]     │ │
│  │ [乃琳七夕 21:10]     │ │
│  └────────────────────┘ │
│  ...                    │
└─────────────────────────┘
```

---

## 三、核心组件规格

### 3.1 Header 组件

| 属性 | 值 |
|:---|:---|
| 高度 | 160dp (含状态栏安全区) |
| 背景 | LinearGradient: `#6C5CE7` → `#A29BFE` → `#FD79A8` (135°) |
| 标题字体 | "CALENDAR" - 48sp, FontWeight.w900, 白色, letterSpacing: 2 |
| 副标题 | "枝江娱乐直播日历" - 16sp, 白色70%透明度 |
| 日期范围 | 14sp, `AsoulColors.zhijiangAccent` (#FFD93D) |
| 装饰元素 | 左上角斜条带 "Z.J. ENTERTAINMENT"，右上角Logo占位 |

```dart
Widget buildHeader() {
  return Container(
    height: 160,
    decoration: BoxDecoration(
      gradient: LinearGradient(
        colors: [Color(0xFF6C5CE7), Color(0xFFA29BFE), Color(0xFFFD79A8)],
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
      ),
    ),
    child: SafeArea(
      bottom: false,
      child: Stack(
        children: [
          // 斜条带装饰
          Positioned(
            top: 8, left: -20,
            child: Transform.rotate(
              angle: -0.5,
              child: Container(
                padding: EdgeInsets.symmetric(horizontal: 24, vertical: 4),
                color: Colors.white.withOpacity(0.2),
                child: Text('Z.J. ENTERTAINMENT', 
                  style: TextStyle(color: Colors.white, fontSize: 10, letterSpacing: 1)),
              ),
            ),
          ),
          // 主标题
          Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text('CALENDAR', style: TextStyle(
                  fontSize: 48, fontWeight: FontWeight.w900, 
                  color: Colors.white, letterSpacing: 2,
                  shadows: [Shadow(blurRadius: 8, color: Colors.black26)]
                )),
                SizedBox(height: 4),
                Text('枝江娱乐直播日历', style: TextStyle(
                  fontSize: 16, color: Colors.white.withOpacity(0.85))),
                SizedBox(height: 2),
                Text('08.17 - 08.23', style: TextStyle(
                  fontSize: 14, color: AsoulColors.zhijiangAccent, fontWeight: FontWeight.w600)),
              ],
            ),
          ),
        ],
      ),
    ),
  );
}
```

### 3.2 WeekNavigator 横向日期选择器

| 属性 | 值 |
|:---|:---|
| 高度 | 72dp |
| 背景 | `colorScheme.surfaceContainerLow` |
| 单项宽度 | 56dp |
| 选中态 | 成员色底 + 白色文字 + 圆角12 |
| 未选中 | 透明底 + `onSurfaceVariant` 文字 |
| 日期格式 | 上行 "08.17" (14sp bold)，下行 "星期一" (11sp) |
| 交互 | 点击切换当日，左右滑动浏览整周 |

```dart
class WeekNavigator extends StatelessWidget {
  final List<DateTime> weekDates;
  final DateTime selectedDate;
  final ValueChanged<DateTime> onDateSelected;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 72,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: EdgeInsets.symmetric(horizontal: 16),
        itemCount: weekDates.length,
        itemBuilder: (context, index) {
          final date = weekDates[index];
          final isSelected = DateUtils.isSameDay(date, selectedDate);
          final weekday = _getWeekdayLabel(date.weekday);
          
          return GestureDetector(
            onTap: () => onDateSelected(date),
            child: AnimatedContainer(
              duration: Duration(milliseconds: 200),
              width: 56,
              margin: EdgeInsets.symmetric(horizontal: 4),
              decoration: BoxDecoration(
                color: isSelected ? currentMemberColor : Colors.transparent,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text('${date.month.toString().padLeft(2,'0')}.${date.day.toString().padLeft(2,'0')}',
                    style: TextStyle(
                      fontSize: 14, fontWeight: FontWeight.w700,
                      color: isSelected ? Colors.white : Theme.of(context).colorScheme.onSurfaceVariant,
                    )),
                  SizedBox(height: 2),
                  Text(weekday,
                    style: TextStyle(
                      fontSize: 11,
                      color: isSelected ? Colors.white70 : Theme.of(context).colorScheme.onSurfaceVariant,
                    )),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
  
  String _getWeekdayLabel(int weekday) => 
    ['星期一','星期二','星期三','星期四','星期五','星期六','星期日'][weekday - 1];
}
```

### 3.3 DaySection 每日区块

```dart
class DaySection extends StatelessWidget {
  final DateTime date;
  final List<LiveEvent> events;

  @override
  Widget build(BuildContext context) {
    final isRestDay = events.isEmpty;
    final isTrainingOnly = events.every((e) => e.type == EventType.training);
    
    return Padding(
      padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 日期标题行
          Row(
            children: [
              Container(
                width: 4, height: 20,
                decoration: BoxDecoration(
                  color: currentMemberColor,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              SizedBox(width: 8),
              Text(
                '${date.month}.${date.day.toString().padLeft(2,'0')}  ${_getWeekdayLabel(date.weekday)}',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
              ),
            ],
          ),
          SizedBox(height: 8),
          
          // 内容区
          if (isRestDay)
            _buildEmptyState('💤', '休息日', '今天没有直播安排哦~')
          else if (isTrainingOnly)
            _buildEmptyState('🏋️', '训练时间', '成员们正在努力练习中')
          else
            ...events.map((event) => LiveEventCard(event: event)),
        ],
      ),
    );
  }
  
  Widget _buildEmptyState(String emoji, String title, String subtitle) {
    return Container(
      width: double.infinity,
      padding: EdgeInsets.symmetric(vertical: 24),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        children: [
          Text(emoji, style: TextStyle(fontSize: 32)),
          SizedBox(height: 8),
          Text(title, style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
          SizedBox(height: 4),
          Text(subtitle, style: TextStyle(fontSize: 12, color: Colors.grey)),
        ],
      ),
    );
  }
}
```

### 3.4 LiveEventCard 直播事件卡片（核心）

这是还原官方海报视觉的关键组件。

| 属性 | 值 |
|:---|:---|
| 高度 | 自适应，最小 88dp |
| 圆角 | 16dp |
| 左侧色条 | 4dp宽，成员应援色，全高 |
| 背景 | 成员色 8% 透明度叠加在 surfaceContainerLow 上 |
| 标签Badge | 左上角："特别"/"2D"/"节目"，对应不同底色 |
| 头像 | 36dp圆形，左侧色条内嵌或紧邻 |
| 标题 | 15sp w600，单行省略 |
| 副标题 | 13sp，节目内容描述 |
| 时间 | 右下角，12sp mono字体，成员色 |
| 提醒按钮 | 右侧 IconButton，铃铛图标 |

```dart
class LiveEventCard extends StatelessWidget {
  final LiveEvent event;

  @override
  Widget build(BuildContext context) {
    final memberColor = event.member.accentColor;
    
    return Container(
      margin: EdgeInsets.only(bottom: 8),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        color: memberColor.withOpacity(0.08),
        border: Border(left: BorderSide(color: memberColor, width: 4)),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () => _showEventDetail(context),
          child: Padding(
            padding: EdgeInsets.all(12),
            child: Row(
              children: [
                // 头像
                ClipOval(
                  child: Image.asset(event.member.avatarAsset, width: 36, height: 36),
                ),
                SizedBox(width: 12),
                
                // 信息区
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // 标签 + 标题
                      Row(
                        children: [
                          if (event.tag != null) _buildTag(event.tag!, memberColor),
                          if (event.tag != null) SizedBox(width: 6),
                          Expanded(
                            child: Text(event.title,
                              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
                              maxLines: 1, overflow: TextOverflow.ellipsis),
                          ),
                        ],
                      ),
                      SizedBox(height: 4),
                      // 节目内容
                      if (event.description != null)
                        Text(event.description!,
                          style: TextStyle(fontSize: 13, color: Colors.grey[600]),
                          maxLines: 1, overflow: TextOverflow.ellipsis),
                    ],
                  ),
                ),
                
                // 时间 + 提醒
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(event.timeFormatted,
                      style: TextStyle(
                        fontSize: 13, fontWeight: FontWeight.w700,
                        color: memberColor,
                        fontFamily: 'JetBrainsMono', // 等宽数字
                      )),
                    SizedBox(height: 4),
                    ReminderToggleButton(eventId: event.id),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
  
  Widget _buildTag(EventTag tag, Color baseColor) {
    final config = switch (tag) {
      EventTag.special => ('特别', Color(0xFFFF4757)),
      EventTag.live2d  => ('2D',   Color(0xFF6C5CE7)),
      EventTag.show    => ('节目', Color(0xFF2ED573)),
    };
    
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: config.$2,
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(config.$1,
        style: TextStyle(fontSize: 10, color: Colors.white, fontWeight: FontWeight.w700)),
    );
  }
}
```

---

## 四、数据模型

```dart
// lib/models/live_event.dart
enum EventTag { special, live2d, show }
enum EventType { live, training, rest }

class LiveEvent {
  final String id;
  final DateTime dateTime;
  final Member member;
  final String title;        // "嘉然七夕直播"
  final String? description; // "我们时代的偏爱"
  final EventTag? tag;       // 特别/2D/节目
  final EventType type;
  final bool isReminderSet;

  String get timeFormatted => 
    '${dateTime.hour.toString().padLeft(2,'0')}:${dateTime.minute.toString().padLeft(2,'0')}';
}

class DaySchedule {
  final DateTime date;
  final List<LiveEvent> events;
  
  bool get isRestDay => events.isEmpty;
  bool get isTrainingOnly => events.isNotEmpty && events.every((e) => e.type == EventType.training);
}
```

---

## 五、主题与全局样式

```dart
// lib/theme/app_theme.dart
class AppTheme {
  static ThemeData lightTheme(Member? activeMember) {
    final seedColor = activeMember?.accentColor ?? AsoulColors.asoulPrimary;
    return ThemeData(
      colorSchemeSeed: seedColor,
      useMaterial3: true,
      brightness: Brightness.light,
      fontFamily: 'NotoSansSC',
      cardTheme: CardThemeData(
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
    );
  }

  static ThemeData darkTheme(Member? activeMember) {
    final seedColor = activeMember?.accentColor ?? AsoulColors.asoulPrimary;
    return ThemeData(
      colorSchemeSeed: seedColor,
      useMaterial3: true,
      brightness: Brightness.dark,
      fontFamily: 'NotoSansSC',
    );
  }
}
```

### 字体资源

```yaml
# pubspec.yaml
fonts:
  - family: NotoSansSC
    fonts:
      - asset: assets/fonts/NotoSansSC-Regular.otf
      - asset: assets/fonts/NotoSansSC-Medium.otf
        weight: 500
      - asset: assets/fonts/NotoSansSC-Bold.otf
        weight: 700
  - family: JetBrainsMono
    fonts:
      - asset: assets/fonts/JetBrainsMono-Medium.ttf
        weight: 500
```

---

## 六、交互行为规范

| 交互 | 行为 | 动效 |
|:---|:---|:---|
| 点击 WeekNavigator 日期 | 滚动到对应 DaySection | `ScrollController.animateTo` 300ms easeOut |
| 点击 LiveEventCard | 弹出底部Sheet显示详情+分享 | `showModalBottomSheet` + slide up |
| 点击提醒按钮 | 切换提醒状态 + Haptic反馈 | 图标缩放弹跳 200ms + `HapticFeedback.lightImpact` |
| 下拉刷新 | 重新拉取周程数据 | 自定义Loading动画 |
| 左右滑动整页 | 切换上/下周 | `PageView` + 淡入淡出 |
| 长按事件卡片 | 弹出快捷菜单(分享/添加日历) | `showMenu` |

---

## 七、暗色模式适配要点

```dart
// 在 LiveEventCard 中
final bgColor = Theme.of(context).brightness == Brightness.dark
  ? memberColor.withOpacity(0.15)  // 暗色下提高透明度到15%
  : memberColor.withOpacity(0.08);

// 头像加边框防止融入深色背景
ClipOval(
  child: Container(
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      border: Border.all(color: Colors.white.withOpacity(0.1), width: 1),
    ),
    child: Image.asset(...),
  ),
)
```

---

## 八、无障碍检查清单

- [ ] 所有文本对比度 ≥ 4.5:1（用 `flutter_color_tool` 验证）
- [ ] 成员色不作为唯一区分手段（同时有头像+文字）
- [ ] 触摸目标 ≥ 48×48dp（提醒按钮区域扩大hitTest）
- [ ] 动画尊重 `MediaQuery.of(context).disableAnimations`
- [ ] Semantics 标签完整包裹每个卡片

---

## 九、文件结构建议

```
lib/
├── config/
│   └── members.dart          # 成员枚举+色值
├── models/
│   ├── live_event.dart       # 事件数据模型
│   └── day_schedule.dart     # 日 schedule 模型
├── theme/
│   ├── asoul_colors.dart     # 色板常量
│   ├── app_theme.dart        # ThemeData 工厂
│   └── type_scale.dart       # 排版尺度
├── widgets/
│   ├── header_banner.dart    # 顶部渐变Header
│   ├── week_navigator.dart   # 横向日期选择器
│   ├── day_section.dart      # 每日区块
│   ├── live_event_card.dart  # 直播事件卡片 ⭐核心
│   ├── empty_state.dart      # 休息日/训练日空状态
│   └── reminder_toggle.dart  # 提醒开关按钮
├── screens/
│   └── schedule_screen.dart  # 主页面，组装以上组件
└── data/
    └── mock_schedule.dart    # 开发用Mock数据（基于08.17-08.23海报）
```

---

## 十、Mock数据（基于官方海报）

```dart
// lib/data/mock_schedule.dart
final mockWeekSchedule = [
  DaySchedule(
    date: DateTime(2025, 8, 17),
    events: [], // 休息日
  ),
  DaySchedule(
    date: DateTime(2025, 8, 18),
    events: [
      LiveEvent(id: '1', dateTime: DateTime(2025,8,18,17,0), 
        member: Member.sinuo, title: '思诺直播', tag: EventTag.live2d, type: EventType.live),
    ],
  ),
  DaySchedule(
    date: DateTime(2025, 8, 19),
    events: [
      LiveEvent(id: '2', dateTime: DateTime(2025,8,19,19,0),
        member: Member.diana, title: '嘉然七夕直播', description: '我们时代的偏爱',
        tag: EventTag.special, type: EventType.live),
      LiveEvent(id: '3', dateTime: DateTime(2025,8,19,20,5),
        member: Member.bella, title: '贝拉七夕直播', description: '红线大危机',
        tag: EventTag.special, type: EventType.live),
      LiveEvent(id: '4', dateTime: DateTime(2025,8,19,21,10),
        member: Member.eileen, title: '乃琳七夕直播', description: '爱人不错过',
        tag: EventTag.special, type: EventType.live),
    ],
  ),
  DaySchedule(
    date: DateTime(2025, 8, 20),
    events: [
      LiveEvent(id: '5', dateTime: DateTime(2025,8,20,18,0),
        member: Member.xinyi, title: '心宜七夕直播', description: '约会大作战',
        tag: EventTag.special, type: EventType.live),
      LiveEvent(id: '6', dateTime: DateTime(2025,8,20,19,5),
        member: Member.sinuo, title: '思诺七夕直播', description: '夏日迎新日记',
        tag: EventTag.special, type: EventType.live),
    ],
  ),
  DaySchedule(
    date: DateTime(2025, 8, 21),
    events: [], // 训练时间
  ),
  DaySchedule(
    date: DateTime(2025, 8, 22),
    events: [
      LiveEvent(id: '7', dateTime: DateTime(2025,8,22,16,0),
        member: Member.xinyi, title: '心宜直播', tag: EventTag.live2d, type: EventType.live),
      LiveEvent(id: '8', dateTime: DateTime(2025,8,22,20,0),
        member: Member.bella, title: 'A-SOUL夜谈', description: '我的奥德赛时期',
        tag: EventTag.show, type: EventType.live),
    ],
  ),
  DaySchedule(
    date: DateTime(2025, 8, 23),
    events: [
      LiveEvent(id: '9', dateTime: DateTime(2025,8,23,14,0),
        member: Member.xinyi, title: '心宜直播', tag: EventTag.live2d, type: EventType.live),
      LiveEvent(id: '10', dateTime: DateTime(2025,8,23,18,50),
        member: Member.diana, title: 'A-SOUL夜谈', description: '一起看战双音乐会',
        tag: EventTag.show, type: EventType.live),
      LiveEvent(id: '11', dateTime: DateTime(2025,8,23,20,0),
        member: Member.xinyi, title: '心宜思诺的聊天室', description: '现在一起想想想~',
        tag: EventTag.show, type: EventType.live),
    ],
  ),
];
```

---

## 十一、验收标准

| # | 检查项 | 通过条件 |
|:---|:---|:---|
| 1 | 视觉还原度 | 与官方海报配色/布局一致性 ≥ 85% |
| 2 | 成员色正确性 | 5位成员色值准确，切换主题即时生效 |
| 3 | 竖屏适配 | 320dp~428dp宽度下无溢出/截断 |
| 4 | 暗色模式 | 所有组件在dark theme下可读性达标 |
| 5 | 交互流畅 | 滚动/切换动画 60fps，无掉帧 |
| 6 | 无障碍 | Flutter accessibility scanner 零error |
| 7 | Mock数据渲染 | 08.17-08.23全部11个事件正确显示 |

---

> 📌 **Agent执行指令**：请按照本规格书，从文件结构开始逐步实现。优先完成 `members.dart` → `live_event.dart` → `live_event_card.dart` → `schedule_screen.dart` 的最小可运行链路，用Mock数据验证视觉效果后，再接入真实数据源。