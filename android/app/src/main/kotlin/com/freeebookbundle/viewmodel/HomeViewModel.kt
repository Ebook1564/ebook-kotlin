package com.freeebookbundle.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.freeebookbundle.EbookApp
import com.freeebookbundle.db.toBook
import com.freeebookbundle.models.Book
import com.freeebookbundle.models.ReadingStatus
import com.freeebookbundle.repository.CatalogRepository
import com.freeebookbundle.repository.ExternalMetadataRepository
import com.freeebookbundle.utils.GenrePreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class InitialBounds(
    val left: Float, val top: Float, val width: Float, val height: Float
)

@Immutable
data class HomeSection(
    val title: String,
    val categoryCode: String,
    val books: List<Book>
)

@Immutable
data class BookCollection(
    val title: String,
    val description: String,
    val accentColor: Color,
    val query: String
)

@Immutable
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

@Immutable
data class CategoryWithCount(
    val code: String,
    val name: String,
    val color: String,
    val bookCount: Int
)

@Immutable
data class SubjectWithCount(
    val name: String,
    val bookCount: Int
)

@Immutable
data class HomeUiState(
    val userName: String = "User",
    val isLoading: Boolean = false,
    val isCatalogLoading: Boolean = true, // Track catalog parsing specifically
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
    val openAudioPlayer: Boolean = false,
    val resumeFromLastPosition: Boolean = false
)

@Immutable
data class HomeNavigationState(
    val currentCollection: BookCollection? = null,
    val currentCuratedCollection: CuratedCollection? = null,
    val selectedCategoryForSearch: String? = null
)

