package com.freeebookbundle.reader

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freeebookbundle.viewmodel.ReaderTheme
import com.freeebookbundle.viewmodel.ReaderSettings

import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.*

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import org.readium.r2.navigator.epub.EpubNavigatorFragment.Configuration as EpubConfig

@OptIn(ExperimentalReadiumApi::class)
class ReaderFragment : Fragment(), TextToSpeech.OnInitListener {

    private val viewModel: ReaderViewModel by viewModels()
    private val appViewModel: com.freeebookbundle.viewmodel.AppViewModel by viewModels()
    private var bookId: String = "unknown"
    private var navigator: EpubNavigatorFragment? = null
    private var currentSelection: Locator? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    
    private lateinit var topBar: View
    private lateinit var bottomBar: View
    private lateinit var btnBookmarkStar: ImageButton
    private lateinit var btnHighlight: ImageButton
    private lateinit var btnBookList: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var btnTts: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var readerTitle: TextView
    private lateinit var readerChapter: TextView
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
    private var isProgressVisible by mutableStateOf(false)
    
    private var currentSearchLocators = mutableListOf<Locator>()
    private var currentSearchIndex = -1
    private var highlightsJob: kotlinx.coroutines.Job? = null
    
    // Reading progress tracking
    private var totalSpineItems = 0
    private var currentSpineIndex = 0

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
        readerTitle = view.findViewById(R.id.reader_title)
        readerChapter = view.findViewById(R.id.reader_chapter)
        btnBack = view.findViewById(R.id.btn_back)
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

        // Set up reading progress tracking
        totalSpineItems = publication.readingOrder.size
        currentSpineIndex = 0
        updateProgressUI()

        readerTitle.text = publication.metadata.title

        setupToolbar()
        setupSearchNavBar()
        applyInsets(view)

        val settings = viewModel.settings.value
        val startIndex = if (publication.readingOrder.size > 1) 1 else 0
        val initialLink = publication.readingOrder[startIndex]
        val initialLocator = publication.locatorFromLink(initialLink) ?: return view

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
            
