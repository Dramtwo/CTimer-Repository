package com.ctimer2.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ctimer2.ui.AppFont
import com.ctimer2.ui.FontPalette
import com.ctimer2.ui.theme.DragonCatBlue
import com.ctimer2.ui.theme.GlassMode
import com.ctimer2.ui.theme.PerfMode

/** 设置页的三个分区 */
private enum class SettingsTab(val label: String) {
    APPEARANCE("外观"),
    FEATURES("功能"),
    ABOUT("关于")
}

/**
 * 设置页。
 *
 * 版式：上方一条**导航栏**（返回 + 三个分区标签 + 选中下划线），下方是可滚动的**内容区**。
 * 分区切换用很短的一次交叉淡入，避免整页重建带来的跳动。
 *
 * 分区划分：
 * - 外观：背景图 / 字体 / 字体颜色 / 材质与模糊
 * - 功能：性能档位 / 成绩数据 / 卡片位置
 * - 关于：版本与作者信息
 */
@Composable
fun SettingsPage(
    useCustomBackground: Boolean,
    font: AppFont,
    palette: FontPalette,
    glassMode: GlassMode,
    glassBlur: Float,
    bgBlur: Float,
    perfMode: PerfMode,
    hintMessage: String?,
    onPickBackground: () -> Unit,
    onClearBackground: () -> Unit,
    onSelectFont: (AppFont) -> Unit,
    onSelectPalette: (FontPalette) -> Unit,
    onSelectGlassMode: (GlassMode) -> Unit,
    onGlassBlurChange: (Float) -> Unit,
    onBgBlurChange: (Float) -> Unit,
    onSelectPerfMode: (PerfMode) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onResetCards: () -> Unit,
    onBack: () -> Unit
) {
    var tab by remember { mutableStateOf(SettingsTab.APPEARANCE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        // ---------------- 上方导航栏 ----------------
        FrostedGlass(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            tintAlphaShift = -0.04f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", fontSize = 20.sp, color = DragonCatBlue.TextPrimary)
                }

                Spacer(Modifier.width(2.dp))

                SettingsTab.entries.forEach { t ->
                    TabLabel(
                        text = t.label,
                        selected = t == tab,
                        onClick = { tab = t }
                    )
                    Spacer(Modifier.width(2.dp))
                }

                Spacer(Modifier.weight(1f))

                HintChip(
                    text = when (perfMode) {
                        PerfMode.LUXE -> "华丽"
                        PerfMode.BALANCED -> "中等"
                        PerfMode.LOW -> "低性能"
                    }
                )
            }
        }

        if (!hintMessage.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = hintMessage,
                fontSize = 11.sp,
                color = DragonCatBlue.ResultBetter,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // ---------------- 下方内容区 ----------------
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(tween(140, easing = FastOutSlowInEasing))
            },
            label = "settingsTab",
            modifier = Modifier.weight(1f)
        ) { current ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                when (current) {
                    SettingsTab.APPEARANCE -> AppearanceSection(
                        useCustomBackground = useCustomBackground,
                        font = font,
                        palette = palette,
                        glassMode = glassMode,
                        glassBlur = glassBlur,
                        bgBlur = bgBlur,
                        onPickBackground = onPickBackground,
                        onClearBackground = onClearBackground,
                        onSelectFont = onSelectFont,
                        onSelectPalette = onSelectPalette,
                        onSelectGlassMode = onSelectGlassMode,
                        onGlassBlurChange = onGlassBlurChange,
                        onBgBlurChange = onBgBlurChange
                    )

                    SettingsTab.FEATURES -> FeaturesSection(
                        perfMode = perfMode,
                        onSelectPerfMode = onSelectPerfMode,
                        onExport = onExport,
                        onImport = onImport,
                        onResetCards = onResetCards
                    )

                    SettingsTab.ABOUT -> AboutSection()
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ---------------- 导航栏标签 ----------------

@Composable
private fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(
        targetValue = if (selected) DragonCatBlue.PrimaryBlue else DragonCatBlue.TextSecondary,
        animationSpec = tween(170),
        label = "tabColor"
    )
    val barWidth by animateDpAsState(
        targetValue = if (selected) 26.dp else 0.dp,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "tabBar"
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = color
        )
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .width(barWidth)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(DragonCatBlue.PrimaryBlue)
        )
    }
}

@Composable
private fun HintChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DragonCatBlue.PrimaryBlue.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 10.sp, color = DragonCatBlue.PrimaryBlue)
    }
}

// ---------------- 外观 ----------------

