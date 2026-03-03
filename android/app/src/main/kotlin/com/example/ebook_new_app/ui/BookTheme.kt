package com.example.ebook_new_app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ebook_new_app.R

enum class ThemeType {
    VINTAGE, PITCH_BLACK
}

val ITAUrlionexFontFamily = FontFamily(
    Font(R.font.ita_urlionex_regular, FontWeight.Normal),
    Font(R.font.ita_urlionex_regular_italic, FontWeight.Normal, style = androidx.compose.ui.text.font.FontStyle.Italic),
    Font(R.font.ita_urlionex_medium, FontWeight.Medium),
    Font(R.font.ita_urlionex_medium_italic, FontWeight.Medium, style = androidx.compose.ui.text.font.FontStyle.Italic),
    Font(R.font.ita_urlionex_semi_bold, FontWeight.SemiBold),
    Font(R.font.ita_urlionex_semi_bold_italic, FontWeight.SemiBold, style = androidx.compose.ui.text.font.FontStyle.Italic),
    Font(R.font.ita_urlionex_bold, FontWeight.Bold),
    Font(R.font.ita_urlionex_bold_italic, FontWeight.Bold, style = androidx.compose.ui.text.font.FontStyle.Italic),
)

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
    primary = Color(0xFF3E2723), // Changed from red to brown
    onPrimary = Color.White,
    surface = Color(0xFF3E2723).copy(alpha = 0.05f),
    onSurface = Color(0xFF3E2723),
    fontFamily = ITAUrlionexFontFamily,
    type = ThemeType.VINTAGE
)

val PitchBlackTheme = BookThemeColors(
    background = Color.Black,
    onBackground = Color.White,
    primary = Color(0xFFE50914),
    onPrimary = Color.White,
    surface = Color(0xFF1A1A1A),
    onSurface = Color.White,
    fontFamily = ITAUrlionexFontFamily,
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
