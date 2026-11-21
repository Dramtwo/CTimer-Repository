package com.ctimer2.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 面板材质：毛玻璃（强模糊+高透）、云母（轻模糊+较实）、关闭（实色） */
enum class GlassMode(val label: String) {
    FRIT("毛玻璃"),
    MICA("云母"),
    SOLID("关闭")
}

data class GlassStyle(
    val mode: GlassMode = GlassMode.SOLID,
    /** 面板自身的模糊半径 */
    val blurDp: Float = 18f,
    /** 表面白色叠层不透明度 */
    val tintAlpha: Float = 0.72f
)

val LocalGlass = staticCompositionLocalOf { GlassStyle() }

/** 背景来源（供面板做局部取景模糊） */
data class BackgroundSpec(
    val custom: ImageBitmap? = null,
    val version: Int = 0
)

val LocalBackgroundSpec = staticCompositionLocalOf<BackgroundSpec?> { null }

/** 设置页滑块用到的模糊范围 */
val MinBlur: Dp = 0.dp
val MaxBlur: Dp = 40.dp
