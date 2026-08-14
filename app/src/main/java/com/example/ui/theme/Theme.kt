package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkStudioColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = Color.White,
    primaryContainer = StudioSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    tertiary = NeonPink,
    background = StudioBackground,
    onBackground = Color.White,
    surface = StudioSurface,
    onSurface = Color.White,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = Color(0xFFC7C2DA),
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
