package com.example.ebook_new_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val currentlyReading: List<Book> = emptyList(),
    val completed: List<Book> = emptyList(),
    val wishlist: List<Book> = emptyList(),
    val genres: List<String> = listOf("All", "Mystery", "Romance", "Fantasy", "Sci-Fi", "Thriller", "Horror", "Non-Fiction", "History"),
    val selectedGenre: String = "All",
    val isLoading: Boolean = false
)

class LibraryViewModel : ViewModel() {
    private val repository = BookRepository()
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var allBooksInternal: List<Book> = emptyList()

    init {
        loadLibraryData()
    }

    fun loadLibraryData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val allBooks = repository.getAllBooksAcrossCategories()
            allBooksInternal = allBooks
            
            filterAndPopulate()
        }
    }

    fun selectGenre(genre: String) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
        filterAndPopulate()
    }

    private fun filterAndPopulate() {
        val filtered = if (_uiState.value.selectedGenre == "All") {
            allBooksInternal
        } else {
            allBooksInternal.filter { it.categories.contains(_uiState.value.selectedGenre) }
        }

        _uiState.value = _uiState.value.copy(
            currentlyReading = filtered.shuffled().take(3),
            completed = filtered.shuffled().take(5),
            wishlist = filtered.shuffled().take(5),
            isLoading = false
        )
    }
}