@Composable
private fun AppearanceSection(
    useCustomBackground: Boolean,
    font: AppFont,
    palette: FontPalette,
    glassMode: GlassMode,
    glassBlur: Float,
    bgBlur: Float,
    onPickBackground: () -> Unit,
    onClearBackground: () -> Unit,
    onSelectFont: (AppFont) -> Unit,
    onSelectPalette: (FontPalette) -> Unit,
    onSelectGlassMode: (GlassMode) -> Unit,
    onGlassBlurChange: (Float) -> Unit,
    onBgBlurChange: (Float) -> Unit
) {
    SectionTitle("背景图")
    SettingsCard {
        Text(
            text = if (useCustomBackground) "当前使用自定义背景图" else "当前使用内置默认背景",
            fontSize = 12.sp,
            color = DragonCatBlue.TextSecondary
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(text = "选择图片", filled = true, onClick = onPickBackground)
            if (useCustomBackground) {
                PillButton(text = "恢复默认背景", onClick = onClearBackground)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "调用系统相册选择器（Android 13+ 内置），不需要任何存储权限。选中的图片会保存到应用目录，重启不会丢失。",
            fontSize = 10.sp,
            color = DragonCatBlue.TextSecondary,
            lineHeight = 14.sp
        )
    }

    Spacer(Modifier.height(12.dp))
    SectionTitle("字体")
    SettingsCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFont.entries.forEach { f ->
                ChoiceChip(text = f.label, selected = f == font, onClick = { onSelectFont(f) })
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "12.34  0.00  ao5",
            fontFamily = font.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = palette.color
        )
    }

    Spacer(Modifier.height(12.dp))
    SectionTitle("字体颜色")
    SettingsCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FontPalette.entries.forEach { p ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectPalette(p) }
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.color)
                            .border(
                                width = if (p == palette) 2.dp else 0.dp,
                                color = if (p == palette) DragonCatBlue.TextPrimary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(text = p.label, fontSize = 9.sp, color = DragonCatBlue.TextSecondary)
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    SectionTitle("材质")
    SettingsCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassMode.entries.forEach { m ->
                ChoiceChip(
                    text = m.label,
                    selected = m == glassMode,
                    onClick = { onSelectGlassMode(m) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "毛玻璃＝强模糊高透；云母＝轻模糊较实；关闭＝原来的淡蓝实色",
            fontSize = 10.sp,
            color = DragonCatBlue.TextSecondary
        )

        if (glassMode != GlassMode.SOLID) {
            Spacer(Modifier.height(12.dp))
            SliderRow(
                label = "面板模糊",
                value = glassBlur,
                range = 0f..40f,
                suffix = "dp",
                onChange = onGlassBlurChange
            )
        }

        Spacer(Modifier.height(10.dp))
        SliderRow(
            label = "背景模糊",
            value = bgBlur,
            range = 0f..40f,
            suffix = "dp",
            onChange = onBgBlurChange
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "背景模糊设为 0 时背景保持清晰通亮，只让面板本身做磨砂。",
            fontSize = 10.sp,
            color = DragonCatBlue.TextSecondary
        )
    }
}

// ---------------- 功能 ----------------

@Composable
private fun FeaturesSection(
    perfMode: PerfMode,
    onSelectPerfMode: (PerfMode) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onResetCards: () -> Unit
) {
    SectionTitle("性能调节")
    SettingsCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PerfMode.entries.forEach { m ->
                ChoiceChip(
                    text = m.label,
                    selected = m == perfMode,
                    onClick = { onSelectPerfMode(m) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = perfMode.desc, fontSize = 11.sp, color = DragonCatBlue.PrimaryBlue)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "调低档位会关掉背景取景模糊与边缘呼吸高光，动画只保留最短淡入 —— 老机型或发热时选「低」最稳。",
            fontSize = 10.sp,
            color = DragonCatBlue.TextSecondary,
            lineHeight = 14.sp
        )
    }

    Spacer(Modifier.height(12.dp))
    SectionTitle("成绩数据")
    SettingsCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(text = "导出成绩文件", filled = true, onClick = onExport)
            PillButton(text = "导入成绩文件", onClick = onImport)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "每次停表都会自动备份一份到 下载/CTimer2/ctimer2_scores.json。换包名重装后，用「导入成绩文件」选中这个文件即可恢复全部列表和成绩。",
            fontSize = 10.sp,
            color = DragonCatBlue.TextSecondary,
            lineHeight = 14.sp
        )
    }

    Spacer(Modifier.height(12.dp))
    SectionTitle("布局")
    SettingsCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(text = "重置卡片位置", onClick = onResetCards)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "把 Logo 卡与成绩卡拖回默认位置。长按卡片可以随意拖动，位置会记住。",
            fontSize = 10.sp,
            color = DragonCatBlue.TextSecondary,
            lineHeight = 14.sp
        )
    }
}

// ---------------- 关于 ----------------

@Composable
private fun AboutSection() {
    SettingsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "来自 Boziwan   And   Deepseek™",
                fontSize = 12.sp,
                color = DragonCatBlue.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "BOZIWAN™",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = DragonCatBlue.PrimaryBlue,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "使用 Deepseek V4.1Flash",
                fontSize = 12.sp,
                color = DragonCatBlue.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .width(46.dp)
                    .height(1.dp)
                    .background(DragonCatBlue.Divider)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "CTimer2 · 魔方计时器",
                fontSize = 10.sp,
                color = DragonCatBlue.TextSecondary
            )
        }
    }
}

// ---------------- 通用小组件 ----------------

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = DragonCatBlue.TextSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    FrostedGlass(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        tintAlphaShift = -0.08f
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

@Composable
private fun PillButton(text: String, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (filled) DragonCatBlue.PrimaryBlue else DragonCatBlue.CardSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (filled) Color.White else DragonCatBlue.TextPrimary
        )
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) DragonCatBlue.PrimaryBlue else DragonCatBlue.CardSurface,
        animationSpec = tween(160),
        label = "chipBg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            color = if (selected) Color.White else DragonCatBlue.TextPrimary
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = DragonCatBlue.TextPrimary,
            modifier = Modifier.width(58.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${value.toInt()}$suffix",
            fontSize = 10.sp,
            color = DragonCatBlue.TextSecondary,
            modifier = Modifier.width(38.dp),
            textAlign = TextAlign.End
        )
    }
}
