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

enum class LibrarySortOrder {
    RECENTLY_OPENED,
    DATE_ADDED,
    TITLE,
    AUTHOR,
    PROGRESS
}

data class LibraryUiState(
    val currentlyReading: List<Book> = emptyList(),
    val completed: List<Book> = emptyList(),
    val wishlist: List<Book> = emptyList(),
    val dynamicTags: List<String> = listOf("All"),
    val dynamicAuthors: List<String> = emptyList(),
    val selectedTag: String = "All",
    val selectedAuthor: String? = null,
    val isLoading: Boolean = false,
    val seeAllStatus: ReadingStatus? = null,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_OPENED
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

                updateSortedBooks(libraryBooks, tags, authors)
            }
        }
    }

    private fun updateSortedBooks(libraryBooks: List<Book>, tags: List<String> = emptyList(), authors: List<String> = emptyList()) {
        val sorted = when (_uiState.value.sortOrder) {
            LibrarySortOrder.RECENTLY_OPENED -> libraryBooks.sortedByDescending { it.lastOpened ?: 0L }
            LibrarySortOrder.DATE_ADDED -> libraryBooks.sortedByDescending { it.dateAdded ?: 0L }
            LibrarySortOrder.TITLE -> libraryBooks.sortedBy { it.title }
            LibrarySortOrder.AUTHOR -> libraryBooks.sortedBy { it.author }
            LibrarySortOrder.PROGRESS -> libraryBooks.sortedByDescending { it.getReadingProgressPercent() }
        }

        _uiState.value = _uiState.value.copy(
            currentlyReading = sorted.filter { it.readingStatus == ReadingStatus.CURRENTLY_READING },
            completed = sorted.filter { it.readingStatus == ReadingStatus.COMPLETED },
            wishlist = sorted.filter { it.readingStatus == ReadingStatus.WISHLIST },
            dynamicTags = if (tags.isNotEmpty()) listOf("All") + tags else _uiState.value.dynamicTags,
            dynamicAuthors = if (authors.isNotEmpty()) authors else _uiState.value.dynamicAuthors,
            isLoading = false
        )
    }

    fun setSortOrder(order: LibrarySortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
        // Re-trigger observation or manually re-sort
        viewModelScope.launch {
            bookDao.getLibraryBooksFlow().collectLatest { entities ->
                updateSortedBooks(entities.map { it.toBook() })
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
            val existing = bookDao.getBook(book.id)
            val timestamp = System.currentTimeMillis()
            
            if (existing == null) {
                // Insert new entity
                val entity = com.freeebookbundle.db.BookEntity(
                    id = book.id,
                    title = book.title,
                    author = book.author,
                    description = book.description,
                    categoryCode = book.categories.firstOrNull() ?: "",
                    sourceFolder = book.sourceFolder ?: "remote",
                    coverImageName = null,
                    epubFileName = book.epubFileName,
                    isDownloaded = false,
                    readingStatus = status,
                    dateAdded = if (status != ReadingStatus.NONE) timestamp else null,
                    subjects = book.subjects,
                    externalCoverUrl = book.externalCoverUrl,
                    audioUrl = book.audioUrl
                )
                bookDao.insertBook(entity)
            } else {
                // Update existing entity status and dateAdded
                val dateAdded = if (status != ReadingStatus.NONE) timestamp else existing.dateAdded
                bookDao.updateReadingStatus(book.id, status, dateAdded)
            }
            
            if (status == ReadingStatus.CURRENTLY_READING) {
                bookDao.updateLastOpened(book.id, timestamp)
            }
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
