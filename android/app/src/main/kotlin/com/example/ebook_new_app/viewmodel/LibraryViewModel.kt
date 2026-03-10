package com.example.ebook_new_app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.ebook_new_app.EbookApp
import com.example.ebook_new_app.db.toBook
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.models.ReadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map as flowMap
import kotlinx.coroutines.launch

data class LibraryUiState(
    val currentlyReading: List<Book> = emptyList(),
    val completed: List<Book> = emptyList(),
    val wishlist: List<Book> = emptyList(),
    val genres: List<String> = listOf("All", "Legal Classics", "Historical Law", "Trials", "Popular"),
    val selectedGenre: String = "All",
    val isLoading: Boolean = false,
    val seeAllStatus: ReadingStatus? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as EbookApp).database
    private val bookDao = database.bookDao()
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeBooks()
    }

    private fun observeBooks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            bookDao.getAllBooks().collectLatest { entities ->
                val allBooks = entities.map { it.toBook() }
                _uiState.value = _uiState.value.copy(
                    currentlyReading = allBooks.filter { it.readingStatus == ReadingStatus.CURRENTLY_READING },
                    completed = allBooks.filter { it.readingStatus == ReadingStatus.COMPLETED },
                    wishlist = allBooks.filter { it.readingStatus == ReadingStatus.WISHLIST },
                    isLoading = false
                )
            }
        }
    }

    // PAGING FOR SEE ALL SCREEN
    fun getPagedBooks(status: ReadingStatus): Flow<PagingData<Book>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { bookDao.getBooksByStatusPaging(status) }
        ).flow.flowMap { pagingData ->
            pagingData.map { it.toBook() }
        }.cachedIn(viewModelScope)
    }

    fun selectGenre(genre: String) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
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
