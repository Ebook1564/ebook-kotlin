package com.example.ebook_new_app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ebook_new_app.EbookApp
import com.example.ebook_new_app.db.toBook
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.models.ReadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class LibraryUiState(
    val currentlyReading: List<Book> = emptyList(),
    val completed: List<Book> = emptyList(),
    val wishlist: List<Book> = emptyList(),
    val genres: List<String> = listOf("All", "Legal Classics", "Historical Law", "Trials", "Popular"),
    val selectedGenre: String = "All",
    val isLoading: Boolean = false,
    val seeAllStatus: ReadingStatus? = null // When not null, show dedicated see all page
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as EbookApp).database
    private val bookDao = database.bookDao()
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var allBooksInternal: List<Book> = emptyList()

    init {
        observeBooks()
    }

    private fun observeBooks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            bookDao.getAllBooks().collectLatest { entities ->
                allBooksInternal = entities.map { it.toBook() }
                filterAndPopulate()
            }
        }
    }

    fun selectGenre(genre: String) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
        filterAndPopulate()
    }

    private fun filterAndPopulate() {
        val selectedGenre = _uiState.value.selectedGenre
        val filtered = if (selectedGenre == "All") {
            allBooksInternal
        } else {
            // Mapping UI genre names to DB codes if needed, or searching title/categories
            // For now, let's just filter by our internal list logic or metadata
            allBooksInternal
        }

        _uiState.value = _uiState.value.copy(
            currentlyReading = filtered.filter { it.readingStatus == ReadingStatus.CURRENTLY_READING },
            completed = filtered.filter { it.readingStatus == ReadingStatus.COMPLETED },
            wishlist = filtered.filter { it.readingStatus == ReadingStatus.WISHLIST },
            isLoading = false
        )
    }

    fun updateReadingStatus(book: Book, status: ReadingStatus) {
        viewModelScope.launch {
            val dateStarted = if (status == ReadingStatus.CURRENTLY_READING) System.currentTimeMillis() else book.dateStarted
            bookDao.updateReadingStatus(book.id, status, dateStarted)
        }
    }

    fun markAsFinished(book: Book) {
        viewModelScope.launch {
            bookDao.markAsCompleted(book.id, ReadingStatus.COMPLETED, System.currentTimeMillis())
        }
    }

    fun setSeeAll(status: ReadingStatus?) {
        _uiState.value = _uiState.value.copy(seeAllStatus = status)
    }
}
