package com.ctimer2.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** 全局外观：用户可在设置页切换字体与字体颜色 */
val LocalAppFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }
val LocalAppFontColor = staticCompositionLocalOf<Color> { Color(0xFF23384D) }
