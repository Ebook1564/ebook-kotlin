package com.example.ebook_new_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InitialBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

data class HomeUiState(
    val billboardBook: Book? = null,
    val trendingBooks: List<Book> = emptyList(),
    val continueReading: List<Book> = emptyList(),
    val newReleases: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val transitionBook: Book? = null,
    val initialBounds: InitialBounds? = null,
    val userName: String = "Reader",
    val selectedCategoryForSearch: String? = null // Navigation trigger
)

class HomeViewModel : ViewModel() {
    private val repository = BookRepository()
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val allBooks = repository.getAllBooksAcrossCategories()
            
            if (allBooks.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    billboardBook = allBooks.firstOrNull(),
                    trendingBooks = allBooks.shuffled().take(7),
                    continueReading = allBooks.take(3),
                    newReleases = allBooks.reversed().take(7),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun navigateToCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryForSearch = category)
    }

    fun clearCategoryNavigation() {
        _uiState.value = _uiState.value.copy(selectedCategoryForSearch = null)
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
