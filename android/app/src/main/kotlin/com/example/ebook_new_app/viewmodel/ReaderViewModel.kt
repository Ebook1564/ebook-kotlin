package com.example.ebook_new_app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ebook_new_app.db.AppDatabase
import com.example.ebook_new_app.db.Bookmark
import com.example.ebook_new_app.db.Highlight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).locatorDao()

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
}
