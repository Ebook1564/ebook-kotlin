package com.example.ebookapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ebookapp.models.Book
import com.example.ebookapp.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val continueReading: List<Book> = emptyList(),
    val myLibrary: List<Book> = emptyList(),
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

            val allBooks = repository.getLibraryBooks().ifEmpty {
                repository.getAllBooksAcrossCategories()
            }

            if (allBooks.isNotEmpty()) {
                // Placeholder logic to separate continue reading from the rest of the library
                val continueReadingBooks = allBooks.filter { it.readingProgress > 0 && it.readingProgress < 1 }
                _uiState.value = LibraryUiState(
                    continueReading = continueReadingBooks,
                    myLibrary = allBooks,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

