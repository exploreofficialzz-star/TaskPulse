package com.chastechgroup.taskpulse.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = Blue500,
    onPrimary        = Color.White,
    primaryContainer = Navy700,
    onPrimaryContainer = Blue300,
    secondary        = Cyan400,
    onSecondary      = Navy900,
    background       = Navy950,
    onBackground     = TextOnDark,
    surface          = SurfaceDark,
    onSurface        = TextOnDark,
    surfaceVariant   = SurfaceDark2,
    onSurfaceVariant = TextOnDark70,
    outline          = BorderDark,
    error            = Red500,
    onError          = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary          = Blue600,
    onPrimary        = Color.White,
    primaryContainer = LightSurface2,
    onPrimaryContainer = Blue600,
    secondary        = Blue400,
    onSecondary      = Color.White,
    background       = LightBg,
    onBackground     = TextOnLight,
    surface          = LightSurface,
    onSurface        = TextOnLight,
    surfaceVariant   = LightSurface2,
    onSurfaceVariant = TextOnLight70,
    outline          = LightBorder,
    error            = Red500,
    onError          = Color.White
)

@Composable
fun TaskPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
