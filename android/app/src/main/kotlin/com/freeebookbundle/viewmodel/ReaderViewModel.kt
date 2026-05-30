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
    val fontFamily: String = "serif",
    val lineHeight: Float = 1.5f,
    val textAlign: String = "left"
)

enum class ReaderTheme {
    WHITE, DARK, SEPIA
}

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.locatorDao()
    private val bookDao = database.bookDao()
    private val bookProgressDao = database.bookProgressDao()
    private val prefs = application.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ReaderSettings> = _settings.asStateFlow()

    private fun loadSettings(): ReaderSettings {
        return ReaderSettings(
            fontSize = prefs.getFloat("font_size", 100f).toDouble(),
            theme = try {
                ReaderTheme.valueOf(prefs.getString("theme", ReaderTheme.SEPIA.name) ?: ReaderTheme.SEPIA.name)
            } catch (e: Exception) {
                ReaderTheme.SEPIA
            },
            scrollMode = prefs.getBoolean("scroll_mode", true),
            fontFamily = prefs.getString("font_family", "serif") ?: "serif",
            lineHeight = prefs.getFloat("line_height", 1.5f),
            textAlign = prefs.getString("text_align", "left") ?: "left"
        )
    }

    fun updateSettings(newSettings: ReaderSettings) {
        _settings.value = newSettings
        prefs.edit().apply {
            putFloat("font_size", newSettings.fontSize.toFloat())
            putString("theme", newSettings.theme.name)
            putBoolean("scroll_mode", newSettings.scrollMode)
            putString("font_family", newSettings.fontFamily)
            putFloat("line_height", newSettings.lineHeight)
            putString("text_align", newSettings.textAlign)
            apply()
        }
    }

    fun getBookmarks(bookId: String): Flow<List<Bookmark>> = dao.getBookmarksForBook(bookId)

    fun getHighlights(bookId: String): Flow<List<Highlight>> = dao.getHighlightsForBook(bookId)

    private var lastProgressUpdate = 0L

    fun recordBookOpened(bookId: String) {
        viewModelScope.launch {
            bookDao.updateLastOpened(bookId, System.currentTimeMillis())
        }
    }

    fun updateReadingProgress(bookId: String, spineIndex: Int, progression: Float) {
        val now = System.currentTimeMillis()
        if (now - lastProgressUpdate < 2000) {
            android.util.Log.d("PerfTrace", "Throttled progress update for $bookId (last update was ${now - lastProgressUpdate}ms ago)")
            return // Throttle to 2 seconds
        }
        
        lastProgressUpdate = now
        android.util.Log.d("PerfTrace", "Executing progress update for $bookId at spine=$spineIndex progress=$progression")
        viewModelScope.launch {
            bookProgressDao.upsertProgress(
                com.freeebookbundle.db.BookProgressEntity(
                    bookId = bookId,
                    lastSpineIndex = spineIndex,
                    chapterProgression = progression,
                    lastOpened = now
                )
            )
        }
    }

    fun insertBookmark(bookId: String, chapterIndex: Int, scrollOffset: Int, progress: Float, title: String = "", locator: String = "") {
        viewModelScope.launch {
            dao.insertBookmark(
                Bookmark(
                    bookId = bookId,
                    chapterIndex = chapterIndex,
                    scrollOffset = scrollOffset,
                    progress = progress,
                    title = title,
                    locator = locator
                )
            )
        }
    }

    // Legacy overload for Readium-based ReaderFragment
    fun insertBookmark(bookId: String, locatorJson: String) {
        viewModelScope.launch {
            dao.insertBookmark(
                Bookmark(
                    bookId = bookId,
                    chapterIndex = 0,
                    scrollOffset = 0,
                    progress = 0f,
                    title = "Bookmark",
                    locator = locatorJson
                )
            )
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            dao.deleteBookmark(bookmark)
        }
    }

    fun insertHighlight(
        bookId: String,
        chapterIndex: Int,
        startOffset: Int,
        endOffset: Int,
        selectedText: String,
        color: Int,
        annotation: String? = null,
        locator: String = ""
    ) {
        viewModelScope.launch {
            dao.insertHighlight(
                Highlight(
                    bookId = bookId,
                    chapterIndex = chapterIndex,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    selectedText = selectedText,
                    color = color,
                    annotation = annotation,
                    locator = locator
                )
            )
        }
    }

    // Legacy overload for Readium-based ReaderFragment
    fun insertHighlight(bookId: String, locatorJson: String, color: Int, annotation: String? = null) {
        viewModelScope.launch {
            dao.insertHighlight(
                Highlight(
                    bookId = bookId,
                    chapterIndex = 0,
                    startOffset = 0,
                    endOffset = 0,
                    selectedText = "",
                    color = color,
                    annotation = annotation,
                    locator = locatorJson
                )
            )
        }
    }

    fun deleteHighlight(highlight: Highlight) {
        viewModelScope.launch {
            dao.deleteHighlight(highlight)
        }
    }

    suspend fun getHighlightsSync(bookId: String): List<Highlight> = dao.getHighlightsForBookSync(bookId)
}
