package com.freeebookbundle.epub

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freeebookbundle.db.Highlight
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

private fun TextLayoutResult.getWordRange(offset: Int): IntRange {
    val text = layoutInput.text.text
    if (text.isEmpty() || offset < 0 || offset >= text.length) return IntRange.EMPTY
    val clamped = offset.coerceIn(0, text.length - 1)
    if (text[clamped].isWhitespace()) return IntRange.EMPTY
    var start = clamped
    while (start > 0 && !text[start - 1].isWhitespace()) start--
    var end = clamped
    while (end < text.length && !text[end].isWhitespace()) end++
    return start until end
}

@Composable
fun NativeEpubReader(
    filePath: String,
    bookId: String? = null,
    initialChapter: Int = 0,
    targetChapter: Int? = null,
    targetAnchor: String? = null,
    targetScrollOffset: Int = 0,
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

    val textLayoutResults = remember { HashMap<String, TextLayoutResult>() }
    val itemPositions = remember { HashMap<String, Offset>() }

    LaunchedEffect(filePath) { viewModel.loadBook(filePath, initialChapter) }
    LaunchedEffect(targetChapter, targetAnchor) {
        if (targetChapter != null) {
            onJumpToChapter(targetChapter, targetAnchor)
        }
    }

    val clipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val ttsManager = remember { com.freeebookbundle.voice.TtsVoiceManager(context) }
    LaunchedEffect(Unit) { ttsManager.initialize { } }

    val selStartItemId: MutableState<String?> = remember { mutableStateOf(null) }
    val selStartOffset: MutableIntState = remember { mutableIntStateOf(0) }
    val selEndItemId: MutableState<String?> = remember { mutableStateOf(null) }
    val selEndOffset: MutableIntState = remember { mutableIntStateOf(0) }
    val isSelecting: MutableState<Boolean> = remember { mutableStateOf(false) }
    val activeSelectionText by remember {
        derivedStateOf {
            if (selStartItemId.value == null || selEndItemId.value == null || !isSelecting.value) ""
            else buildSelectedText(selStartItemId.value!!, selStartOffset.intValue, selEndItemId.value!!, selEndOffset.intValue, readerState.items)
        }
    }

    var selectedTextInfoOld by remember { mutableStateOf<SelectionInfo?>(null) }

    LaunchedEffect(readerState.currentChapterIndex) {
        textLayoutResults.clear()
        itemPositions.clear()
        selStartItemId.value = null
        selEndItemId.value = null
        isSelecting.value = false
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
                if (targetScrollOffset > 0) {
                    delay(100)
                    val progress = targetScrollOffset / 1000f
                    val targetChapterPrefix2 = "${readerState.currentChapterIndex}_item_"
                    val itemsInChapter = readerState.items.count { it.id.startsWith(targetChapterPrefix2) }
                    if (itemsInChapter > 0) {
                        val extraItems = (itemsInChapter * progress).toInt().coerceAtMost(itemsInChapter - 1)
                        val itemIndex = (targetIndex + extraItems).coerceAtMost(readerState.items.lastIndex)
                        if (itemIndex > targetIndex) {
                            listState.animateScrollToItem(itemIndex)
                        }
                    }
                }
            }
            viewModel.consumeJumpTrigger()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { visibleIndex ->
                if (readerState.items.isNotEmpty()) {
                    viewModel.updateProgress(visibleIndex)
                    onProgressChange(viewModel.currentProgress.value)
                    onChapterChange(viewModel.currentChapter.value, viewModel.currentProgress.value)
                }
            }
    }

    Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {
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
                        TextItemRow(
                            item = item,
                            highlights = readerState.highlights,
                            fontSize = adjustedFontSize,
                            fontFamily = fontFamily,
                            textAlign = textAlign,
                            lineHeight = lineHeight,
                            textColor = textColor,
                            onPositioned = { coords ->
                                val windowPos = coords.positionInWindow()
                                if (itemPositions[item.id] != windowPos) {
                                    itemPositions[item.id] = windowPos
                                }
                            },
                            onLayoutChange = { layout ->
                                textLayoutResults[item.id] = layout
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
                                    selectedTextInfoOld = SelectionInfo(item.id, existingHl.startOffset to existingHl.endOffset, existingHl.selectedText, existingHl)
                                } else {
                                    selectedTextInfoOld = null
                                }
                            },
                            selStartItemIdState = selStartItemId,
                            selStartOffsetState = selStartOffset,
                            selEndItemIdState = selEndItemId,
                            selEndOffsetState = selEndOffset,
                            isSelectingState = isSelecting,
                            onSelectionStart = { itemId, startOff ->
                                selStartItemId.value = itemId
                                selStartOffset.intValue = startOff
                                selEndItemId.value = itemId
                                selEndOffset.intValue = startOff
                                isSelecting.value = true
                            },
                            onSelectionExtend = { itemId, endOff ->
                                selEndItemId.value = itemId
                                selEndOffset.intValue = endOff
                            },
                            onSelectionEnd = {
                            }
                        )
                    }
                    is ReaderItem.Heading -> {
                        HeadingItemRow(
                            item = item, 
                            textColor = textColor, 
                            adjustedFontSize = adjustedFontSize,
                            onPositioned = { coords ->
                                val windowPos = coords.positionInWindow()
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
                                val windowPos = coords.positionInWindow()
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
        
        // Tap-on-existing-highlight popup
        selectedTextInfoOld?.let { info ->
            if (info.existingHighlight != null) {
                Popup(
                    onDismissRequest = { selectedTextInfoOld = null },
                    properties = PopupProperties(focusable = true)
                ) {
                    Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 12.dp, color = Color(0xFF333333)) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf(0xFFFFF176, 0xFFAED581, 0xFF81D4FA, 0xFFF06292).forEach { color ->
                                Box(Modifier.size(32.dp).background(Color(color), CircleShape).clickable {
                                    val hl = info.existingHighlight.copy(color = color.toInt(), id = System.nanoTime() and Long.MAX_VALUE)
                                    onHighlightDeleted(info.existingHighlight)
                                    viewModel.deleteHighlight(info.existingHighlight)
                                    viewModel.addHighlight(hl)
                                    onHighlightsAdded(listOf(hl))
                                    selectedTextInfoOld = null
                                })
                            }
                            Box(Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Note",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp).clickable { 
                                    onHighlightNoteClicked(info.existingHighlight)
                                    selectedTextInfoOld = null
                                }
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp).clickable {
                                    onHighlightDeleted(info.existingHighlight)
                                    selectedTextInfoOld = null
                                }
                            )
                        }
                    }
                }
            }
        }

        // Selection action toolbar
        if (isSelecting.value && selStartItemId.value != null && selEndItemId.value != null) {
            val selItem = readerState.items.find { it.id == selStartItemId.value } as? ReaderItem.Text
            if (selItem != null) {
                val layout = textLayoutResults[selStartItemId.value]
                val anchorRect = if (layout != null) {
                    val selMin = minOf(selStartOffset.intValue, selEndOffset.intValue)
                    val selMax = maxOf(selStartOffset.intValue, selEndOffset.intValue)
                    layout.getBoundingBox(if (selMax < layout.layoutInput.text.length) selMax else selMin)
                } else null
                
                if (anchorRect != null) {
                    val selItemPosInWindow = itemPositions[selStartItemId.value] ?: Offset.Zero
                    val anchorTopLeft = Offset(selItemPosInWindow.x + anchorRect.left, selItemPosInWindow.y + anchorRect.top)
                    Popup(
                        offset = IntOffset(anchorTopLeft.x.toInt(), (anchorTopLeft.y.toInt() - 220).coerceAtLeast(0)),
                        onDismissRequest = {
                            isSelecting.value = false
                            selStartItemId.value = null
                            selEndItemId.value = null
                        },
                        properties = PopupProperties(focusable = true)
                    ) {
                        Surface(
                            modifier = Modifier.wrapContentSize().padding(8.dp),
                            shape = RoundedCornerShape(28.dp),
                            tonalElevation = 12.dp,
                            shadowElevation = 16.dp,
                            color = Color(0xFF222222)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(0xFFFFF176, 0xFFAED581, 0xFF81D4FA, 0xFFF06292).forEach { colorLong ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Color(colorLong), CircleShape)
                                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                                .clickable {
                                                    saveSelectionAsHighlight(
                                                        selStartItemId.value!!, selStartOffset.intValue, selEndItemId.value!!, selEndOffset.intValue,
                                                        activeSelectionText, colorLong.toInt(),
                                                        readerState.items, bookId, viewModel, onHighlightsAdded
                                                    )
                                                    isSelecting.value = false
                                                    selStartItemId.value = null
                                                    selEndItemId.value = null
                                                }
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    IconButton(onClick = {
                                        if (activeSelectionText.isNotBlank()) {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(activeSelectionText))
                                        }
                                        isSelecting.value = false
                                        selStartItemId.value = null
                                        selEndItemId.value = null
                                    }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.ContentCopy, "Copy", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = {
                                        if (activeSelectionText.isNotBlank()) {
                                            ttsManager.speak(text = activeSelectionText, utteranceId = "selection")
                                        }
                                        isSelecting.value = false
                                        selStartItemId.value = null
                                        selEndItemId.value = null
                                    }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.VolumeUp, "Speak", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = {
                                        if (activeSelectionText.isNotBlank()) {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                data = android.net.Uri.parse("https://www.google.com/search?q=define+${activeSelectionText}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.MenuBook, "Dict", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = {
                                        if (activeSelectionText.isNotBlank()) {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                data = android.net.Uri.parse("https://en.wikipedia.org/wiki/${activeSelectionText}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Search, "Wiki", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
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
    fontSize: TextUnit,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    textAlign: TextAlign,
    lineHeight: TextUnit,
    textColor: Color,
    onPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit,
    onLayoutChange: (TextLayoutResult) -> Unit,
    onTap: (Offset) -> Unit,
    selStartItemIdState: State<String?>,
    selStartOffsetState: IntState,
    selEndItemIdState: State<String?>,
    selEndOffsetState: IntState,
    isSelectingState: State<Boolean>,
    onSelectionStart: (itemId: String, offset: Int) -> Unit,
    onSelectionExtend: (itemId: String, offset: Int) -> Unit,
    onSelectionEnd: () -> Unit
) {
    val itemTextAlign = when(item.textAlign) {
        "center" -> TextAlign.Center
        "justify" -> TextAlign.Justify
        "right" -> TextAlign.Right
        "left" -> TextAlign.Left
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

    val activeSelectionPath by remember(localLayout) {
        derivedStateOf {
            if (!isSelectingState.value || localLayout == null) return@derivedStateOf null
            val layout = localLayout!!
            val textLen = layout.layoutInput.text.length
            val thisItemId = item.id

            val selStartInThis = if (selStartItemIdState.value == thisItemId) selStartOffsetState.intValue.coerceIn(0, textLen) else null
            val selEndInThis = if (selEndItemIdState.value == thisItemId) selEndOffsetState.intValue.coerceIn(0, textLen) else null

            if (selStartItemIdState.value == thisItemId && selEndItemIdState.value == thisItemId) {
                val s = minOf(selStartInThis!!, selEndInThis!!)
                val e = maxOf(selStartInThis, selEndInThis!!)
                if (s < e) runCatching { layout.getPathForRange(s, e) }.getOrNull() else null
            } else if (selStartItemIdState.value == thisItemId) {
                runCatching { layout.getPathForRange(selStartInThis!!, textLen) }.getOrNull()
            } else if (selEndItemIdState.value == thisItemId) {
                runCatching { layout.getPathForRange(0, selEndInThis!!) }.getOrNull()
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
                val selPath = activeSelectionPath
                if (selPath != null && !selPath.isEmpty) {
                    drawPath(selPath, Color(0xFFE50914).copy(alpha = 0.35f))
                }
                drawContent()
            }
            .pointerInput(item.id) {
                detectTapGestures(onTap = onTap)
            }
            .pointerInput(item.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val layout = localLayout ?: return@detectDragGesturesAfterLongPress
                        val charOffset = layout.getOffsetForPosition(offset)
                        val wordRange = layout.getWordRange(charOffset)
                        if (!wordRange.isEmpty()) {
                            onSelectionStart(item.id, wordRange.first)
                            onSelectionExtend(item.id, wordRange.last + 1)
                        }
                    },
                    onDrag = { change, _ ->
                        if (isSelectingState.value) {
                            val layout = localLayout ?: return@detectDragGesturesAfterLongPress
                            val pos = change.position
                            val clampedX = pos.x.coerceIn(0f, layout.size.width.toFloat())
                            val clampedY = pos.y.coerceIn(0f, layout.size.height.toFloat())
                            val charOffset = layout.getOffsetForPosition(Offset(clampedX, clampedY))
                            val wordRange = layout.getWordRange(charOffset)
                            if (!wordRange.isEmpty()) {
                                onSelectionExtend(item.id, wordRange.last + 1)
                            }
                        }
                    },
                    onDragEnd = {
                        onSelectionEnd()
                    }
                )
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

private fun saveSelectionAsHighlight(
    selStartItemId: String,
    selStartOffset: Int,
    selEndItemId: String,
    selEndOffset: Int,
    text: String,
    color: Int,
    items: List<ReaderItem>,
    bookId: String?,
    viewModel: EpubReaderViewModel,
    onHighlightsAdded: (List<Highlight>) -> Unit
) {
    val startItem = items.find { it.id == selStartItemId } as? ReaderItem.Text ?: return
    val endItem = items.find { it.id == selEndItemId } as? ReaderItem.Text ?: return
    val chapterIndex = startItem.id.substringBefore("_").toIntOrNull() ?: 0
    val hl = Highlight(
        id = System.nanoTime() and Long.MAX_VALUE,
        bookId = bookId ?: "",
        chapterIndex = chapterIndex,
        startOffset = startItem.charOffsetInChapter + minOf(selStartOffset, selEndOffset),
        endOffset = endItem.charOffsetInChapter + maxOf(selStartOffset, selEndOffset),
        selectedText = text,
        color = color,
        annotation = null,
        createdAt = System.currentTimeMillis()
    )
    viewModel.addHighlight(hl)
    onHighlightsAdded(listOf(hl))
}

private fun buildSelectedText(
    selStartItemId: String,
    selStartOffset: Int,
    selEndItemId: String,
    selEndOffset: Int,
    items: List<ReaderItem>
): String {
    val startIdx = items.indexOfFirst { it.id == selStartItemId }
    val endIdx = items.indexOfFirst { it.id == selEndItemId }
    if (startIdx < 0 || endIdx < 0) return ""
    val actualStart = minOf(startIdx, endIdx)
    val actualEnd = maxOf(startIdx, endIdx)
    val builder = StringBuilder()
    for (i in actualStart..actualEnd) {
        val item = items[i] as? ReaderItem.Text ?: continue
        val s = if (i == actualStart && item.id == selStartItemId) {
            minOf(selStartOffset, selEndOffset).coerceIn(0, item.text.length)
        } else 0
        val e = if (i == actualEnd && item.id == selEndItemId) {
            maxOf(selStartOffset, selEndOffset).coerceIn(0, item.text.length)
        } else item.text.length
        if (s < e) builder.append(item.text.substring(s, e)).append(" ")
    }
    return builder.toString().trimEnd()
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
