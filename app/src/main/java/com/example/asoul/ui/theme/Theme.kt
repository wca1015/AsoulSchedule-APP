package com.example.asoul.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Asoul 应援色系：粉/蓝/紫
private val AsoulPink = Color(0xFFF06292)
private val AsoulBlue = Color(0xFF7C9CF5)
private val AsoulPurple = Color(0xFF8E7CC3)

private val LightColors = lightColorScheme(
    primary = AsoulPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEF8),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = AsoulPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8E4),
    onSecondaryContainer = Color(0xFF3F0022),
    tertiary = AsoulBlue,
    surface = Color(0xFFFEF7FF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCDB8FF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFF2A9C4),
    onSecondary = Color(0xFF571233),
    secondaryContainer = Color(0xFF7B2949),
    onSecondaryContainer = Color(0xFFFFD8E4),
    tertiary = AsoulBlue,
    surface = Color(0xFF141218),
)

/**
 * 枝江日历主题：Asoul 应援色系（粉/蓝/紫），支持深色模式与动态取色。
 */
@Composable
fun AsoulTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
