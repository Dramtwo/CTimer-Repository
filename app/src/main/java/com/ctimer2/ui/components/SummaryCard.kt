package com.ctimer2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ctimer2.ui.theme.DragonCatBlue
import com.ctimer2.util.Stats
import com.ctimer2.util.Summary

@Composable
fun SummaryCard(
    summary: Summary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DragonCatBlue.CardSurface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "成绩总评",
                style = MaterialTheme.typography.titleMedium,
                color = DragonCatBlue.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                "共 ${summary.count} 次",
                style = MaterialTheme.typography.labelSmall,
                color = DragonCatBlue.TextSecondary
            )
        }

        if (summary.count == 0) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("还没有成绩", style = MaterialTheme.typography.bodyMedium, color = DragonCatBlue.TextSecondary)
            }
            return@Column
        }

        Spacer(Modifier.height(12.dp))

        // 三宫格核心指标
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox("最佳", summary.bestMs?.let { Stats.format(it) } ?: "--", DragonCatBlue.ResultBetter, Modifier.weight(1f))
            MetricBox("最差", summary.worstMs?.let { Stats.format(it) } ?: "--", DragonCatBlue.ResultWorse, Modifier.weight(1f))
            MetricBox("平均", summary.avgMs?.let { Stats.format(it) } ?: "--", DragonCatBlue.TextPrimary, Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox("中位数", summary.medianMs?.let { Stats.format(it) } ?: "--", DragonCatBlue.TextPrimary, Modifier.weight(1f))
            MetricBox("标准差", summary.stdDevMs?.let { Stats.format(it) } ?: "--", DragonCatBlue.TextSecondary, Modifier.weight(1f))
            MetricBox("稳定性", "${summary.consistencyScore}", DragonCatBlue.PrimaryBlue, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(10.dp))

        LineItem("近 5 次平均", summary.recent5AvgMs?.let { Stats.format(it) } ?: "--")
        LineItem("近 12 次平均", summary.recent12AvgMs?.let { Stats.format(it) } ?: "--")
        LineItem("最早 10 次平均", summary.first10AvgMs?.let { Stats.format(it) } ?: "--")

        summary.improvementPct?.let { pct ->
            Spacer(Modifier.height(6.dp))
            val better = pct >= 0
            Text(
                text = if (better) "近期进步 ${String.format("%.1f", pct)}%" else "近期退步 ${String.format("%.1f", -pct)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = if (better) DragonCatBlue.ResultBetter else DragonCatBlue.ResultWorse
            )
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DragonCatBlue.CardSurfaceAlt)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = DragonCatBlue.TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun LineItem(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = DragonCatBlue.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = DragonCatBlue.TextPrimary)
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DragonCatBlue.Divider)
    )
}
