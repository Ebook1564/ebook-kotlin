package com.freeebookbundle.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.freeebookbundle.viewmodel.ReaderSettings
import com.freeebookbundle.viewmodel.ReaderTheme

class ReaderSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): ReaderSettings {
        return ReaderSettings(
            fontSize = prefs.getFloat("font_size", 100f).toDouble(),
            theme = try {
                ReaderTheme.valueOf(prefs.getString("theme", ReaderTheme.WHITE.name) ?: ReaderTheme.WHITE.name)
            } catch (e: Exception) {
                ReaderTheme.WHITE
            },
            scrollMode = prefs.getBoolean("scroll_mode", true),
            fontFamily = prefs.getString("font_family", "serif") ?: "serif"
        )
    }

    fun getThemeColor(): Color {
        return when (loadSettings().theme) {
            ReaderTheme.WHITE -> Color.White
            ReaderTheme.DARK -> Color(0xFF121212)
            ReaderTheme.SEPIA -> Color(0xFFFDFCF0)
        }
    }
    
    fun getTextColor(): Color {
        return when (loadSettings().theme) {
            ReaderTheme.WHITE -> Color.Black
            ReaderTheme.DARK -> Color.White
            ReaderTheme.SEPIA -> Color(0xFF5B4636)
        }
    }
}
