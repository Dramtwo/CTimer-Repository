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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.ctimer2.ui.ListViewMode
import com.ctimer2.ui.theme.DragonCatBlue

/** 成绩卡片顶部的两格切换：按时间 / 按成绩 */
@Composable
fun ListModeSwitcher(
    mode: ListViewMode,
    onChange: (ListViewMode) -> Unit,
    modifier: Modifier = Modifier,
    /** true 时不自带圆角与底色，作为整体卡片的一部分 */
    embedded: Boolean = false
) {
    val containerModifier = if (embedded) {
        modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(DragonCatBlue.CardSurfaceAlt)
            .padding(3.dp)
    } else {
        modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(DragonCatBlue.CardSurfaceAlt)
            .padding(3.dp)
    }
    Row(
        modifier = containerModifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        SegmentCell(
            text = "按时间",
            selected = mode == ListViewMode.BY_TIME,
            modifier = Modifier.weight(1f),
            tint = DragonCatBlue.PrimaryBlue
        ) { onChange(ListViewMode.BY_TIME) }
        SegmentCell(
            text = "按成绩",
            selected = mode == ListViewMode.BY_SCORE,
            modifier = Modifier.weight(1f),
            tint = DragonCatBlue.PrimaryBlue
        ) { onChange(ListViewMode.BY_SCORE) }
    }
}

@Composable
private fun SegmentCell(
    text: String,
    selected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) tint else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else DragonCatBlue.TextSecondary
        )
    }
}
