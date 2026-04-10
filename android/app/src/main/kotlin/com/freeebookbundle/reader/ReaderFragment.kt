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
import androidx.lifecycle.viewModelScope
import com.freeebookbundle.R
import com.freeebookbundle.EbookApp
import com.freeebookbundle.db.Bookmark
import com.freeebookbundle.db.Highlight
import com.freeebookbundle.db.BookDao
import com.freeebookbundle.models.ReadingStatus
import com.freeebookbundle.voice.TtsVoiceManager
import com.freeebookbundle.voice.TtsSettingsPanel
import com.freeebookbundle.ui.AudioPlayerScreen
import com.freeebookbundle.ui.ChapterInfo
import com.freeebookbundle.ui.TtsPlayerState
import com.freeebookbundle.viewmodel.AppThemeMode
import com.freeebookbundle.viewmodel.ReaderViewModel
import com.freeebookbundle.utils.ReviewManager
import com.freeebookbundle.repository.ReaderSettingsRepository
import com.freeebookbundle.utils.EpubPreviewUtils
import com.freeebookbundle.utils.GenrePreferences
import com.freeebookbundle.utils.LocaleManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubDefaults
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.navigator.preferences.ReadingProgression
import androidx.compose.ui.graphics.Color
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.Selection
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.util.Try

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.darkColorScheme
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.freeebookbundle.viewmodel.ReaderTheme
import com.freeebookbundle.viewmodel.ReaderSettings

import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.*

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import org.readium.r2.navigator.epub.EpubNavigatorFragment.Configuration as EpubConfig

