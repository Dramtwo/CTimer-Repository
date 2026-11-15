package com.ctimer2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部秒表图标加深，避免在浅色玻璃上看不清
            IconCell(onClick = onOpen) { StopwatchIcon(tint = DragonCatBlue.PrimaryBlue) }

            Spacer(Modifier.height(10.dp))
            HLine()
            Spacer(Modifier.height(10.dp))

            IconCell(onClick = onOpenSummary) { IconSummary() }
            Spacer(Modifier.height(4.dp))
            IconCell(onClick = onOpenFormula) { IconFormula() }
            Spacer(Modifier.height(4.dp))
            IconCell(onClick = onOpenLeaderboard) { IconCloud() }

            Spacer(Modifier.weight(1f))

            // 底部三项：倒数第三 重置卡片位置 → 倒数第二 导入 → 倒数第一 设置
            IconCell(onClick = onResetCards) { IconReset() }
            Spacer(Modifier.height(4.dp))
            IconCell(onClick = onImport) { IconImport() }
            Spacer(Modifier.height(4.dp))
            IconCell(onClick = onOpenSettings) { IconSettings() }
            Spacer(Modifier.height(4.dp))
            IconCell(onClick = onOpenUser) { IconUser() }
        }
    }
}

@Composable
private fun IconCell(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
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
