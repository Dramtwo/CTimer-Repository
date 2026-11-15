package com.ctimer2.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ctimer2.ui.theme.LocalAppFontFamily

/** 计时数字：清晰深色数字（玻璃字观感不如原版，已回退） */
@Composable
fun TimerDisplay(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    enlarged: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(
            fontFamily = LocalAppFontFamily.current,
            fontSize = if (enlarged) 110.sp else 96.sp
        ),
        color = color,
        modifier = modifier.padding(horizontal = 16.dp)
    )
}
