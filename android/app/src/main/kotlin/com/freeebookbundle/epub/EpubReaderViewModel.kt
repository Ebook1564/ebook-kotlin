package com.freeebookbundle.epub

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freeebookbundle.db.Highlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Immutable
data class EpubReaderState(
    val book: EpubBook? = null,
    val items: List<ReaderItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentChapterIndex: Int = 0,
    val progress: Float = 0f,
    val loadedImages: Map<String, Bitmap?> = emptyMap(),
    val jumpTrigger: Int? = null,
    val jumpAnchor: String? = null,
    val highlights: List<Highlight> = emptyList()
)

class EpubReaderViewModel : ViewModel() {
    private val parser = EpubParser()
    private val _state = MutableStateFlow(EpubReaderState())
    val state: StateFlow<EpubReaderState> = _state

    // Separate Flow for progress to avoid triggering full UI recomposition
    private val _currentProgress = MutableStateFlow(0f)
    val currentProgress: StateFlow<Float> = _currentProgress

    private val _currentChapter = MutableStateFlow(0)
    val currentChapter: StateFlow<Int> = _currentChapter

    private val TAG = "EpubReaderVM"
    private val loadingImagePaths = ConcurrentHashMap.newKeySet<String>()
    private var lastLoadedChapter = -1
    private var firstLoadedChapter = -1

    // Guard against duplicate loadBook calls
    private var loadedFilePath: String? = null
    private val isLoadInProgress = AtomicBoolean(false)

    fun loadBook(
        filePath: String?,
        initialChapter: Int = 0,
        bookId: String? = null,
        savedHighlights: List<Highlight> = emptyList()
    ) {
        if (filePath == null) {
            _state.update { it.copy(error = "File path is null") }
            return
        }

        if (filePath == loadedFilePath && _state.value.book != null) {
            Log.d(TAG, "loadBook: $filePath already loaded, skipping duplicate call")
            return
        }
        if (!isLoadInProgress.compareAndSet(false, true)) {
            Log.d(TAG, "loadBook: load already in progress, skipping duplicate call")
            return
        }

        loadedFilePath = filePath

        viewModelScope.launch {
            Log.d(TAG, "loadBook: Starting load for $filePath")
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    items = emptyList(),
                    highlights = savedHighlights
                )
            }
            try {
                val book = parser.createEpubBook(filePath)
                Log.d(TAG, "loadBook: Book created successfully. Title: ${book.title}")
                _state.update { it.copy(book = book) }
                
                jumpToChapterInternal(book, initialChapter, fragmentId = null)
            } catch (e: Exception) {
                Log.e(TAG, "loadBook: Failed to load $filePath", e)
                _state.update { it.copy(isLoading = false, error = e.message) }
            } finally {
                isLoadInProgress.set(false)
            }
        }
    }

    fun jumpToChapter(chapterIndex: Int, fragmentId: String? = null) {
        val book = _state.value.book ?: return
        viewModelScope.launch {
            jumpToChapterInternal(book, chapterIndex, fragmentId)
        }
    }

    private suspend fun jumpToChapterInternal(
        book: EpubBook,
        chapterIndex: Int,
        fragmentId: String?
    ) {
        Log.d(TAG, "jumpToChapterInternal: chapter=$chapterIndex, anchor=$fragmentId")
        _state.update { it.copy(isLoading = true) }

        val start = (chapterIndex - 1).coerceAtLeast(0)
        val end = (chapterIndex + 1).coerceAtMost(book.chapters.size - 1)
        Log.d(TAG, "jumpToChapterInternal: loading range $start..$end")

        firstLoadedChapter = start
        lastLoadedChapter = end

        try {
            val items = (start..end).map { i ->
                viewModelScope.async(Dispatchers.IO) {
                    Log.d(TAG, "jumpToChapterInternal: parsing chapter $i")
                    parser.getChapterItems(book, i)
                }
            }.awaitAll().flatten()

            Log.d(TAG, "jumpToChapterInternal: successfully loaded ${items.size} items total")

            _state.update {
                it.copy(
                    items = items,
                    currentChapterIndex = chapterIndex,
                    isLoading = false,
                    jumpTrigger = (it.jumpTrigger ?: 0) + 1,
                    jumpAnchor = fragmentId
                )
            }
            _currentChapter.value = chapterIndex
        } catch (e: Exception) {
            Log.e(TAG, "jumpToChapterInternal: Failed to load chapters", e)
            _state.update { it.copy(isLoading = false, error = e.message) }
            throw e
        }
    }

    fun addHighlight(highlight: Highlight) {
        _state.update { it.copy(highlights = it.highlights + highlight) }
    }

    fun addHighlights(newHighlights: List<Highlight>) {
        if (newHighlights.isEmpty()) return
        _state.update { it.copy(highlights = it.highlights + newHighlights) }
    }

    fun deleteHighlight(highlight: Highlight) {
        _state.update { state ->
            state.copy(highlights = state.highlights.filter { it.id != highlight.id })
        }
    }

    fun loadMoreNext() {
        val book = _state.value.book ?: return
        if (lastLoadedChapter >= book.chapters.size - 1) return

        viewModelScope.launch {
            val nextChapter = lastLoadedChapter + 1
            val newItems = parser.getChapterItems(book, nextChapter)
            lastLoadedChapter = nextChapter
            _state.update { it.copy(items = it.items + newItems) }
        }
    }

    fun loadMorePrevious() {
        val book = _state.value.book ?: return
        if (firstLoadedChapter <= 0) return

        viewModelScope.launch {
            val prevChapter = firstLoadedChapter - 1
            val newItems = parser.getChapterItems(book, prevChapter)
            firstLoadedChapter = prevChapter
            _state.update { it.copy(items = newItems + it.items) }
        }
    }

    fun loadImage(path: String) {
        val book = _state.value.book ?: return
        if (_state.value.loadedImages.containsKey(path) || !loadingImagePaths.add(path)) return

        viewModelScope.launch {
            val bitmap = parser.getImage(book.filePath, path)
            _state.update { it.copy(loadedImages = it.loadedImages + (path to bitmap)) }
            loadingImagePaths.remove(path)
        }
    }

    fun updateProgress(visibleItemIndex: Int) {
        val currentState = _state.value
        val items = currentState.items
        if (items.isEmpty()) return

        val book = currentState.book ?: return
        val currentItem = items.getOrNull(visibleItemIndex)
        val chapterIndex = currentItem?.id?.substringBefore("_")?.toIntOrNull()
            ?: currentState.currentChapterIndex

        val chapter = book.chapters.getOrNull(chapterIndex)
        val totalChars = book.totalCharacters

        if (chapter != null && totalChars > 0) {
            val itemOffset = when (currentItem) {
                is ReaderItem.Text -> currentItem.charOffsetInChapter.toLong()
                else -> 0L
            }
            val absoluteCharOffset = chapter.characterOffset + itemOffset
            val progress = (absoluteCharOffset.toFloat() / totalChars.toFloat()).coerceIn(0f, 1f)

            if (progress != _currentProgress.value) {
                _currentProgress.value = progress
            }
            if (chapterIndex != _currentChapter.value) {
                _currentChapter.value = chapterIndex
                _state.update { it.copy(currentChapterIndex = chapterIndex, progress = progress) }
            }
        }
    }

    fun consumeJumpTrigger() {
        _state.update { it.copy(jumpTrigger = null, jumpAnchor = null) }
    }

    override fun onCleared() {
        super.onCleared()
        parser.close()
    }
}
