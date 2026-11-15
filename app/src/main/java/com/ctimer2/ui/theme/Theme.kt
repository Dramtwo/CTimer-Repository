package com.ctimer2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CubeLightScheme = lightColorScheme(
    primary = DragonCatBlue.PrimaryBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = DragonCatBlue.PrimaryBlueLight,
    background = DragonCatBlue.BackgroundDeep,
    onBackground = DragonCatBlue.TextPrimary,
    surface = DragonCatBlue.CardSurface,
    onSurface = DragonCatBlue.TextPrimary,
    surfaceVariant = DragonCatBlue.CardSurfaceAlt,
    outline = DragonCatBlue.Divider
)

@Composable
fun CTimer2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CubeLightScheme,
        typography = AppTypography,
        content = content
    )
}
