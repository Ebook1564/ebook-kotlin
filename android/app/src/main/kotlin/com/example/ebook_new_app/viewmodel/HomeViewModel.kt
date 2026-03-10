package com.example.ebook_new_app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ebook_new_app.EbookApp
import com.example.ebook_new_app.db.toBook
import com.example.ebook_new_app.models.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    val billboardBook: Book? = null,
    val sections: List<HomeSection> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCategoryForSearch: String? = null,
    val transitionBook: Book? = null,
    val initialBounds: InitialBounds? = null
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
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Observe Categories
            bookDao.getAllCategories().collectLatest { categoryEntities ->
                val sections = mutableListOf<HomeSection>()
                
                // For each category, fetch only a few books (LIMIT 15)
                categoryEntities.forEach { cat ->
                    val books = bookDao.getBooksByCategoryLimit(cat.code, 15).map { it.toBook() }
                    if (books.isNotEmpty()) {
                        sections.add(HomeSection(cat.name, books))
                    }
                }

                _uiState.value = _uiState.value.copy(
                    sections = sections,
                    billboardBook = sections.firstOrNull()?.books?.shuffled()?.firstOrNull(),
                    isLoading = false
                )
            }
        }
    }

    fun navigateToCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryForSearch = category)
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
