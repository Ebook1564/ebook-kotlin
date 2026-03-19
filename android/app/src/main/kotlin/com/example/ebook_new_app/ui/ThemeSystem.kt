package com.example.ebook_new_app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
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

val DayColors = AppColors(
    background = Color(0xFFFDFCF0),
    surface = Color(0xFFF5F1E0),
    primary = Color(0xFF5B4636),
    textPrimary = Color(0xFF2C1E11),
    textSecondary = Color(0xFF8D7A66),
    navBackground = Color(0xAAFFFFFF),
    isDark = false
)

val SpaceColors = AppColors(
    background = Color.Transparent, 
    surface = Color(0xFF1A1B23),
    primary = Color(0xFF6A1B9A),
    textPrimary = Color.White,
    textSecondary = Color(0xFFB0B0C0),
    navBackground = Color(0x9903040F),
    isDark = true
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

val SpaceTypography = AppTypography(
    headline = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = 1.sp),
    body = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    label = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    serif = false
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
    val context = LocalContext.current
    
    val spaceFont = remember {
        val assets = context.assets.list("fonts") ?: emptyArray()
        val fontPath = "fonts/SpaceGrotesk-VariableFont_wght.ttf"
        val fileName = fontPath.removePrefix("fonts/")
        
        if (assets.contains(fileName)) {
            FontFamily(Font(assetManager = context.assets, path = fontPath))
        } else {
            val fallbackFileName = assets.find { it.contains("ITAurlionexDEMO-Regular", ignoreCase = true) }
            if (fallbackFileName != null) {
                FontFamily(Font(assetManager = context.assets, path = "fonts/$fallbackFileName"))
            } else {
                FontFamily.SansSerif
            }
        }
    }

    val colors = when (themeMode) {
        AppThemeMode.DAY -> DayColors
        AppThemeMode.NIGHT -> NightColors
        AppThemeMode.COSMIC -> SpaceColors
    }
    val typography = when (themeMode) {
        AppThemeMode.DAY -> DayTypography
        AppThemeMode.NIGHT -> NightTypography
        AppThemeMode.COSMIC -> SpaceTypography.copy(
            headline = SpaceTypography.headline.copy(fontFamily = spaceFont),
            body = SpaceTypography.body.copy(fontFamily = spaceFont),
            label = SpaceTypography.label.copy(fontFamily = spaceFont)
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        content = content
    )
}
