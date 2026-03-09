package com.example.ebook_new_app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ebook_new_app.EbookApp
import com.example.ebook_new_app.db.toBook
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.repository.BookRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeSection(
    val title: String,
    val books: List<Book>
)

data class InitialBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

data class HomeUiState(
    val billboardBook: Book? = null,
    val sections: List<HomeSection> = emptyList(),
    val isLoading: Boolean = false,
    val transitionBook: Book? = null,
    val initialBounds: InitialBounds? = null,
    val userName: String = "Reader",
    val selectedCategoryForSearch: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as EbookApp).database
    private val bookDao = database.bookDao()
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Collect categories and books reactively
            combine(
                bookDao.getAllCategories(),
                bookDao.getAllBooks()
            ) { categories, allBooks ->
                if (allBooks.isEmpty()) return@combine null

                val sections = categories.map { category ->
                    HomeSection(
                        title = category.name,
                        books = allBooks.filter { it.categoryCode == category.code }.map { it.toBook() }
                    )
                }.filter { it.books.isNotEmpty() }

                val billboard = allBooks.shuffled().firstOrNull()?.toBook()

                HomeUiState(
                    billboardBook = billboard,
                    sections = sections,
                    isLoading = false,
                    userName = "Aayush"
                )
            }.filterNotNull().collect { newState ->
                _uiState.value = newState
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
