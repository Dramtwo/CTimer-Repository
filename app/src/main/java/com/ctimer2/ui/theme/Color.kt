package com.ctimer2.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 云母／毛玻璃材质。
 * 卡片类表面改为半透明白，配合主界面背景图的整体模糊，形成磨砂玻璃观感
 * （背景被模糊后，透出卡片的就是模糊影像，等效 iOS/Windows Mica 效果）。
 * 保留 DragonCatBlue 这个对象名以免全工程改引用。
 */
object DragonCatBlue {
    val BackgroundDeep = Color(0xFFFAF8F3)   // 兜底底色（奶白）
    val PrimaryBlue    = Color(0xFF5B9BD5)   // 淡蓝主色
    val PrimaryBlueLight = Color(0xFF8FC0E8) // 高亮/激活态

    // ---- 玻璃表面 ----
    val CardSurface    = Color(0xCCFFFFFF)   // 卡片：约 80% 不透明白
    val CardSurfaceAlt = Color(0x99FFFFFF)   // 更透：切换条等次级面
    val GlassBorder    = Color(0x40FFFFFF)   // 玻璃边缘高光描边
    val GlassBorderSoft = Color(0x1AFFFFFF)

    val TimerText      = Color(0xFF23384D)   // 计时数字，深蓝灰
    val TextPrimary    = Color(0xFF23384D)
    val TextSecondary  = Color(0xFF4A6272)   // 玻璃上略加深，保证可读

    val ResultBetter = Color(0xFF14713A)     // 优于上次：深绿
    val ResultWorse  = Color(0xFFB23A36)     // 差于上次：红
    val ResultFirst  = TextPrimary           // 首次/无对比

    val HoldColor  = Color(0xFFD9781A)       // 按住未就绪：橙
    val ReadyColor = Color(0xFF177A43)       // 按住已就绪：绿

    val Scrim         = Color(0x52000000)    // 抽屉遮罩
    val DrawerSurface = Color(0xE6FFFFFF)    // 抽屉：更实一点，保证阅读
    val Divider       = Color(0x1A23384D)    // 分隔线
    val MenuSurface   = Color(0xF2FFFFFF)    // 悬浮菜单面板

    // 成就横幅：半透明绿玻璃
    val BadgeGlass  = Color(0xE6309A5C)
    val BadgeGlassEnd = Color(0xE65B9BD5)

    // 关闭玻璃时的实色（沿用早期淡蓝卡片）
    val SolidCard    = Color(0xFFDCEAF7)
    val SolidCardAlt = Color(0xFFEDF5FC)
    val SolidDrawer  = Color(0xFFE8F2FA)
}
