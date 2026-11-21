package com.ctimer2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
    // 按下缩放 + 选中底色渐变 + 左侧指示条宽度动画
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "entryScale"
    )
    val bg by animateColorAsState(
        targetValue = when {
            pressed -> DragonCatBlue.PrimaryBlue.copy(alpha = 0.28f)
            selected -> DragonCatBlue.CardSurface
            else -> DragonCatBlue.DrawerSurface
        },
        animationSpec = tween(170),
        label = "entryBg"
    )
    val titleColor by animateColorAsState(
        targetValue = if (selected) DragonCatBlue.PrimaryBlue else DragonCatBlue.TextPrimary,
        animationSpec = tween(170),
        label = "entryTitle"
    )
    val barHeight by animateDpAsState(
        targetValue = if (selected) 26.dp else 0.dp,
        animationSpec = tween(190),
        label = "entryBar"
    )
    val iconBg by animateColorAsState(
        targetValue = if (selected) DragonCatBlue.PrimaryBlue.copy(alpha = 0.22f) else DragonCatBlue.CardSurfaceAlt,
        animationSpec = tween(170),
        label = "entryIconBg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = DragonCatBlue.PrimaryBlue),
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = DragonCatBlue.TextSecondary
            )
        }
        Box(
            Modifier
                .width(3.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(DragonCatBlue.PrimaryBlue)
        )
    }
}
