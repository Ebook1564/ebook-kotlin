package com.example.ebook_new_app.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.example.ebook_new_app.EbookApp
import com.example.ebook_new_app.db.toBook
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.repository.ExternalMetadataRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InitialBounds(
    val left: Float, val top: Float, val width: Float, val height: Float
)

data class HomeSection(
    val title: String,
    val books: List<Book>
)

data class BookCollection(
    val title: String,
    val description: String,
    val accentColor: Color,
    val query: String
)

sealed class SearchFilter {
    object None : SearchFilter()
    data class Author(val name: String) : SearchFilter()
    data class Category(val code: String) : SearchFilter()
}

data class HomeUiState(
    val userName: String = "Aayush",
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val authorSearchQuery: String = "",
    val activeFilter: SearchFilter = SearchFilter.None,
    val selectedCategoryForSearch: String? = null,
    val currentCollection: BookCollection? = null,
    val transitionBook: Book? = null,
    val initialBounds: InitialBounds? = null,
    val isMetadataLoading: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as EbookApp).database
    private val bookDao = database.bookDao()
    private val metadataRepo = ExternalMetadataRepository(bookDao)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchRequest = MutableStateFlow(SearchRequest())
    
    data class SearchRequest(
        val query: String = "",
        val filter: SearchFilter = SearchFilter.None
    )

    private val _homeSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val homeSections: StateFlow<List<HomeSection>> = _homeSections.asStateFlow()

    private val _billboardBooks = MutableStateFlow<List<Book>>(emptyList())
    val billboardBooks: StateFlow<List<Book>> = _billboardBooks.asStateFlow()

    @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filterResults: Flow<PagingData<Book>> = _searchRequest
        .debounce { request ->
            if (request.filter is SearchFilter.None) 300L else 0L
        }
        .flatMapLatest { request ->
            Pager(config = PagingConfig(pageSize = 30, enablePlaceholders = false)) {
                when (val filter = request.filter) {
                    is SearchFilter.Author -> bookDao.getBooksByAuthorPaging(filter.name)
                    is SearchFilter.Category -> bookDao.getBooksByCategoryNamePaging(filter.code)
                    else -> bookDao.searchBooksPaging(request.query)
                }
            }.flow.map { data -> data.map { it.toBook() } }
        }.cachedIn(viewModelScope)

    @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val curatedCollectionBooks: Flow<PagingData<Book>> = _uiState
        .map { it.currentCollection }
        .distinctUntilChanged()
        .flatMapLatest { collection ->
            if (collection == null) flowOf(PagingData.empty())
            else {
                Pager(config = PagingConfig(pageSize = 30)) {
                    bookDao.getCollectionBooksPaging(collection.query)
                }.flow.map { data -> data.map { it.toBook() } }
            }
        }.cachedIn(viewModelScope)

    val authorResults: Flow<PagingData<String>> = _uiState
        .map { it.authorSearchQuery }
        .debounce(300)
        .flatMapLatest { query ->
            Pager(config = PagingConfig(pageSize = 50, enablePlaceholders = false)) {
                bookDao.getAuthorsPaging(query)
            }.flow
        }.cachedIn(viewModelScope)

    init {
        loadStableHomeData()
        
        viewModelScope.launch {
            homeSections.first { it.isNotEmpty() }
            
            _billboardBooks.value.forEach { book ->
                if (book.externalCoverUrl.isNullOrEmpty()) {
                    launch { 
                        val updated = metadataRepo.fetchAndCacheMetadata(book)
                        updateBookInLists(updated)
                    }
                }
            }
            
            _homeSections.value.forEach { section ->
                section.books.take(2).forEach { book ->
                    if (book.externalCoverUrl.isNullOrEmpty()) {
                        launch {
                            val updated = metadataRepo.fetchAndCacheMetadata(book)
                            updateBookInLists(updated)
                        }
                    }
                }
            }
        }
    }

    private fun loadStableHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val categories = bookDao.getAllCategories().first()
            val sections = categories.map { cat ->
                val books = bookDao.getBooksByCategoryLimit(cat.code, 15).map { it.toBook() }
                HomeSection(cat.name, books)
            }
            _homeSections.value = sections
            _billboardBooks.value = sections.flatMap { it.books }.shuffled().take(6)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun updateBookInLists(updated: Book) {
        _billboardBooks.value = _billboardBooks.value.map { if (it.id == updated.id) updated else it }
        _homeSections.value = _homeSections.value.map { section ->
            section.copy(books = section.books.map { if (it.id == updated.id) updated else it })
        }
    }

    fun openCollection(tag: String) = viewModelScope.launch {
        val collection = when {
            tag.contains("Gothic", ignoreCase = true) -> BookCollection("Gothic Echoes", "Dark, moody tales.", Color(0xFF37474F), "Gothic")
            else -> BookCollection(tag, "A curated selection.", Color(0xFFE50914), tag)
        }
        _uiState.value = _uiState.value.copy(currentCollection = collection)
    }

    fun closeCollection() { _uiState.value = _uiState.value.copy(currentCollection = null) }

    fun fetchMetadataForBook(book: Book, onUpdated: (Book) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMetadataLoading = true)
            val updated = metadataRepo.fetchAndCacheMetadata(book)
            _uiState.value = _uiState.value.copy(isMetadataLoading = false)
            updateBookInLists(updated)
            onUpdated(updated)
        }
    }

    fun onSearchQueryChanged(q: String) {
        _searchRequest.value = SearchRequest(query = q, filter = SearchFilter.None)
        _uiState.value = _uiState.value.copy(searchQuery = q, activeFilter = SearchFilter.None, selectedCategoryForSearch = null)
    }

    fun onAuthorSearchQueryChanged(q: String) {
        _uiState.value = _uiState.value.copy(authorSearchQuery = q)
    }

    fun setAuthorFilter(name: String) {
        _searchRequest.value = SearchRequest(query = "", filter = SearchFilter.Author(name))
        _uiState.value = _uiState.value.copy(activeFilter = SearchFilter.Author(name), searchQuery = "", selectedCategoryForSearch = null)
    }

    fun navigateToCategory(cat: String) {
        val f = if (cat.isEmpty()) SearchFilter.None else SearchFilter.Category(cat)
        _searchRequest.value = SearchRequest(query = "", filter = f)
        _uiState.value = _uiState.value.copy(selectedCategoryForSearch = cat, activeFilter = f, searchQuery = "")
    }

    fun clearFilters() { 
        _searchRequest.value = SearchRequest()
        _uiState.value = _uiState.value.copy(searchQuery = "", activeFilter = SearchFilter.None, selectedCategoryForSearch = null)
    }

    fun startReadTransition(b: Book, bounds: InitialBounds? = null) { _uiState.value = _uiState.value.copy(transitionBook = b, initialBounds = bounds) }
    fun endReadTransition() { _uiState.value = _uiState.value.copy(transitionBook = null, initialBounds = null) }
}
