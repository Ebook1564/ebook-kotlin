package com.example.ebook_new_app.reader

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.ebook_new_app.R
import com.example.ebook_new_app.db.Bookmark
import com.example.ebook_new_app.db.Highlight
import com.example.ebook_new_app.viewmodel.ReaderViewModel
import com.example.ebook_new_app.repository.ReaderSettingsRepository
import com.example.ebook_new_app.utils.EpubPreviewUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
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
import java.util.Locale

import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.darkColorScheme
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ebook_new_app.viewmodel.ReaderTheme
import com.example.ebook_new_app.viewmodel.ReaderSettings

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
    
    private var currentSearchLocators = mutableListOf<Locator>()
    private var currentSearchIndex = -1
    private var highlightsJob: kotlinx.coroutines.Job? = null

    // TTS Compose State
    private var isTtsActive by mutableStateOf(false)
    private var isTtsPlaying by mutableStateOf(false)

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
        
        // Add TTS control view to layout
        ttsControlComposeView = ComposeView(requireContext()).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM
            )
            visibility = View.GONE
        }
        (view as ViewGroup).addView(ttsControlComposeView)

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

        readerTitle.text = publication.metadata.title

        setupToolbar()
        setupSearchNavBar()
        applyInsets(view)

        val settings = viewModel.settings.value
        val initialLocator = publication.locatorFromLink(publication.readingOrder.first()) ?: return view

        setupNavigator(initialLocator, settings)
        observeHighlights()
        
        return view
    }

    private fun setupTtsControls() {
        ttsControlComposeView.setContent {
            if (isTtsActive) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Nav previous */ }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null)
                        }
                        
                        LargeFloatingActionButton(
                            onClick = { togglePlayPauseTts() },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                if (isTtsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(onClick = { /* Nav next */ }) {
                            Icon(Icons.Default.SkipNext, contentDescription = null)
                        }

                        IconButton(onClick = { stopTts() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close TTS")
                        }
                    }
                }
            }
        }
    }

    private fun togglePlayPauseTts() {
        if (isTtsPlaying) {
            tts?.stop()
            isTtsPlaying = false
        } else {
            speakCurrentPage()
        }
    }

    private fun stopTts() {
        tts?.stop()
        isTtsActive = false
        isTtsPlaying = false
        ttsControlComposeView.visibility = View.GONE
    }

    private fun speakCurrentPage() {
        if (tts == null) {
            tts = TextToSpeech(requireContext().applicationContext, this)
            return
        }
        if (!isTtsInitialized) return

        lifecycleScope.launch {
            val publication = ReaderRepository.currentPublication ?: return@launch
            val locator = navigator?.currentLocator?.value ?: return@launch
            
            var textToSpeak = locator.text.highlight ?: ""
            
            if (textToSpeak.isEmpty()) {
                val resource = publication.get(locator.href)
                val content = resource?.read()?.getOrNull()
                if (content != null) {
                    val html = String(content, Charsets.UTF_8)
                    textToSpeak = parseHtmlToTextSimple(html)
                }
            }
            
            if (textToSpeak.isNotEmpty()) {
                tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "ReaderTTS")
                isTtsPlaying = true
            } else {
                Toast.makeText(context, "No text found to read on this page.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun parseHtmlToTextSimple(html: String): String {
        return html.replace("<style[^>]*>.*?</style>".toRegex(RegexOption.DOT_MATCHES_ALL), "")
            .replace("<script[^>]*>.*?</script>".toRegex(RegexOption.DOT_MATCHES_ALL), "")
            .replace("<[^>]+>".toRegex(), " ")
            .replace("&nbsp;", " ")
            .replace("\\s+".toRegex(), " ").trim()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            tts?.language = Locale.US
            speakCurrentPage()
        }
    }

    private fun setupNavigator(initialLocator: Locator, settings: ReaderSettings) {
        val publication = ReaderRepository.currentPublication ?: return
        val config = EpubConfig(
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
        val factory = EpubNavigatorFactory(publication)
        val fragmentFactory = factory.createFragmentFactory(
            initialLocator = initialLocator,
            initialPreferences = createEpubPreferences(settings),
            configuration = config,
            listener = object : EpubNavigatorFragment.Listener {
                override fun onExternalLinkActivated(url: AbsoluteUrl) {}
            }
        )
        val navigatorFragment = fragmentFactory.instantiate(requireContext().classLoader, EpubNavigatorFragment::class.java.name) as EpubNavigatorFragment
        this.navigator = navigatorFragment
        childFragmentManager.beginTransaction().replace(R.id.fragment_reader_container, navigatorFragment).commit()
    }

    private fun createEpubPreferences(settings: ReaderSettings): EpubPreferences {
        return EpubPreferences(
            fontSize = settings.fontSize / 100.0,
            theme = when(settings.theme) {
                ReaderTheme.WHITE -> Theme.LIGHT
                ReaderTheme.DARK -> Theme.DARK
                ReaderTheme.SEPIA -> Theme.SEPIA
            },
            scroll = settings.scrollMode,
            columnCount = if (settings.scrollMode) ColumnCount.ONE else null,
            spread = if (settings.scrollMode) Spread.NEVER else null,
            readingProgression = ReadingProgression.LTR,
            verticalText = false
        )
    }

    override fun onDestroy() {
        try { tts?.stop(); tts?.shutdown() } catch (e: Exception) {}
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
        } else {
            topBar.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
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
                val decorations = highlights.map { highlight ->
                    val locator = try { Locator.fromJSON(org.json.JSONObject(highlight.locator)) } catch (e: Exception) { null } ?: return@map null
                    Decoration(id = highlight.id.toString(), locator = locator, style = Decoration.Style.Highlight(tint = highlight.color))
                }.filterNotNull()
                (navigator as? DecorableNavigator)?.applyDecorations(decorations, "highlights")
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
        btnBookmarkStar.setImageResource(R.drawable.ic_star)
        btnBookmarkStar.postDelayed({ if (isAdded) btnBookmarkStar.setImageResource(R.drawable.ic_star_border) }, 1000)
    }

    private fun showBookListDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_book_navigation, null)
        val listView = dialogView.findViewById<ListView>(R.id.list_navigation)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).show()
        val toc = ReaderRepository.currentPublication?.tableOfContents ?: emptyList()
        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, toc.map { it.title ?: "Untitled" })
        listView.setOnItemClickListener { _, _, pos, _ ->
            ReaderRepository.currentPublication?.locatorFromLink(toc[pos])?.let { navigator?.go(it, animated = true) }
            dialog.dismiss()
        }
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
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Vertical Scrolling", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                    Text("Enable continuous scroll mode", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Switch(
                                    checked = settings.scrollMode,
                                    onCheckedChange = {
                                        val ns = settings.copy(scrollMode = it)
                                        viewModel.updateSettings(ns)
                                        recreateNavigator(ns)
                                    }
                                )
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

    private fun recreateNavigator(settings: ReaderSettings) { navigator?.currentLocator?.value?.let { setupNavigator(it, settings) } }
    fun isScrollMode(): Boolean = viewModel.settings.value.scrollMode
}
