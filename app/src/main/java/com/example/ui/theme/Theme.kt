package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkStudioColorScheme = darkColorScheme(
    primary = FlatViolet,
    onPrimary = Color(0xFF1E1E1E), // Dark text on light accent
    primaryContainer = StudioSurfaceVariant,
    onPrimaryContainer = FlatCyan,
    secondary = FlatCyan,
    onSecondary = Color(0xFF121212),
    tertiary = FlatPink,
    background = StudioBackground,
    onBackground = Color(0xFFEEEEEE),
    surface = StudioSurface,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = StudioBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkStudioColorScheme,
        typography = Typography,
        content = content
    )
}
