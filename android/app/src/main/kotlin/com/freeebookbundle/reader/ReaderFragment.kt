package com.freeebookbundle.reader

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import androidx.activity.compose.BackHandler
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.freeebookbundle.R
import com.freeebookbundle.db.Bookmark
import com.freeebookbundle.db.Highlight
import com.freeebookbundle.voice.TtsVoiceManager
import com.freeebookbundle.voice.TtsSettingsPanel
import com.freeebookbundle.ui.AudioPlayerScreen
import com.freeebookbundle.ui.ChapterInfo
import com.freeebookbundle.ui.TtsPlayerState
import com.freeebookbundle.viewmodel.AppThemeMode
import com.freeebookbundle.viewmodel.ReaderViewModel
import com.freeebookbundle.repository.ReaderSettingsRepository
import com.freeebookbundle.utils.EpubPreviewUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.navigator.web.reflowable.ReflowableWebRendition
import org.readium.navigator.web.reflowable.ReflowableWebRenditionState
import org.readium.navigator.web.reflowable.ReflowableWebRenditionFactory
import org.readium.navigator.web.reflowable.ReflowableWebDecorationLocation
import org.readium.navigator.common.Decoration
import org.readium.navigator.common.DecorationListener
import org.readium.navigator.common.HyperlinkListener
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.TapEvent
import org.readium.navigator.common.TapContext
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.readium.navigator.web.reflowable.preferences.ReflowableWebSettings
import org.readium.navigator.web.reflowable.ReflowableWebGoLocation
import androidx.compose.ui.graphics.Color
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.util.Try

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import java.util.Locale
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.darkColorScheme
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freeebookbundle.viewmodel.ReaderTheme
import com.freeebookbundle.viewmodel.ReaderSettings

import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.*

