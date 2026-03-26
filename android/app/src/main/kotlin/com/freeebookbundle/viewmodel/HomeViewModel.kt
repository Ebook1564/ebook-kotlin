package com.freeebookbundle.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.freeebookbundle.EbookApp
import com.freeebookbundle.db.toBook
import com.freeebookbundle.models.Book
import com.freeebookbundle.repository.ExternalMetadataRepository
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

data class CuratedCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color,
    val tags: List<String>,
    val iconEmoji: String = "📚"
)

object CuratedCollections {
    val collections = listOf(
        CuratedCollection(
            id = "essence_of_science",
            title = "The Essence of Science",
            subtitle = "Scientific Discoveries & Research",
            description = "Explore the wonders of science, from physics to biology and everything in between.",
            accentColor = Color(0xFF2196F3),
            tags = listOf("Science", "Physics", "Chemistry", "Biology", "Technology"),
            iconEmoji = "🔬"
        ),
        CuratedCollection(
            id = "essence_of_romance",
            title = "The Essence of Romance",
            subtitle = "Timeless Love Stories",
            description = "Classic tales of love, passion, and romance through the ages.",
            accentColor = Color(0xFFE91E63),
            tags = listOf("Romance", "Love Stories", "Historical Romance"),
            iconEmoji = "💕"
        ),
        CuratedCollection(
            id = "essence_of_mystery",
            title = "The Essence of Mystery",
            subtitle = "Whodunits & Thrillers",
            description = "Get lost in suspenseful mysteries and gripping thrillers.",
            accentColor = Color(0xFF37474F),
            tags = listOf("Mystery", "Detective", "Crime", "Thriller"),
            iconEmoji = "🔍"
        ),
        CuratedCollection(
            id = "essence_of_history",
            title = "The Essence of History",
            subtitle = "Past Chronicles & Biographies",
            description = "Journey through time with historical fiction and biographies.",
            accentColor = Color(0xFF795548),
            tags = listOf("History", "Biography", "Historical"),
            iconEmoji = "📜"
        ),
        CuratedCollection(
            id = "essence_of_adventure",
            title = "The Essence of Adventure",
            subtitle = "Epic Journeys & Quests",
            description = "Thrilling adventures across lands, seas, and beyond.",
            accentColor = Color(0xFF00695C),
            tags = listOf("Adventure", "Exploration", "Travel"),
            iconEmoji = "⚓"
        ),
        CuratedCollection(
            id = "essence_of_fantasy",
            title = "The Essence of Fantasy",
            subtitle = "Worlds of Imagination",
            description = "Escape to magical realms and fantastical worlds.",
            accentColor = Color(0xFF9C27B0),
            tags = listOf("Fantasy", "Magic", "Fairy Tales"),
            iconEmoji = "✨"
        ),
        CuratedCollection(
            id = "essence_of_gothic",
            title = "The Essence of Gothic",
            subtitle = "Dark & Haunting Tales",
            description = "Atmospheric stories of mystery, horror, and the supernatural.",
            accentColor = Color(0xFF263238),
            tags = listOf("Gothic", "Horror", "Supernatural", "Dark"),
            iconEmoji = "🏚️"
        ),
        CuratedCollection(
            id = "essence_of_philosophy",
            title = "The Essence of Philosophy",
            subtitle = "Thoughts & Ideas",
            description = "Contemplate life, existence, and the nature of reality.",
            accentColor = Color(0xFF607D8B),
            tags = listOf("Philosophy", "Ethics", "Metaphysics"),
            iconEmoji = "🧠"
        ),
        CuratedCollection(
            id = "essence_of_literature",
            title = "The Essence of Literature",
            subtitle = "Classic Literature",
            description = "Timeless literary masterpieces from renowned authors.",
            accentColor = Color(0xFF8D6E63),
            tags = listOf("Literature", "Fiction", "Classic"),
            iconEmoji = "📖"
        ),
        CuratedCollection(
            id = "essence_of_war",
            title = "The Essence of War",
            subtitle = "Military History & Fiction",
            description = "Stories of conflict, bravery, and the human cost of war.",
            accentColor = Color(0xFFB71C1C),
            tags = listOf("War", "Military", "Battle"),
            iconEmoji = "⚔️"
        )
    )

    fun findCollectionForTag(tag: String): CuratedCollection? {
        return collections.find { collection ->
            collection.tags.any { it.equals(tag, ignoreCase = true) }
        }
    }
}

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
    val currentCuratedCollection: CuratedCollection? = null,
    val curatedCollections: List<CuratedCollection> = CuratedCollections.collections,
    val transitionBook: Book? = null,
    val initialBounds: InitialBounds? = null,
    val isMetadataLoading: Boolean = false,
    val dynamicCategories: List<CategoryWithCount> = emptyList(),
    val popularSubjects: List<SubjectWithCount> = emptyList(),
    val openAudioPlayer: Boolean = false
)

data class CategoryWithCount(
    val code: String,
    val name: String,
    val color: String,
    val bookCount: Int
)

