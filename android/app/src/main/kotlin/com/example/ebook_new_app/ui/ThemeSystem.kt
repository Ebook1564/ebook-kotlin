package com.example.ebook_new_app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ebook_new_app.viewmodel.AppThemeMode

data class AppColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val navBackground: Color,
    val isDark: Boolean
)

data class AppTypography(
    val headline: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val serif: Boolean
)

val NightColors = AppColors(
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    primary = Color(0xFFE50914),
    textPrimary = Color.White,
    textSecondary = Color.Gray,
    navBackground = Color(0x99121212),
    isDark = true
)

val SpaceColorsTheme = AppColors(
    background = Color.Transparent,
    surface = Color(0xFF121212),
    primary = Color(0xFFE50914),
    textPrimary = Color.White,
    textSecondary = Color.Gray,
    navBackground = Color(0x99121212),
    isDark = true
)

val DayColors = AppColors(
    background = Color(0xFFFDFCF0),
    surface = Color(0xFFF5F1E0),
    primary = Color(0xFF5B4636),
    textPrimary = Color(0xFF2C1E11),
    textSecondary = Color(0xFF8D7A66),
    navBackground = Color(0xAAFFFFFF),
    isDark = false
)

val NightTypography = AppTypography(
    headline = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 24.sp),
    body = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    label = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp),
    serif = false
)

val DayTypography = AppTypography(
    headline = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    body = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    label = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    serif = true
)

val LocalAppColors = staticCompositionLocalOf { NightColors }
val LocalAppTypography = staticCompositionLocalOf { NightTypography }

object BookVerseTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current
}

@Composable
fun BookVerseTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val colors = when (themeMode) {
        AppThemeMode.NIGHT -> NightColors
        AppThemeMode.DAY -> DayColors
        AppThemeMode.SPACE -> SpaceColorsTheme
    }
    val typography = if (themeMode == AppThemeMode.DAY) DayTypography else NightTypography

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        content = content
    )
}
