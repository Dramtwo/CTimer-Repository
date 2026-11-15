package com.ctimer2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ctimer2.data.SessionEntity
import com.ctimer2.ui.theme.DragonCatBlue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionItem(
    session: SessionEntity,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nameColor by animateColorAsState(
        targetValue = if (selected) DragonCatBlue.PrimaryBlueLight else DragonCatBlue.TimerText,
        label = "sessionName"
    )
    val barWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else 0.dp,
        label = "sessionBar"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(DragonCatBlue.DrawerSurface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 选中指示竖条
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(24.dp)
                .background(DragonCatBlue.PrimaryBlueLight)
        )
        Spacer(Modifier.width(if (selected) 13.dp else 16.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(DragonCatBlue.CardSurfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            IconList()
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = session.name,
            style = MaterialTheme.typography.bodyLarge,
            color = nameColor,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = DragonCatBlue.TextSecondary
        )
    }
}
