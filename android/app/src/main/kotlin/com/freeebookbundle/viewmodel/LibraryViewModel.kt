package com.freeebookbundle.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map as flowMap
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

enum class LibrarySortOrder {
    RECENTLY_OPENED, DATE_ADDED, TITLE, AUTHOR, PROGRESS
}

@Immutable
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

    private val _sortOrder     = MutableStateFlow(LibrarySortOrder.RECENTLY_OPENED)
    private val _isLoading     = MutableStateFlow(false)
    private val _selectedTag   = MutableStateFlow("All")
    private val _selectedAuthor = MutableStateFlow<String?>(null)
    private val _seeAllStatus  = MutableStateFlow<ReadingStatus?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        _sortOrder,
        _selectedTag,
        _selectedAuthor,
        _seeAllStatus,
        bookDao.getLibraryBooksFlow().distinctUntilChanged()
    ) { sortOrder, tag, author, seeAll, entities ->
        DataBundle(entities, sortOrder, tag, author, seeAll)
    }
    .debounce(80)
    .flowMap { bundle ->
        val entities = bundle.entities
        val libraryBooks = entities.map { it.toBook() }

        val tags    = libraryBooks.flatMap { it.subjects }.distinct().take(10)
        val authors = libraryBooks.map { it.author }.distinct().sorted()

        val sorted = when (bundle.sortOrder) {
            LibrarySortOrder.RECENTLY_OPENED -> libraryBooks.sortedByDescending { it.lastOpened ?: 0L }
            LibrarySortOrder.DATE_ADDED      -> libraryBooks.sortedByDescending { it.dateAdded ?: 0L }
            LibrarySortOrder.TITLE           -> libraryBooks.sortedBy { it.title }
            LibrarySortOrder.AUTHOR          -> libraryBooks.sortedBy { it.author }
            LibrarySortOrder.PROGRESS        -> libraryBooks.sortedByDescending { it.getReadingProgressPercent() }
        }

        android.util.Log.d("LibraryVM", "Processed ${entities.size} active books (Sort: ${bundle.sortOrder})")

        LibraryUiState(
            currentlyReading = sorted.filter { it.readingStatus == ReadingStatus.CURRENTLY_READING },
            completed        = sorted.filter { it.readingStatus == ReadingStatus.COMPLETED },
            wishlist         = sorted.filter { it.readingStatus == ReadingStatus.WISHLIST },
            dynamicTags      = listOf("All") + tags,
            dynamicAuthors   = authors,
            selectedTag      = bundle.tag,
            selectedAuthor   = bundle.author,
            isLoading        = _isLoading.value,
            seeAllStatus     = bundle.seeAll,
            sortOrder        = bundle.sortOrder
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    private data class DataBundle(
        val entities: List<com.freeebookbundle.db.BookEntity>,
        val sortOrder: LibrarySortOrder,
        val tag: String,
        val author: String?,
        val seeAll: ReadingStatus?
    )

    fun setSortOrder(order: LibrarySortOrder)   { _sortOrder.value = order }
    fun selectTag(tag: String)                  { _selectedTag.value = tag; _selectedAuthor.value = null }
    fun selectAuthor(author: String?)           { _selectedAuthor.value = author; _selectedTag.value = "All" }
    fun setSeeAll(status: ReadingStatus?)       { _seeAllStatus.value = status }

    fun getPagedBooks(status: ReadingStatus): Flow<PagingData<Book>> =
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { bookDao.getBooksByStatusPaging(status) }
        ).flow.flowMap { it.map { entity -> entity.toBook() } }.cachedIn(viewModelScope)

    fun updateReadingStatus(book: Book, status: ReadingStatus) {
        viewModelScope.launch {
            val existing  = bookDao.getBook(book.id)
            val timestamp = System.currentTimeMillis()
            if (existing == null) {
                bookDao.insertBook(com.freeebookbundle.db.BookEntity(
                    id = book.id, title = book.title, author = book.author,
                    description = book.description,
                    categoryCode = book.categories.firstOrNull() ?: "",
                    sourceFolder = book.sourceFolder ?: "remote",
                    coverImageName = null, epubFileName = book.epubFileName,
                    isDownloaded = false, readingStatus = status,
                    dateAdded = if (status != ReadingStatus.NONE) timestamp else null,
                    subjects = book.subjects, externalCoverUrl = book.externalCoverUrl,
                    audioUrl = book.audioUrl
                ))
            } else {
                bookDao.updateReadingStatus(
                    book.id, status,
                    if (status != ReadingStatus.NONE) timestamp else existing.dateAdded
                )
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
}
