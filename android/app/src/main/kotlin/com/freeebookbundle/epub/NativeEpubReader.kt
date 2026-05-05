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
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freeebookbundle.db.Highlight
import kotlinx.coroutines.launch

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
    val readerState by viewModel.state.collectAsState()

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
    var activeNativeSelection by remember { mutableStateOf<SelectionInfo?>(null) }
    
    val textLayoutResults = remember { mutableStateMapOf<String, TextLayoutResult>() }
    val itemPositions = remember { mutableStateMapOf<String, Offset>() }

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
    var selectionKey by remember { mutableIntStateOf(0) }

    // CRITICAL: Capture current state in refs to avoid stale closures in the toolbar callbacks
    val currentReaderState by rememberUpdatedState(readerState)
    val currentActiveSelections by rememberUpdatedState(activeSelections)

    // Clear stale layout data ONLY when the chapter changes
    LaunchedEffect(readerState.currentChapterIndex) {
        textLayoutResults.clear()
    }

    val textToolbar = remember { 
        EpubTextToolbar(
            onColorSelected = { color ->
                android.util.Log.d("NativeEpubReader", "onColorSelected: Processing ${currentActiveSelections.size} selections")
                if (currentActiveSelections.isNotEmpty()) {
                    val highlightsToSave = mutableListOf<Highlight>()
                    currentActiveSelections.forEach { selection ->
                        // Use currentReaderState to avoid stale item list
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
                        } else {
                            android.util.Log.e("NativeEpubReader", "onColorSelected: Could not find item ${selection.itemId}")
                        }
                    }
                    if (highlightsToSave.isNotEmpty()) {
                        android.util.Log.d("NativeEpubReader", "onColorSelected: Saving ${highlightsToSave.size} highlights")
                        viewModel.addHighlights(highlightsToSave)
                        onHighlightsAdded(highlightsToSave)
                    }
                }
                selectedTextInfo = null
                // Force clear selection handles
                selectionKey++
                activeSelections.clear()
            },
            onCopyRequested = {
                val fullText = currentActiveSelections.joinToString(" ") { it.text }
                if (fullText.isNotEmpty()) {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(fullText))
                }
                selectedTextInfo = null
                selectionKey++
                activeSelections.clear()
            },
            onSpeakRequested = {
                val fullText = currentActiveSelections.joinToString(" ") { it.text }
                if (fullText.isNotEmpty()) {
                    ttsManager.speak(text = fullText, utteranceId = "selection_multi")
                }
                selectionKey++
                activeSelections.clear()
            },
            onDictionaryRequested = {
                val text = currentActiveSelections.firstOrNull()?.text ?: ""
                if (text.isNotEmpty()) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://www.google.com/search?q=define+${text}")
                    }
                    context.startActivity(intent)
                }
                // Don't clear selection yet as user might want to highlight after dict lookup
            },
            onWikipediaRequested = {
                val text = currentActiveSelections.firstOrNull()?.text ?: ""
                if (text.isNotEmpty()) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://en.wikipedia.org/wiki/${text}")
                    }
                    context.startActivity(intent)
                }
                // Don't clear selection yet
            },
            onDismissSelection = {
                selectionKey++
                activeSelections.clear()
            },
            onMenuShown = { rect ->
                val density = context.resources.displayMetrics.density
                activeSelections.clear()

                android.util.Log.d("NativeEpubReader", "onMenuShown: Window Rect: $rect")

                if (rect.height > 0f && rect.width > 0f) {
                    val handleHeightPx = 25f * density
                    val visibleKeys = listState.layoutInfo.visibleItemsInfo.map { it.key }.toSet()

                    textLayoutResults.forEach { (itemId, result) ->
                    if (!visibleKeys.contains(itemId)) return@forEach
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

                    // ── Pass 1: identify the first and last selected line indices ──────────
                    // We need these BEFORE the char loop so we know which horizontal
                    // constraint to apply (left-only / right-only / none / both).
                    var firstSelectedLine = Int.MAX_VALUE
                    var lastSelectedLine  = Int.MIN_VALUE

                    for (i in 0 until textLength) {
                        val line       = result.getLineForOffset(i)
                        val lineCenter = (result.getLineTop(line) + result.getLineBottom(line)) / 2f
                        if (lineCenter in localRect.top..localRect.bottom) {
                            if (line < firstSelectedLine) firstSelectedLine = line
                            if (line > lastSelectedLine)  lastSelectedLine  = line
                        }
                    }

                    if (firstSelectedLine == Int.MAX_VALUE) return@forEach // block not in selection

                    // ── Pass 2: collect characters with per-line horizontal rules ──────────
                    var currentStart  = -1
                    var capturedCount = 0

                    for (i in 0 until textLength) {
                        val line       = result.getLineForOffset(i)
                        val lineCenter = (result.getLineTop(line) + result.getLineBottom(line)) / 2f

                        // Vertical gate: line centre must be inside the (handle-trimmed) rect
                        if (lineCenter !in localRect.top..localRect.bottom) {
                            if (currentStart != -1) {
                                activeSelections.add(SelectionInfo(
                                    itemId, currentStart to i,
                                    result.layoutInput.text.text.substring(currentStart, i)
                                ))
                                currentStart = -1
                            }
                            continue
                        }

                        val charBounds = result.getBoundingBox(i)

                        // Horizontal rule depends on which line we're on:
                        //
                        //  ┌─────── first line ────────────────────────────────────┐
                        //  │ [localRect.left ──────────────────── line right edge] │
                        //  ├─────── middle lines ──────────────────────────────────┤
                        //  │ [line left edge ─────────────── line right edge]      │
                        //  ├─────── last line ─────────────────────────────────────┤
                        //  │ [line left edge ──────── localRect.right]             │
                        //  └───────────────────────────────────────────────────────┘
                        //  (when firstLine == lastLine both constraints apply)

                        val effectiveLeft = when {
                            line == firstSelectedLine -> localRect.left  // start of selection
                            else                     -> Float.NEGATIVE_INFINITY // full line
                        }
                        val effectiveRight = when {
                            line == lastSelectedLine -> localRect.right  // end of selection
                            else                    -> Float.POSITIVE_INFINITY // full line
                        }

                        val overlapLeft  = maxOf(charBounds.left,  effectiveLeft)
                        val overlapRight = minOf(charBounds.right, effectiveRight)
                        val overlapWidth = maxOf(0f, overlapRight - overlapLeft)

                        val isCharSelected = overlapWidth > charBounds.width * 0.5f

                        if (isCharSelected) {
                            if (currentStart == -1) currentStart = i
                            capturedCount++
                        } else if (currentStart != -1) {
                            activeSelections.add(SelectionInfo(
                                itemId, currentStart to i,
                                result.layoutInput.text.text.substring(currentStart, i)
                            ))
                            currentStart = -1
                        }
                    }

                    // Flush any trailing run
                    if (currentStart != -1) {
                        activeSelections.add(SelectionInfo(
                            itemId, currentStart to textLength,
                            result.layoutInput.text.text.substring(currentStart, textLength)
                        ))
                    }

                    if (capturedCount > 0) {
                        android.util.Log.d("NativeEpubReader", "Block $itemId: Precision-captured $capturedCount chars across ${lastSelectedLine - firstSelectedLine + 1} line(s).")
                    }
                }
                android.util.Log.d("NativeEpubReader", "onMenuShown: Sweep complete. Total regions: ${activeSelections.size}")
            }
        }
        )
    }

    LaunchedEffect(readerState.jumpTrigger) {
        if (readerState.jumpTrigger != null && readerState.items.isNotEmpty()) {
            val anchor = readerState.jumpAnchor
            
            // 1. Precise Match: Look for specific fragment/anchor
            var targetIndex = if (!anchor.isNullOrEmpty()) {
                readerState.items.indexOfFirst { item ->
                    (item is ReaderItem.Text && item.locator == anchor)
                }
            } else -1
            
            // 2. Chapter Match: Fallback to top of chapter
            if (targetIndex == -1) {
                val targetChapterPrefix = "${readerState.currentChapterIndex}_item_"
                targetIndex = readerState.items.indexOfFirst { it.id.startsWith(targetChapterPrefix) }
            }
            
            android.util.Log.d("NativeEpubReader", "Jump Trigger: chapter=${readerState.currentChapterIndex}, anchor=$anchor, targetIndex=$targetIndex")
            
            if (targetIndex >= 0) {
                // Yield to let LazyColumn recompose with the new items list first
                kotlinx.coroutines.yield()
                listState.scrollToItem(targetIndex)
            }
            viewModel.consumeJumpTrigger()
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (readerState.items.isNotEmpty()) {
            viewModel.updateProgress(listState.firstVisibleItemIndex)
            onProgressChange(readerState.progress)
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
            itemPositions["container"] = it.positionInWindow() 
        }) {
            textToolbar.Content()
            
            key(selectionKey) {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        items(items = readerState.items, key = { it.id }) { item ->
                            when (item) {
                                is ReaderItem.Text -> {
                                    val itemTextAlign = when(item.textAlign) {
                                        "center" -> androidx.compose.ui.text.style.TextAlign.Center
                                        "justify" -> androidx.compose.ui.text.style.TextAlign.Justify
                                        "right" -> androidx.compose.ui.text.style.TextAlign.Right
                                        "left" -> androidx.compose.ui.text.style.TextAlign.Left
                                        else -> textAlign // fallback to global
                                    }
                                    
                                    val itemStart = item.charOffsetInChapter
                                    val itemEnd = itemStart + item.text.length
                                    val chapterIndex = item.id.substringBefore("_").toIntOrNull() ?: -1
                                    
                                    val relevantHighlights = remember(readerState.highlights, item.id) {
                                        readerState.highlights.filter { hl ->
                                            hl.chapterIndex == chapterIndex &&
                                            ((hl.startOffset >= itemStart && hl.startOffset < itemEnd) ||
                                             (hl.endOffset > itemStart && hl.endOffset <= itemEnd) ||
                                             (hl.startOffset <= itemStart && hl.endOffset >= itemEnd))
                                        }
                                    }

                                    Text(
                                        text = item.text,
                                        color = textColor,
                                        fontSize = adjustedFontSize,
                                        fontFamily = fontFamily,
                                        textAlign = itemTextAlign,
                                        lineHeight = lineHeight,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { layoutCoordinates ->
                                                itemPositions[item.id] = layoutCoordinates.positionInWindow()
                                            }
                                            .drawWithContent {
                                                // DRAW HIGHLIGHTS BEFORE CONTENT (so they appear behind text)
                                                val layout = textLayoutResults[item.id]
                                                if (layout != null && relevantHighlights.isNotEmpty()) {
                                                    relevantHighlights.forEach { hl ->
                                                        val start = (hl.startOffset - itemStart).coerceIn(0, item.text.length)
                                                        val end = (hl.endOffset - itemStart).coerceIn(0, item.text.length)
                                                        
                                                        if (start < end) {
                                                            try {
                                                                val path = layout.getPathForRange(start, end)
                                                                if (!path.isEmpty) {
                                                                    drawPath(
                                                                        path = path,
                                                                        color = Color(hl.color).copy(alpha = 0.5f)
                                                                    )
                                                                }
                                                            } catch (e: Exception) {
                                                                // Ignore range errors
                                                            }
                                                        }
                                                    }
                                                }
                                                drawContent()
                                            }
                                            .pointerInput(item.id) {
                                                detectTapGestures(
                                                    onTap = { offset ->
                                                        val layoutResult = textLayoutResults[item.id] ?: return@detectTapGestures
                                                        val charOffset = layoutResult.getOffsetForPosition(offset)
                                                        val absoluteOffset = item.charOffsetInChapter + charOffset
                                                        
                                                        val existingHl = relevantHighlights.find { hl ->
                                                            absoluteOffset >= hl.startOffset && absoluteOffset <= hl.endOffset
                                                        }
                                                        
                                                        if (existingHl != null) {
                                                            selectedTextInfo = SelectionInfo(item.id, existingHl.startOffset to existingHl.endOffset, existingHl.selectedText, existingHl)
                                                            selectionRect = layoutResult.getCursorRect(charOffset)
                                                            activeNativeSelection = null
                                                        } else {
                                                            selectedTextInfo = null
                                                            activeNativeSelection = null
                                                        }
                                                    }
                                                )
                                            },
                                        onTextLayout = { result ->
                                            textLayoutResults[item.id] = result
                                        }
                                    )
                                }
                                is ReaderItem.Heading -> {
                                    Text(
                                        text = item.text, 
                                        color = textColor, 
                                        fontSize = adjustedFontSize * 1.5f, 
                                        fontWeight = FontWeight.Bold, 
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp)
                                            .onGloballyPositioned { layoutCoordinates ->
                                                itemPositions[item.id] = layoutCoordinates.positionInWindow()
                                            }
                                    )
                                }
                                is ReaderItem.Image -> EpubImageItem(item.path, readerState.loadedImages) { viewModel.loadImage(item.path) }
                            }
                        }
                    }
                }
            }
            
            // Existing Highlight Toolbar (for editing existing highlights)
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
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { 
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

data class SelectionInfo(
    val itemId: String,
    val range: Pair<Int, Int>,
    val text: String,
    val existingHighlight: Highlight? = null
)

@Composable
fun EpubImageItem(path: String, loadedImages: Map<String, Bitmap?>, onLoad: () -> Unit) {
    val bitmap = loadedImages[path]
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.fillMaxWidth())
        else { LaunchedEffect(path) { onLoad() }; CircularProgressIndicator() }
    }
}
