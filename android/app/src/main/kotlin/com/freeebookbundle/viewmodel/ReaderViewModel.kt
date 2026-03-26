package com.freeebookbundle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freeebookbundle.db.AppDatabase
import com.freeebookbundle.db.Bookmark
import com.freeebookbundle.db.Highlight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReaderSettings(
    val fontSize: Double = 100.0,
    val theme: ReaderTheme = ReaderTheme.WHITE,
    val scrollMode: Boolean = true,
    val fontFamily: String = "serif"
)

enum class ReaderTheme {
    WHITE, DARK, SEPIA
}

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).locatorDao()
    private val prefs = application.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ReaderSettings> = _settings.asStateFlow()

    private fun loadSettings(): ReaderSettings {
        return ReaderSettings(
            fontSize = prefs.getFloat("font_size", 100f).toDouble(),
            theme = ReaderTheme.valueOf(prefs.getString("theme", ReaderTheme.WHITE.name) ?: ReaderTheme.WHITE.name),
            scrollMode = prefs.getBoolean("scroll_mode", false),
            fontFamily = prefs.getString("font_family", "serif") ?: "serif"
        )
    }

    fun updateSettings(newSettings: ReaderSettings) {
        _settings.value = newSettings
        prefs.edit().apply {
            putFloat("font_size", newSettings.fontSize.toFloat())
            putString("theme", newSettings.theme.name)
            putBoolean("scroll_mode", newSettings.scrollMode)
            putString("font_family", newSettings.fontFamily)
            apply()
        }
    }

    fun getBookmarks(bookId: String): Flow<List<Bookmark>> = dao.getBookmarksForBook(bookId)
    
    fun getHighlights(bookId: String): Flow<List<Highlight>> = dao.getHighlightsForBook(bookId)

    fun insertBookmark(bookId: String, locatorJson: String) {
        viewModelScope.launch {
            dao.insertBookmark(Bookmark(bookId = bookId, locator = locatorJson))
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            dao.deleteBookmark(bookmark)
        }
    }

    fun insertHighlight(bookId: String, locatorJson: String, color: Int, annotation: String? = null) {
        viewModelScope.launch {
            dao.insertHighlight(Highlight(bookId = bookId, locator = locatorJson, color = color, annotation = annotation))
        }
    }

    fun deleteHighlight(highlight: Highlight) {
        viewModelScope.launch {
            dao.deleteHighlight(highlight)
        }
    }

    suspend fun getHighlightsSync(bookId: String): List<Highlight> = dao.getHighlightsForBookSync(bookId)
}
