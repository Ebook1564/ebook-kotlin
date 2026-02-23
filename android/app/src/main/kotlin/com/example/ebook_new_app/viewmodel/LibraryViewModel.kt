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
    val isLoading: Boolean = false
)

class LibraryViewModel : ViewModel() {
    private val repository = BookRepository()
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLibraryData()
    }

    fun loadLibraryData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val allBooks = repository.getAllBooksAcrossCategories()
            
            if (allBooks.isNotEmpty()) {
                _uiState.value = LibraryUiState(
                    currentlyReading = allBooks.take(3),
                    completed = allBooks.drop(3).take(5),
                    wishlist = allBooks.drop(8).take(5),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
