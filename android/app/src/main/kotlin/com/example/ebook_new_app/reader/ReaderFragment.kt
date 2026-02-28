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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ebook_new_app.viewmodel.ReaderTheme
import com.example.ebook_new_app.viewmodel.ReaderSettings

import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import org.readium.r2.navigator.epub.EpubNavigatorFragment.Configuration

@OptIn(ExperimentalReadiumApi::class)
class ReaderFragment : Fragment(), TextToSpeech.OnInitListener {

    private val viewModel: ReaderViewModel by viewModels()
    private var bookId: String = "unknown"
    private var navigator: EpubNavigatorFragment? = null
    private var currentSelection: Locator? = null
    private var tts: TextToSpeech? = null
    private var isTtsSpeaking = false
    
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

    // Search UI
    private lateinit var searchNavBar: View
    private lateinit var btnPrevSearch: ImageButton
    private lateinit var btnNextSearch: ImageButton
    private lateinit var btnCloseSearch: ImageButton
    private lateinit var searchCountText: TextView
    
    private var currentSearchLocators = mutableListOf<Locator>()
    private var currentSearchIndex = -1
    private var highlightsJob: kotlinx.coroutines.Job? = null

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

        // Setup ViewTree owners for ComposeViews
        listOf(settingsComposeView, selectionComposeView).forEach { composeView ->
            composeView.setViewTreeLifecycleOwner(viewLifecycleOwner)
            composeView.setViewTreeViewModelStoreOwner(this)
            composeView.setViewTreeSavedStateRegistryOwner(this)
            composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        // Search UI init
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
        
        // Initialize TTS
        tts = TextToSpeech(requireContext(), this)
        
        return view
    }

    private fun setupNavigator(initialLocator: Locator, settings: ReaderSettings) {
        val publication = ReaderRepository.currentPublication ?: return
        
        val config = Configuration(
            selectionActionModeCallback = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    lifecycleScope.launch {
                        val selection = navigator?.currentSelection()
                        if (selection != null) {
                            showSelectionMenu(selection)
                        }
                    }
                    return false // Suppress default menu
                }

                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                    return false
                }

