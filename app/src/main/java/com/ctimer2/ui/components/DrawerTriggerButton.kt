package com.ctimer2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ctimer2.ui.theme.DragonCatBlue

/** 左上角浮动抽屉触发按钮：48dp 触控区，内含几何秒表图标 */
@Composable
fun DrawerTriggerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DragonCatBlue.CardSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        StopwatchIcon()
    }
}
