package com.ctimer2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.RectangleShape
import com.ctimer2.ui.theme.DragonCatBlue
import com.ctimer2.util.Summary

/** 成绩总评独立页面：全屏、可滚动、左上角返回 */
@Composable
fun SummaryPage(
    summary: Summary,
    sessionName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    FrostedGlass(
        modifier = modifier.fillMaxSize(),
        shape = RectangleShape,
        showBorder = false,
        tintAlphaShift = -0.14f
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DragonCatBlue.CardSurface)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                BackArrow(tint = DragonCatBlue.TextPrimary)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "成绩总评",
                    style = MaterialTheme.typography.titleLarge,
                    color = DragonCatBlue.TextPrimary
                )
                Text(
                    sessionName,
                    style = MaterialTheme.typography.labelSmall,
                    color = DragonCatBlue.TextSecondary
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SummaryCard(
            summary = summary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = summaryHint(summary),
            style = MaterialTheme.typography.bodyMedium,
            color = DragonCatBlue.TextSecondary
        )

        Spacer(Modifier.height(24.dp))
    }
    }
}

private fun summaryHint(s: Summary): String = when {
    s.count == 0 -> "还没有成绩，先去测几把吧。"
    s.count < 5 -> "成绩还少，近 5 次平均和进步率要再多测几条才准。"
    s.consistencyScore >= 80 -> "稳定性很好，节奏控制得不错。"
    s.consistencyScore >= 60 -> "稳定性中等，注意别忽快忽慢。"
    else -> "波动偏大，可以先放慢速度求稳再提速。"
}

@Composable
private fun BackArrow(tint: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.62f, 0f),
            end = androidx.compose.ui.geometry.Offset(w * 0.22f, h / 2f),
            strokeWidth = stroke
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.22f, h / 2f),
            end = androidx.compose.ui.geometry.Offset(w * 0.62f, h),
            strokeWidth = stroke
        )
    }
}