                override fun onDestroyActionMode(mode: ActionMode?) {}
            }
        )

        val factory = EpubNavigatorFactory(publication)
        val fragmentFactory = factory.createFragmentFactory(
            initialLocator = initialLocator,
            initialPreferences = createEpubPreferences(settings),
            configuration = config,
            listener = object : EpubNavigatorFragment.Listener {
                override fun onExternalLinkActivated(url: AbsoluteUrl) {
                }
            }
        )
        
        val navigatorFragment = fragmentFactory.instantiate(
            requireContext().classLoader, 
            EpubNavigatorFragment::class.java.name
        ) as EpubNavigatorFragment
        this.navigator = navigatorFragment

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_reader_container, navigatorFragment)
            .commit()
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    activity?.runOnUiThread {
                        isTtsSpeaking = false
                        btnTts.setImageResource(R.drawable.ic_tts)
                    }
                }
                override fun onError(utteranceId: String?) {
                    activity?.runOnUiThread {
                        isTtsSpeaking = false
                        btnTts.setImageResource(R.drawable.ic_tts)
                    }
                }
            })
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun applyInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            topBar.updatePadding(top = bars.top)
            bottomBar.updatePadding(bottom = bars.bottom)
            
            searchNavBar.updateLayoutParams<FrameLayout.LayoutParams> {
                bottomMargin = bars.bottom + (80 * resources.displayMetrics.density).toInt()
            }
            
            insets
        }
    }

    private fun setupToolbar() {
        btnBack.setOnClickListener {
            activity?.finish()
        }

        btnBookmarkStar.setOnClickListener {
            bookmarkCurrentLocation()
        }

        btnHighlight.setOnClickListener {
            val selection = currentSelection
            if (selection != null) {
                // If there's a selection, show the menu if not already visible
                // But the listener should handle this. This is for manual triggers.
                val publication = ReaderRepository.currentPublication
                if (publication != null) {
                    showSelectionMenu(Selection(locator = selection, rect = null))
                }
            } else {
                Toast.makeText(context, "Please select text first", Toast.LENGTH_SHORT).show()
            }
        }

        btnBookList.setOnClickListener {
            showBookListDialog()
        }

        btnSearch.setOnClickListener {
            showSearchDialog()
        }

        btnTts.setOnClickListener {
            toggleTts()
        }

        btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun setupSearchNavBar() {
        btnPrevSearch.setOnClickListener {
            if (currentSearchLocators.isNotEmpty()) {
                currentSearchIndex = if (currentSearchIndex <= 0) currentSearchLocators.size - 1 else currentSearchIndex - 1
                updateSearchNavigation()
            }
        }

        btnNextSearch.setOnClickListener {
            if (currentSearchLocators.isNotEmpty()) {
                currentSearchIndex = if (currentSearchIndex >= currentSearchLocators.size - 1) 0 else currentSearchIndex + 1
                updateSearchNavigation()
            }
        }

        btnCloseSearch.setOnClickListener {
            searchNavBar.visibility = View.GONE
            currentSearchLocators.clear()
            currentSearchIndex = -1
        }
    }

    private fun updateSearchNavigation() {
        if (currentSearchIndex >= 0 && currentSearchIndex < currentSearchLocators.size) {
            val locator = currentSearchLocators[currentSearchIndex]
            navigator?.go(locator, animated = true)
            searchCountText.text = "${currentSearchIndex + 1}/${currentSearchLocators.size}"
        }
    }

    fun toggleBars() {
        if (!::topBar.isInitialized || !::bottomBar.isInitialized) return
        
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

    private fun bookmarkCurrentLocation() {
        val locator = navigator?.currentLocator?.value ?: return
        viewModel.insertBookmark(bookId, locator.toJSON().toString())
        Toast.makeText(context, "Bookmark added", Toast.LENGTH_SHORT).show()
        
        btnBookmarkStar.setImageResource(R.drawable.ic_star) 
        btnBookmarkStar.postDelayed({
            if (isAdded) btnBookmarkStar.setImageResource(R.drawable.ic_star_border)
        }, 1000)
    }

    private fun showBookListDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_book_navigation, null)
        val btnTabContents = dialogView.findViewById<Button>(R.id.btn_tab_contents)
        val btnTabBookmarks = dialogView.findViewById<Button>(R.id.btn_tab_bookmarks)
        val btnTabHighlights = dialogView.findViewById<Button>(R.id.btn_tab_highlights)
        val listView = dialogView.findViewById<ListView>(R.id.list_navigation)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .show()

        var currentTab = 0 // 0: Contents, 1: Bookmarks, 2: Highlights

        fun createWhiteTextAdapter(items: List<String>): ListAdapter {
            return object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, items) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val textView = view.findViewById<TextView>(android.R.id.text1)
                    textView.setTextColor(0xFFFFFFFF.toInt()) // Force white text for dark dialog
                    return view
                }
            }
        }

        fun updateList() {
            // Reset all backgrounds
            btnTabContents.setBackgroundColor(0xFF222222.toInt())
            btnTabBookmarks.setBackgroundColor(0xFF222222.toInt())
            btnTabHighlights.setBackgroundColor(0xFF222222.toInt())

            when (currentTab) {
                0 -> {
                    btnTabContents.setBackgroundColor(0xFF444444.toInt())
                    val publication = ReaderRepository.currentPublication
                    val toc = publication?.tableOfContents ?: emptyList()
                    listView.adapter = createWhiteTextAdapter(toc.map { it.title ?: "Untitled" })
                    listView.setOnItemClickListener { _, _, position, _ ->
                        val link = toc[position]
                        val locator = publication?.locatorFromLink(link)
                        if (locator != null) {
                            navigator?.go(locator, animated = true)
                            topBar.visibility = View.GONE
                            bottomBar.visibility = View.GONE
                            dialog.dismiss()
                        }
                    }
                }
                1 -> {
                    btnTabBookmarks.setBackgroundColor(0xFF444444.toInt())
                    lifecycleScope.launch {
                        viewModel.getBookmarks(bookId).collectLatest { bookmarks ->
                            listView.adapter = createWhiteTextAdapter(bookmarks.map { bookmark ->
                                val locator = try {
                                    Locator.fromJSON(org.json.JSONObject(bookmark.locator))
                                } catch (e: Exception) { null }
                                val chapter = locator?.title ?: "Chapter"
                                val progression = locator?.locations?.progression?.let { " (${(it * 100).toInt()}%)" } ?: ""
                                "$chapter$progression"
                            })
                            listView.setOnItemClickListener { _, _, position, _ ->
                                val bookmark = bookmarks[position]
                                val locator = try {
                                    Locator.fromJSON(org.json.JSONObject(bookmark.locator))
                                } catch (e: Exception) { null }
                                if (locator != null) {
                                    navigator?.go(locator, animated = true)
                                    topBar.visibility = View.GONE
                                    bottomBar.visibility = View.GONE
                                    dialog.dismiss()
                                }
                            }
                        }
                    }
                }
                2 -> {
                    btnTabHighlights.setBackgroundColor(0xFF444444.toInt())
                    lifecycleScope.launch {
                        viewModel.getHighlights(bookId).collectLatest { highlights ->
                            listView.adapter = createWhiteTextAdapter(highlights.map { highlight ->
                                val locator = try {
                                    Locator.fromJSON(org.json.JSONObject(highlight.locator))
                                } catch (e: Exception) { null }
                                val text = locator?.text?.highlight ?: "Highlighted text"
                                "\"$text\""
                            })
                            listView.setOnItemClickListener { _, _, position, _ ->
                                val highlight = highlights[position]
                                val locator = try {
                                    Locator.fromJSON(org.json.JSONObject(highlight.locator))
                                } catch (e: Exception) { null }
                                if (locator != null) {
                                    navigator?.go(locator, animated = true)
                                    topBar.visibility = View.GONE
                                    bottomBar.visibility = View.GONE
                                    dialog.dismiss()
                                }
                            }
                        }
                    }
                }
            }
        }

        btnTabContents.setOnClickListener { currentTab = 0; updateList() }
        btnTabBookmarks.setOnClickListener { currentTab = 1; updateList() }
        btnTabHighlights.setOnClickListener { currentTab = 2; updateList() }

        updateList()
    }

    private fun highlightSelection(locator: Locator, color: Int = 0x8800AAFF.toInt(), annotation: String? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            val existingHighlights = viewModel.getHighlightsSync(bookId)
            val existing = existingHighlights.find { 
                it.locator == locator.toJSON().toString()
            }

            if (existing != null) {
                if (existing.color == color && annotation == existing.annotation) {
                    viewModel.deleteHighlight(existing)
                    Toast.makeText(context, "Highlight removed", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.deleteHighlight(existing)
                    viewModel.insertHighlight(bookId, locator.toJSON().toString(), color, annotation)
                    Toast.makeText(context, "Highlight updated", Toast.LENGTH_SHORT).show()
                }
            } else {
                viewModel.insertHighlight(bookId, locator.toJSON().toString(), color, annotation)
                Toast.makeText(context, "Text highlighted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSelectionMenu(selection: Selection) {
        selectionComposeView.visibility = View.VISIBLE
        selectionComposeView.setContent {
            MaterialTheme {
                val colors = listOf(
                    0x88FFFF00.toInt(), // Yellow
                    0x8800FF00.toInt(), // Green
                    0x8800AAFF.toInt(), // Blue
                    0x88FF00FF.toInt(), // Pink
                    0x88FF0000.toInt()  // Red
                )
                
                var showTagInput by remember { mutableStateOf(false) }
                var tagText by remember { mutableStateOf("") }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Transparent background to catch clicks outside
                    Surface(
                        modifier = Modifier.fillMaxSize().selectable(selected = true, onClick = { 
                            selectionComposeView.visibility = View.GONE 
                            navigator?.clearSelection()
                        }),
                        color = Color.Transparent
                    ) {}

                    // Menu content
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (!showTagInput) {
                                Text("Highlight", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    colors.forEach { colorInt ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(colorInt), CircleShape)
                                                .selectable(selected = false, onClick = {
                                                    highlightSelection(selection.locator, colorInt)
                                                    selectionComposeView.visibility = View.GONE
                                                    navigator?.clearSelection()
                                                })
                                        )
                                    }
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = { showTagInput = true }) {
                                        Text("Add Tag/Note")
                                    }
                                    TextButton(onClick = {
                                        Toast.makeText(context, "Lookup: ${selection.locator.text.highlight}", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Lookup")
                                    }
                                }
                            } else {
                                Text("Add Tag/Note", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(
                                    value = tagText,
                                    onValueChange = { tagText = it },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    placeholder = { Text("Enter tag or note...") }
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showTagInput = false }) {
                                        Text("Back")
                                    }
                                    Button(onClick = {
                                        highlightSelection(selection.locator, 0x8800AAFF.toInt(), tagText)
                                        selectionComposeView.visibility = View.GONE
                                        navigator?.clearSelection()
                                    }) {
                                        Text("Save")
                                    }
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
                val decorations = highlights.map { highlight ->
                    val locator = try {
                        Locator.fromJSON(org.json.JSONObject(highlight.locator))
                    } catch (e: Exception) {
                        null
                    } ?: return@map null
                    
                    Decoration(
                        id = highlight.id.toString(),
                        locator = locator,
                        style = Decoration.Style.Highlight(tint = highlight.color)
                    )
                }.filterNotNull()
                
                val currentNav = navigator
                if (currentNav != null && currentNav.isAdded && !currentNav.isDetached) {
                    (currentNav as? DecorableNavigator)?.applyDecorations(decorations, "highlights")
                }
            }
        }
    }

    private fun showSearchDialog() {
        val input = EditText(context)
        input.setHint("Enter text to search...")
        AlertDialog.Builder(context)
            .setTitle("Search Book")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performSearch(query: String) {
        val publication = ReaderRepository.currentPublication ?: return
        lifecycleScope.launch {
            try {
                val iterator = publication.search(query)
                if (iterator != null) {
                    currentSearchLocators.clear()
                    
                    var resultTry = iterator.next()
                    while (resultTry is Try.Success<*, *>) {
                        val collection = resultTry.value as? LocatorCollection
                        if (collection == null || collection.locators.isEmpty()) break
                        currentSearchLocators.addAll(collection.locators)
                        resultTry = iterator.next()
                    }
                    
                    if (currentSearchLocators.isNotEmpty()) {
                        currentSearchIndex = 0
                        searchNavBar.visibility = View.VISIBLE
                        searchNavBar.bringToFront()
                        updateSearchNavigation()
                        topBar.visibility = View.GONE
                        bottomBar.visibility = View.GONE
                    } else {
                        Toast.makeText(context, "No results found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Search not supported", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Search error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleTts() {
        if (isTtsSpeaking) {
            tts?.stop()
            isTtsSpeaking = false
            btnTts.setImageResource(R.drawable.ic_tts)
            return
        }

        val textToSpeak = currentSelection?.text?.highlight 
            ?: navigator?.currentLocator?.value?.text?.highlight
            ?: ""

        if (textToSpeak.isNotEmpty()) {
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "ReaderTTS")
            isTtsSpeaking = true
            // Use a pause icon if available, otherwise just keep same or change color
            btnTts.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            Toast.makeText(context, "No text found to read. Try selecting some text first.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSettingsDialog() {
        settingsComposeView.visibility = View.VISIBLE
        settingsComposeView.setContent {
            MaterialTheme {
                val settings by viewModel.settings.collectAsState()
                
                // Full screen box to handle background dim and click-to-close
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Dimmed background
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .selectable(selected = true, onClick = { settingsComposeView.visibility = View.GONE }),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {}

                    // Settings Panel (Bottom)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Reading Settings", style = MaterialTheme.typography.titleLarge)
                                IconButton(onClick = { settingsComposeView.visibility = View.GONE }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Font Size: ${settings.fontSize.toInt()}%")
                            Slider(
                                value = settings.fontSize.toFloat(),
                                onValueChange = {
                                    val newSettings = settings.copy(fontSize = it.toDouble())
                                    viewModel.updateSettings(newSettings)
                                    recreateNavigator(newSettings)
                                },
                                valueRange = 50f..300f,
                                steps = 10
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Theme")
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ReaderTheme.entries.forEach { theme ->
                                    val (chipBgColor, chipTextColor) = when(theme) {
                                        ReaderTheme.WHITE -> Color.White to Color.Black
                                        ReaderTheme.DARK -> Color.Black to Color.White
                                        ReaderTheme.SEPIA -> Color(0xFFF4ECD8) to Color(0xFF5B4636)
                                    }

                                    FilterChip(
                                        selected = settings.theme == theme,
                                        onClick = {
                                            val newSettings = settings.copy(theme = theme)
                                            viewModel.updateSettings(newSettings)
                                            recreateNavigator(newSettings)
                                        },
                                        label = { Text(theme.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = chipBgColor,
                                            labelColor = chipTextColor,
                                            selectedContainerColor = chipBgColor,
                                            selectedLabelColor = chipTextColor
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = settings.theme == theme,
                                            borderColor = if (settings.theme == theme) MaterialTheme.colorScheme.primary else Color.Gray,
                                            borderWidth = if (settings.theme == theme) 2.dp else 1.dp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Scroll Mode", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        if (settings.scrollMode) "Vertical Scrolling" else "Side-to-Side Paging",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.scrollMode,
                                    onCheckedChange = {
                                        val newSettings = settings.copy(scrollMode = it)
                                        viewModel.updateSettings(newSettings)
                                        recreateNavigator(newSettings)
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    private fun recreateNavigator(settings: ReaderSettings) {
        val currentLocator = navigator?.currentLocator?.value ?: return
        setupNavigator(currentLocator, settings)
    }

    fun isScrollMode(): Boolean {
        return viewModel.settings.value.scrollMode
    }
}
