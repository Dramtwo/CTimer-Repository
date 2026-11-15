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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ctimer2.ui.theme.DragonCatBlue
import com.ctimer2.util.Stats

/**
 * csTimer 风格悬浮菜单：拖动呼出后浮在计时区上方。
 * 提供上一条 / 下一条浏览，以及删除当前成绩。
 */
@Composable
fun SolveActionMenu(
    displayMs: Long,
    positionText: String,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlass(
        modifier = modifier.width(300.dp),
        shape = RoundedCornerShape(16.dp),
        tintAlphaShift = 0.06f
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Stats.format(displayMs),
                style = MaterialTheme.typography.titleLarge,
                color = DragonCatBlue.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = positionText,
                style = MaterialTheme.typography.labelSmall,
                color = DragonCatBlue.TextSecondary
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuButton("◀ 上一条", canPrev, DragonCatBlue.PrimaryBlue, Modifier.weight(1f), onPrev)
            MenuButton("下一条 ▶", canNext, DragonCatBlue.PrimaryBlue, Modifier.weight(1f), onNext)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuButton("删除此成绩", true, DragonCatBlue.ResultWorse, Modifier.weight(1f), onDelete)
            MenuButton("关闭", true, DragonCatBlue.TextSecondary, Modifier.weight(0.6f), onClose)
        }
    }
    }
}

@Composable
private fun MenuButton(
    text: String,
    enabled: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (enabled) tint.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.22f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) tint else DragonCatBlue.TextSecondary
        )
    }
}
