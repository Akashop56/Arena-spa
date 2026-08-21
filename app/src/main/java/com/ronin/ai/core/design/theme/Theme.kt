package com.ronin.ai.core.design.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val RoninColorScheme = darkColorScheme(
    primary = RoninCyan,
    onPrimary = RoninBlack,
    primaryContainer = RoninCyanDeep,
    onPrimaryContainer = RoninCyanSoft,

    // Amber / gold is RONIN's secondary accent
    secondary = RoninAmber,
    onSecondary = RoninBlack,
    secondaryContainer = RoninAmber.copy(alpha = 0.18f),
    onSecondaryContainer = RoninAmberSoft,

    tertiary = RoninGold,
    onTertiary = RoninBlack,

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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // Dark HUD: always use light (white) system bar icons.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = RoninColorScheme,
        typography = RoninTypography,
        content = content
    )
}
