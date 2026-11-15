package com.ctimer2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ctimer2.ui.theme.DragonCatBlue

/** 抽屉顶部的固定入口项（成绩总评 / 按时间 / 按成绩），带几何图标 */
@Composable
fun DrawerEntryItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) DragonCatBlue.CardSurface else DragonCatBlue.DrawerSurface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DragonCatBlue.CardSurfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) DragonCatBlue.PrimaryBlue else DragonCatBlue.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = DragonCatBlue.TextSecondary
            )
        }
        if (selected) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(DragonCatBlue.PrimaryBlue)
            )
        }
    }
}
