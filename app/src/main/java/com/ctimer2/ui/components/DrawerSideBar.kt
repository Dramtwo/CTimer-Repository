package com.ctimer2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.ctimer2.ui.ListViewMode
import com.ctimer2.ui.theme.DragonCatBlue

/**
 * 左侧竖直侧栏（56dp 宽，便于点击）。
 * 从上到下：秒表（打开抽屉）→ 成绩总评 → 公式速记 → … → 重置卡片位置。
 * 排序切换已移到成绩卡片顶部，此处不再重复。
 */
@Composable
fun DrawerSideBar(
    onOpen: () -> Unit,
    onOpenSummary: () -> Unit,
    onOpenFormula: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onResetCards: () -> Unit,
    onImport: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUser: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlass(
        modifier = modifier
            .width(56.dp)
            .fillMaxHeight(),
        shape = RectangleShape,
        tintAlphaShift = 0.1f
    ) {
        // 侧栏共 8 个图标（秒表 + 3 功能 + 重置/导入/设置 + 用户），
        // 横屏矮屏按可用高度自动缩小格子，保证最底部的用户图标永远不会被挤出屏幕。
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // 固定占用：上下 padding 16 + 秒表下方分隔 21 + 图标间隙 20
            val fixed = 16.dp + 21.dp + 20.dp
            val perCell = (maxHeight - fixed) / 8
            val cell = perCell.coerceIn(26.dp, 44.dp)
            val gap = if (cell >= 38.dp) 4.dp else 2.dp
            val iconSize = if (cell >= 34.dp) 22.dp else 19.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部秒表图标加深，避免在浅色玻璃上看不清
                IconCell(onClick = onOpen, cell = cell) {
                    StopwatchIcon(tint = DragonCatBlue.PrimaryBlue, iconSize = iconSize)
                }

                Spacer(Modifier.height(8.dp))
                HLine()
                Spacer(Modifier.height(8.dp))

                IconCell(onClick = onOpenSummary, cell = cell, iconSize = iconSize) { IconSummary(iconSize = iconSize) }
                Spacer(Modifier.height(gap))
                IconCell(onClick = onOpenFormula, cell = cell, iconSize = iconSize) { IconFormula(iconSize = iconSize) }
                Spacer(Modifier.height(gap))
                IconCell(onClick = onOpenLeaderboard, cell = cell, iconSize = iconSize) { IconCloud(iconSize = iconSize) }

                Spacer(Modifier.weight(1f))

                // 底部四项：重置卡片位置 → 导入 → 设置 → 用户中心
                IconCell(onClick = onResetCards, cell = cell, iconSize = iconSize) { IconReset(iconSize = iconSize) }
                Spacer(Modifier.height(gap))
                IconCell(onClick = onImport, cell = cell, iconSize = iconSize) { IconImport(iconSize = iconSize) }
                Spacer(Modifier.height(gap))
                IconCell(onClick = onOpenSettings, cell = cell, iconSize = iconSize) { IconSettings(iconSize = iconSize) }
                Spacer(Modifier.height(gap))
                // 用户中心给一块淡蓝底 + 描边，视觉上更显眼、更好点
                IconCell(
                    onClick = onOpenUser,
                    cell = cell,
                    iconSize = iconSize,
                    highlighted = true
                ) {
                    IconUser(iconSize = iconSize)
                }
            }
        }
    }
}

@Composable
private fun IconCell(
    onClick: () -> Unit,
    cell: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    // 按下时整格轻轻缩一下 + 底色变蓝，给一点「真的按到了」的反馈
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cellScale"
    )
    val bg by animateColorAsState(
        targetValue = when {
            pressed -> DragonCatBlue.PrimaryBlue.copy(alpha = 0.34f)
            highlighted -> DragonCatBlue.PrimaryBlue.copy(alpha = 0.16f)
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        animationSpec = tween(150),
        label = "cellBg"
    )

    Box(
        modifier = Modifier
            .size(cell)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (highlighted) {
                    Modifier.border(
                        1.dp,
                        DragonCatBlue.PrimaryBlue.copy(alpha = 0.45f),
                        RoundedCornerShape(10.dp)
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = DragonCatBlue.PrimaryBlue),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun HLine() {
    Box(
        Modifier
            .width(30.dp)
            .height(1.dp)
            .background(DragonCatBlue.Divider)
    )
}
