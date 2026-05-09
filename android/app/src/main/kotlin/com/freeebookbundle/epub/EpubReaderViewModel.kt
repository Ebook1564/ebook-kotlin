package com.freeebookbundle.epub

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freeebookbundle.db.Highlight
import kotlinx.coroutines.Dispatchers
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

    private val TAG = "EpubReaderVM"
    private val loadingImagePaths = ConcurrentHashMap.newKeySet<String>()
    private var lastLoadedChapter = -1
    private var firstLoadedChapter = -1

    // Guard against duplicate loadBook calls (e.g. from both Fragment and NativeEpubReader)
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

        // Deduplicate: if this exact file is already loaded or loading, skip
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
                _state.update { it.copy(book = book, isLoading = false) }
                // Single internal jump — callers must NOT call jumpToChapter separately
                // for the initial load. Use targetChapter state for subsequent jumps.
                jumpToChapterInternal(book, initialChapter, fragmentId = null)
            } catch (e: Exception) {
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

    // Internal version — avoids launching a new coroutine when already inside one
    private suspend fun jumpToChapterInternal(
        book: EpubBook,
        chapterIndex: Int,
        fragmentId: String?
    ) {
        // Don't clear items immediately — keep old content visible while loading
        // to avoid the blank flash that causes layout jank
        _state.update { it.copy(isLoading = true) }

        val start = (chapterIndex - 1).coerceAtLeast(0)
        val end = (chapterIndex + 1).coerceAtMost(book.chapters.size - 1)

        firstLoadedChapter = start
        lastLoadedChapter = end

        val items = mutableListOf<ReaderItem>()
        for (i in start..end) {
            items.addAll(parser.getChapterItems(book, i))
        }

        _state.update {
            it.copy(
                items = items,
                currentChapterIndex = chapterIndex,
                isLoading = false,
                // Only increment jumpTrigger if chapter actually changed,
                // or if this is a fresh load (items were previously empty)
                jumpTrigger = if (it.currentChapterIndex != chapterIndex || it.items.isEmpty())
                    (it.jumpTrigger ?: 0) + 1
                else
                    it.jumpTrigger,
                jumpAnchor = fragmentId
            )
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

            // Only update state if values actually changed to avoid unnecessary recompositions
            if (progress != currentState.progress || chapterIndex != currentState.currentChapterIndex) {
                _state.update { it.copy(progress = progress, currentChapterIndex = chapterIndex) }
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
