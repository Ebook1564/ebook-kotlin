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
                ReaderTheme.valueOf(prefs.getString("theme", ReaderTheme.SEPIA.name) ?: ReaderTheme.SEPIA.name)
            } catch (e: Exception) {
                ReaderTheme.SEPIA
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

    fun isNativeReaderEnabled(): Boolean {
        return prefs.getBoolean("use_native_reader", true)
    }

    fun setNativeReaderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("use_native_reader", enabled).apply()
    }

    fun getNativeFontSize(): Int {
        return prefs.getInt("native_font_size", 100)
    }

    fun setNativeFontSize(size: Int) {
        prefs.edit().putInt("native_font_size", size).apply()
    }

    fun setTheme(theme: ReaderTheme) {
        prefs.edit().putString("theme", theme.name).apply()
    }

    fun getNativeReaderPosition(bookId: String): Pair<Int, Float>? {
        val position = prefs.getString("native_pos_$bookId", null) ?: return null
        val parts = position.split(",")
        if (parts.size == 2) {
            val chapter = parts[0].toIntOrNull()
            val progress = parts[1].toFloatOrNull()
            if (chapter != null && progress != null) {
                return chapter to progress
            }
        }
        return null
    }

    fun setNativeReaderPosition(bookId: String, chapterIndex: Int, scrollOffset: Float) {
        prefs.edit().putString("native_pos_$bookId", "$chapterIndex,$scrollOffset").apply()
    }

    fun markAsReading(bookId: String) {
        val recentReads = prefs.getStringSet("recent_reads", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        recentReads.add(bookId)
        val limited = recentReads.toList().takeLast(10).toSet()
        prefs.edit().putStringSet("recent_reads", limited).apply()
    }

    fun getRecentlyReadBookIds(): Set<String> {
        return prefs.getStringSet("recent_reads", emptySet()) ?: emptySet()
    }

    fun getLastTabIndex(): Int = prefs.getInt("last_tab", 0)

    fun setLastTabIndex(index: Int) {
        prefs.edit().putInt("last_tab", index).apply()
    }

    fun hasSeenReaderTutorial(): Boolean {
        return prefs.getBoolean("has_seen_reader_tutorial", false)
    }

    fun setHasSeenReaderTutorial(hasSeen: Boolean) {
        prefs.edit().putBoolean("has_seen_reader_tutorial", hasSeen).apply()
    }
}
