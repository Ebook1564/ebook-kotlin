package com.example.ebookapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ebookapp.models.Book
import com.example.ebookapp.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val billboardBook: Book? = null,
    val trendingBooks: List<Book> = emptyList(),
    val continueReading: List<Book> = emptyList(),
    val newReleases: List<Book> = emptyList(),
    val genres: List<String> = emptyList(),
    val isLoading: Boolean = false
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
            android.util.Log.d("HomeViewModel", "Starting to load home data...")

            val allBooks = repository.getAllBooksAcrossCategories()
            android.util.Log.d("HomeViewModel", "Loaded ${allBooks.size} books total")

            if (allBooks.isNotEmpty()) {
                val genres = allBooks.flatMap { it.genres }.distinct().shuffled()
                _uiState.value = HomeUiState(
                    billboardBook = allBooks.firstOrNull(),
                    trendingBooks = allBooks.shuffled().take(7),
                    continueReading = allBooks.take(3), // Placeholder logic
                    newReleases = allBooks.reversed().take(7),
                    genres = genres.take(10),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
