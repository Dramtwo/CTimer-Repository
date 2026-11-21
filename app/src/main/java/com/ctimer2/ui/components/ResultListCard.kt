package com.ctimer2.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ctimer2.ui.theme.LocalAppFontFamily
import com.ctimer2.util.Stats

/** 一行成绩：序号 + 毫秒 + 颜色 + 相对上一条差值 */
data class ResultRow(
    val index: Int,
    val timeMs: Long,
    val color: Color,
    val deltaMs: Long? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResultListCard(
    rows: List<ResultRow>,
    modifier: Modifier = Modifier,
    /** true 时自带圆角与卡片底色；放进统一卡片容器时传 false，避免漏出背景 */
    embedded: Boolean = false
) {
    val containerModifier = if (embedded) {
        modifier.padding(vertical = 6.dp)
    } else {
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DragonCatBlue.CardSurface)
            .padding(vertical = 6.dp)
    }
    Column(modifier = containerModifier) {
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "暂无成绩",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DragonCatBlue.TextSecondary
                )
            }
        } else {
            LazyColumn {
                items(rows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                fadeInSpec = tween(170),
                                placementSpec = tween(210),
                                fadeOutSpec = tween(130)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${row.index}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DragonCatBlue.TextSecondary,
                            modifier = Modifier.width(36.dp)
                        )
                        Text(
                            text = Stats.format(row.timeMs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = row.color,
                            fontFamily = LocalAppFontFamily.current,
                            modifier = Modifier.weight(1f)
                        )
                        row.deltaMs?.let { delta ->
                            val deltaColor = when {
                                delta < 0 -> DragonCatBlue.ResultBetter
                                delta > 0 -> DragonCatBlue.ResultWorse
                                else -> DragonCatBlue.TextSecondary
                            }
                            Text(
                                text = Stats.formatDelta(delta),
                                style = MaterialTheme.typography.labelSmall,
                                color = deltaColor
                            )
                        }
                    }
                }
            }
        }
    }
}
