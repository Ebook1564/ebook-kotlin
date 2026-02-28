package com.example.ebookapp.reader

import android.app.AlertDialog
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ebookapp.R
import com.example.ebookapp.viewmodel.ReaderViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import java.util.*

@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class)
class ReaderFragment : Fragment(), TextToSpeech.OnInitListener {

    private val viewModel: ReaderViewModel by viewModels()
    private var bookId: String = "unknown"
    private var navigator: EpubNavigatorFragment? = null
    private var tts: TextToSpeech? = null
    private var isTtsSpeaking = false

    private lateinit var topBar: View
    private lateinit var bottomBar: View
    private lateinit var readerTitle: TextView
    private lateinit var settingsComposeView: ComposeView
    private lateinit var selectionComposeView: ComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reader_modern, container, false)

        bookId = arguments?.getString("BOOK_ID") ?: "unknown"
        val publication = ReaderRepository.currentPublication ?: return view

        topBar = view.findViewById(R.id.reader_top_bar)
        bottomBar = view.findViewById(R.id.reader_bottom_bar)
        readerTitle = view.findViewById(R.id.reader_title)
        settingsComposeView = view.findViewById(R.id.settings_compose_view)
        selectionComposeView = view.findViewById(R.id.selection_compose_view)

        listOf(settingsComposeView, selectionComposeView).forEach { composeView ->
            composeView.setViewTreeLifecycleOwner(viewLifecycleOwner)
            composeView.setViewTreeViewModelStoreOwner(this)
            composeView.setViewTreeSavedStateRegistryOwner(this)
            composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        readerTitle.text = publication.metadata.title

        setupToolbar(view)
        applyInsets(view)

        val settings = viewModel.settings.value
        val initialLocator = publication.locatorFromLink(publication.readingOrder.first())

        if (initialLocator != null) {
            setupNavigator(initialLocator, settings)
        }

        observeHighlights()
        observeCurrentLocator()

        tts = TextToSpeech(requireContext(), this)

        return view
    }

    private fun setupNavigator(initialLocator: Locator, settings: com.example.ebookapp.viewmodel.ReaderSettings) {
        val publication = ReaderRepository.currentPublication ?: return

        val config = EpubNavigatorFragment.Configuration(
            selectionActionModeCallback = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = true
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    lifecycleScope.launch {
                        navigator?.currentSelection()?.let { showSelectionMenu(it.locator) }
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

        val navigatorFragment = fragmentFactory.instantiate(
            requireContext().classLoader,
            EpubNavigatorFragment::class.java.name
        ) as EpubNavigatorFragment
        this.navigator = navigatorFragment

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_reader_container, navigatorFragment)
            .commit()
    }

    private fun createEpubPreferences(settings: com.example.ebookapp.viewmodel.ReaderSettings): EpubPreferences {
        return EpubPreferences(
            fontSize = settings.fontSize / 100.0,
            theme = when (settings.theme) {
                com.example.ebookapp.viewmodel.ReaderTheme.WHITE -> Theme.LIGHT
                com.example.ebookapp.viewmodel.ReaderTheme.DARK -> Theme.DARK
                com.example.ebookapp.viewmodel.ReaderTheme.SEPIA -> Theme.SEPIA
            },
            scroll = settings.scrollMode,
            columnCount = if (settings.scrollMode) ColumnCount.ONE else null,
            spread = if (settings.scrollMode) Spread.NEVER else null,
            readingProgression = ReadingProgression.LTR
        )
    }

    fun toggleBars() {
        if (!::topBar.isInitialized || !::bottomBar.isInitialized) return

        val isVisible = topBar.visibility == View.VISIBLE
        topBar.visibility = if (isVisible) View.GONE else View.VISIBLE
        bottomBar.visibility = if (isVisible) View.GONE else View.VISIBLE
        if (!isVisible) {
            topBar.bringToFront()
            bottomBar.bringToFront()
        }
    }

    private fun setupToolbar(view: View) {
        view.findViewById<View>(R.id.btn_back).setOnClickListener { activity?.finish() }
        view.findViewById<View>(R.id.btn_toc).setOnClickListener { showTOC() }
        view.findViewById<View>(R.id.btn_search).setOnClickListener { showSearchDialog() }
        view.findViewById<View>(R.id.btn_settings).setOnClickListener { showSettingsMenu() }
        view.findViewById<View>(R.id.btn_bookmark).setOnClickListener { bookmarkCurrentLocation() }
    }

    private fun showTOC() {
        settingsComposeView.visibility = View.VISIBLE
        settingsComposeView.setContent {
            ModalBottomSheet(onDismissRequest = { settingsComposeView.visibility = View.GONE }) {
                val publication = ReaderRepository.currentPublication
                val toc = publication?.tableOfContents ?: emptyList()

                Column {
                    Text("Table of Contents", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                    LazyColumn {
                        items(toc) { link ->
                            Text(
                                text = link.title ?: "Untitled Chapter",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        publication
                                            ?.locatorFromLink(link)
                                            ?.let { navigator?.go(it, animated = true) }
                                        settingsComposeView.visibility = View.GONE
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showSearchDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Search..."
            maxLines = 1
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Search Book")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString()
                if (query.isNotBlank()) performSearch(query)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performSearch(query: String) {
        val publication = ReaderRepository.currentPublication ?: return
        lifecycleScope.launch {
            val iterator = publication.search(query)
            val results = mutableListOf<Locator>()
            var result = iterator.next().getOrNull()
            while (result != null) {
                results.addAll(result.locators)
                result = iterator.next().getOrNull()
            }
            if (results.isNotEmpty()) {
                // For simplicity, just go to the first result.
                // A real app would show a list of results.
                navigator?.go(results.first(), animated = true)
            } else {
                Toast.makeText(context, "No results found", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun bookmarkCurrentLocation() {
        val locator = navigator?.currentLocator?.value ?: return
        viewModel.insertBookmark(bookId, locator.toJSON().toString())
        Toast.makeText(context, "Bookmark added", Toast.LENGTH_SHORT).show()
    }

    private fun showSettingsMenu() {
        settingsComposeView.visibility = View.VISIBLE
        settingsComposeView.setContent {
            val settings by viewModel.settings.collectAsState()
            ModalBottomSheet(onDismissRequest = { settingsComposeView.visibility = View.GONE }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Font Size: ${settings.fontSize.toInt()}%")
                    Slider(
                        value = settings.fontSize.toFloat(),
                        onValueChange = {
                            val newSettings = settings.copy(fontSize = it.toDouble())
                            viewModel.updateSettings(newSettings)
                            recreateNavigator(newSettings)
                        },
                        valueRange = 50f..300f
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Theme")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.example.ebookapp.viewmodel.ReaderTheme.entries.forEach { theme ->
                            Chip(theme.name, settings.theme == theme) {
                                val newSettings = settings.copy(theme = theme)
                                viewModel.updateSettings(newSettings)
                                recreateNavigator(newSettings)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scroll Mode")
                        Switch(
                            checked = settings.scrollMode,
                            onCheckedChange = {
                                val newSettings = settings.copy(scrollMode = it)
                                viewModel.updateSettings(newSettings)
                                recreateNavigator(newSettings)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
        Surface(
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Text(
                text = text,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }


    private fun showSelectionMenu(locator: Locator) {
        selectionComposeView.visibility = View.VISIBLE
        selectionComposeView.setContent {
            ModalBottomSheet(onDismissRequest = {
                selectionComposeView.visibility = View.GONE
                navigator?.clearSelection()
            }) {
                val highlightColors = listOf(
                    Color.Yellow.copy(alpha = 0.5f),
                    Color.Green.copy(alpha = 0.5f),
                    Color.Blue.copy(alpha = 0.5f),
                    Color.Red.copy(alpha = 0.5f)
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Highlight", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        highlightColors.forEach { color ->
                            Box(modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    highlightSelection(locator, color.value.toInt())
                                    selectionComposeView.visibility = View.GONE
                                    navigator?.clearSelection()
                                }
                            )
                        }
                    }
                    Button(onClick = {
                        val text = locator.text.highlight ?: ""
                        if (text.isNotEmpty()) {
                            showLookupDialog(text)
                        }
                    }) {
                        Text("Look up")
                    }
                }
            }
        }
    }
    
    private fun showLookupDialog(term: String) {
        // Keeping the old lookup dialog for now as it's a good feature
        val view = ComposeView(requireContext())
        view.setContent {
             MaterialTheme {
                var summary by remember { mutableStateOf<String?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                var error by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(term) {
                    try {
                        val encodedTerm = java.net.URLEncoder.encode(term, "UTF-8")
                        val url = java.net.URL("https://en.wikipedia.org/api/rest_v1/page/summary/$encodedTerm")
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        if (connection.responseCode == 200) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            val json = org.json.JSONObject(response)
                            summary = json.optString("extract", "No summary available.")
                        } else {
                            error = "Could not find \"$term\" on Wikipedia."
                        }
                    } catch (e: Exception) {
                        error = "Network error."
                    } finally {
                        isLoading = false
                    }
                }
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text(term) },
                    text = {
                        if (isLoading) {
                            CircularProgressIndicator()
                        } else if (error != null) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(summary ?: "")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { 
                            // How to dismiss? Needs a state passed in.
                        }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
        
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        // How to close dialog from compose? Pass dialog instance into composable
        dialog.show()
    }


    private fun highlightSelection(locator: Locator, color: Int) {
        lifecycleScope.launch {
            viewModel.insertHighlight(bookId, locator.toJSON().toString(), color)
            Toast.makeText(context, "Highlight added", Toast.LENGTH_SHORT).show()
        }
    }


    private fun observeHighlights() {
        lifecycleScope.launch {
            viewModel.getHighlights(bookId).collectLatest { highlights ->
                val decorations = highlights.mapNotNull { highlight ->
                    try {
                        val locator = Locator.fromJSON(org.json.JSONObject(highlight.locator))
                        Decoration(
                            id = highlight.id.toString(),
                            locator = locator,
                            style = Decoration.Style.Highlight(tint = highlight.color)
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                (navigator as? DecorableNavigator)?.applyDecorations(decorations, "highlights")
            }
        }
    }

    private fun observeCurrentLocator() {
        lifecycleScope.launch {
            navigator?.currentLocator?.collectLatest { locator ->
                activity?.runOnUiThread {
                    readerTitle.text = locator.title ?: ReaderRepository.currentPublication?.metadata?.title
                }
            }
        }
    }

    private fun applyInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.updatePadding(top = bars.top)
            bottomBar.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun recreateNavigator(settings: com.example.ebookapp.viewmodel.ReaderSettings) {
        val currentLocator = navigator?.currentLocator?.value ?: return
        setupNavigator(currentLocator, settings)
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }
    
    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
    
    fun isScrollMode(): Boolean = viewModel.settings.value.scrollMode
}
