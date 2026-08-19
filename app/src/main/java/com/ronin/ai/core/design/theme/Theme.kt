package com.ronin.ai.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RoninColorScheme = darkColorScheme(
    primary = RoninCyan,
    onPrimary = RoninBlack,
    primaryContainer = RoninCyanSoft,
    onPrimaryContainer = RoninBlack,
    secondary = RoninViolet,
    onSecondary = Color.White,
    secondaryContainer = RoninVioletSoft,
    onSecondaryContainer = RoninBlack,
    tertiary = RoninMagenta,
    background = RoninBlack,
    onBackground = RoninTextPrimary,
    surface = RoninSurface,
    onSurface = RoninTextPrimary,
    surfaceVariant = RoninSurfaceHigh,
    onSurfaceVariant = RoninTextSecondary,
    surfaceContainerHighest = RoninSurfaceHighest,
    surfaceContainerHigh = RoninSurfaceHigh,
    surfaceContainer = RoninSurface,
    surfaceContainerLow = RoninSurface,
    surfaceContainerLowest = RoninBlack,
    error = RoninError,
    onError = Color.Black,
    outline = RoninBorder,
    outlineVariant = RoninBorder,
    scrim = RoninBlack
)

@Composable
fun RoninTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RoninColorScheme,
        typography = RoninTypography,
        content = content
    )
}
