package com.example.checker.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CyberColorScheme = lightColorScheme(
    primary = NeonGreen,
    secondary = NeonBlue,
    tertiary = CyberPurple,
    background = ObsidianBg,
    surface = CardCarbon,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite,
    outline = CardBorder
)

@Composable
fun CheckerTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ObsidianBg.toArgb()
            window.navigationBarColor = ObsidianBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}