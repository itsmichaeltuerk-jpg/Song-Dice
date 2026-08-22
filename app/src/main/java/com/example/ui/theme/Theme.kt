package com.example.ui.theme

import androidx.compose.runtime.Composable
import com.songdice.ui.theme.SongDiceTheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    SongDiceTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
