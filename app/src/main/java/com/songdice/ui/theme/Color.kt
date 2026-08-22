package com.songdice.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary Dark Slate Background & Elevated Surfaces
val DeepSlateBackground = Color(0xFF0D0E15)
val ElevatedDarkSlateSurface = Color(0xFF161822)
val ElevatedDarkSlateContainer = Color(0xFF1E2230)

// Accent / Tonal Containers
val IndigoAccent = Color(0xFF3D5AFE)
val ElectricAmber = Color(0xFFFFAB00)
val SubtleCyan = Color(0xFF00E5FF)

// Text & Content Colors
val OnDarkTextPrimary = Color(0xFFEEEEEE)
val OnDarkTextSecondary = Color(0xFFB0B3C6)

// Track / Instrumental Accent Colors for Musical Components
val TrackDrumsColor = SubtleCyan
val TrackBassColor = Color(0xFF7C4DFF)
val TrackChordsColor = ElectricAmber
val TrackMelodyColor = Color(0xFFFF4081)

// Bespoke Dark Fallback Color Scheme
val DarkColorScheme = darkColorScheme(
    primary = IndigoAccent,
    onPrimary = Color.White,
    primaryContainer = ElevatedDarkSlateContainer,
    onPrimaryContainer = SubtleCyan,
    secondary = ElectricAmber,
    onSecondary = Color.Black,
    secondaryContainer = ElevatedDarkSlateSurface,
    onSecondaryContainer = ElectricAmber,
    tertiary = SubtleCyan,
    onTertiary = Color.Black,
    background = DeepSlateBackground,
    onBackground = OnDarkTextPrimary,
    surface = ElevatedDarkSlateSurface,
    onSurface = OnDarkTextPrimary,
    surfaceVariant = ElevatedDarkSlateContainer,
    onSurfaceVariant = OnDarkTextSecondary,
    outline = Color(0xFF2A2D3E)
)

// Bespoke Light Fallback Color Scheme
val LightColorScheme = lightColorScheme(
    primary = IndigoAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EAF6),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = Color(0xFFFF8F00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = Color(0xFFFF6F00),
    tertiary = Color(0xFF00B8D4),
    onTertiary = Color.White,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF121212),
    surface = Color.White,
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF455A64),
    outline = Color(0xFFCFD8DC)
)