@OptIn(ExperimentalReadiumApi::class)
class ReaderFragment : Fragment(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "ReaderFragment"
    }

    private val viewModel: ReaderViewModel by viewModels()
    private val appViewModel: com.freeebookbundle.viewmodel.AppViewModel by viewModels()
    private var bookId: String = "unknown"
    private var renditionState: ReflowableWebRenditionState? = null
    private var currentSelection: Locator? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var highlightDeleteComposeView: ComposeView? = null
    
    private lateinit var topBar: View
    private lateinit var bottomBar: View
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var chapterList: RecyclerView
    private lateinit var btnBookmarkStar: ImageButton
    private lateinit var btnHighlight: ImageButton
    private lateinit var btnBookList: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var btnTts: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var readerTitle: TextView
    private lateinit var readerChapter: TextView
    private lateinit var titleContainer: View
    private lateinit var settingsComposeView: ComposeView
    private lateinit var selectionComposeView: ComposeView
    private lateinit var revealOverlayView: ComposeView
    private lateinit var fragmentReaderContainer: View
    private lateinit var ttsControlComposeView: ComposeView

    // Search UI
    private lateinit var searchNavBar: View
    private lateinit var btnPrevSearch: ImageButton
    private lateinit var btnNextSearch: ImageButton
    private lateinit var btnCloseSearch: ImageButton
    private lateinit var searchCountText: TextView
    
    // Progress UI
    private lateinit var progressComposeView: ComposeView
    private var progressProgress by mutableStateOf(0f)
    private var showNextChapter by mutableStateOf(false)
    private var bottomBarVisible by mutableStateOf(false)
    
    private var currentSearchLocators = mutableListOf<Locator>()
    private var currentSearchIndex = -1
    private var highlightsJob: kotlinx.coroutines.Job? = null
    private var highlightsById: Map<String, com.freeebookbundle.db.Highlight> = emptyMap()
    
    // Reading progress tracking
    private var totalSpineItems = 0
    private var currentSpineIndex = 0
    private var isAutoAdvancing = false

    // TTS Compose State - using mutableStateOf for Compose recomposition
    private var isTtsActive by mutableStateOf(false)
    private var isTtsPlaying by mutableStateOf(false)
    private var showTtsSettings by mutableStateOf(false)
    private var ttsSentences = mutableListOf<String>()
    private var currentSentenceIndex by mutableStateOf(0)
    private var currentResourceLocator: Locator? = null
    private var currentFullText by mutableStateOf("")
    private var paragraphSentences = mutableListOf<String>()
    private var currentParagraphIndex = 0
    
    private lateinit var ttsVoiceManager: com.freeebookbundle.voice.TtsVoiceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reader, container, false)
        
        bookId = arguments?.getString("BOOK_ID") ?: "unknown"
        val publication = ReaderRepository.currentPublication ?: run {
            Log.e("ReaderFragment", "Publication is null!")
            return view
        }
        
        topBar = view.findViewById(R.id.reader_top_bar)
        bottomBar = view.findViewById(R.id.reader_bottom_bar)
        topBar.visibility = View.GONE // Start hidden like before
        drawerLayout = view.findViewById(R.id.drawer_layout)
        navView = view.findViewById(R.id.nav_view)
        chapterList = view.findViewById(R.id.chapter_list)
        readerTitle = view.findViewById(R.id.reader_title)
        readerChapter = view.findViewById(R.id.reader_chapter)
        btnMenu = view.findViewById(R.id.btn_menu)
        titleContainer = view.findViewById(R.id.title_container)
        btnBookmarkStar = view.findViewById(R.id.btn_star_bookmark)
        btnHighlight = view.findViewById<ImageButton>(R.id.btn_highlight)
        btnBookList = view.findViewById(R.id.btn_book_list)
        btnSearch = view.findViewById(R.id.btn_search)
        btnTts = view.findViewById(R.id.btn_tts)
        btnSettings = view.findViewById(R.id.btn_settings)
        settingsComposeView = view.findViewById(R.id.settings_compose_view)
        selectionComposeView = view.findViewById(R.id.selection_compose_view)
        revealOverlayView = view.findViewById(R.id.reveal_overlay_view)
        fragmentReaderContainer = view.findViewById(R.id.fragment_reader_container)
        ttsControlComposeView = view.findViewById(R.id.tts_control_compose_view)

        listOf(settingsComposeView, selectionComposeView, revealOverlayView, ttsControlComposeView).forEach { cv ->
            cv.setViewTreeLifecycleOwner(viewLifecycleOwner)
            cv.setViewTreeViewModelStoreOwner(this)
            cv.setViewTreeSavedStateRegistryOwner(this)
            cv.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        val settingsRepo = ReaderSettingsRepository(requireContext())
        val themeColor = settingsRepo.getThemeColor()

        revealOverlayView.setContent {
            val alpha = remember { Animatable(1f) }
            LaunchedEffect(Unit) {
                delay(800)
                alpha.animateTo(0f, tween(1000))
                revealOverlayView.visibility = View.GONE
            }
            
            SideEffect {
                fragmentReaderContainer.alpha = 1f - alpha.value
            }

            Box(modifier = Modifier.fillMaxSize().background(themeColor.copy(alpha = alpha.value)))
        }

        setupTtsControls()

        searchNavBar = view.findViewById(R.id.search_nav_bar)
        btnPrevSearch = view.findViewById(R.id.btn_prev_search)
        btnNextSearch = view.findViewById(R.id.btn_next_search)
        btnCloseSearch = view.findViewById(R.id.btn_close_search)
        searchCountText = view.findViewById(R.id.search_count_text)

        // Initialize progress ComposeView
        progressComposeView = view.findViewById(R.id.progress_compose_view)
        progressComposeView.setViewTreeLifecycleOwner(viewLifecycleOwner)
        progressComposeView.setViewTreeViewModelStoreOwner(this)
        progressComposeView.setViewTreeSavedStateRegistryOwner(this)
        progressComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        
        setupProgressBar()

        // Set up chapter drawer
        setupChapterDrawer(publication)

        // Set up reading progress tracking
        totalSpineItems = publication.readingOrder.size
        currentSpineIndex = 0
        updateProgressUI()

        readerTitle.text = publication.metadata.title

        setupToolbar()
        setupSearchNavBar()
        applyInsets(view)

        val settings = viewModel.settings.value

        lifecycleScope.launch {
            // Load saved position from DB
            val savedPosition = withContext(Dispatchers.IO) {
                viewModel.getLastPosition(bookId)
            }

            val startIndex = if (savedPosition != null) {
                savedPosition.spineIndex.coerceIn(0, publication.readingOrder.size - 1)
            } else {
                if (publication.readingOrder.size > 1) 1 else 0
            }

            val initialLink = publication.readingOrder[startIndex]
            val initialLocator = if (savedPosition != null && savedPosition.progression > 0f) {
                publication.locatorFromLink(initialLink)?.copy(
                    locations = Locator.Locations(progression = savedPosition.progression.toDouble())
                )
            } else {
                publication.locatorFromLink(initialLink)
            } ?: return@launch

            setupNavigator(initialLocator, settings)
        }
        
        // Auto-open audio player if requested
        val openAudioPlayer = arguments?.getBoolean("OPEN_AUDIO_PLAYER", false) ?: false
        if (openAudioPlayer) {
            view.post {
                isTtsActive = true
                ttsControlComposeView.visibility = View.VISIBLE
                topBar.visibility = View.GONE
                bottomBar.visibility = View.GONE
                speakCurrentPage()
            }
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize TtsVoiceManager for natural voices
        ttsVoiceManager = TtsVoiceManager(requireContext())
        ttsVoiceManager.initialize { success ->
            if (success) {
                Log.d("TTS", "TtsVoiceManager initialized with neural voice support")
            }
        }
        
        observeHighlights()
    }

    private fun setupTtsControls() {
        val currentBook = ReaderRepository.currentBook
        val publication = ReaderRepository.currentPublication
        val bookTitle = currentBook?.title ?: publication?.metadata?.title ?: "Unknown Book"
        val authorName = currentBook?.author ?: extractAuthorName(publication?.metadata?.authors)
        val coverUrl = currentBook?.getBestCoverUrl()
        val chapters = publication?.tableOfContents?.mapIndexed { index, link ->
            com.freeebookbundle.ui.ChapterInfo(link.title ?: "Chapter ${index + 1}", index)
        } ?: emptyList()

        ttsControlComposeView.setContent {
            val playerState = remember { mutableStateOf(TtsPlayerState()) }
            
            LaunchedEffect(isTtsActive, isTtsPlaying, currentSentenceIndex, currentFullText, ttsSentences.size, currentSpineIndex, ttsVoiceManager.speechRate.value, ttsVoiceManager.currentPiperVoice.value, ttsVoiceManager.availablePiperVoices.value, appViewModel.themeMode.value) {
                playerState.value = TtsPlayerState(
                    bookTitle = bookTitle,
                    authorName = authorName,
                    coverUrl = coverUrl,
                    isPlaying = isTtsPlaying,
                    currentProgress = if (ttsSentences.isNotEmpty()) currentSentenceIndex.toFloat() / ttsSentences.size.coerceAtLeast(1) else 0f,
                    speechRate = ttsVoiceManager.speechRate.value,
                    selectedVoice = ttsVoiceManager.currentPiperVoice.value,
                    availableVoices = ttsVoiceManager.availablePiperVoices.value,
                    chapters = chapters,
                    currentChapter = currentSpineIndex,
                    currentParagraph = currentFullText,
                    currentSentenceIndex = currentSentenceIndex.coerceIn(0, (ttsSentences.size - 1).coerceAtLeast(0)),
                    totalSentences = ttsSentences.size.coerceAtLeast(1),
                    themeMode = appViewModel.themeMode.value
                )
            }
            
            if (isTtsActive) {
                BackHandler {
                    stopTts()
                }
                
                AudioPlayerScreen(
                    state = playerState.value,
                    onPlayPause = { togglePlayPauseTts() },
                    onSkipForward = { nextTts() },
                    onSkipBackward = { prevTts() },
                    onSeek = { progress ->
                        val newIndex = (progress * ttsSentences.size.coerceAtLeast(1)).toInt().coerceIn(0, (ttsSentences.size - 1).coerceAtLeast(0))
                        if (newIndex != currentSentenceIndex) {
                            currentSentenceIndex = newIndex
                            speakCurrentSentence()
                        }
                    },
                    onSpeechRateChange = { rate -> 
                        val wasPlaying = isTtsPlaying
                        ttsVoiceManager.stop()
                        ttsVoiceManager.setSpeechRate(rate)
                        if (wasPlaying && currentSentenceIndex in ttsSentences.indices) {
                            speakCurrentSentence()
                        }
                    },
                    onVoiceChange = { voice -> ttsVoiceManager.setPiperVoice(voice) },
                    onClose = { stopTts() },
                    onChapterSelect = { index -> navigateToChapter(index) },
                    onSettingsClick = { showSettingsDialog() },
                    onBookmarksClick = { showBookListDialog() }
                )
            }
        }
    }

    private fun navigateToChapter(index: Int) {
        val publication = ReaderRepository.currentPublication ?: return
        val toc = publication.tableOfContents
        if (index in toc.indices) {
            lifecycleScope.launch {
                publication.locatorFromLink(toc[index])?.let { locator ->
                    renditionState?.controller?.goTo(ReflowableWebGoLocation(locator))
                    speakCurrentPage()
                }
            }
        }
    }

    private fun extractAuthorName(authors: Any?): String {
        if (authors == null) return "Unknown Author"
        return authors.toString().ifEmpty { "Unknown Author" }
    }

    private fun togglePlayPauseTts() {
        if (isTtsPlaying) {
            ttsVoiceManager.stop()
            isTtsPlaying = false
        } else {
            if (ttsSentences.isEmpty()) {
                speakCurrentPage()
            } else {
                speakCurrentSentence()
            }
            isTtsPlaying = true
        }
    }

    private fun nextTts() {
        ttsVoiceManager.stop()
        if (currentSentenceIndex < ttsSentences.size - 1) {
            currentSentenceIndex++
            speakCurrentSentence()
        } else {
            lifecycleScope.launch {
                renditionState?.controller?.moveForward()
                delay(500)
                speakCurrentPage()
            }
        }
    }

    private fun prevTts() {
        ttsVoiceManager.stop()
        if (currentSentenceIndex > 0) {
            currentSentenceIndex--
            speakCurrentSentence()
        } else {
            lifecycleScope.launch {
                renditionState?.controller?.moveBackward()
                delay(500)
                speakCurrentPage()
            }
        }
    }

    private fun stopTts() {
        ttsVoiceManager.stop()
        isTtsActive = false
        isTtsPlaying = false
        showTtsSettings = false
        ttsSentences.clear()
        currentSentenceIndex = 0
        currentFullText = ""
        paragraphSentences.clear()
        currentParagraphIndex = 0
        updateTtsHighlight(null)
        ttsControlComposeView.visibility = View.GONE
        topBar.visibility = View.VISIBLE
        bottomBar.visibility = View.VISIBLE
        bottomBarVisible = true
    }

    private fun speakCurrentPage() {
        if (tts == null) {
            tts = TextToSpeech(requireContext().applicationContext, this)
            return
        }
        if (!isTtsInitialized) return

        lifecycleScope.launch {
            val publication = ReaderRepository.currentPublication ?: return@launch
            val currentLocator = renditionState?.controller?.location?.toLocator() ?: return@launch
            
            val fullText = try {
                val resource = publication.get(currentLocator.href)
                val content = resource?.read()?.getOrNull()
                if (content != null) {
                    val html = String(content, Charsets.UTF_8)
                    parseHtmlToTextRobust(html)
                } else ""
            } catch (e: Exception) {
                Log.e("TTS", "Extraction failed", e)
                ""
            }
            
            if (fullText.isNotEmpty()) {
                ttsSentences = fullText.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }.toMutableList()
                currentFullText = fullText
                paragraphSentences = ttsSentences.toMutableList()
                currentResourceLocator = currentLocator
                
                val visibleText = currentLocator.text.highlight ?: ""
                currentSentenceIndex = if (visibleText.isNotEmpty()) {
                    val index = ttsSentences.indexOfFirst { it.contains(visibleText, ignoreCase = true) }
                    if (index != -1) index else 0
                } else {
                    0
                }
                
                speakCurrentSentence()
            } else {
                Toast.makeText(context, "No text found to read on this page.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun speakCurrentSentence() {
        if (currentSentenceIndex !in ttsSentences.indices) return
        
        val text = ttsSentences[currentSentenceIndex]
        
        // Update highlight for the current sentence
        updateTtsHighlight(text)

        // Use TtsVoiceManager for natural voices
        ttsVoiceManager.setOnSpeakComplete { utteranceId ->
            lifecycleScope.launch(Dispatchers.Main) {
                isTtsPlaying = false
                if (isTtsActive) nextTts() // Auto-advance
            }
        }
        ttsVoiceManager.speak(text, "ReaderTTS_$currentSentenceIndex")
        isTtsPlaying = true
    }

    private fun updateTtsHighlight(text: String?, isWord: Boolean = false) {
        val state = renditionState ?: return
        val controller = state.controller ?: return
        val baseLocator = currentResourceLocator ?: return
        
        if (text == null) {
            // Clear TTS highlights and re-apply regular highlights
            viewLifecycleOwner.lifecycleScope.launch {
                val highlights = viewModel.getHighlightsSync(bookId)
                highlightsById = highlights.associateBy { it.id.toString() }
                val decorations = highlights.mapNotNull { highlight ->
                    try {
                        val locator = org.json.JSONObject(highlight.locator)
                        val parsedLocator = org.readium.r2.shared.publication.Locator.fromJSON(locator) ?: return@mapNotNull null
                        val decoLocation = ReflowableWebDecorationLocation(parsedLocator) ?: return@mapNotNull null
                        Decoration(
                            id = Decoration.Id(highlight.id.toString()),
                            location = decoLocation,
                            style = Decoration.Style.Highlight(tint = highlight.color),
                        )
                    } catch (e: Exception) { null }
                }
                controller.decorations = persistentMapOf("highlights" to decorations.toPersistentList())
            }
            return
        }

        // Create a locator for the text within the current resource
        val locator = baseLocator.copy(
            text = org.readium.r2.shared.publication.Locator.Text(highlight = text)
        )

        // Build decoration list: TTS highlight + existing highlights
        viewLifecycleOwner.lifecycleScope.launch {
            val existingHighlights = viewModel.getHighlightsSync(bookId)
            val allDecorations = mutableListOf<Decoration<ReflowableWebDecorationLocation>>()

            // Add TTS highlight
            val ttsDecoLocation = ReflowableWebDecorationLocation(locator)
            if (ttsDecoLocation != null) {
                allDecorations.add(Decoration(
                    id = Decoration.Id(if (isWord) "tts-word" else "tts-sentence"),
                    location = ttsDecoLocation,
                    style = Decoration.Style.Highlight(tint = if (isWord) 0x44FF0000.toInt() else 0x44FFFF00.toInt()),
                ))
            }

            // Add existing highlights
            existingHighlights.forEach { highlight ->
                try {
                    val highlightLocator = org.json.JSONObject(highlight.locator)
                    val parsedLocator = org.readium.r2.shared.publication.Locator.fromJSON(highlightLocator) ?: return@forEach
                    val decoLocation = ReflowableWebDecorationLocation(parsedLocator) ?: return@forEach
                    allDecorations.add(Decoration(
                        id = Decoration.Id(highlight.id.toString()),
                        location = decoLocation,
                        style = Decoration.Style.Highlight(tint = highlight.color),
                    ))
                } catch (e: Exception) {}
            }
            controller.decorations = persistentMapOf("highlights" to allDecorations.toPersistentList())
        }
    }

    private fun parseHtmlToTextRobust(html: String): String {
        return try {
            val doc = Jsoup.parse(html)
            // Remove non-content elements
            doc.select("style, script, nav, footer, header").remove()
            // Extract text preserving some structure (blocks as newlines)
            val cleaned = Jsoup.clean(doc.body().html(), Safelist.none().addTags("p", "br", "div"))
            Jsoup.parse(cleaned).text()
        } catch (e: Exception) {
            // Fallback to simple extraction if Jsoup fails
            html.replace("<[^>]+>".toRegex(), " ").trim()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported or missing data")
            } else {
                if (isTtsActive) speakCurrentPage()
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    private fun setupNavigator(initialLocator: Locator, settings: ReaderSettings, forceScrollMode: Boolean = true) {
        val publication = ReaderRepository.currentPublication
        if (publication == null) {
            android.util.Log.e("ReaderFrag", "setupNavigator: ReaderRepository.currentPublication is NULL")
            return
        }
        android.util.Log.d("ReaderFrag", "setupNavigator: START - href=${initialLocator.href}, progression=${initialLocator.locations.progression}")
        android.util.Log.d("ReaderFrag", "setupNavigator: readingOrder.size=${publication.readingOrder.size}")
        android.util.Log.d("ReaderFrag", "setupNavigator: publication.conformsToEPUB=${publication.conformsTo(org.readium.r2.shared.publication.Publication.Profile.EPUB)}")
        val activity = requireActivity()

        lifecycleScope.launch {
            android.util.Log.d("ReaderFrag", "setupNavigator: Creating ReflowableWebRenditionFactory...")
            val factory = ReflowableWebRenditionFactory(
                application = activity.application,
                publication = publication,
            )
            if (factory == null) {
                android.util.Log.e("ReaderFrag", "setupNavigator: ReflowableWebRenditionFactory returned NULL!")
                return@launch
            }
            android.util.Log.d("ReaderFrag", "setupNavigator: Factory created successfully")

            val (textColorHex, bgColorHex) = when (settings.theme) {
                ReaderTheme.WHITE -> 0xFF000000.toInt() to 0xFFFFFFFF.toInt()
                ReaderTheme.DARK -> 0xFFE0E0E0.toInt() to 0xFF1A1A1A.toInt()
                ReaderTheme.SEPIA -> 0xFF5B4636.toInt() to 0xFFF5E6C8.toInt()
            }

            val initialLocation = ReflowableWebGoLocation(
                href = initialLocator.href,
                progression = org.readium.navigator.common.Progression(initialLocator.locations.progression ?: 0.0)
            )

            val webSettings = ReflowableWebSettings(
                scroll = true,
                fontSize = settings.fontSize / 100.0,
                textColor = org.readium.r2.navigator.preferences.Color(textColorHex),
                backgroundColor = org.readium.r2.navigator.preferences.Color(bgColorHex),
                linkColor = org.readium.r2.navigator.preferences.Color(textColorHex),
                visitedColor = org.readium.r2.navigator.preferences.Color(textColorHex),
                overridePublisherColors = true,
                fontFamily = when (settings.fontFamily) {
                    "serif" -> org.readium.r2.navigator.preferences.FontFamily.SERIF
                    "sans-serif" -> org.readium.r2.navigator.preferences.FontFamily.SANS_SERIF
                    "monospace" -> org.readium.r2.navigator.preferences.FontFamily.MONOSPACE
                    else -> org.readium.r2.navigator.preferences.FontFamily.SANS_SERIF
                },
                readingProgression = org.readium.r2.navigator.preferences.ReadingProgression.LTR,
                columnCount = null,
                fontWeight = null,
                hyphens = null,
                imageFilter = null,
                language = null,
                letterSpacing = null,
                ligatures = null,
                lineHeight = null,
                maximalLineLength = null,
                minimalLineLength = null,
                minMargins = 0.0,
                optimalLineLength = 0.0,
                paragraphIndent = null,
                paragraphSpacing = null,
                textAlign = null,
                textNormalization = false,
                verticalText = false,
                wordSpacing = null,
            )

            android.util.Log.d("ReaderFrag", "setupNavigator: Creating ReflowableWebRenditionState...")
            val state = factory.createRenditionState(
                initialSettings = webSettings,
                initialLocation = initialLocation,
            ).getOrNull()
            if (state == null) {
                android.util.Log.e("ReaderFrag", "setupNavigator: createRenditionState returned failure!")
                return@launch
            }
            renditionState = state
            android.util.Log.d("ReaderFrag", "setupNavigator: State created, adding ComposeView...")

            withContext(Dispatchers.Main) {
                val container = fragmentReaderContainer as? ViewGroup
                if (container == null) {
                    android.util.Log.e("ReaderFrag", "setupNavigator: fragmentReaderContainer is NOT a ViewGroup! type=${fragmentReaderContainer::class.java.simpleName}")
                    return@withContext
                }
                container.removeAllViews()
                android.util.Log.d("ReaderFrag", "setupNavigator: Container cleared, creating ComposeView")

                val composeView = ComposeView(requireContext()).apply {
                    setViewTreeLifecycleOwner(viewLifecycleOwner)
                    setViewTreeViewModelStoreOwner(this@ReaderFragment)
                    setViewTreeSavedStateRegistryOwner(this@ReaderFragment)
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        val highlightTapListener = remember {
                            object : DecorationListener<ReflowableWebDecorationLocation> {
                                override fun onDecorationActivated(event: DecorationListener.OnActivatedEvent<ReflowableWebDecorationLocation>) {
                                    val highlightId = event.decoration.id.value
                                    val highlight = highlightsById[highlightId]
                                    if (highlight != null) {
                                        showHighlightPopup(highlight, null)
                                    }
                                }
                            }
                        }
                        val externalLinkListener = remember {
                            object : HyperlinkListener {
                                override fun onReadingOrderLinkActivated(url: org.readium.r2.shared.util.Url, context: org.readium.navigator.common.LinkContext?) {
                                    lifecycleScope.launch {
                                        renditionState?.controller?.goTo(ReflowableWebGoLocation(url))
                                    }
                                }
                                override fun onNonLinearLinkActivated(url: org.readium.r2.shared.util.Url, context: org.readium.navigator.common.LinkContext?) {}
                                override fun onExternalLinkActivated(url: AbsoluteUrl, context: org.readium.navigator.common.LinkContext?) {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url.toString()))
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        android.util.Log.w("ReaderFrag", "Failed to open external link: $url", e)
                                    }
                                }
                            }
                        }
                        ReflowableWebRendition(
                            state = state,
                            windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
                            inputListener = object : InputListener {
                                override fun onTap(event: TapEvent, context: TapContext) {}
                            },
                            hyperlinkListener = externalLinkListener,
                            decorationListener = highlightTapListener,
                            textSelectionActionModeCallback = createSelectionActionModeCallback(),
                        )
                    }
                }
                container.addView(composeView)
                android.util.Log.d("ReaderFrag", "setupNavigator: ComposeView added to container, childCount=${container.childCount}")
            }

            android.util.Log.d("ReaderFrag", "setupNavigator: Collecting locator changes...")
            // Collect locator changes for progress tracking
            viewLifecycleOwner.lifecycleScope.launch {
                androidx.compose.runtime.snapshotFlow { renditionState?.controller?.location }
                    .collect { location ->
                        if (location != null) {
                            updateReadingProgress(location.toLocator())
                        }
                    }
            }
        }
    }

    private var chapterProgression = 0f
    
    private fun updateReadingProgress(locator: Locator) {
        val publication = ReaderRepository.currentPublication ?: return
        val href = locator.href.toString()
        
        // Find current spine index
        val spineIndex = publication.readingOrder.indexOfFirst { it.href.toString() == href }
        if (spineIndex >= 0) {
            currentSpineIndex = spineIndex
            isAutoAdvancing = false
        }
        
        // Get intra-chapter progress from locator (0.0 to 1.0)
        chapterProgression = locator.locations?.progression?.toFloat() ?: 0f
        
        // Persist to database (throttled via ViewModel)
        viewModel.updateReadingProgress(bookId, currentSpineIndex, chapterProgression)
        
        Log.d(TAG, "[Progress] locator=$locator, progression=$chapterProgression, spineIndex=$currentSpineIndex, total=$totalSpineItems")
        
        activity?.runOnUiThread {
            updateProgressUI()
        }
    }

    private fun updateProgressUI() {
        progressProgress = chapterProgression.coerceIn(0f, 1f)
        
        val isLastChapter = currentSpineIndex >= totalSpineItems - 1
        showNextChapter = false
        
        val chapterTitle = if (tableOfContents.isNotEmpty() && currentSpineIndex < tableOfContents.size) {
            tableOfContents[currentSpineIndex].title ?: "Chapter ${currentSpineIndex + 1}"
        } else {
            "Chapter ${currentSpineIndex + 1}"
        }
        readerChapter.text = chapterTitle
    }

    private fun autoAdvanceToNextChapter() {
        val publication = ReaderRepository.currentPublication ?: return
        val nextIndex = currentSpineIndex + 1
        if (nextIndex >= totalSpineItems) return

        val nextLink = publication.readingOrder.getOrNull(nextIndex) ?: return
        lifecycleScope.launch {
            try {
                publication.locatorFromLink(nextLink)?.let { locator ->
                    Log.d(TAG, "[AutoAdvance] Moving to chapter $nextIndex")
                    renditionState?.controller?.goTo(ReflowableWebGoLocation(locator))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto-advance failed: ${e.message}")
                isAutoAdvancing = false
            }
        }
    }

    private fun setupProgressBar() {
        progressComposeView.setContent {
            val animatedProgress by animateFloatAsState(
                targetValue = progressProgress,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                label = "progress"
            )
            
            val systemNavPadding = androidx.compose.foundation.layout.WindowInsets.systemBars.asPaddingValues()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = systemNavPadding.calculateBottomPadding()
                    )
                    .background(Color.Transparent)
            ) {
                if (showNextChapter) {
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                try {
                                    val publication = ReaderRepository.currentPublication
                                    if (publication != null && currentSpineIndex < totalSpineItems - 1) {
                                        val nextLink = publication.readingOrder.getOrNull(currentSpineIndex + 1)
                                        if (nextLink != null) {
                                            publication.locatorFromLink(nextLink)?.let { locator ->
                                                renditionState?.controller?.goTo(ReflowableWebGoLocation(locator))
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error going to next chapter: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE50914)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Text(
                            text = if (currentSpineIndex < totalSpineItems - 1) "Next Chapter →" else "Finish",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(Color(0xFFE50914))
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        // Force-save final reading position before teardown
        if (bookId != "unknown") {
            viewModel.updateReadingProgress(bookId, currentSpineIndex, chapterProgression, force = true)
        }
        try { tts?.stop(); tts?.shutdown() } catch (e: Exception) {}
        if (::ttsVoiceManager.isInitialized) ttsVoiceManager.shutdown()
        super.onDestroy()
    }

    private fun applyInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.updatePadding(top = bars.top)
            bottomBar.updatePadding(bottom = bars.bottom)
            searchNavBar.updateLayoutParams<FrameLayout.LayoutParams> { bottomMargin = bars.bottom + (80 * resources.displayMetrics.density).toInt() }
            insets
        }
    }

    private fun setupToolbar() {
        btnMenu.setOnClickListener { openChapterDrawer() }
        titleContainer.setOnClickListener { openChapterDrawer() }
        btnBookmarkStar.setOnClickListener { bookmarkCurrentLocation() }
        btnHighlight.setOnClickListener {
            lifecycleScope.launch {
                val selection = renditionState?.controller?.currentSelection()
                if (selection != null) showSelectionMenu(selection.location.toLocator())
                else Toast.makeText(context, "Select text first", Toast.LENGTH_SHORT).show()
            }
        }
        btnBookList.setOnClickListener { showBookListDialog() }
        btnSearch.setOnClickListener { showSearchDialog() }
        btnTts.setOnClickListener { 
            isTtsActive = true
            ttsControlComposeView.visibility = View.VISIBLE
            topBar.visibility = View.GONE
            bottomBar.visibility = View.GONE
            speakCurrentPage() 
        }
        btnSettings.setOnClickListener { showSettingsDialog() }
    }

    private fun openChapterDrawer() {
        drawerLayout.openDrawer(Gravity.START)
    }

    private var tableOfContents: List<org.readium.r2.shared.publication.Link> = emptyList()
    
    private fun setupChapterDrawer(publication: org.readium.r2.shared.publication.Publication) {
        tableOfContents = publication.tableOfContents
        val toc = tableOfContents
        val chapterCountText = view?.findViewById<TextView>(R.id.drawer_chapter_count)
        chapterCountText?.text = "${toc.size} chapters"
        
        // Enable swipe gesture to open drawer
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        
        chapterList.layoutManager = LinearLayoutManager(context)
        val adapter = ChapterAdapter(toc, currentSpineIndex) { index ->
            lifecycleScope.launch {
                toc.getOrNull(index)?.let { link ->
                    drawerLayout.closeDrawers()
                    val locator = publication.locatorFromLink(link) ?: run {
                        // Fallback: create Locator manually from link href
                        android.util.Log.w("ReaderFrag", "locatorFromLink returned null for ${link.href}, mediaType=${link.mediaType}, title=${link.title}")
                        try {
                            val resolvedUrl = link.href.resolve()
                            Locator(
                                href = resolvedUrl,
                                mediaType = link.mediaType ?: org.readium.r2.shared.util.mediatype.MediaType.XHTML,
                                title = link.title,
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("ReaderFrag", "Failed to resolve href: ${link.href}", e)
                            null
                        }
                    }
                    if (locator != null) {
                        android.util.Log.d("ReaderFrag", "TOC click: index=$index, locator.href=${locator.href}, title=${locator.title}")
                        renditionState?.controller?.goTo(ReflowableWebGoLocation(locator))
                    } else {
                        android.util.Log.e("ReaderFrag", "TOC click: could not create locator for index=$index, link.href=${link.href}")
                    }
                }
            }
        }
        chapterList.adapter = adapter
    }

    private class ChapterAdapter(
        private val chapters: List<org.readium.r2.shared.publication.Link>,
        private val currentChapterIndex: Int,
        private val onChapterClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

        class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleText: TextView = itemView.findViewById(android.R.id.text1)
            val container: View = itemView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            view.setPadding(64, 32, 16, 32)
            view.setBackgroundResource(android.R.drawable.list_selector_background)
            return ChapterViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
            val isCurrentChapter = position == currentChapterIndex
            holder.titleText.text = chapters[position].title ?: "Chapter ${position + 1}"
            holder.titleText.setTextColor(if (isCurrentChapter) 0xFFE50914.toInt() else 0xFFFFFFFF.toInt())
            holder.titleText.textSize = 15f
            holder.titleText.alpha = if (isCurrentChapter) 1f else 0.7f
            holder.container.setOnClickListener { onChapterClick(position) }
        }

        override fun getItemCount() = chapters.size
    }

    fun toggleBars() {
        if (!::topBar.isInitialized || !::bottomBar.isInitialized || isTtsActive) return
        if (topBar.visibility == View.VISIBLE) {
            topBar.visibility = View.GONE
            bottomBar.visibility = View.GONE
            bottomBarVisible = false
        } else {
            topBar.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
            bottomBarVisible = true
            topBar.bringToFront()
        }
    }
    
    fun toggleBottomBar() {
        if (isTtsActive) return
        if (bottomBar.visibility == View.VISIBLE) {
            bottomBar.visibility = View.GONE
            bottomBarVisible = false
        } else {
            bottomBar.visibility = View.VISIBLE
            bottomBarVisible = true
        }
    }
    
    fun onTap(x: Float, y: Float) {
        if (!::topBar.isInitialized || !::bottomBar.isInitialized) return
        
        if (topBar.visibility == View.VISIBLE) {
            topBar.visibility = View.GONE
            bottomBar.visibility = View.GONE
            bottomBarVisible = false
        } else {
            topBar.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
            bottomBarVisible = true
            topBar.bringToFront()
        }
    }

    private fun showSelectionMenu(locator: Locator) {
        selectionComposeView.visibility = View.VISIBLE
        selectionComposeView.setContent {
            MaterialTheme {
                val colors = listOf(0x88FFFF00.toInt(), 0x8800FF00.toInt(), 0x8800AAFF.toInt(), 0x88FF00FF.toInt(), 0x88FF0000.toInt())
                var showTagInput by remember { mutableStateOf(false) }
                var tagText by remember { mutableStateOf("") }
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize().clickable { selectionComposeView.visibility = View.GONE; renditionState?.controller?.clearSelection() }, color = Color.Transparent) {}
                    Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(), shape = RoundedCornerShape(12.dp), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (!showTagInput) {
                                Text("Highlight", style = MaterialTheme.typography.titleMedium)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    colors.forEach { colorInt -> Box(modifier = Modifier.size(40.dp).background(Color(colorInt), CircleShape).clickable { highlightSelection(locator, colorInt); selectionComposeView.visibility = View.GONE; renditionState?.controller?.clearSelection() }) }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = { showTagInput = true }) { Text("Add Note") }
                                    TextButton(onClick = { Toast.makeText(context, "Lookup: ${locator.text.highlight}", Toast.LENGTH_SHORT).show() }) { Text("Lookup") }
                                }
                            } else {
                                OutlinedTextField(value = tagText, onValueChange = { tagText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Note...") })
                                Button(onClick = { highlightSelection(locator, 0x8800AAFF.toInt(), tagText); selectionComposeView.visibility = View.GONE; renditionState?.controller?.clearSelection() }) { Text("Save") }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showHighlightPopup(highlight: Highlight, tapPoint: android.graphics.PointF?) {
        selectionComposeView.visibility = View.VISIBLE
        selectionComposeView.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize().selectable(selected = true, onClick = { selectionComposeView.visibility = View.GONE }),
                        color = Color.Transparent
                    ) {}
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp,
                        color = Color(0xFF1A1A1A)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Highlight", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (!highlight.selectedText.isNullOrBlank()) {
                                Text(
                                    text = "\"${highlight.selectedText.take(100)}\"",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        viewLifecycleOwner.lifecycleScope.launch {
                                            viewModel.deleteHighlight(highlight)
                                            Toast.makeText(context, "Highlight deleted", Toast.LENGTH_SHORT).show()
                                        }
                                        selectionComposeView.visibility = View.GONE
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE53935),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Delete",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Surface(
                                    onClick = { selectionComposeView.visibility = View.GONE },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Cancel",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeHighlights() {
        highlightsJob?.cancel()
        highlightsJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getHighlights(bookId).collectLatest { highlights ->
                val state = renditionState ?: return@collectLatest
                val controller = state.controller ?: return@collectLatest

                // Update lookup map for decoration taps
                highlightsById = highlights.associateBy { it.id.toString() }

                val decorations = highlights.mapNotNull { highlight ->
                    try {
                        val locator = try {
                            org.json.JSONObject(highlight.locator)
                        } catch (e: Exception) { null } ?: return@mapNotNull null

                        val parsedLocator = org.readium.r2.shared.publication.Locator.fromJSON(locator) ?: return@mapNotNull null
                        val decoLocation = ReflowableWebDecorationLocation(parsedLocator) ?: return@mapNotNull null
                        Decoration(
                            id = Decoration.Id(highlight.id.toString()),
                            location = decoLocation,
                            style = Decoration.Style.Highlight(tint = highlight.color),
                        )
                    } catch (e: Exception) { null }
                }
                controller.decorations = persistentMapOf("highlights" to decorations.toPersistentList())
            }
        }
    }

    private fun showSearchDialog() {
        val input = EditText(context)
        input.setHint("Search...")
        AlertDialog.Builder(context).setTitle("Search Book").setView(input).setPositiveButton("Search") { _, _ -> performSearch(input.text.toString()) }.setNegativeButton("Cancel", null).show()
    }

    private fun performSearch(query: String) {
        val publication = ReaderRepository.currentPublication ?: return
        lifecycleScope.launch {
            val iterator = publication.search(query)
            if (iterator != null) {
                currentSearchLocators.clear()
                var resultTry = iterator.next()
                while (resultTry is Try.Success<*, *>) {
                    val collection = resultTry.value as? LocatorCollection ?: break
                    currentSearchLocators.addAll(collection.locators)
                    resultTry = iterator.next()
                }
                if (currentSearchLocators.isNotEmpty()) {
                    currentSearchIndex = 0
                    searchNavBar.visibility = View.VISIBLE
                    updateSearchNavigation()
                }
            }
        }
    }

    private fun updateSearchNavigation() {
        if (currentSearchIndex in currentSearchLocators.indices) {
            lifecycleScope.launch {
                renditionState?.controller?.goTo(ReflowableWebGoLocation(currentSearchLocators[currentSearchIndex]))
            }
            searchCountText.text = "${currentSearchIndex + 1}/${currentSearchLocators.size}"
        }
    }

    private fun setupSearchNavBar() {
        btnPrevSearch.setOnClickListener { if (currentSearchLocators.isNotEmpty()) { currentSearchIndex = if (currentSearchIndex <= 0) currentSearchLocators.size - 1 else currentSearchIndex - 1; updateSearchNavigation() } }
        btnNextSearch.setOnClickListener { if (currentSearchLocators.isNotEmpty()) { currentSearchIndex = if (currentSearchIndex >= currentSearchLocators.size - 1) 0 else currentSearchIndex + 1; updateSearchNavigation() } }
        btnCloseSearch.setOnClickListener { searchNavBar.visibility = View.GONE; currentSearchLocators.clear() }
    }

    private fun bookmarkCurrentLocation() {
        val locator = renditionState?.controller?.location?.toLocator() ?: return
        viewModel.insertBookmark(bookId, locator.toJSON().toString())
        btnBookmarkStar.setImageResource(R.drawable.ic_bookmark)
        btnBookmarkStar.postDelayed({ if (isAdded) btnBookmarkStar.setImageResource(R.drawable.ic_bookmark_border) }, 1000)
    }

    private fun showBookListDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_book_navigation, null)
        val listView = dialogView.findViewById<ListView>(R.id.list_navigation)
        val btnContents = dialogView.findViewById<Button>(R.id.btn_tab_contents)
        val btnBookmarks = dialogView.findViewById<Button>(R.id.btn_tab_bookmarks)
        val btnHighlights = dialogView.findViewById<Button>(R.id.btn_tab_highlights)
        
        // Hide Contents tab since we now have it in the slide-out drawer
        btnContents.visibility = View.GONE
        
        // Make Bookmarks and Highlights share the space equally
        btnBookmarks.layoutParams = (btnBookmarks.layoutParams as LinearLayout.LayoutParams).apply {
            weight = 1f
        }
        btnHighlights.layoutParams = (btnHighlights.layoutParams as LinearLayout.LayoutParams).apply {
            weight = 1f
        }
        
        val toc = ReaderRepository.currentPublication?.tableOfContents ?: emptyList()
        
        fun highlightTab(selected: Button, others: List<Button>) {
            selected.setBackgroundColor(0xFF333333.toInt())
            others.forEach { it.setBackgroundColor(0xFF222222.toInt()) }
        }
        
        fun showBookmarksList() {
            highlightTab(btnBookmarks, listOf(btnHighlights))
            lifecycleScope.launch {
                viewModel.getBookmarks(bookId).collectLatest { bookmarks ->
                    if (bookmarks.isEmpty()) {
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listOf("No bookmarks yet"))
                        listView.setOnItemClickListener(null)
                    } else {
                        val publication = ReaderRepository.currentPublication
                        val tocLinks = publication?.tableOfContents ?: emptyList()
                        val bookmarkTitles = bookmarks.mapIndexed { index, bookmark ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(bookmark.locator))
                                if (locator != null) {
                                    val locatorHref = locator.href.toString()
                                    var chapterName = "Chapter ${currentSpineIndex + 1}"
                                    for (tocLink in tocLinks) {
                                        val tocHref = tocLink.href.toString()
                                        if (locatorHref.contains(tocHref.substringAfterLast("/"))) {
                                            chapterName = tocLink.title ?: chapterName
                                            break
                                        }
                                    }
                                    
                                    val locationInfo = locator.locations?.fragments?.firstOrNull()?.let { fragment ->
                                        fragment.replace("cfi=", "").take(20)
                                    } ?: locator.locations?.progression?.let { 
                                        "${(it * 100).toInt()}%"
                                    } ?: ""
                                    
                                    if (locationInfo.isNotEmpty()) {
                                        "$chapterName ($locationInfo)"
                                    } else {
                                        chapterName
                                    }
                                } else {
                                    "Bookmark ${index + 1}"
                                }
                            } catch (e: Exception) {
                                "Bookmark ${index + 1}"
                            }
                        }
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, bookmarkTitles)
                        listView.setOnItemClickListener { _: android.widget.AdapterView<*>, _: View, pos: Int, _: Long ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(bookmarks[pos].locator))
                                locator?.let {
                                    lifecycleScope.launch {
                                        renditionState?.controller?.goTo(ReflowableWebGoLocation(it))
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error loading bookmark", Toast.LENGTH_SHORT).show()
                            }
                        }
                        listView.setOnItemLongClickListener { _: android.widget.AdapterView<*>, _: View, pos: Int, _: Long ->
                            lifecycleScope.launch {
                                viewModel.deleteBookmark(bookmarks[pos])
                                Toast.makeText(context, "Bookmark deleted", Toast.LENGTH_SHORT).show()
                            }
                            true
                        }
                    }
                }
            }
        }
        
        fun showHighlightsList() {
            highlightTab(btnHighlights, listOf(btnBookmarks))
            lifecycleScope.launch {
                viewModel.getHighlights(bookId).collectLatest { highlights ->
                    if (highlights.isEmpty()) {
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listOf("No highlights yet"))
                        listView.setOnItemClickListener(null)
                    } else {
                        val publication = ReaderRepository.currentPublication
                        val tocLinks = publication?.tableOfContents ?: emptyList()
                        val highlightTitles = highlights.mapIndexed { index, highlight ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(highlight.locator))
                                if (locator != null) {
                                    val locatorHref = locator.href.toString()
                                    var chapterName = "Chapter ${currentSpineIndex + 1}"
                                    for (tocLink in tocLinks) {
                                        val tocHref = tocLink.href.toString()
                                        if (locatorHref.contains(tocHref.substringAfterLast("/"))) {
                                            chapterName = tocLink.title ?: chapterName
                                            break
                                        }
                                    }
                                    
                                    val locationInfo = locator.locations?.fragments?.firstOrNull()?.let { fragment ->
                                        fragment.replace("cfi=", "").take(20)
                                    } ?: locator.locations?.progression?.let { 
                                        "${(it * 100).toInt()}%"
                                    } ?: ""
                                    
                                    val highlightText = locator.text?.highlight?.take(30)
                                    val displayText = if (highlightText != null) "\"$highlightText...\"" else null
                                    
                                    val prefix = if (displayText != null) "$displayText - " else ""
                                    if (locationInfo.isNotEmpty()) {
                                        "$prefix$chapterName ($locationInfo)"
                                    } else {
                                        "$prefix$chapterName"
                                    }
                                } else {
                                    "Highlight ${index + 1}"
                                }
                            } catch (e: Exception) {
                                "Highlight ${index + 1}"
                            }
                        }
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, highlightTitles)
                        listView.setOnItemClickListener { _: android.widget.AdapterView<*>, _: View, pos: Int, _: Long ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(highlights[pos].locator))
                                locator?.let {
                                    lifecycleScope.launch {
                                        renditionState?.controller?.goTo(ReflowableWebGoLocation(it))
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error loading highlight", Toast.LENGTH_SHORT).show()
                            }
                        }
                        listView.setOnItemLongClickListener { _: android.widget.AdapterView<*>, _: View, pos: Int, _: Long ->
                            lifecycleScope.launch {
                                viewModel.deleteHighlight(highlights[pos])
                                Toast.makeText(context, "Highlight deleted", Toast.LENGTH_SHORT).show()
                            }
                            true
                        }
                    }
                }
            }
        }
        
        btnBookmarks.setOnClickListener { showBookmarksList() }
        btnHighlights.setOnClickListener { showHighlightsList() }
        
        val dialog: android.app.AlertDialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .create()
        
        // Start with Highlights tab
        showHighlightsList()
        dialog.show()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun showSettingsDialog() {
        settingsComposeView.visibility = View.VISIBLE
        settingsComposeView.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val settings by viewModel.settings.collectAsState()
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize().selectable(selected = true, onClick = { settingsComposeView.visibility = View.GONE }),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {}
                    
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        tonalElevation = 8.dp,
                        color = Color(0xFF1A1A1A)
                    ) {
                        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Reading Settings", style = MaterialTheme.typography.titleLarge, color = Color.White)
                                IconButton(onClick = { settingsComposeView.visibility = View.GONE }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text("Theme", color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReaderTheme.entries.forEach { theme ->
                                    val (bgColor, textColor) = when(theme) {
                                        ReaderTheme.WHITE -> Color.White to Color.Black
                                        ReaderTheme.DARK -> Color(0xFF2C2C2C) to Color.White
                                        ReaderTheme.SEPIA -> Color(0xFFF4ECD8) to Color(0xFF5B4636)
                                    }
                                    
                                    FilterChip(
                                        selected = settings.theme == theme,
                                        onClick = {
                                            val ns = settings.copy(theme = theme)
                                            viewModel.updateSettings(ns)
                                            recreateNavigator(ns)
                                        },
                                        label = { Text(theme.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = bgColor,
                                            labelColor = textColor,
                                            selectedContainerColor = bgColor,
                                            selectedLabelColor = textColor
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = settings.theme == theme,
                                            borderColor = if (settings.theme == theme) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            borderWidth = 2.dp,
                                            selectedBorderColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("TYPEFACE", color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            val fontOptions = listOf(
                                Triple("Serif", "serif", FontFamily.Serif),
                                Triple("Sans", "sans-serif", FontFamily.SansSerif),
                                Triple("Mono", "monospace", FontFamily.Monospace),
                                Triple("Merriweather", "Merriweather", FontFamily.Serif),
                                Triple("Lora", "Lora", FontFamily.Serif),
                                Triple("Dyslexic", "OpenDyslexic", FontFamily.SansSerif)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                fontOptions.forEach { (label, value, _) ->
                                    FilterChip(
                                        selected = settings.fontFamily == value,
                                        onClick = {
                                            val ns = settings.copy(fontFamily = value)
                                            viewModel.updateSettings(ns)
                                            recreateNavigator(ns)
                                        },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color.White.copy(alpha = 0.08f),
                                            labelColor = Color.LightGray,
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            selectedLabelColor = Color.White
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = settings.fontFamily == value,
                                            borderColor = Color.Transparent,
                                            borderWidth = 1.dp,
                                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                                            selectedBorderWidth = 2.dp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(stringResource(R.string.reader_font_size, settings.fontSize.toInt()), color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        val new = (settings.fontSize - 5.0).coerceIn(50.0, 300.0)
                                        val ns = settings.copy(fontSize = new)
                                        viewModel.updateSettings(ns)
                                        recreateNavigator(ns)
                                    },
                                    enabled = settings.fontSize > 50.0,
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (settings.fontSize > 50.0) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease font size", tint = if (settings.fontSize > 50.0) Color.White else Color.Gray, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Slider(
                                    value = settings.fontSize.toFloat(),
                                    onValueChange = { raw ->
                                        val snapped = (Math.round(raw / 5.0) * 5.0).coerceIn(50.0, 300.0)
                                        val ns = settings.copy(fontSize = snapped)
                                        viewModel.updateSettings(ns)
                                    },
                                    onValueChangeFinished = {
                                        recreateNavigator(viewModel.settings.value)
                                    },
                                    valueRange = 50f..300f,
                                    steps = 49,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                                )
                                Surface(
                                    onClick = {
                                        val new = (settings.fontSize + 5.0).coerceIn(50.0, 300.0)
                                        val ns = settings.copy(fontSize = new)
                                        viewModel.updateSettings(ns)
                                        recreateNavigator(ns)
                                    },
                                    enabled = settings.fontSize < 300.0,
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (settings.fontSize < 300.0) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase font size", tint = if (settings.fontSize < 300.0) Color.White else Color.Gray, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun highlightSelection(locator: Locator, color: Int, annotation: String? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            val selectedText = locator.text.highlight ?: ""
            if (selectedText.isNotBlank()) {
                val existingHighlights = viewModel.getHighlightsSync(bookId)
                val overlapping = existingHighlights.any { existing ->
                    val existingText = try {
                        val existingLocator = Locator.fromJSON(org.json.JSONObject(existing.locator))
                        existingLocator?.text?.highlight ?: ""
                    } catch (e: Exception) { "" }
                    existingText.isNotBlank() && selectedText.isNotBlank() && (
                        existingText.contains(selectedText, ignoreCase = true) ||
                        selectedText.contains(existingText, ignoreCase = true)
                    )
                }
                if (overlapping) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Text already highlighted", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
            }
            viewModel.insertHighlight(bookId, locator.toJSON().toString(), color, annotation)
        }
    }

    private fun createSelectionActionModeCallback(): ActionMode.Callback {
        return object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.clear()
                menu.add(0, 1, 0, "Highlight")
                menu.add(0, 2, 0, "Copy")
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    1 -> { // Highlight
                        lifecycleScope.launch {
                            val selection = renditionState?.controller?.currentSelection()
                            if (selection != null) {
                                showSelectionMenu(selection.location.toLocator())
                            }
                            withContext(Dispatchers.Main) { mode.finish() }
                        }
                    }
                    2 -> { // Copy
                        lifecycleScope.launch {
                            val selection = renditionState?.controller?.currentSelection()
                            if (selection != null) {
                                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("selected text", selection.text)
                                clipboard.setPrimaryClip(clip)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                }
                            }
                            withContext(Dispatchers.Main) { mode.finish() }
                        }
                    }
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {}
        }
    }

    private fun recreateNavigator(settings: ReaderSettings) { renditionState?.controller?.location?.toLocator()?.let { setupNavigator(it, settings, forceScrollMode = true) } }
    fun isScrollMode(): Boolean = viewModel.settings.value.scrollMode
}
