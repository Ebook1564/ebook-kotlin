package com.example.ebook_new_app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class ThemeType {
    VINTAGE, PITCH_BLACK
}

data class BookThemeColors(
    val background: Color,
    val onBackground: Color,
    val primary: Color,
    val onPrimary: Color,
    val surface: Color,
    val onSurface: Color,
    val fontFamily: FontFamily,
    val type: ThemeType
)

val VintageTheme = BookThemeColors(
    background = Color(0xFFF4EBD0),
    onBackground = Color(0xFF3E2723),
    primary = Color(0xFFE50914),
    onPrimary = Color.White,
    surface = Color(0xFF3E2723).copy(alpha = 0.05f),
    onSurface = Color(0xFF3E2723),
    fontFamily = FontFamily.Serif,
    type = ThemeType.VINTAGE
)

val PitchBlackTheme = BookThemeColors(
    background = Color.Black,
    onBackground = Color.White,
    primary = Color(0xFFE50914),
    onPrimary = Color.White,
    surface = Color(0xFF1A1A1A),
    onSurface = Color.White,
    fontFamily = FontFamily.SansSerif,
    type = ThemeType.PITCH_BLACK
)

val LocalBookTheme = staticCompositionLocalOf { VintageTheme }

@Composable
fun ProvideBookTheme(themeType: ThemeType, content: @Composable () -> Unit) {
    val theme = when (themeType) {
        ThemeType.VINTAGE -> VintageTheme
        ThemeType.PITCH_BLACK -> PitchBlackTheme
    }
    CompositionLocalProvider(LocalBookTheme provides theme) {
        content()
    }
}
