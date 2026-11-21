package com.ctimer2.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * 性能档位。用来决定动效与模糊的「开销上限」：
 * - LUXE 华丽：弹簧物理全开、玻璃取景模糊、边缘呼吸高光
 * - BALANCED 中等：保留位移/淡入，关闭无限呼吸，模糊按设置值打折
 * - LOW 低：关掉取景模糊与呼吸高光，动画只留最短的淡入，最省电最稳
 */
enum class PerfMode(val label: String, val desc: String) {
    LUXE("华丽", "弹簧物理 + 玻璃取景 + 呼吸高光"),
    BALANCED("中等", "保留位移与淡入，关闭呼吸高光"),
    LOW("低", "关闭玻璃取景与高光，只留最短动效")
}

val LocalPerfMode = compositionLocalOf { PerfMode.BALANCED }
