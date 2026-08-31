package com.example.asoul.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 枝江日历色板（UI 规格书：基于官方周程表海报视觉）。
 */
object AsoulColors {

    /** Header 渐变：紫 → 淡紫 → 粉（135°）。 */
    val HeaderGradientStart = Color(0xFF6C5CE7)
    val HeaderGradientMid = Color(0xFFA29BFE)
    val HeaderGradientEnd = Color(0xFFFD79A8)

    /** 枝江强调黄（Header 日期范围文字）。 */
    val ZhijiangAccent = Color(0xFFFFD93D)

    // ===== 事件卡片 Badge 底色（规格 3.4） =====

    /** 「特别」：七夕/生日/首播等特别企划。 */
    val BadgeSpecial = Color(0xFFFF4757)

    /** 「节目」：小剧场/夜谈/游戏室等团播节目。 */
    val BadgeShow = Color(0xFF2ED573)

    /** 「联动」。 */
    val BadgeCollab = Color(0xFF3B82F6)

    /** 「工商」商务合作场。 */
    val BadgeCommercial = Color(0xFFF59E0B)

    /** 「团播」。 */
    val BadgeGroup = Color(0xFF6C5CE7)

    /** 「录像」：往日已结束直播的 B 站回放入口。 */
    val BadgeRecording = Color(0xFFFB7299)
}