@OptIn(ExperimentalReadiumApi::class)
class ReaderFragment : Fragment(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "ReaderFragment"
    }

    private val viewModel: ReaderViewModel by viewModels()
    private val appViewModel: com.freeebookbundle.viewmodel.AppViewModel by viewModels()
    private var bookId: String = "unknown"
    private var navigator: EpubNavigatorFragment? = null
    private var currentSelection: Locator? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    
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
    private lateinit var btnHelp: ImageButton
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
    
    // Reading progress tracking
    private var totalSpineItems by mutableStateOf(0)
    private var currentSpineIndex by mutableStateOf(0)

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
    
    // Tutorial state
    private var showTutorial by mutableStateOf(false)
    private var tutorialStep by mutableStateOf(0)
    private var currentBookIdString by mutableStateOf("")
    
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
        btnHelp = view.findViewById(R.id.btn_help)
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

        // Check and show tutorial after reveal overlay fades
        lifecycleScope.launch {
            delay(2000) // Wait for reveal overlay to finish
            checkAndShowTutorial()
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
        val initialLocator = findRealContentStartLocator(publication) ?: return view

        setupNavigator(initialLocator, settings)
        
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
                // Restore TTS state if available
                lifecycleScope.launch {
                    val database = (requireActivity().application as EbookApp).database
                    val bookDao = database.bookDao()
                    val savedTts = bookDao.getTtsPosition(bookId)
                    if (savedTts != null) {
                        currentSpineIndex = savedTts.spineIndex
                        currentSentenceIndex = savedTts.sentenceIndex
                        currentParagraphIndex = savedTts.paragraphIndex
                    }
                }
            }
        }
        
        observeHighlights()
    }

    private fun setupTtsControls() {
        val currentBook = ReaderRepository.currentBook
        val publication = ReaderRepository.currentPublication
        val bookTitle = currentBook?.title ?: publication?.metadata?.title ?: getString(R.string.reader_unknown_book)
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
                    navigator?.go(locator, animated = true)
                    delay(500)
                    speakCurrentPage()
                }
            }
        }
    }

    private fun extractAuthorName(authors: Any?): String {
        if (authors == null) return getString(R.string.reader_unknown_author)
        return authors.toString().ifEmpty { getString(R.string.reader_unknown_author) }
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
                navigator?.goForward(animated = true)
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
                navigator?.goBackward(animated = true)
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
            val currentLocator = navigator?.currentLocator?.value ?: return@launch
            
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
                    Toast.makeText(context, getString(R.string.reader_no_text), Toast.LENGTH_SHORT).show()
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
        val nav = navigator as? DecorableNavigator ?: return
        val baseLocator = currentResourceLocator ?: return
        
        lifecycleScope.launch {
            if (text == null) {
                nav.applyDecorations(emptyList(), "tts-highlight")
                return@launch
            }

            // Create a locator for the text within the current resource
            val locator = baseLocator.copy(
                text = Locator.Text(highlight = text)
            )

            val decoration = Decoration(
                id = if (isWord) "tts-word" else "tts-sentence",
                locator = locator,
                style = if (isWord) 
                    Decoration.Style.Highlight(tint = 0x44FF0000.toInt()) // Light Red for word
                else 
                    Decoration.Style.Highlight(tint = 0x44FFFF00.toInt()) // Light Yellow for sentence
            )

            nav.applyDecorations(listOf(decoration), "tts-highlight")
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
        val publication = ReaderRepository.currentPublication ?: return
        val config = EpubConfig(
            disablePageTurnsWhileScrolling = true,
            selectionActionModeCallback = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = true
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    lifecycleScope.launch {
                        val selection = navigator?.currentSelection()
                        if (selection != null) {
                            showSelectionMenu(selection.locator)
                        }
                    }
                    return false
                }
                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
                override fun onDestroyActionMode(mode: ActionMode?) {}
            }
        )
        val factory = EpubNavigatorFactory(
            publication = publication,
            configuration = EpubNavigatorFactory.Configuration(
                defaults = EpubDefaults(
                    scroll = true,
                    columnCount = ColumnCount.ONE,
                    spread = Spread.NEVER
                )
            )
        )
        val fragmentFactory = factory.createFragmentFactory(
            initialLocator = initialLocator,
            initialPreferences = createEpubPreferences(settings, forceScrollMode),
            configuration = config,
            listener = object : EpubNavigatorFragment.Listener {
                override fun onExternalLinkActivated(url: AbsoluteUrl) {}
                override fun onJumpToLocator(locator: Locator) {
                    activity?.runOnUiThread {
                        updateReadingProgress(locator)
                    }
                }
            }
        )
        val navigatorFragment = fragmentFactory.instantiate(requireContext().classLoader, EpubNavigatorFragment::class.java.name) as EpubNavigatorFragment
        this.navigator = navigatorFragment
        
        // Restore reading position if available - delay until fragment is ready
        viewLifecycleOwner.lifecycleScope.launch {
            val database = (requireActivity().application as EbookApp).database
            val bookDao = database.bookDao()
            val savedPosition = bookDao.getLastPosition(bookId)
            // Only restore if there's actual meaningful progress (not at start)
            val hasProgress = savedPosition != null && (savedPosition.spineIndex > 0 || savedPosition.progression > 0.01f)
            if (hasProgress) {
                // Wait for fragment to be ready before navigating
                view?.post {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val publication = ReaderRepository.currentPublication
                        if (publication != null && savedPosition.spineIndex < publication.readingOrder.size) {
                            val link = publication.readingOrder.get(savedPosition.spineIndex)
                            if (
                                link != null) {
                                val locator = publication.locatorFromLink(link)
                                if (locator != null && locator.locations != null) {
                                    val originalLocations = locator.locations!!
                                    val newLocations = originalLocations.copy(progression = savedPosition.progression.toDouble())
                                    val restoredLocator = locator.copy(locations = newLocations)
                                    navigatorFragment.go(restoredLocator, animated = false)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        childFragmentManager.beginTransaction().replace(R.id.fragment_reader_container, navigatorFragment).commit()
        
        // Collect locator changes for responsive progress updates
        viewLifecycleOwner.lifecycleScope.launch {
            navigatorFragment.currentLocator.collect { locator ->
                updateReadingProgress(locator)
            }
        }
    }

    private fun findRealContentStartLocator(publication: org.readium.r2.shared.publication.Publication): Locator? {
        val toc = publication.tableOfContents
        val readingOrder = publication.readingOrder
        
        Log.d(TAG, "[SmartStart] Finding real content start")
        Log.d(TAG, "[SmartStart] TOC size: ${toc.size}, ReadingOrder size: ${readingOrder.size}")
        
        val skipPatterns = listOf("cover", "wrapper", "pg-header", "pg-footer", "license", "copyright", "gutenberg", "title-page", " verso", "imprint")
        
        fun String.matchesSkipPattern(): Boolean {
            val lower = this.lowercase()
            return skipPatterns.any { lower.contains(it) }
        }
        
        // Strategy 1: Use TOC (most reliable for structured EPUBs)
        if (toc.isNotEmpty()) {
            // Find first TOC entry that isn't cover/gutenberg
            val firstContentToc = toc.firstOrNull { link ->
                val href = link.href.toString()
                Log.d(TAG, "[SmartStart] Checking TOC: ${link.title ?: "no title"} -> $href")
                !href.matchesSkipPattern()
            }
            
            if (firstContentToc != null) {
                Log.d(TAG, "[SmartStart] Using TOC entry: ${firstContentToc.title ?: "no title"}")
                return publication.locatorFromLink(firstContentToc)
            }
        }
        
        // Strategy 2: Fallback to reading order, skipping cover/gutenberg items
        val firstContentSpineIndex = readingOrder.indexOfFirst { link ->
            val href = link.href.toString()
            Log.d(TAG, "[SmartStart] Checking spine: $href")
            !href.matchesSkipPattern()
        }
        
        if (firstContentSpineIndex >= 0) {
            Log.d(TAG, "[SmartStart] Using spine index: $firstContentSpineIndex")
            return publication.locatorFromLink(readingOrder[firstContentSpineIndex])
        }
        
        // Strategy 3: Last resort - use first item
        Log.d(TAG, "[SmartStart] No content found, using first item")
        readingOrder.firstOrNull()?.let { return publication.locatorFromLink(it) }
        return null
    }

    private fun createEpubPreferences(settings: ReaderSettings, forceScrollMode: Boolean = false): EpubPreferences {
        val isScrollMode = forceScrollMode || settings.scrollMode
        val isRTL = activity?.let { LocaleManager.isCurrentLocaleRTL(it) } ?: false
        return EpubPreferences(
            fontSize = settings.fontSize / 100.0,
            theme = when(settings.theme) {
                ReaderTheme.WHITE -> Theme.LIGHT
                ReaderTheme.DARK -> Theme.DARK
                ReaderTheme.SEPIA -> Theme.SEPIA
            },
            scroll = isScrollMode,
            columnCount = if (isScrollMode) ColumnCount.ONE else null,
            spread = if (isScrollMode) Spread.NEVER else null,
            readingProgression = if (isRTL) ReadingProgression.RTL else ReadingProgression.LTR,
            verticalText = false
        )
    }

    private var chapterProgression = 0f
    
    private fun updateReadingProgress(locator: Locator) {
        val publication = ReaderRepository.currentPublication ?: return
        val href = locator.href.toString()
        
        // Find current spine index
        val spineIndex = publication.readingOrder.indexOfFirst { it.href.toString() == href }
        if (spineIndex >= 0) {
            currentSpineIndex = spineIndex
        }
        
        // Get intra-chapter progress from locator (0.0 to 1.0)
        val rawProgression = locator.locations?.progression?.toFloat()
        if (rawProgression != null) {
            chapterProgression = rawProgression
        }
        // If null, keep current value (user likely at end of content)
        
        Log.d(TAG, "[Progress] locator=$locator, progression=$chapterProgression, spineIndex=$currentSpineIndex, total=$totalSpineItems")
        
        activity?.runOnUiThread {
            updateProgressUI()
        }
    }

    private fun updateProgressUI() {
        progressProgress = chapterProgression.coerceIn(0f, 1f)
        
        val isLastChapter = currentSpineIndex >= totalSpineItems - 1
        val isNearEndOfChapter = chapterProgression >= 0.95f
        showNextChapter = isNearEndOfChapter || isLastChapter
        
        val chapterTitle = if (tableOfContents.isNotEmpty() && currentSpineIndex < tableOfContents.size) {
            tableOfContents[currentSpineIndex].title ?: "Chapter ${currentSpineIndex + 1}"
        } else {
            "Chapter ${currentSpineIndex + 1}"
        }
        readerChapter.text = chapterTitle
    }

    private fun setupProgressBar() {
        progressComposeView.setContent {
            val animatedProgress by animateFloatAsState(
                targetValue = progressProgress,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                label = "progress"
            )
            
            val systemNavPadding = androidx.compose.foundation.layout.WindowInsets.systemBars.asPaddingValues()
            
            val isFirstChapter = currentSpineIndex == 0
            val isLastChapter = currentSpineIndex >= totalSpineItems - 1
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = systemNavPadding.calculateBottomPadding()
                    )
            ) {
                if (showNextChapter) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Previous Chapter button - only show if not on first chapter
                        if (!isFirstChapter) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable {
                                        lifecycleScope.launch {
                                            try {
                                                val publication = ReaderRepository.currentPublication
                                                if (publication != null && currentSpineIndex > 0) {
                                                    val prevLink = publication.readingOrder.getOrNull(currentSpineIndex - 1)
                                                    if (prevLink != null) {
                                                        publication.locatorFromLink(prevLink)?.let { locator ->
                                                            navigator?.go(locator, animated = true)
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error going to previous chapter: ${e.message}")
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFF333333),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowLeft,
                                        contentDescription = "Previous",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Previous",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        
                        // Next/Finish Chapter button
                        Surface(
                            modifier = Modifier
                                .weight(if (!isFirstChapter) 1f else 1f)
                                .height(48.dp)
                                .clickable {
                                    if (isLastChapter) {
                                        markBookAsCompleted()
                                    } else {
                                        lifecycleScope.launch {
                                            try {
                                                val publication = ReaderRepository.currentPublication
                                                if (publication != null) {
                                                    val nextLink = publication.readingOrder.getOrNull(currentSpineIndex + 1)
                                                    if (nextLink != null) {
                                                        publication.locatorFromLink(nextLink)?.let { locator ->
                                                            navigator?.go(locator, animated = true)
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error going to next chapter: ${e.message}")
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFE50914),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = if (!isFirstChapter) Arrangement.End else Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val buttonText = when {
                                    isLastChapter -> "Finish"
                                    else -> "Next"
                                }
                                Text(
                                    text = buttonText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                                if (!isLastChapter) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Next",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Progress bar - always visible
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
    
    private fun markBookAsCompleted() {
        val currentBook = ReaderRepository.currentBook ?: return
        
        lifecycleScope.launch {
            try {
                val app = requireActivity().application as EbookApp
                app.database.bookDao().markAsCompleted(
                    bookId = currentBook.id,
                    status = ReadingStatus.COMPLETED,
                    dateFinished = System.currentTimeMillis()
                )
                
                Log.d(TAG, "Book marked as completed: ${currentBook.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking book as completed: ${e.message}")
            }
        }
        
        activity?.finish()
    }

    override fun onDestroy() {
        // Save reading progress before destroying fragment
        saveReadingProgress()
        
        // Save TTS state before destroying fragment
        saveTtsState()
        
        // Track book close and check if we should show review prompt
        if (context != null) {
            ReviewManager.onBookClosed(requireContext())
            if (ReviewManager.shouldShowRatingPrompt(requireContext())) {
                ReviewManager.requestRatingWithStars(requireActivity(), AppThemeMode.DAY) { rating ->
                    // Rating handled by onRatingShown in handleRatingWithCallback
                    // Just log for debugging
                    Log.d(TAG, "User rated: $rating stars")
                }
            }
        }
        
        try { tts?.stop(); tts?.shutdown() } catch (e: Exception) {}
        if (::ttsVoiceManager.isInitialized) ttsVoiceManager.shutdown()
        super.onDestroy()
    }
    
    private fun saveReadingProgress() {
        lifecycleScope.launch {
            val database = (requireActivity().application as EbookApp).database
            val bookDao = database.bookDao()
            bookDao.updateReadingProgress(bookId, currentSpineIndex, chapterProgression)
        }
    }
    
    private fun saveTtsState() {
        lifecycleScope.launch {
            val database = (requireActivity().application as EbookApp).database
            val bookDao = database.bookDao()
            bookDao.updateTtsPosition(bookId, currentSpineIndex, currentSentenceIndex, currentParagraphIndex)
        }
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
                val selection = navigator?.currentSelection()
                if (selection != null) showSelectionMenu(selection.locator)
                else Toast.makeText(context, getString(R.string.reader_select_text), Toast.LENGTH_SHORT).show()
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
        btnHelp.setOnClickListener { checkAndShowTutorial() }
    }

    private fun openChapterDrawer() {
        drawerLayout.openDrawer(Gravity.START)
    }

    private var tableOfContents: List<org.readium.r2.shared.publication.Link> = emptyList()
    
    private fun setupChapterDrawer(publication: org.readium.r2.shared.publication.Publication) {
        tableOfContents = publication.tableOfContents
        val toc = tableOfContents
        val chapterCountText = view?.findViewById<TextView>(R.id.drawer_chapter_count)
        
        // Use toc size, fallback to readingOrder size if empty
        val actualCount = if (toc.isNotEmpty()) toc.size else publication.readingOrder.size
        chapterCountText?.text = getString(R.string.reader_chapters_count, actualCount)
        
        // Enable swipe gesture to open drawer
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        
        chapterList.layoutManager = LinearLayoutManager(context)
        
        // Use toc if available, otherwise use readingOrder as chapters
        val chapters = if (toc.isNotEmpty()) toc else publication.readingOrder
        val adapter = ChapterAdapter(chapters, currentSpineIndex) { index ->
            lifecycleScope.launch {
                chapters.getOrNull(index)?.let { link ->
                    drawerLayout.closeDrawers()
                    publication.locatorFromLink(link)?.let { locator ->
                        navigator?.go(locator, animated = true)
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
        if (isTtsActive || showTutorial) return
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
        
        // Don't toggle UI during tutorial
        if (showTutorial) return
        
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
                    Surface(modifier = Modifier.fillMaxSize().selectable(selected = true, onClick = { selectionComposeView.visibility = View.GONE; navigator?.clearSelection() }), color = Color.Transparent) {}
                    Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(), shape = RoundedCornerShape(12.dp), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (!showTagInput) {
                                Text(stringResource(R.string.reader_highlight_title), style = MaterialTheme.typography.titleMedium)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    colors.forEach { colorInt -> Box(modifier = Modifier.size(40.dp).background(Color(colorInt), CircleShape).selectable(selected = false, onClick = { highlightSelection(locator, colorInt); selectionComposeView.visibility = View.GONE; navigator?.clearSelection() })) }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = { showTagInput = true }) { Text(stringResource(R.string.reader_add_note)) }
                                    TextButton(onClick = { Toast.makeText(context, "Lookup: ${locator.text.highlight}", Toast.LENGTH_SHORT).show() }) { Text(stringResource(R.string.reader_lookup)) }
                                }
                            } else {
                                OutlinedTextField(value = tagText, onValueChange = { tagText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.reader_note_placeholder)) })
                                Button(onClick = { highlightSelection(locator, 0x8800AAFF.toInt(), tagText); selectionComposeView.visibility = View.GONE; navigator?.clearSelection() }) { Text(stringResource(R.string.reader_save)) }
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
                val nav = navigator
                if (nav == null || !nav.isAdded) return@collectLatest

                val decorations = highlights.map { highlight ->
                    val locator = try { Locator.fromJSON(org.json.JSONObject(highlight.locator)) } catch (e: Exception) { null } ?: return@map null
                    Decoration(id = highlight.id.toString(), locator = locator, style = Decoration.Style.Highlight(tint = highlight.color))
                }.filterNotNull()
                (nav as? DecorableNavigator)?.applyDecorations(decorations, "highlights")
            }
        }
    }

    private fun showSearchDialog() {
        val input = EditText(context)
        input.setHint(getString(R.string.reader_search_placeholder))
        AlertDialog.Builder(context).setTitle(getString(R.string.reader_search_title)).setView(input).setPositiveButton(getString(R.string.reader_search_action)) { _, _ -> performSearch(input.text.toString()) }.setNegativeButton(getString(R.string.reader_cancel), null).show()
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
            navigator?.go(currentSearchLocators[currentSearchIndex], animated = true)
            searchCountText.text = "${currentSearchIndex + 1}/${currentSearchLocators.size}"
        }
    }

    private fun setupSearchNavBar() {
        btnPrevSearch.setOnClickListener { if (currentSearchLocators.isNotEmpty()) { currentSearchIndex = if (currentSearchIndex <= 0) currentSearchLocators.size - 1 else currentSearchIndex - 1; updateSearchNavigation() } }
        btnNextSearch.setOnClickListener { if (currentSearchLocators.isNotEmpty()) { currentSearchIndex = if (currentSearchIndex >= currentSearchLocators.size - 1) 0 else currentSearchIndex + 1; updateSearchNavigation() } }
        btnCloseSearch.setOnClickListener { searchNavBar.visibility = View.GONE; currentSearchLocators.clear() }
    }

    private fun bookmarkCurrentLocation() {
        val locator = navigator?.currentLocator?.value ?: return
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
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listOf(getString(R.string.reader_no_bookmarks)))
                        listView.setOnItemClickListener(null)
                    } else {
                        val publication = ReaderRepository.currentPublication
                        val tocLinks = publication?.tableOfContents ?: emptyList()
                        val bookmarkTitles = bookmarks.mapIndexed { index, bookmark ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(bookmark.locator))
                                if (locator != null) {
                                    val locatorHref = locator.href.toString()
                                    var chapterName = getString(R.string.reader_chapter_default, currentSpineIndex + 1)
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
                                    getString(R.string.reader_bookmark_default, index + 1)
                                }
                            } catch (e: Exception) {
                                getString(R.string.reader_bookmark_default, index + 1)
                            }
                        }
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, bookmarkTitles)
                        listView.setOnItemClickListener { _: android.widget.AdapterView<*>, _: View, pos: Int, _: Long ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(bookmarks[pos].locator))
                                locator?.let { navigator?.go(it, animated = true) }
                            } catch (e: Exception) {
                                Toast.makeText(context, getString(R.string.reader_bookmark_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                        listView.setOnItemLongClickListener { _: android.widget.AdapterView<*>, _: View, pos: Int, _: Long ->
                            lifecycleScope.launch {
                                viewModel.deleteBookmark(bookmarks[pos])
                                Toast.makeText(context, getString(R.string.reader_bookmark_deleted), Toast.LENGTH_SHORT).show()
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
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listOf(getString(R.string.reader_no_highlights)))
                        listView.setOnItemClickListener(null)
                    } else {
                        val publication = ReaderRepository.currentPublication
                        val tocLinks = publication?.tableOfContents ?: emptyList()
                        val highlightTitles = highlights.mapIndexed { index, highlight ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(highlight.locator))
                                if (locator != null) {
                                    val locatorHref = locator.href.toString()
                                    var chapterName = getString(R.string.reader_chapter_default, currentSpineIndex + 1)
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
                                    getString(R.string.reader_highlight_default, index + 1)
                                }
                            } catch (e: Exception) {
                                getString(R.string.reader_highlight_default, index + 1)
                            }
                        }
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, highlightTitles)
                        listView.setOnItemClickListener { _: android.widget.AdapterView<*>, _: View, pos: Int, _: Long ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(highlights[pos].locator))
                                locator?.let { navigator?.go(it, animated = true) }
                            } catch (e: Exception) {
                                Toast.makeText(context, getString(R.string.reader_highlight_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                        listView.setOnItemLongClickListener { _: android.widget.AdapterView<*>, _: View, pos: Int, _: Long ->
                            lifecycleScope.launch {
                                viewModel.deleteHighlight(highlights[pos])
                                Toast.makeText(context, getString(R.string.reader_highlight_deleted), Toast.LENGTH_SHORT).show()
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
            .setNegativeButton(getString(R.string.reader_close), null)
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
                                Text(stringResource(R.string.reader_reading_settings_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
                                IconButton(onClick = { settingsComposeView.visibility = View.GONE }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(stringResource(R.string.reader_theme_label), color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
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
                            
                            Text(stringResource(R.string.reader_font_size, settings.fontSize.toInt()), color = Color.LightGray)
                            Slider(
                                value = settings.fontSize.toFloat(),
                                onValueChange = {
                                    val ns = settings.copy(fontSize = it.toDouble())
                                    viewModel.updateSettings(ns)
                                    recreateNavigator(ns)
                                },
                                valueRange = 50f..300f,
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    private fun highlightSelection(locator: Locator, color: Int, annotation: String? = null) {
        viewLifecycleOwner.lifecycleScope.launch { viewModel.insertHighlight(bookId, locator.toJSON().toString(), color, annotation) }
    }

    private fun recreateNavigator(settings: ReaderSettings) { navigator?.currentLocator?.value?.let { setupNavigator(it, settings, forceScrollMode = true) } }
    fun isScrollMode(): Boolean = viewModel.settings.value.scrollMode

    private fun checkAndShowTutorial() {
        val currentBook = ReaderRepository.currentBook
        if (currentBook == null) return
        
        // Only show tutorial on first ever time opening ANY ebook
        if (GenrePreferences.hasReaderTutorialBeenShownForAnyBook()) {
            return
        }
        
        val bookIdString = currentBook.id
        
        // Show tutorial (help button will force show it again if needed)
        currentBookIdString = bookIdString
        showTutorial = true
        tutorialStep = 0
        
        // Lock UI - show both bars and prevent them from toggling
        topBar?.visibility = View.VISIBLE
        bottomBar?.visibility = View.VISIBLE
        
        // Small delay to ensure bottom bar is laid out
        view?.postDelayed({
            if (isAdded && view != null) {
                val positions = getAllButtonPositions()
                showTutorialWithPositions(positions)
            }
        }, 100)
    }
    
    private fun showTutorialWithPositions(positions: Map<String, Pair<Float, Float>>) {
        revealOverlayView.visibility = View.VISIBLE
        revealOverlayView.setContent {
            InteractiveTutorialOverlay(
                positions = positions,
                currentStep = tutorialStep,
                onStepComplete = { advanceTutorial() },
                onSkip = { finishTutorial() }
            )
        }
    }
    
    private fun advanceTutorial() {
        if (tutorialStep < 6) {
            tutorialStep++
            val newPositions = getAllButtonPositions()
            revealOverlayView.setContent {
                InteractiveTutorialOverlay(
                    positions = newPositions,
                    currentStep = tutorialStep,
                    onStepComplete = { 
                        if (tutorialStep < 6) {
                            advanceTutorial()
                        } else {
                            finishTutorial()
                        }
                    },
                    onSkip = { finishTutorial() }
                )
            }
        } else {
            finishTutorial()
        }
    }
    
    private fun finishTutorial() {
        showTutorial = false
        GenrePreferences.markReaderTutorialDone(currentBookIdString)
        revealOverlayView.visibility = View.GONE
        // Hide bars again after tutorial
        topBar?.visibility = View.GONE
    }
    
    private fun getAllButtonPositions(): Map<String, Pair<Float, Float>> {
        val positions = mutableMapOf<String, Pair<Float, Float>>()
        
        fun addPosition(key: String, viewId: Int) {
            val view = view?.findViewById<View>(viewId) ?: return
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val w = view.width
            val h = view.height
            positions[key] = Pair(location[0] + w / 2f, location[1] + h / 2f)
        }
        
        addPosition("menu", R.id.btn_menu)
        addPosition("search", R.id.btn_search)
        addPosition("highlight", R.id.btn_highlight)
        addPosition("tts", R.id.btn_tts)
        addPosition("bookmark", R.id.btn_star_bookmark)
        addPosition("books", R.id.btn_book_list)
        addPosition("settings", R.id.btn_settings)
        addPosition("help", R.id.btn_help)
        
        return positions
    }
}

@Composable
private fun InteractiveTutorialOverlay(
    positions: Map<String, Pair<Float, Float>>,
    currentStep: Int,
    onStepComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val stepOrder = listOf("menu", "search", "highlight", "tts", "bookmark", "books", "settings")
    val currentKey = stepOrder.getOrNull(currentStep) ?: "menu"
    val (centerXPx, centerYPx) = positions[currentKey] ?: Pair(0f, 0f)
    
    val accentColor = Color(0xFFE50914)
    val density = androidx.compose.ui.platform.LocalDensity.current

    val tutorialInfo = listOf(
        TutorialStepData(
            title = "Chapter Menu",
            description = "Tap here to open the chapter list and jump to any section of the book",
            icon = "📖"
        ),
        TutorialStepData(
            title = "Search",
            description = "Search for words or phrases anywhere in the book",
            icon = "🔍"
        ),
        TutorialStepData(
            title = "Highlight",
            description = "Select any text to highlight, add notes, or copy",
            icon = "✏️"
        ),
        TutorialStepData(
            title = "Listen",
            description = "Have the book read aloud with text-to-speech. Tap again to pause",
            icon = "🔊"
        ),
        TutorialStepData(
            title = "Bookmark",
            description = "Save your current position and access your saved bookmarks",
            icon = "⭐"
        ),
        TutorialStepData(
            title = "Library",
            description = "View all your books, highlights, and reading history",
            icon = "📚"
        ),
        TutorialStepData(
            title = "Settings",
            description = "Adjust font size, theme, margins, and other reading preferences",
            icon = "⚙️"
        )
    )
    
    val currentInfo = tutorialInfo.getOrNull(currentStep)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))) {
        // Ring indicator around button position (outline only, not filled)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val x = centerXPx
            val y = centerYPx
            if (x > 0 && y > 0) {
                // Outer transparent ring
                drawCircle(
                    color = accentColor.copy(alpha = 0.3f),
                    radius = 55.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
                // Ring outline
                drawCircle(
                    color = accentColor,
                    radius = 40.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
                // Pulsing dot indicator
                drawCircle(
                    color = accentColor,
                    radius = 6.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }
        
        // Header - avoid top bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to the Reader",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Step ${currentStep + 1} of ${tutorialInfo.size}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
        
        // Skip button
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            Text(
                "Skip",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Progress indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            repeat(tutorialInfo.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentStep) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentStep) accentColor 
                            else if (index < currentStep) accentColor.copy(alpha = 0.7f)
                            else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }
        
        // Info card at bottom - avoid bottom bar
        currentInfo?.let { info ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 100.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E1E1E),
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(text = info.icon, fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = info.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = info.description,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Next button
                Button(
                    onClick = onStepComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = if (currentStep < tutorialInfo.size - 1) "Next" else "Get Started",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentStep < tutorialInfo.size - 1) {
                        Spacer(Modifier.width(8.dp))
                        Text("→", color = Color.White, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

data class TutorialStepData(
    val title: String,
    val description: String,
    val icon: String
)