@Immutable
data class HomeTransitionState(
    val transitionBook: Book? = null,
    val initialBounds: InitialBounds? = null,
    val isMetadataLoading: Boolean = false,
    val openAudioPlayer: Boolean = false,
    val resumeFromLastPosition: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as EbookApp).database
    private val bookDao = database.bookDao()
    private val metadataRepo = ExternalMetadataRepository(bookDao)
    private val catalogRepo = (application as EbookApp).catalogRepo

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Split navigation state to avoid recomposing BookVerseApp on every uiState change
    private val _navigationState = MutableStateFlow(HomeNavigationState())
    val navigationState: StateFlow<HomeNavigationState> = _navigationState.asStateFlow()

    // Split transition state to avoid recomposing BookVerseApp on metadata/transition changes
    private val _transitionState = MutableStateFlow(HomeTransitionState())
    val transitionState: StateFlow<HomeTransitionState> = _transitionState.asStateFlow()

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

    val filterResults: Flow<PagingData<Book>> = _searchRequest
        .debounce { request ->
            // Only debounce search queries, not category/author filter clicks
            if (request.filter is SearchFilter.None && request.query.isNotEmpty()) 300L else 0L
        }
        .flatMapLatest { request ->
            when (val filter = request.filter) {
                is SearchFilter.Author -> catalogRepo.getBooksByAuthor(filter.name)
                is SearchFilter.Category -> catalogRepo.getBooksByTopic(filter.code)
                else -> catalogRepo.searchBooks(request.query)
            }
        }.cachedIn(viewModelScope)

    val curatedCollectionBooks: Flow<PagingData<Book>> = _uiState
        .map { it.currentCollection }
        .distinctUntilChanged()
        .flatMapLatest { collection ->
            if (collection == null) flowOf(PagingData.empty())
            else catalogRepo.getBooksByTopic(collection.query)
        }.cachedIn(viewModelScope)

    val curatedCollectionBooksByTag: Flow<PagingData<Book>> = _uiState
        .map { it.currentCuratedCollection }
        .distinctUntilChanged()
        .flatMapLatest { collection ->
            if (collection == null) flowOf(PagingData.empty())
            else catalogRepo.getBooksByTopic(collection.tags.first())
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
            // Observe continue reading from local DB
            bookDao.getContinueReadingBooks(10)
                .distinctUntilChanged()
                .debounce(100)
                .flowOn(Dispatchers.IO)
                .collect { books ->
                    _continueReadingBooks.value = books.map { it.toBook() }
                }
        }
    }

    private fun loadStableHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isCatalogLoading = true)
            
            // Get selected genres from preferences
            val selectedGenres = GenrePreferences.getSelectedGenres()
            val topicsToLoad = if (selectedGenres.isEmpty()) {
                listOf("Fiction", "History", "Science", "Philosophy", "Romance", "Adventure")
            } else {
                selectedGenres.toList()
            }
            
            // 1. Load Billboard first (High priority) — IO thread to avoid blocking main
            val rawBillboard = withContext(Dispatchers.IO) {
                catalogRepo.getStaticBooksByTopic(topicsToLoad.first(), 6)
            }
            val billboard = withContext(Dispatchers.IO) {
                catalogRepo.enrichWithCachedMetadata(rawBillboard)
            }
            _billboardBooks.value = billboard
            _uiState.value = _uiState.value.copy(isCatalogLoading = false)

            // 2. Load all other sections in parallel, then update ONCE
            val sections = topicsToLoad.map { topic ->
                async(Dispatchers.IO) {
                    val rawBooks = catalogRepo.getStaticBooksByTopic(topic, 15)
                    HomeSection(topic, topic, catalogRepo.enrichWithCachedMetadata(rawBooks))
                }
            }.awaitAll()
            
            _homeSections.value = sections
            _uiState.value = _uiState.value.copy(isLoading = false)

            // 3. Fetch metadata for visible books with priority
            prefetchVisibleBookMetadata(billboard, sections)
        }
    }

    private fun prefetchVisibleBookMetadata(billboard: List<Book>, sections: List<HomeSection>) {
        viewModelScope.launch {
            // Priority 1: Billboard books (most visible)
            // Priority 2: First 3 books from each section (visible in horizontal scroll)
            // Priority 3: Remaining section books

            val priority1 = billboard
            val priority2 = sections.flatMap { section -> section.books.take(3) }
            val priority3 = sections.flatMap { section -> section.books.drop(3) }

            val allPriority = (priority1 + priority2 + priority3).distinctBy { it.id }
            val toFetch = allPriority.filter { metadataRepo.needsMetadata(it) }

            if (toFetch.isEmpty()) return@launch

            Log.d("HomeViewModel", "Prefetching metadata for ${toFetch.size} books (P1=${priority1.size}, P2=${priority2.size}, P3=${priority3.size})")

            // Update UI state for billboard books as they get metadata
            metadataRepo.fetchMetadataBatch(toFetch) { updatedBook ->
                // Update billboard if this book is in it
                val currentBillboard = _billboardBooks.value
                val billboardIndex = currentBillboard.indexOfFirst { it.id == updatedBook.id }
                if (billboardIndex >= 0) {
                    _billboardBooks.value = currentBillboard.toMutableList().apply {
                        set(billboardIndex, updatedBook)
                    }
                }

                // Update sections
                val currentSections = _homeSections.value
                val updatedSections = currentSections.map { section ->
                    val bookIndex = section.books.indexOfFirst { it.id == updatedBook.id }
                    if (bookIndex >= 0) {
                        section.copy(books = section.books.toMutableList().apply {
                            set(bookIndex, updatedBook)
                        })
                    } else section
                }
                if (updatedSections != currentSections) {
                    _homeSections.value = updatedSections
                }
            }
        }
    }

    suspend fun getTopGenres(count: Int = 10): List<CategoryWithCount> {
        val counts = catalogRepo.getGenreCounts()
        
        return counts.entries
            .sortedByDescending { it.value }
            .take(count)
            .map { (name, bookCount) ->
                val code = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                CategoryWithCount(
                    code = code,
                    name = name,
                    color = "#E50914", // Default accent
                    bookCount = bookCount
                )
            }
    }

    fun openCollection(tag: String) = viewModelScope.launch {
        val curatedCollection = CuratedCollections.findCollectionForTag(tag)
        if (curatedCollection != null) {
            _navigationState.value = _navigationState.value.copy(currentCuratedCollection = curatedCollection, currentCollection = null)
            return@launch
        }

        val collection = when {
            tag.contains("Gothic", ignoreCase = true) -> BookCollection("Gothic Echoes", "Dark, moody tales of mystery and the supernatural.", Color(0xFF37474F), "Gothic")
            tag.contains("Adventure", ignoreCase = true) -> BookCollection("High Seas & Beyond", "Epic journeys and daring adventures through history.", Color(0xFF00695C), "Adventure")
            tag.contains("Romance", ignoreCase = true) -> BookCollection("Classic Romance", "Timeless stories of love, passion, and high society.", Color(0xFFAD1457), "Romance")
            tag.contains("Mystery", ignoreCase = true) -> BookCollection("Detective Noir", "Classic whodunnits and complex investigations.", Color(0xFF263238), "Mystery")
            else -> BookCollection(tag, "A curated selection of books.", Color(0xFFE50914), tag)
        }
        _navigationState.value = _navigationState.value.copy(currentCollection = collection, currentCuratedCollection = null)
    }

    fun openCuratedCollection(collection: CuratedCollection) {
        _navigationState.value = _navigationState.value.copy(currentCuratedCollection = collection, currentCollection = null)
    }

    fun closeCollection() { 
        _navigationState.value = _navigationState.value.copy(currentCollection = null, currentCuratedCollection = null) 
    }

    fun fetchMetadataForBook(book: Book, onUpdated: (Book) -> Unit) {
        viewModelScope.launch {
            _transitionState.value = _transitionState.value.copy(isMetadataLoading = true)
            val updated = metadataRepo.fetchAndCacheMetadata(book)
            _transitionState.value = _transitionState.value.copy(isMetadataLoading = false)
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
        _navigationState.value = _navigationState.value.copy(selectedCategoryForSearch = cat)
        _uiState.value = _uiState.value.copy(selectedCategoryForSearch = cat, activeFilter = f, searchQuery = "")
    }

    fun clearFilters() { 
        _searchRequest.value = SearchRequest()
        _uiState.value = _uiState.value.copy(searchQuery = "", activeFilter = SearchFilter.None, selectedCategoryForSearch = null)
    }
    
    fun reloadHomeData() {
        loadStableHomeData()
    }

    fun startReadTransition(b: Book, bounds: InitialBounds? = null, resumeFromLastPosition: Boolean = false, openAudioPlayer: Boolean = false) { 
        _transitionState.value = HomeTransitionState(
            transitionBook = b, 
            initialBounds = bounds, 
            resumeFromLastPosition = resumeFromLastPosition,
            openAudioPlayer = openAudioPlayer
        )
    }
    fun endReadTransition() { _transitionState.value = HomeTransitionState() }
}