            LaunchedEffect(isTtsActive, isTtsPlaying, currentSentenceIndex, currentFullText, ttsSentences.size, currentSpineIndex) {
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
        isProgressVisible = true
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
            }
        )
        val navigatorFragment = fragmentFactory.instantiate(requireContext().classLoader, EpubNavigatorFragment::class.java.name) as EpubNavigatorFragment
        this.navigator = navigatorFragment
        childFragmentManager.beginTransaction().replace(R.id.fragment_reader_container, navigatorFragment).commit()
        
        viewLifecycleOwner.lifecycleScope.launch {
            navigatorFragment.currentLocator.collect { locator ->
                updateReadingProgress(locator)
            }
        }
    }

    private fun createEpubPreferences(settings: ReaderSettings, forceScrollMode: Boolean = false): EpubPreferences {
        val isScrollMode = forceScrollMode || settings.scrollMode
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
            readingProgression = ReadingProgression.LTR,
            verticalText = false
        )
    }

    private fun updateReadingProgress(locator: Locator) {
        val publication = ReaderRepository.currentPublication ?: return
        val href = locator.href.toString()
        
        // Find current spine index
        val spineIndex = publication.readingOrder.indexOfFirst { it.href.toString() == href }
        if (spineIndex >= 0) {
            currentSpineIndex = spineIndex
        }
        
        // Calculate overall progress
        val progress = if (totalSpineItems > 0) {
            ((currentSpineIndex.toFloat() / totalSpineItems) * 100).toInt()
        } else {
            0
        }
        
        activity?.runOnUiThread {
            updateProgressUI()
        }
    }

    private fun updateProgressUI() {
        val progress = if (totalSpineItems > 0) {
            ((currentSpineIndex.toFloat() / totalSpineItems) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        
        progressProgress = progress / 100f
        showNextChapter = currentSpineIndex >= totalSpineItems - 1
        
        // Update chapter subtitle in top bar
        val chapterTitle = "Chapter ${currentSpineIndex + 1}"
        readerChapter.text = chapterTitle
    }

    private fun setupProgressBar() {
        progressComposeView.setContent {
            val animatedProgress by animateFloatAsState(
                targetValue = progressProgress,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "progress"
            )
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isProgressVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "alpha"
            )
            
            val bottomPadding = if (isProgressVisible && bottomBar.visibility == View.VISIBLE) 80.dp else 16.dp
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = bottomPadding)
                    .graphicsLayer { alpha = animatedAlpha }
            ) {
                if (showNextChapter && isProgressVisible) {
                    Button(
                        onClick = {
                            if (currentSpineIndex < totalSpineItems - 1) {
                                lifecycleScope.launch {
                                    navigator?.goForward(animated = true)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE50914)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentSpineIndex < totalSpineItems - 1) "Next Chapter" else "Last Chapter",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFFE50914),
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
            }
        }
    }

    override fun onDestroy() {
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
        btnBack.setOnClickListener { activity?.finish() }
        btnBookmarkStar.setOnClickListener { bookmarkCurrentLocation() }
        btnHighlight.setOnClickListener {
            lifecycleScope.launch {
                val selection = navigator?.currentSelection()
                if (selection != null) showSelectionMenu(selection.locator)
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

    fun toggleBars() {
        if (!::topBar.isInitialized || !::bottomBar.isInitialized || isTtsActive) return
        if (topBar.visibility == View.VISIBLE) {
            topBar.visibility = View.GONE
            bottomBar.visibility = View.GONE
            isProgressVisible = false
        } else {
            topBar.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
            isProgressVisible = true
            topBar.bringToFront()
            bottomBar.bringToFront()
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
                                Text("Highlight", style = MaterialTheme.typography.titleMedium)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    colors.forEach { colorInt -> Box(modifier = Modifier.size(40.dp).background(Color(colorInt), CircleShape).selectable(selected = false, onClick = { highlightSelection(locator, colorInt); selectionComposeView.visibility = View.GONE; navigator?.clearSelection() })) }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = { showTagInput = true }) { Text("Add Note") }
                                    TextButton(onClick = { Toast.makeText(context, "Lookup: ${locator.text.highlight}", Toast.LENGTH_SHORT).show() }) { Text("Lookup") }
                                }
                            } else {
                                OutlinedTextField(value = tagText, onValueChange = { tagText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Note...") })
                                Button(onClick = { highlightSelection(locator, 0x8800AAFF.toInt(), tagText); selectionComposeView.visibility = View.GONE; navigator?.clearSelection() }) { Text("Save") }
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
        val btnHighlights = dialogView.findViewById<Button>(R.id.btn_highlight)
        
        val toc = ReaderRepository.currentPublication?.tableOfContents ?: emptyList()
        
        fun highlightTab(selected: Button, others: List<Button>) {
            selected.setBackgroundColor(0xFF333333.toInt())
            others.forEach { it.setBackgroundColor(0xFF222222.toInt()) }
        }
        
        fun showChaptersList() {
            highlightTab(btnContents, listOf(btnBookmarks, btnHighlights))
            listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, toc.map { it.title ?: "Untitled" })
            listView.setOnItemClickListener { _: android.widget.AdapterView<*>, _: View, pos: Int, _: Long ->
                ReaderRepository.currentPublication?.locatorFromLink(toc[pos])?.let { navigator?.go(it, animated = true) }
            }
            listView.setOnItemLongClickListener(null)
        }
        
        fun showBookmarksList() {
            highlightTab(btnBookmarks, listOf(btnContents, btnHighlights))
            lifecycleScope.launch {
                viewModel.getBookmarks(bookId).collectLatest { bookmarks ->
                    if (bookmarks.isEmpty()) {
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listOf("No bookmarks yet"))
                        listView.setOnItemClickListener(null)
                    } else {
                        val bookmarkTitles = bookmarks.map { bookmark ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(bookmark.locator))
                                locator?.text?.highlight?.take(50)?.let { "\"$it...\"" } ?: "Bookmark ${bookmark.id}"
                            } catch (e: Exception) {
                                "Bookmark ${bookmark.id}"
                            }
                        }
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, bookmarkTitles)
                        listView.setOnItemClickListener { _: android.widget.AdapterView<*>, _: View, pos: Int, _: Long ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(bookmarks[pos].locator))
                                locator?.let { navigator?.go(it, animated = true) }
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
            highlightTab(btnHighlights, listOf(btnContents, btnBookmarks))
            lifecycleScope.launch {
                viewModel.getHighlights(bookId).collectLatest { highlights ->
                    if (highlights.isEmpty()) {
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listOf("No highlights yet"))
                        listView.setOnItemClickListener(null)
                    } else {
                        val highlightTitles = highlights.map { highlight ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(highlight.locator))
                                locator?.text?.highlight?.take(50)?.let { "\"$it...\"" } ?: "Highlight ${highlight.id}"
                            } catch (e: Exception) {
                                "Highlight ${highlight.id}"
                            }
                        }
                        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, highlightTitles)
                        listView.setOnItemClickListener { _: android.widget.AdapterView<*>, _: View, pos: Int, _: Long ->
                            try {
                                val locator = org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(highlights[pos].locator))
                                locator?.let { navigator?.go(it, animated = true) }
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
        
        btnContents.setOnClickListener { showChaptersList() }
        btnBookmarks.setOnClickListener { showBookmarksList() }
        btnHighlights.setOnClickListener { showHighlightsList() }
        
        val dialog: android.app.AlertDialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .create()
        
        showChaptersList()
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
                            
                            Text("Font Size: ${settings.fontSize.toInt()}%", color = Color.LightGray)
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
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Vertical Scrolling", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                        Text("Always enabled for seamless reading", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Enabled",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
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
}
