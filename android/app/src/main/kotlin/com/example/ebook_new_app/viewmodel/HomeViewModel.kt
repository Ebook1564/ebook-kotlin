package com.example.ebook_new_app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.example.ebook_new_app.EbookApp
import com.example.ebook_new_app.db.toBook
import com.example.ebook_new_app.models.Book
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InitialBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

data class HomeSection(
    val title: String,
    val books: List<Book>
)

data class HomeUiState(
    val userName: String = "Aayush",
    val billboardBooks: List<Book> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedCategoryForSearch: String? = null,
    val transitionBook: Book? = null,
    val initialBounds: InitialBounds? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as EbookApp).database
    private val bookDao = database.bookDao()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    
    @OptIn(FlowPreview::class)
    val searchResults: Flow<PagingData<Book>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(
                    pageSize = 30,
                    enablePlaceholders = true,
                    initialLoadSize = 30
                )
            ) {
                bookDao.searchBooksPaging(query)
            }.flow.map { pagingData ->
                pagingData.map { it.toBook() }
            }
        }
        .cachedIn(viewModelScope)

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            bookDao.getAllCategories().collectLatest { categoryEntities ->
                val sections = mutableListOf<HomeSection>()
                
                categoryEntities.forEach { cat ->
                    val books = bookDao.getBooksByCategoryLimit(cat.code, 15).map { it.toBook() }
                    if (books.isNotEmpty()) {
                        sections.add(HomeSection(cat.name, books))
                    }
                }

                val featured = sections.flatMap { it.books }.shuffled().take(6)

                _uiState.value = _uiState.value.copy(
                    sections = sections,
                    billboardBooks = featured,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun navigateToCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryForSearch = category)
        onSearchQueryChanged(category)
    }

    fun startReadTransition(book: Book, bounds: InitialBounds? = null) {
        _uiState.value = _uiState.value.copy(
            transitionBook = book,
            initialBounds = bounds
        )
    }

    fun endReadTransition() {
        _uiState.value = _uiState.value.copy(
            transitionBook = null,
            initialBounds = null
        )
    }
}
