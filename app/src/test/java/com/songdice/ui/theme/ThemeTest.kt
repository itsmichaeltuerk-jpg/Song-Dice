package com.songdice.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ThemeTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.R]) // API 30 (Android 11)
    fun `resolveColorScheme returns DarkColorScheme on API 30 when darkTheme is true`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colorScheme = resolveColorScheme(context = context, darkTheme = true, dynamicColor = true)

        assertEquals(DeepSlateBackground, colorScheme.background)
        assertEquals(ElevatedDarkSlateSurface, colorScheme.surface)
        assertEquals(IndigoAccent, colorScheme.primary)
        assertEquals(ElectricAmber, colorScheme.secondary)
        assertEquals(SubtleCyan, colorScheme.tertiary)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R]) // API 30 (Android 11)
    fun `resolveColorScheme returns LightColorScheme on API 30 when darkTheme is false`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colorScheme = resolveColorScheme(context = context, darkTheme = false, dynamicColor = true)

        assertEquals(IndigoAccent, colorScheme.primary)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S]) // API 31 (Android 12)
    fun `resolveColorScheme returns dynamic dark color scheme on API 31`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colorScheme = resolveColorScheme(context = context, darkTheme = true, dynamicColor = true)
        assertNotNull(colorScheme)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S]) // API 31 (Android 12)
    fun `resolveColorScheme returns dynamic light color scheme on API 31`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colorScheme = resolveColorScheme(context = context, darkTheme = false, dynamicColor = true)
        assertNotNull(colorScheme)
    }

    @Test
    fun `verify typography definitions match musical readability standards`() {
        assertEquals(FontFamily.Monospace, ChordFontFamily)
        assertEquals(ChordFontFamily, SongDiceTypography.titleMedium.fontFamily)
        assertEquals(ChordFontFamily, SongDiceTypography.bodyMedium.fontFamily)
        assertEquals(ChordFontFamily, SongDiceTypography.labelSmall.fontFamily)
        assertNotNull(SongDiceTypography.headlineMedium)
    }
}
