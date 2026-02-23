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
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.util.Try

@OptIn(ExperimentalReadiumApi::class)
class ReaderFragment : Fragment() {

    private val viewModel: ReaderViewModel by viewModels()
    private var bookId: String = "unknown"
    private var navigator: EpubNavigatorFragment? = null
    
    private lateinit var topBar: View
    private lateinit var bottomBar: View
    private lateinit var btnBookmarkStar: ImageButton
    private lateinit var btnBookList: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var btnTts: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var readerTitle: TextView

    // Search UI
    private lateinit var searchNavBar: View
    private lateinit var btnPrevSearch: ImageButton
    private lateinit var btnNextSearch: ImageButton
    private lateinit var btnCloseSearch: ImageButton
    private lateinit var searchCountText: TextView
    
    private var currentSearchLocators = mutableListOf<Locator>()
    private var currentSearchIndex = -1

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
        btnBookList = view.findViewById(R.id.btn_book_list)
        btnSearch = view.findViewById(R.id.btn_search)
        btnTts = view.findViewById(R.id.btn_tts)

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

        val initialLocator = publication.locatorFromLink(publication.readingOrder.first()) ?: return view

        val factory = EpubNavigatorFactory(publication)
        val fragmentFactory = factory.createFragmentFactory(
            initialLocator = initialLocator,
            listener = object : EpubNavigatorFragment.Listener {
                override fun onExternalLinkActivated(url: AbsoluteUrl) {
                }
            },
            paginationListener = object : EpubNavigatorFragment.PaginationListener {
                override fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {
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
        
        observeHighlights()
        
        return view
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

        btnBookList.setOnClickListener {
            showBookListDialog()
        }

        btnSearch.setOnClickListener {
            showSearchDialog()
        }

        btnTts.setOnClickListener {
            toggleTts()
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
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, toc.map { it.title ?: "Untitled" })
                    listView.adapter = adapter
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
                            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, bookmarks.map { bookmark ->
                                val locator = try {
                                    Locator.fromJSON(org.json.JSONObject(bookmark.locator))
                                } catch (e: Exception) { null }
                                val chapter = locator?.title ?: "Chapter"
                                val progression = locator?.locations?.progression?.let { " (${(it * 100).toInt()}%)" } ?: ""
                                "$chapter$progression"
                            })
                            listView.adapter = adapter
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
                            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, highlights.map { highlight ->
                                val locator = try {
                                    Locator.fromJSON(org.json.JSONObject(highlight.locator))
                                } catch (e: Exception) { null }
                                val text = locator?.text?.highlight ?: "Highlighted text"
                                "\"$text\""
                            })
                            listView.adapter = adapter
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

    private fun highlightSelection(locator: Locator) {
        viewModel.insertHighlight(bookId, locator.toJSON().toString(), 0x8800AAFF.toInt())
        Toast.makeText(context, "Text highlighted", Toast.LENGTH_SHORT).show()
    }

    private fun observeHighlights() {
        lifecycleScope.launch {
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
                
                (navigator as? DecorableNavigator)?.applyDecorations(decorations, "highlights")
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
        Toast.makeText(context, "TTS feature coming soon!", Toast.LENGTH_SHORT).show()
    }
}