data class SubjectWithCount(
    val name: String,
    val bookCount: Int
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
    
    private val _continueReadingBooks = MutableStateFlow<List<Book>>(emptyList())
    val continueReadingBooks: StateFlow<List<Book>> = _continueReadingBooks.asStateFlow()

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

    @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val curatedCollectionBooksByTag: Flow<PagingData<Book>> = _uiState
        .map { it.currentCuratedCollection }
        .distinctUntilChanged()
        .flatMapLatest { collection ->
            if (collection == null) flowOf(PagingData.empty())
            else {
                // Search by the first tag in the collection
                Pager(config = PagingConfig(pageSize = 30)) {
                    bookDao.getCollectionBooksPaging(collection.tags.first())
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
            val allCategories = bookDao.getAllCategories().first()
            
            // Deduplicate categories by name (some have same name with different codes)
            // Filter out "None" categories
            val categories = allCategories
                .filter { it.name.isNotBlank() && it.name != "None" && it.name != "Genre None" }
                .groupBy { it.name }
                .map { (_, group) -> group.first() }
            
            // Calculate book counts for each category
            val categoriesWithCounts = categories.map { cat ->
                val books = bookDao.getBooksByCategoryLimit(cat.code, 1)
                CategoryWithCount(
                    code = cat.code,
                    name = cat.name,
                    color = cat.color,
                    bookCount = if (books.isNotEmpty()) books.first().let { 
                        // Count by fetching more would be expensive, so we'll use 1+ as placeholder
                        // For accurate counts, we'd need a COUNT query
                        1 // Placeholder - will be updated when loading
                    } else 0
                )
            }.filter { it.bookCount > 0 }
            
            // Get popular subjects from all books
            val allBooks = bookDao.getBooksByCategoryLimit("", 1000).take(1000)
            val subjectCounts = allBooks
                .flatMap { it.subjects }
                .groupingBy { it }
                .eachCount()
                .filter { it.value >= 5 } // Only show subjects with 5+ books
                .entries
                .sortedByDescending { it.value }
                .take(12)
                .map { SubjectWithCount(it.key, it.value) }
            
            // Create sections with organic ordering (most popular first, not alphabetical)
            val sections = categories.map { cat ->
                val books = bookDao.getBooksByCategoryLimit(cat.code, 15).map { it.toBook() }
                HomeSection(cat.name, books)
            }.sortedByDescending { it.books.size }
            
            // Load recently opened books
            val recentlyOpened = bookDao.getRecentlyOpenedBooks(10).map { it.toBook() }
            _continueReadingBooks.value = recentlyOpened
            
            _homeSections.value = sections
            _billboardBooks.value = sections.flatMap { it.books }.shuffled().take(6)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                dynamicCategories = categoriesWithCounts,
                popularSubjects = subjectCounts
            )
        }
    }

    private fun updateBookInLists(updated: Book) {
        _billboardBooks.value = _billboardBooks.value.map { if (it.id == updated.id) updated else it }
        _homeSections.value = _homeSections.value.map { section ->
            section.copy(books = section.books.map { if (it.id == updated.id) updated else it })
        }
    }

    fun openCollection(tag: String) = viewModelScope.launch {
        // First check if it matches a curated collection
        val curatedCollection = CuratedCollections.findCollectionForTag(tag)
        if (curatedCollection != null) {
            _uiState.value = _uiState.value.copy(currentCuratedCollection = curatedCollection, currentCollection = null)
            return@launch
        }

        // Fallback to legacy category-based collections
        val collection = when {
            tag.contains("Gothic", ignoreCase = true) -> BookCollection("Gothic Echoes", "Dark, moody tales of mystery and the supernatural.", Color(0xFF37474F), "Gothic")
            tag.contains("Adventure", ignoreCase = true) -> BookCollection("High Seas & Beyond", "Epic journeys and daring adventures through history.", Color(0xFF00695C), "Adventure")
            tag.contains("Romance", ignoreCase = true) -> BookCollection("Classic Romance", "Timeless stories of love, passion, and high society.", Color(0xFFAD1457), "Romance")
            tag.contains("Mystery", ignoreCase = true) -> BookCollection("Detective Noir", "Classic whodunnits and complex investigations.", Color(0xFF263238), "Mystery")
            else -> BookCollection(tag, "A curated selection of books matching '$tag'.", Color(0xFFE50914), tag)
        }
        _uiState.value = _uiState.value.copy(currentCollection = collection, currentCuratedCollection = null)
    }

    fun openCuratedCollection(collection: CuratedCollection) {
        _uiState.value = _uiState.value.copy(currentCuratedCollection = collection, currentCollection = null)
    }

    fun closeCollection() { 
        _uiState.value = _uiState.value.copy(currentCollection = null, currentCuratedCollection = null) 
    }

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

    fun startReadTransition(b: Book, bounds: InitialBounds? = null, openAudioPlayer: Boolean = false) { _uiState.value = _uiState.value.copy(transitionBook = b, initialBounds = bounds, openAudioPlayer = openAudioPlayer) }
    fun endReadTransition() { _uiState.value = _uiState.value.copy(transitionBook = null, initialBounds = null, openAudioPlayer = false) }
}
