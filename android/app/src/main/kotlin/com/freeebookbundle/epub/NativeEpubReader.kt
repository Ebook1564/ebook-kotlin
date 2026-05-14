package com.freeebookbundle.epub

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freeebookbundle.db.Highlight
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

@Composable
fun NativeEpubReader(
    filePath: String,
    bookId: String? = null,
    initialChapter: Int = 0,
    targetChapter: Int? = null,
    targetAnchor: String? = null,
    settings: com.freeebookbundle.viewmodel.ReaderSettings = com.freeebookbundle.viewmodel.ReaderSettings(),
    backgroundColor: Color = Color(0xFFFDFCF0),
    textColor: Color = Color(0xFF5B4636),
    onProgressChange: (progress: Float) -> Unit = {},
    onJumpToChapter: (Int, String?) -> Unit = { _, _ -> },
    onChapterChange: (chapter: Int, chapterProgress: Float) -> Unit = { _, _ -> },
    onHighlightsAdded: (List<Highlight>) -> Unit = {},
    onHighlightDeleted: (Highlight) -> Unit = {},
    onHighlightNoteClicked: (Highlight) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: EpubReaderViewModel = viewModel()
    val readerState by viewModel.state.collectAsStateWithLifecycle()

    val adjustedFontSize = (18.sp.value * settings.fontSize / 100).sp
    val fontFamily = when(settings.fontFamily) {
        "sans-serif" -> FontFamily.SansSerif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.Serif
    }
    val textAlign = when(settings.textAlign) {
        "center" -> androidx.compose.ui.text.style.TextAlign.Center
        "justify" -> androidx.compose.ui.text.style.TextAlign.Justify
        "right" -> androidx.compose.ui.text.style.TextAlign.Right
        else -> androidx.compose.ui.text.style.TextAlign.Left
    }
    val lineHeight = adjustedFontSize * settings.lineHeight

    val listState = rememberLazyListState()

    var selectionRect by remember { mutableStateOf<Rect?>(null) }
    var selectedTextInfo by remember { mutableStateOf<SelectionInfo?>(null) }
    
    // Performance optimization: Use plain HashMaps for results/positions to avoid scroll snapshot writes
    val textLayoutResults = remember { HashMap<String, TextLayoutResult>() }
    val itemPositions = remember { HashMap<String, Offset>() }
    // Version counter to trigger re-calculation of highlights when maps are updated
    var layoutVersion by remember { mutableIntStateOf(0) }
    var containerPositionInWindow by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(filePath) { viewModel.loadBook(filePath, initialChapter) }
    LaunchedEffect(targetChapter, targetAnchor) {
        if (targetChapter != null) {
            onJumpToChapter(targetChapter, targetAnchor)
        }
    }

    val clipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val ttsManager = remember { com.freeebookbundle.voice.TtsVoiceManager(context) }
    
    val activeSelections = remember { mutableStateListOf<SelectionInfo>() }
    var clearSelectionTick by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current

    val currentReaderState by rememberUpdatedState(readerState)
    val currentActiveSelections by rememberUpdatedState(activeSelections)

    LaunchedEffect(readerState.currentChapterIndex) {
        textLayoutResults.clear()
        itemPositions.clear()
    }

    val textToolbar = remember { 
        lateinit var toolbar: EpubTextToolbar
        toolbar = EpubTextToolbar(
            onColorSelected = { color ->
                if (currentActiveSelections.isNotEmpty()) {
                    val highlightsToSave = mutableListOf<Highlight>()
                    currentActiveSelections.forEach { selection ->
                        val item = currentReaderState.items.find { it.id == selection.itemId } as? ReaderItem.Text
                        if (item != null) {
                            val hl = Highlight(
                                bookId = bookId ?: "",
                                chapterIndex = item.id.substringBefore("_").toIntOrNull() ?: 0,
                                startOffset = item.charOffsetInChapter + selection.range.first,
                                endOffset = item.charOffsetInChapter + selection.range.second,
                                selectedText = selection.text,
                                color = color,
                                annotation = null,
                                createdAt = System.currentTimeMillis()
                            )
                            highlightsToSave.add(hl)
                        }
                    }
                    if (highlightsToSave.isNotEmpty()) {
                        viewModel.addHighlights(highlightsToSave)
                        onHighlightsAdded(highlightsToSave)
                    }
                }
                selectedTextInfo = null
                clearSelectionTick++
                activeSelections.clear()
            },
            onCopyRequested = {
                val fullText = currentActiveSelections.joinToString(" ") { it.text }
                if (fullText.isNotEmpty()) {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(fullText))
                }
                selectedTextInfo = null
                clearSelectionTick++
                activeSelections.clear()
                toolbar.dismissedByUser()
            },
            onSpeakRequested = {
                val fullText = currentActiveSelections.joinToString(" ") { it.text }
                if (fullText.isNotEmpty()) {
                    ttsManager.speak(text = fullText, utteranceId = "selection_multi")
                }
                clearSelectionTick++
                activeSelections.clear()
                toolbar.dismissedByUser()
            },
            onDictionaryRequested = {
                val text = currentActiveSelections.firstOrNull()?.text ?: ""
                if (text.isNotEmpty()) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://www.google.com/search?q=define+${text}")
                    }
                    context.startActivity(intent)
                }
            },
            onWikipediaRequested = {
                val text = currentActiveSelections.firstOrNull()?.text ?: ""
                if (text.isNotEmpty()) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://en.wikipedia.org/wiki/${text}")
                    }
                    context.startActivity(intent)
                }
            },
            onDismissSelection = {
                toolbar.dismissedByUser()
                clearSelectionTick++
                activeSelections.clear()
            },
            onMenuShown = { rect ->
                toolbar.resetDismissLatch()
                val density = context.resources.displayMetrics.density
                activeSelections.clear()

                if (rect.height > 0f && rect.width > 0f) {
                    val handleHeightPx = 25f * density
                    val visibleKeys = listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet()

                    textLayoutResults.forEach { (itemId, result) ->
                        if (visibleKeys.isNotEmpty() && itemId !in visibleKeys) return@forEach
                        val itemPos = itemPositions[itemId] ?: return@forEach

                        val localRect = Rect(
                            left   = rect.left - itemPos.x,
                            top    = rect.top  - itemPos.y,
                            right  = rect.right - itemPos.x,
                            bottom = (rect.bottom - handleHeightPx) - itemPos.y
                        )

                        if (localRect.bottom < 0f || localRect.top > result.size.height) return@forEach
                        val textLength = result.layoutInput.text.length
                        if (textLength == 0) return@forEach

                        var firstSelectedLine = Int.MAX_VALUE
                        var lastSelectedLine  = Int.MIN_VALUE
                        var currentStart      = -1
                        var cachedLine       = -1
                        var cachedLineCenter = 0f

                        for (i in 0 until textLength) {
                            val line = result.getLineForOffset(i)
                            val lineCenter = if (line == cachedLine) cachedLineCenter else {
                                val c = (result.getLineTop(line) + result.getLineBottom(line)) / 2f
                                cachedLine = line
                                cachedLineCenter = c
                                c
                            }

                            val lineInSelection = lineCenter in localRect.top..localRect.bottom
                            if (lineInSelection) {
                                if (line < firstSelectedLine) firstSelectedLine = line
                                if (line > lastSelectedLine)  lastSelectedLine  = line
                            }

                            if (!lineInSelection) {
                                if (currentStart != -1) {
                                    activeSelections.add(SelectionInfo(itemId, currentStart to i, result.layoutInput.text.text.substring(currentStart, i)))
                                    currentStart = -1
                                }
                                continue
                            }

                            val charBounds = result.getBoundingBox(i)
                            val effectiveLeft = if (line == firstSelectedLine) localRect.left else Float.NEGATIVE_INFINITY
                            val effectiveRight = if (line == lastSelectedLine) localRect.right else Float.POSITIVE_INFINITY
                            val overlapWidth = maxOf(0f, minOf(charBounds.right, effectiveRight) - maxOf(charBounds.left,  effectiveLeft))

                            if (overlapWidth > charBounds.width * 0.5f) {
                                if (currentStart == -1) currentStart = i
                            } else if (currentStart != -1) {
                                activeSelections.add(SelectionInfo(itemId, currentStart to i, result.layoutInput.text.text.substring(currentStart, i)))
                                currentStart = -1
                            }
                        }
                        if (currentStart != -1) {
                            activeSelections.add(SelectionInfo(itemId, currentStart to textLength, result.layoutInput.text.text.substring(currentStart, textLength)))
                        }
                    }
                }
            }
        )
        toolbar
    }

    LaunchedEffect(readerState.jumpTrigger) {
        if (readerState.jumpTrigger != null && readerState.items.isNotEmpty()) {
            val anchor = readerState.jumpAnchor
            var targetIndex = if (!anchor.isNullOrEmpty()) {
                readerState.items.indexOfFirst { item ->
                    (item is ReaderItem.Text && item.locator == anchor) ||
                    (item is ReaderItem.Heading && item.locator == anchor)
                }
            } else -1
            
            if (targetIndex == -1) {
                val targetChapterPrefix = "${readerState.currentChapterIndex}_item_"
                targetIndex = readerState.items.indexOfFirst { it.id.startsWith(targetChapterPrefix) }
            }
            
            if (targetIndex >= 0) {
                yield()
                delay(50)
                listState.scrollToItem(targetIndex)
            }
            viewModel.consumeJumpTrigger()
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (readerState.items.isNotEmpty()) {
            val visibleIndex = listState.firstVisibleItemIndex
            viewModel.updateProgress(visibleIndex)
            onProgressChange(viewModel.currentProgress.value)
            onChapterChange(viewModel.currentChapter.value, viewModel.currentProgress.value)
        }
    }

    val customTextSelectionColors = TextSelectionColors(
        handleColor = Color(0xFFE50914),
        backgroundColor = Color(0xFFE50914).copy(alpha = 0.4f)
    )

    CompositionLocalProvider(
        LocalTextSelectionColors provides customTextSelectionColors,
        LocalTextToolbar provides textToolbar
    ) {
        Box(modifier = modifier.fillMaxSize().background(backgroundColor).onGloballyPositioned { 
            containerPositionInWindow = it.positionInWindow()
        }) {
            textToolbar.Content()

            LaunchedEffect(clearSelectionTick) {
                if (clearSelectionTick > 0) {
                    focusManager.clearFocus(force = true)
                }
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp)
            ) {
                if (readerState.items.isEmpty() && !readerState.isLoading) {
                    item {
                         Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No content found in this chapter", color = textColor.copy(alpha = 0.5f))
                         }
                    }
                }

                items(
                    items = readerState.items,
                    key = { it.id },
                    contentType = { item ->
                        when (item) {
                            is ReaderItem.Text    -> 0
                            is ReaderItem.Image   -> 1
                            is ReaderItem.Heading -> 2
                        }
                    }
                ) { item ->
                    when (item) {
                        is ReaderItem.Text -> {
                            SelectionContainer {
                                TextItemRow(
                                    item = item,
                                    highlights = readerState.highlights,
                                    fontSize = adjustedFontSize,
                                    fontFamily = fontFamily,
                                    textAlign = textAlign,
                                    lineHeight = lineHeight,
                                    textColor = textColor,
                                    layoutVersion = layoutVersion,
                                    onPositioned = { coords ->
                                        val posInRoot = coords.positionInRoot()
                                        val windowPos = containerPositionInWindow + posInRoot
                                        if (itemPositions[item.id] != windowPos) {
                                            itemPositions[item.id] = windowPos
                                        }
                                    },
                                    onLayoutChange = { layout ->
                                        textLayoutResults[item.id] = layout
                                        layoutVersion++ // Trigger highlight paths update
                                    },
                                    onTap = { offset ->
                                        val layoutResult = textLayoutResults[item.id] ?: return@TextItemRow
                                        val charOffset = layoutResult.getOffsetForPosition(offset)
                                        val absoluteOffset = item.charOffsetInChapter + charOffset
                                        val chapterIndex = item.id.substringBefore("_").toIntOrNull() ?: -1
                                        val existingHl = readerState.highlights.find { hl ->
                                            hl.chapterIndex == chapterIndex &&
                                            absoluteOffset >= hl.startOffset && absoluteOffset <= hl.endOffset
                                        }
                                        if (existingHl != null) {
                                            selectedTextInfo = SelectionInfo(item.id, existingHl.startOffset to existingHl.endOffset, existingHl.selectedText, existingHl)
                                            selectionRect = layoutResult.getCursorRect(charOffset)
                                        } else {
                                            selectedTextInfo = null
                                        }
                                    }
                                )
                            }
                        }
                        is ReaderItem.Heading -> {
                            HeadingItemRow(
                                item = item, 
                                textColor = textColor, 
                                adjustedFontSize = adjustedFontSize,
                                onPositioned = { coords ->
                                    val posInRoot = coords.positionInRoot()
                                    val windowPos = containerPositionInWindow + posInRoot
                                    if (itemPositions[item.id] != windowPos) {
                                        itemPositions[item.id] = windowPos
                                    }
                                }
                            )
                        }
                        is ReaderItem.Image -> {
                            EpubImageItem(
                                path = item.path, 
                                loadedImages = readerState.loadedImages,
                                onPositioned = { coords ->
                                    val posInRoot = coords.positionInRoot()
                                    val windowPos = containerPositionInWindow + posInRoot
                                    if (itemPositions[item.id] != windowPos) {
                                        itemPositions[item.id] = windowPos
                                    }
                                },
                                onLoad = { viewModel.loadImage(item.path) }
                            )
                        }
                    }
                }
            }
            
            if (readerState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = textColor)
                }
            }
            
            selectedTextInfo?.let { info ->
                if (info.existingHighlight != null) {
                    val rect = selectionRect ?: Rect.Zero
                    Popup(
                        offset = androidx.compose.ui.unit.IntOffset(rect.left.toInt(), (rect.top.toInt() - 180).coerceAtLeast(0)),
                        onDismissRequest = { selectedTextInfo = null }
                    ) {
                        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 12.dp, color = Color(0xFF333333)) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                listOf(0xFFFFF176, 0xFFAED581, 0xFF81D4FA, 0xFFF06292).forEach { color ->
                                    Box(Modifier.size(32.dp).background(Color(color), CircleShape).clickable {
                                        val hl = info.existingHighlight.copy(color = color.toInt())
                                        viewModel.addHighlight(hl)
                                        selectedTextInfo = null
                                    })
                                }
                                Box(Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = "Note",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp).clickable { 
                                        onHighlightNoteClicked(info.existingHighlight)
                                        selectedTextInfo = null
                                    }
                                )
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp).clickable {
                                        onHighlightDeleted(info.existingHighlight)
                                        selectedTextInfo = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextItemRow(
    item: ReaderItem.Text,
    highlights: List<Highlight>,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    textAlign: androidx.compose.ui.text.style.TextAlign,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    textColor: Color,
    layoutVersion: Int,
    onPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit,
    onLayoutChange: (TextLayoutResult) -> Unit,
    onTap: (Offset) -> Unit
) {
    val itemTextAlign = when(item.textAlign) {
        "center" -> androidx.compose.ui.text.style.TextAlign.Center
        "justify" -> androidx.compose.ui.text.style.TextAlign.Justify
        "right" -> androidx.compose.ui.text.style.TextAlign.Right
        "left" -> androidx.compose.ui.text.style.TextAlign.Left
        else -> textAlign
    }
    
    val itemStart = item.charOffsetInChapter
    val itemEnd = itemStart + item.text.length
    val chapterIndex = remember(item.id) { item.id.substringBefore("_").toIntOrNull() ?: -1 }
    
    val chapterHighlightCount = remember(highlights, chapterIndex) {
        highlights.count { it.chapterIndex == chapterIndex }
    }
    val relevantHighlights = remember(chapterHighlightCount, item.id, itemStart, itemEnd) {
        highlights.filter { hl ->
            hl.chapterIndex == chapterIndex &&
            ((hl.startOffset >= itemStart && hl.startOffset < itemEnd) ||
             (hl.endOffset > itemStart && hl.endOffset <= itemEnd) ||
             (hl.startOffset <= itemStart && hl.endOffset >= itemEnd))
        }
    }

    // Capture layout for highlights - key only on layoutVersion (manual map update)
    val highlightPaths = remember(relevantHighlights, layoutVersion) {
        // Find our layout result in the parent's map
        // We can't observe the map, so we use layoutVersion as a trigger
        emptyList<Pair<androidx.compose.ui.graphics.Path, Color>>()
    }
    
    // We actually need a way to draw the highlights.
    // The previous approach with localLayout was better for self-contained drawing.
    var localLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val localHighlightPaths = remember(relevantHighlights, localLayout) {
        val layout = localLayout ?: return@remember emptyList()
        relevantHighlights.mapNotNull { hl ->
            val start = (hl.startOffset - itemStart).coerceIn(0, item.text.length)
            val end   = (hl.endOffset   - itemStart).coerceIn(0, item.text.length)
            if (start < end) {
                runCatching { layout.getPathForRange(start, end) to Color(hl.color) }.getOrNull()
            } else null
        }
    }

    Text(
        text = item.text,
        color = textColor,
        fontSize = fontSize,
        fontFamily = fontFamily,
        textAlign = itemTextAlign,
        lineHeight = lineHeight,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onPositioned(it) }
            .drawWithContent {
                localHighlightPaths.forEach { (path, color) ->
                    if (!path.isEmpty) drawPath(path, color.copy(alpha = 0.5f))
                }
                drawContent()
            }
            .pointerInput(item.id) {
                detectTapGestures(onTap = onTap)
            },
        onTextLayout = { result ->
            localLayout = result
            onLayoutChange(result)
        }
    )
}

@Composable
fun HeadingItemRow(
    item: ReaderItem.Heading,
    textColor: Color,
    adjustedFontSize: androidx.compose.ui.unit.TextUnit,
    onPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit
) {
    Text(
        text = item.text, 
        color = textColor, 
        fontSize = adjustedFontSize * 1.5f, 
        fontWeight = FontWeight.Bold, 
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .onGloballyPositioned { onPositioned(it) }
    )
}

@Immutable
data class SelectionInfo(
    val itemId: String,
    val range: Pair<Int, Int>,
    val text: String,
    val existingHighlight: Highlight? = null
)

@Composable
fun EpubImageItem(
    path: String, 
    loadedImages: Map<String, Bitmap?>, 
    onPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit,
    onLoad: () -> Unit
) {
    val bitmap = loadedImages[path]
    Box(Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .onGloballyPositioned { onPositioned(it) }, 
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.fillMaxWidth())
        else { LaunchedEffect(path) { onLoad() }; CircularProgressIndicator() }
    }
}
