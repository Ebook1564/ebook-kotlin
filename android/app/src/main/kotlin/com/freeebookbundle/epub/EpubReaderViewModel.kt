package com.freeebookbundle.epub

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freeebookbundle.db.Highlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class EpubReaderState(
    val book: EpubBook? = null,
    val items: List<ReaderItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentChapterIndex: Int = 0,
    val progress: Float = 0f,
    val loadedImages: Map<String, Bitmap?> = emptyMap(),
    val jumpTrigger: Int? = null,
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

    fun loadBook(filePath: String?, initialChapter: Int = 0, bookId: String? = null, savedHighlights: List<Highlight> = emptyList()) {
        if (filePath == null) {
            _state.update { it.copy(error = "File path is null") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, items = emptyList(), highlights = savedHighlights) }
            try {
                val book = parser.createEpubBook(filePath)
                _state.update { it.copy(book = book, isLoading = false) }
                
                jumpToChapter(initialChapter)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun jumpToChapter(chapterIndex: Int) {
        val book = _state.value.book ?: return
        val safeIndex = chapterIndex.coerceIn(0, book.chapters.size - 1)
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, items = emptyList()) }
            
            val items = mutableListOf<ReaderItem>()
            val start = (safeIndex - 1).coerceAtLeast(0)
            val end = (safeIndex + 1).coerceAtMost(book.chapters.size - 1)
            
            firstLoadedChapter = start
            lastLoadedChapter = end

            for (i in start..end) {
                val chapterItems = parser.getChapterItems(book, i)
                items.addAll(applyHighlightsToItems(i, chapterItems))
            }

            _state.update { 
                it.copy(
                    items = items, 
                    currentChapterIndex = safeIndex, 
                    isLoading = false,
                    jumpTrigger = (it.jumpTrigger ?: 0) + 1
                ) 
            }
        }
    }

    private fun applyHighlightsToItems(chapterIndex: Int, items: List<ReaderItem>): List<ReaderItem> {
        return items // Highlights will be drawn on canvas in the UI layer
    }

    fun addHighlight(highlight: Highlight) {
        _state.update { it.copy(highlights = it.highlights + highlight) }
    }

    fun addHighlights(newHighlights: List<Highlight>) {
        if (newHighlights.isEmpty()) return
        _state.update { it.copy(highlights = it.highlights + newHighlights) }
    }

    fun deleteHighlight(highlight: Highlight) {
        _state.update { it.copy(highlights = it.highlights.filter { it.id != highlight.id }) }
    }

    private fun refreshHighlightsOnItems() {
        // Force refresh of the current visible items by re-applying highlights
        val currentItems = _state.value.items
        val updatedItems = currentItems.map { item ->
            if (item is ReaderItem.Text) {
                val chapterIndex = item.id.substringBefore("_").toIntOrNull() ?: return@map item
                // This is a bit inefficient (re-parsing would be better but expensive)
                // In this case, we just re-run the applyHighlightsToItems for this chapter
                // For simplicity, we just trigger a state update that rebuilds the list
                item
            } else item
        }
        // Actually, the cleanest way in this infinite scroll is just to re-run jumpToChapter for the current view
        jumpToChapter(_state.value.currentChapterIndex)
    }

    fun loadMoreNext() {
        val book = _state.value.book ?: return
        if (lastLoadedChapter >= book.chapters.size - 1) return
        
        viewModelScope.launch {
            val nextChapter = lastLoadedChapter + 1
            val rawItems = parser.getChapterItems(book, nextChapter)
            val newItems = applyHighlightsToItems(nextChapter, rawItems)
            lastLoadedChapter = nextChapter
            _state.update { it.copy(items = it.items + newItems) }
        }
    }

    fun loadMorePrevious() {
        val book = _state.value.book ?: return
        if (firstLoadedChapter <= 0) return
        
        viewModelScope.launch {
            val prevChapter = firstLoadedChapter - 1
            val rawItems = parser.getChapterItems(book, prevChapter)
            val newItems = applyHighlightsToItems(prevChapter, rawItems)
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
        val chapterIndex = currentItem?.id?.substringBefore("_")?.toIntOrNull() ?: currentState.currentChapterIndex
        
        val chapter = book.chapters.getOrNull(chapterIndex)
        val totalChars = book.totalCharacters
        
        if (chapter != null && totalChars > 0) {
            val itemOffset = when (currentItem) {
                is ReaderItem.Text -> currentItem.charOffsetInChapter.toLong()
                else -> 0L
            }
            val absoluteCharOffset = chapter.characterOffset + itemOffset
            val progress = (absoluteCharOffset.toFloat() / totalChars.toFloat()).coerceIn(0f, 1f)
            _state.update { it.copy(progress = progress, currentChapterIndex = chapterIndex) }
        }
    }

    fun consumeJumpTrigger() {
        _state.update { it.copy(jumpTrigger = null) }
    }

    override fun onCleared() {
        super.onCleared()
        parser.close()
    }
}
