package com.ctimer2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.ctimer2.ui.AppFont
import com.ctimer2.ui.FontPalette
import com.ctimer2.ui.theme.GlassMode
import com.ctimer2.ui.theme.DragonCatBlue

@Composable
fun SettingsPage(
    useCustomBackground: Boolean,
    font: AppFont,
    palette: FontPalette,
    glassMode: GlassMode,
    glassBlur: Float,
    bgBlur: Float,
    hintMessage: String?,
    onPickBackground: () -> Unit,
    onClearBackground: () -> Unit,
    onSelectFont: (AppFont) -> Unit,
    onSelectPalette: (FontPalette) -> Unit,
    onSelectGlassMode: (GlassMode) -> Unit,
    onGlassBlurChange: (Float) -> Unit,
    onBgBlurChange: (Float) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
            .verticalScroll(scroll)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge,
                color = DragonCatBlue.TextPrimary
            )
            Spacer(Modifier.weight(1f))
            PillButton(text = "返回", onClick = onBack)
        }

        if (!hintMessage.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = hintMessage,
                fontSize = 12.sp,
                color = DragonCatBlue.ResultBetter
            )
        }

        Spacer(Modifier.height(14.dp))
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
                color = DragonCatBlue.TextSecondary
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionTitle("字体")
        SettingsCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFont.entries.forEach { f ->
                    ChoiceChip(
                        text = f.label,
                        selected = f == font,
                        onClick = { onSelectFont(f) }
                    )
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

        Spacer(Modifier.height(14.dp))
        SectionTitle("字体颜色")
        SettingsCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FontPalette.entries.forEach { p ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSelectPalette(p) }
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
                        Text(
                            text = p.label,
                            fontSize = 9.sp,
                            color = DragonCatBlue.TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
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

        Spacer(Modifier.height(14.dp))
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

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = DragonCatBlue.TextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    FrostedGlass(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        tintAlphaShift = -0.08f
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            content = content
        )
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) DragonCatBlue.PrimaryBlue else DragonCatBlue.CardSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
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