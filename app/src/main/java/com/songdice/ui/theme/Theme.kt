package com.songdice.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Resolves the appropriate Material 3 [ColorScheme] based on Context, SDK version, and dark theme preference.
 */
fun resolveColorScheme(
    context: Context,
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true
): ColorScheme {
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
}

/**
 * Resolves the appropriate Material 3 [ColorScheme] within a Composable scope.
 */
@Composable
fun resolveColorScheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true
): ColorScheme {
    val context = LocalContext.current
    return resolveColorScheme(context = context, darkTheme = darkTheme, dynamicColor = dynamicColor)
}

/**
 * Foundational Material Design 3 (Material You) Theme for Song Dice.
 * Supports dynamic color extraction on Android 12+ (API 31+) with bespoke fallback palettes.
 */
@Composable
fun SongDiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = resolveColorScheme(darkTheme = darkTheme, dynamicColor = dynamicColor)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val windowInsetsController = WindowCompat.getInsetsController(window, view)
                windowInsetsController.isAppearanceLightStatusBars = !darkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SongDiceTypography,
        content = content
    )
}
