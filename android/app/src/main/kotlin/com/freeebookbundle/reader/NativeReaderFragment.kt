package com.freeebookbundle.reader

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freeebookbundle.db.Bookmark
import com.freeebookbundle.epub.EpubReaderViewModel
import com.freeebookbundle.epub.NativeEpubReader
import com.freeebookbundle.repository.ReaderSettingsRepository
import com.freeebookbundle.viewmodel.AppThemeMode
import com.freeebookbundle.viewmodel.AppViewModel
import com.freeebookbundle.viewmodel.ReaderTheme
import com.freeebookbundle.viewmodel.ReaderViewModel
import com.freeebookbundle.ui.ReaderTutorialOverlay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private const val TAG = "NativeReaderFragment"

class NativeReaderFragment : Fragment() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val filePath = arguments?.getString("FILE_PATH")
                val bookId = arguments?.getString("BOOK_ID")
                NativeReaderScreen(
                    filePath = filePath,
                    bookId = bookId,
                    themeMode = appViewModel.themeMode.value,
                    onBack = { (context as? Activity)?.finish() },
                    onExit = { chapter, progress ->
                        if (bookId != null) {
                            val settingsRepo = ReaderSettingsRepository(requireContext())
                            settingsRepo.setNativeReaderPosition(bookId, chapter, progress)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeReaderScreen(
    filePath: String?,
    bookId: String? = null,
    themeMode: AppThemeMode = AppThemeMode.NIGHT,
    onBack: () -> Unit = {},
    onExit: (chapter: Int, progress: Float) -> Unit = { _, _ -> }
) {
    val epubViewModel: EpubReaderViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    // volatile progress/chapter is now collected only in leaf composables to prevent global recomposition
    
    var targetChapter by remember { mutableStateOf<Int?>(null) }
    var targetAnchor by remember { mutableStateOf<String?>(null) }
    var targetScrollOffset by remember { mutableIntStateOf(0) }
    
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { ReaderSettingsRepository(context) }
    val readerViewModel = remember { ReaderViewModel(context.applicationContext as android.app.Application) }
    val readerSettings by readerViewModel.settings.collectAsState()
    
    var showSettings by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showTOC by remember { mutableStateOf(false) }
    var showBookmarkInput by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var selectedHighlight by remember { mutableStateOf<com.freeebookbundle.db.Highlight?>(null) }
    
    var showTutorial by remember { mutableStateOf(!settingsRepo.hasSeenReaderTutorial()) }
    
    val readerState by epubViewModel.state.collectAsState()
    
    // Throttled DB write state
    var lastSavedProgress by remember { mutableFloatStateOf(-1f) }
    var lastSavedChapter by remember { mutableIntStateOf(-1) }
    var lastSavedTime by remember { mutableLongStateOf(0L) }
    val PROGRESS_SAVE_THROTTLE = 1000L // 1 second
    
    fun scheduleSave(chapter: Int, progress: Float) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSavedTime < PROGRESS_SAVE_THROTTLE) return
        
        if (bookId != null && (chapter != lastSavedChapter || 
            kotlin.math.abs(progress - lastSavedProgress) > 0.01f)) {
            scope.launch {
                readerViewModel.updateReadingProgress(bookId, chapter, progress)
                lastSavedChapter = chapter
                lastSavedProgress = progress
                lastSavedTime = currentTime
            }
        }
    }

    val backgroundColor = when (readerSettings.theme) {
        ReaderTheme.WHITE -> Color.White
        ReaderTheme.DARK -> Color(0xFF121212)
        ReaderTheme.SEPIA -> Color(0xFFFDFCF0)
    }
    val textColor = when (readerSettings.theme) {
        ReaderTheme.WHITE -> Color.Black
        ReaderTheme.DARK -> Color.White
        ReaderTheme.SEPIA -> Color(0xFF5B4636)
    }

    val accentColor = when (themeMode) {
        AppThemeMode.DAY -> Color(0xFF5B4636)
        AppThemeMode.NIGHT -> Color(0xFFE50914)
        AppThemeMode.SPACE -> Color(0xFF4A90D9)
    }

    LaunchedEffect(Unit) {
        bookId?.let { bId ->
            val savedPos = settingsRepo.getNativeReaderPosition(bId)
            val initialChapter = savedPos?.first ?: 0
            val highlights = readerViewModel.getHighlightsSync(bId)
            epubViewModel.loadBook(filePath, initialChapter = initialChapter, bookId = bId, savedHighlights = highlights)
            readerViewModel.recordBookOpened(bId)
        } ?: epubViewModel.loadBook(filePath, 0)
    }

    val sheetState = rememberModalBottomSheetState()

    BackHandler(enabled = showSettings || showBookmarks || showTOC || showBookmarkInput || showNoteEditor || showTutorial) {
        showSettings = false
        showBookmarks = false
        showTOC = false
        showBookmarkInput = false
        showNoteEditor = false
        showTutorial = false
    }

    Column(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        TopAppBar(
            title = {
                Text(
                    text = readerState.book?.title ?: "Reader",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = { 
                    val chapter = epubViewModel.currentChapter.value
                    val progress = epubViewModel.currentProgress.value
                    if (bookId != null) {
                        readerViewModel.updateReadingProgress(bookId, chapter, progress)
                        readerViewModel.recordBookOpened(bookId)
                    }
                    onExit(chapter, progress)
                    onBack() 
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                }
            },
            actions = {
                IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); showTOC = true }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "TOC", tint = textColor)
                }
                IconButton(onClick = { showBookmarkInput = true }) {
                    Icon(imageVector = Icons.Filled.Star, contentDescription = "Bookmark", tint = textColor)
                }
                IconButton(onClick = { showBookmarks = true }) {
                    Icon(imageVector = Icons.Filled.BookmarkBorder, contentDescription = "List", tint = textColor)
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = textColor)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor.copy(alpha = 0.8f) 
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Box(modifier = Modifier.weight(1f)) {
            if (filePath != null) {
                NativeEpubReader(
                    filePath = filePath,
                    bookId = bookId,
                    initialChapter = 0, 
                    targetChapter = targetChapter,
                    targetAnchor = targetAnchor,
                    targetScrollOffset = targetScrollOffset,
                    settings = readerSettings,
                    backgroundColor = backgroundColor,
                    textColor = textColor,
                    onProgressChange = { fraction ->
                        targetChapter = null
                        targetAnchor = null
                        targetScrollOffset = 0
                        scheduleSave(epubViewModel.currentChapter.value, fraction)
                    },
                    onJumpToChapter = { index, fragmentId ->
                        epubViewModel.jumpToChapter(index, fragmentId)
                    },
                    onChapterChange = { chapterIndex, fraction ->
                        scheduleSave(chapterIndex, fraction)
                    },
                    onHighlightsAdded = { highlights ->
                        scope.launch {
                            highlights.forEach { hl ->
                                readerViewModel.insertHighlight(
                                    bookId = bookId ?: "",
                                    chapterIndex = hl.chapterIndex,
                                    startOffset = hl.startOffset,
                                    endOffset = hl.endOffset,
                                    selectedText = hl.selectedText,
                                    color = hl.color,
                                    annotation = hl.annotation
                                )
                            }
                            if (highlights.isNotEmpty()) {
                                Toast.makeText(context, "Highlight saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onHighlightDeleted = { hl ->
                        scope.launch {
                            readerViewModel.deleteHighlight(hl)
                            epubViewModel.deleteHighlight(hl)
                            Toast.makeText(context, "Highlight deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onHighlightNoteClicked = { hl ->
                        selectedHighlight = hl
                        showNoteEditor = true
                    }
                )
            }

            if (showTutorial) {
                ReaderTutorialOverlay(
                    onFinish = {
                        showTutorial = false
                        settingsRepo.setHasSeenReaderTutorial(true)
                    }
                )
            }
        }

        ReaderProgressBar(epubViewModel, accentColor)
        
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
            ReaderChapterProgressText(epubViewModel, textColor)
        }
    }

    if (showTOC) {
        ModalBottomSheet(onDismissRequest = { showTOC = false }, sheetState = sheetState, containerColor = backgroundColor) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Table of Contents", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.fillMaxHeight(0.7f)) {
                    val chapters = readerState.book?.chapters ?: emptyList()
                    itemsIndexed(chapters) { index, chapter ->
                        val isCurrent = epubViewModel.currentChapter.collectAsStateWithLifecycle().value == index
                        Surface(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                targetChapter = index
                                targetAnchor = chapter.fragmentId
                                showTOC = false
                            },
                            color = if (isCurrent) accentColor.copy(alpha = 0.1f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val rawTitle = chapter.title
                            val displayTitle = if (rawTitle.startsWith("Chapter", ignoreCase = true)) {
                                rawTitle
                            } else {
                                "Chapter ${index + 1}: $rawTitle"
                            }
                            
                            Text(
                                text = displayTitle,
                                color = if (isCurrent) accentColor else textColor,
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                                fontSize = 16.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }, sheetState = sheetState, containerColor = backgroundColor) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                Text("Themes", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ThemeOption("Light", Color.White, Color.Black, readerSettings.theme == ReaderTheme.WHITE) {
                        readerViewModel.updateSettings(readerSettings.copy(theme = ReaderTheme.WHITE))
                    }
                    ThemeOption("Sepia", Color(0xFFFDFCF0), Color(0xFF5B4636), readerSettings.theme == ReaderTheme.SEPIA) {
                        readerViewModel.updateSettings(readerSettings.copy(theme = ReaderTheme.SEPIA))
                    }
                    ThemeOption("Dark", Color(0xFF121212), Color.White, readerSettings.theme == ReaderTheme.DARK) {
                        readerViewModel.updateSettings(readerSettings.copy(theme = ReaderTheme.DARK))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Font Family", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("serif", "sans-serif", "monospace").forEach { font ->
                        FilterChip(
                            selected = readerSettings.fontFamily == font,
                            onClick = { readerViewModel.updateSettings(readerSettings.copy(fontFamily = font)) },
                            label = { Text(font.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(labelColor = textColor, selectedLabelColor = Color.White, selectedContainerColor = accentColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Font Size", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = readerSettings.fontSize.toFloat(),
                    onValueChange = { readerViewModel.updateSettings(readerSettings.copy(fontSize = it.toDouble())) },
                    valueRange = 50f..250f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Line Height", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = readerSettings.lineHeight,
                    onValueChange = { readerViewModel.updateSettings(readerSettings.copy(lineHeight = it)) },
                    valueRange = 1f..2.5f,
                    steps = 15,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )
            }
        }
    }

    if (showNoteEditor && selectedHighlight != null) {
        ModalBottomSheet(onDismissRequest = { showNoteEditor = false }, sheetState = sheetState, containerColor = backgroundColor) {
            var noteText by remember { mutableStateOf(selectedHighlight?.annotation ?: "") }
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp)) {
                Text("Highlight Note", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(color = textColor.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(text = "\"${selectedHighlight?.selectedText}\"", color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = noteText, onValueChange = { noteText = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Write your thoughts...", color = textColor.copy(alpha = 0.4f)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = accentColor, unfocusedBorderColor = textColor.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val hl = selectedHighlight!!
                        scope.launch {
                            readerViewModel.insertHighlight(bookId ?: "", hl.chapterIndex, hl.startOffset, hl.endOffset, hl.selectedText, hl.color, noteText)
                            epubViewModel.jumpToChapter(epubViewModel.currentChapter.value)
                            showNoteEditor = false
                            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save Note", color = Color.White) }
            }
        }
    }

    if (showBookmarkInput) {
        ModalBottomSheet(onDismissRequest = { showBookmarkInput = false }, sheetState = sheetState, containerColor = backgroundColor) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Save Bookmark", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val chapter = epubViewModel.currentChapter.collectAsStateWithLifecycle().value
                val progress = epubViewModel.currentProgress.collectAsStateWithLifecycle().value
                Text(text = "Chapter ${chapter + 1}, ${(progress * 100).toInt()}%", color = textColor.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        readerViewModel.insertBookmark(
                            bookId ?: "", chapter, (progress * 1000).toInt(), progress,
                            readerState.book?.chapters?.getOrNull(chapter)?.title ?: "Chapter ${chapter + 1}"
                        )
                        showBookmarkInput = false
                        Toast.makeText(context, "Bookmark saved", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) { Text("Save Bookmark", color = Color.White) }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showBookmarks) {
        ModalBottomSheet(onDismissRequest = { showBookmarks = false }, sheetState = sheetState, containerColor = backgroundColor) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Your Bookmarks", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(16.dp))
                var bookmarks by remember { mutableStateOf<List<Bookmark>>(emptyList()) }
                LaunchedEffect(bookId) {
                    bookId?.let { bId -> readerViewModel.getBookmarks(bId).collectLatest { list -> bookmarks = list } }
                }
                if (bookmarks.isEmpty()) {
                    Text("No bookmarks yet", color = textColor.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 20.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                        itemsIndexed(bookmarks) { _, bookmark ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { targetChapter = bookmark.chapterIndex; targetScrollOffset = bookmark.scrollOffset; showBookmarks = false }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, null, tint = accentColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = bookmark.title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "Chapter ${bookmark.chapterIndex + 1} • ${(bookmark.progress * 100).toInt()}%", color = textColor.copy(alpha = 0.5f), fontSize = 13.sp)
                                }
                            }
                            HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ReaderProgressBar(viewModel: EpubReaderViewModel, accentColor: Color) {
    val progress by viewModel.currentProgress.collectAsStateWithLifecycle()
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(4.dp),
        color = accentColor,
        trackColor = accentColor.copy(alpha = 0.2f)
    )
}

@Composable
fun ReaderChapterProgressText(viewModel: EpubReaderViewModel, textColor: Color) {
    val chapter by viewModel.currentChapter.collectAsStateWithLifecycle()
    val progress by viewModel.currentProgress.collectAsStateWithLifecycle()
    val readerState by viewModel.state.collectAsStateWithLifecycle()
    
    val bookChapters = readerState.book?.chapters
    val currentChapterData = bookChapters?.getOrNull(chapter)
    val rawTitle = currentChapterData?.title ?: "Chapter ${chapter + 1}"
    val displayTitle = if (rawTitle.startsWith("Chapter", ignoreCase = true)) {
        rawTitle
    } else {
        "Chapter ${chapter + 1}: $rawTitle"
    }
    
    Text(
        text = "$displayTitle • ${(progress * 100).toInt()}%",
        color = textColor.copy(alpha = 0.5f),
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
fun ThemeOption(label: String, bg: Color, text: Color, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(bg, RoundedCornerShape(8.dp))
                .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFFE50914) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Aa", color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = if (isSelected) Color(0xFFE50914) else Color.Gray, fontSize = 12.sp)
    }
}
