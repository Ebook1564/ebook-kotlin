package com.freeebookbundle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.freeebookbundle.EbookApp
import com.freeebookbundle.db.toBook
import com.freeebookbundle.models.Book
import com.freeebookbundle.models.ReadingStatus
import com.freeebookbundle.utils.Analytics
import com.freeebookbundle.utils.ReviewManager
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
    val dynamicTags: List<String> = listOf("All"),
    val dynamicAuthors: List<String> = emptyList(),
    val selectedTag: String = "All",
    val selectedAuthor: String? = null,
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
            
            bookDao.getLibraryBooksFlow().collectLatest { entities ->
                android.util.Log.d("LibraryVM", "Received ${entities.size} active books from DB")
                val libraryBooks = entities.map { it.toBook() }
                
                val tags = libraryBooks.flatMap { it.subjects }.distinct().take(10)
                val authors = libraryBooks.map { it.author }.distinct().sorted()

                _uiState.value = _uiState.value.copy(
                    currentlyReading = libraryBooks.filter { it.readingStatus == ReadingStatus.CURRENTLY_READING },
                    completed = libraryBooks.filter { it.readingStatus == ReadingStatus.COMPLETED },
                    wishlist = libraryBooks.filter { it.readingStatus == ReadingStatus.WISHLIST },
                    dynamicTags = listOf("All") + tags,
                    dynamicAuthors = authors,
                    isLoading = false
                )
            }
        }
    }

    fun getPagedBooks(status: ReadingStatus): Flow<PagingData<Book>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { bookDao.getBooksByStatusPaging(status) }
        ).flow.flowMap { pagingData ->
            pagingData.map { it.toBook() }
        }.cachedIn(viewModelScope)
    }

    fun selectTag(tag: String) {
        _uiState.value = _uiState.value.copy(selectedTag = tag, selectedAuthor = null)
    }

    fun selectAuthor(author: String?) {
        _uiState.value = _uiState.value.copy(selectedAuthor = author, selectedTag = "All")
    }

    fun updateReadingStatus(book: Book, status: ReadingStatus) {
        viewModelScope.launch {
            val dateAdded = if (status != ReadingStatus.NONE) System.currentTimeMillis() else null
            bookDao.updateReadingStatus(book.id, status, dateAdded)
        }
    }

    fun markAsFinished(book: Book) {
        viewModelScope.launch {
            bookDao.markAsCompleted(book.id, ReadingStatus.COMPLETED, System.currentTimeMillis())
            Analytics.trackBookCompleted(book.id)
            ReviewManager.onBookCompleted(getApplication())
        }
    }

    fun setSeeAll(status: ReadingStatus?) {
        _uiState.value = _uiState.value.copy(seeAllStatus = status)
    }
}
