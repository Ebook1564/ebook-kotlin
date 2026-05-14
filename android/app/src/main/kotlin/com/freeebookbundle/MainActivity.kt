package com.freeebookbundle

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.compose.collectAsLazyPagingItems
import com.freeebookbundle.reader.ReaderActivity
import com.freeebookbundle.repository.ReaderSettingsRepository
import com.freeebookbundle.ui.*
import com.freeebookbundle.utils.Analytics
import com.freeebookbundle.utils.ReviewManager
import com.freeebookbundle.viewmodel.*
import androidx.compose.ui.res.stringResource
import com.freeebookbundle.R
import com.freeebookbundle.models.ReadingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

// PREMIUM CONSTANTS
val PremiumEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
const val PremiumDuration = 1400

class CircularRevealShape(private val radius: Float, private val center: Offset) : Shape {
    private val cachedPath = Path()
    private var cachedRadius = -1f
    private var cachedCenter = Offset.Unspecified

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (radius != cachedRadius || center != cachedCenter) {
            cachedPath.reset()
            cachedPath.addOval(Rect(center, radius))
            cachedRadius = radius
            cachedCenter = center
        }
        return Outline.Generic(cachedPath)
    }
}

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val appViewModel: AppViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            val themeMode by appViewModel.themeMode
            val previousTheme by appViewModel.previousThemeMode
            val transitionTrigger by appViewModel.themeTransitionTrigger
            val isTransitioning by appViewModel.isTransitioning
            val userProfile by appViewModel.userProfile.collectAsStateWithLifecycle()
            
            var showProfile by remember { mutableStateOf(false) }
            
            val view = this.window.decorView
            SideEffect {
                val window = this@MainActivity.window
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = (themeMode == AppThemeMode.DAY)
            }

            if (!userProfile.isOnboarded) {
                OnboardingScreen(
                    homeViewModel = homeViewModel,
                    onFinish = { name, theme ->
                        appViewModel.completeOnboarding(name)
                        appViewModel.setTheme(theme)
                    },
                    onSyncRequired = { true }
                )
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val context = LocalContext.current
                    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        val params = android.os.Bundle().apply {
                            putBoolean("granted", isGranted)
                        }
                        Analytics.logEvent("notification_permission_result", params)
                    }
                    
                    LaunchedEffect(Unit) {
                        val permission = android.Manifest.permission.POST_NOTIFICATIONS
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) 
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            launcher.launch(permission)
                        }
                    }
                }

                val pagerState = rememberPagerState(pageCount = { 3 })
                val homeScrollState = rememberLazyListState()
                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(homeUiState.selectedCategoryForSearch) {
                    homeUiState.selectedCategoryForSearch?.let {
                        pagerState.animateScrollToPage(1, animationSpec = tween(800, easing = PremiumEasing))
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (previousTheme != null) {
                        val dummyScrollState = rememberLazyListState(
                            initialFirstVisibleItemIndex = homeScrollState.firstVisibleItemIndex,
                            initialFirstVisibleItemScrollOffset = homeScrollState.firstVisibleItemScrollOffset
                        )
                        BookVerseTheme(previousTheme!!) {
                            StaticAppLayout(homeViewModel, appViewModel, libraryViewModel, pagerState.currentPage, dummyScrollState, userProfile.name, onProfileClick = { showProfile = true })
                        }
                    }

                    val config = LocalConfiguration.current
                    val density = LocalDensity.current
                    val maxRadius = hypot(with(density) { config.screenWidthDp.dp.toPx() }, with(density) { config.screenHeightDp.dp.toPx() })
                    val radius = remember(transitionTrigger) { Animatable(if (previousTheme != null) 0f else maxRadius * 1.5f) }
                    
                    LaunchedEffect(transitionTrigger) {
                        if (previousTheme != null) {
                            radius.animateTo(
                                targetValue = maxRadius * 1.5f, 
                                animationSpec = tween(PremiumDuration, easing = PremiumEasing)
                            )
                            appViewModel.endTransition()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                clip = true
                                shape = CircularRevealShape(radius.value, Offset(size.width - 40.dp.toPx(), 60.dp.toPx()))
                            }
                    ) {
                        BookVerseTheme(themeMode) {
                            BookVerseApp(
                                homeViewModel, 
                                appViewModel, 
                                libraryViewModel,
                                pagerState, 
                                homeScrollState, 
                                userProfile.name,
                                isInteractive = !isTransitioning,
                                onProfileClick = { showProfile = true }
                            )
                        }
                    }

                    if (isTransitioning) {
                        Box(modifier = Modifier.fillMaxSize().clickable(enabled = true, onClick = {}, interactionSource = remember { MutableInteractionSource() }, indication = null))
                    }
                    
                    AnimatedVisibility(
                        visible = showProfile,
                        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(700, easing = PremiumEasing)) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(700, easing = PremiumEasing)) + fadeOut()
                    ) {
                        BackHandler { showProfile = false }
                        val database = (androidx.compose.ui.platform.LocalContext.current.applicationContext as? EbookApp)?.database
                        ProfileScreen(userName = userProfile.name, appViewModel = appViewModel, database = database, onBackClick = { showProfile = false })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.endReadTransition()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val type = intent?.getStringExtra("notification_type") ?: return
        val bookId = intent.getStringExtra("book_id")
        val genre = intent.getStringExtra("genre")

        lifecycleScope.launch {
            when (type) {
                "reading_reminder", "book_update", "download_complete" -> {
                    bookId?.let { id ->
                        val repo = (application as EbookApp).catalogRepo
                        repo.getBookDetails(id)?.let { book ->
                            homeViewModel.startReadTransition(book, resumeFromLastPosition = true)
                        }
                    }
                }
                "new_book" -> {
                    bookId?.let { id ->
                        val repo = (application as EbookApp).catalogRepo
                        repo.getBookDetails(id)?.let { book ->
                            homeViewModel.startReadTransition(book)
                        }
                    }
                }
                "genre_update" -> {
                    genre?.let { g ->
                        homeViewModel.navigateToCategory(g)
                    }
                }
                "streak_milestone", "daily_goal" -> {
                    homeViewModel.closeCollection()
                }
                "book_finished" -> {
                    bookId?.let { id ->
                        val repo = (application as EbookApp).catalogRepo
                        repo.getBookDetails(id)?.let { book ->
                            homeViewModel.startReadTransition(book)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaticAppLayout(homeViewModel: HomeViewModel, appViewModel: AppViewModel, libraryViewModel: LibraryViewModel, currentPage: Int, homeScrollState: LazyListState, userName: String, onProfileClick: () -> Unit) {
    val themeMode = appViewModel.themeMode.value
    val isTransitioning = appViewModel.isTransitioning.value
    Box(modifier = Modifier.fillMaxSize().background(BookVerseTheme.colors.background)) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentPage) {
                0 -> HomeScreen(
                    viewModel = homeViewModel, 
                    appViewModel = appViewModel, 
                    scrollState = homeScrollState, 
                    userName = userName,
                    onBookClick = { homeViewModel.startReadTransition(it) },
                    onReadNowClick = { homeViewModel.startReadTransition(it) }
                )
                1 -> SearchScreen(viewModel = homeViewModel, onBookClick = { homeViewModel.startReadTransition(it) })
                2 -> LibraryScreen(viewModel = libraryViewModel, homeViewModel = homeViewModel, onProfileClick = onProfileClick, onBookClick = { homeViewModel.startReadTransition(it) })
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                AppNavigationBar(
                    currentPage = currentPage,
                    themeMode = themeMode,
                    useSpaceTouchNav = appViewModel.useSpaceTouchNav.value,
                    isTransitioning = isTransitioning,
                    isInteractive = true,
                    onTabSelected = { }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookVerseApp(
    homeViewModel: HomeViewModel, 
    appViewModel: AppViewModel, 
    libraryViewModel: LibraryViewModel, 
    pagerState: PagerState, 
    homeScrollState: LazyListState, 
    userName: String, 
    isInteractive: Boolean, 
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // UI state collection
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val libraryUiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    
    // Volatile settings
    val themeMode = appViewModel.themeMode.value
    val useSpaceTouchNav = appViewModel.useSpaceTouchNav.value
    val readerSettingsRepo = remember { ReaderSettingsRepository(context) }
    
    var selectedBookForSheet by remember { mutableStateOf<com.freeebookbundle.models.Book?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var isInspectingByShelf by remember { mutableStateOf(false) }

    // Optimization: Use derivedStateOf for complex conditions to avoid recomposition
    val shouldHideNavBar by remember {
        derivedStateOf {
            isInspectingByShelf || 
            libraryUiState.seeAllStatus != null || 
            homeUiState.currentCollection != null || 
            homeUiState.currentCuratedCollection != null
        }
    }

    fun onReadClick(book: com.freeebookbundle.models.Book, bounds: InitialBounds?, resume: Boolean = false, openAudio: Boolean = false) {
        showSheet = false
        homeViewModel.startReadTransition(book, bounds, resumeFromLastPosition = resume, openAudioPlayer = openAudio)
    }

    fun onBookClick(book: com.freeebookbundle.models.Book) {
        selectedBookForSheet = book
        showSheet = true
        homeViewModel.fetchMetadataForBook(book) { updatedBook ->
            if (selectedBookForSheet?.id == updatedBook.id) {
                selectedBookForSheet = updatedBook
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BookVerseTheme.colors.background)) {
        HorizontalPager(
            state = pagerState, 
            modifier = Modifier.fillMaxSize(), 
            beyondViewportPageCount = 1, 
            userScrollEnabled = isInteractive && !shouldHideNavBar && !showSheet
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    viewModel = homeViewModel, 
                    appViewModel = appViewModel, 
                    scrollState = homeScrollState,
                    userName = userName,
                    onBookClick = { onBookClick(it) },
                    onReadNowClick = { book -> homeViewModel.startReadTransition(book) }
                )
                1 -> SearchScreen(viewModel = homeViewModel, onBookClick = { onBookClick(it) })
                2 -> LibraryScreen(
                    viewModel = libraryViewModel, 
                    homeViewModel = homeViewModel,
                    onProfileClick = onProfileClick, 
                    onBookClick = { onBookClick(it) },
                    onInspectStateChange = { isInspectingByShelf = it }
                )
            }
        }
        
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            AppNavigationBar(
                currentPage = pagerState.currentPage,
                themeMode = themeMode,
                useSpaceTouchNav = useSpaceTouchNav,
                isTransitioning = appViewModel.isTransitioning.value,
                isVisible = !shouldHideNavBar,
                isInteractive = isInteractive,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index, animationSpec = tween(800, easing = PremiumEasing)) }
                }
            )
        }

        // Overlay Manager (extracted logic)
        ReaderOverlayManager(
            homeViewModel = homeViewModel,
            libraryViewModel = libraryViewModel,
            homeUiState = homeUiState,
            readerSettingsRepo = readerSettingsRepo,
            onBookClick = { onBookClick(it) },
            onReadClick = { book, bounds, resume, openAudio -> onReadClick(book, bounds, resume, openAudio) }
        )

        BookDetailSheet(
            book = selectedBookForSheet,
            isVisible = showSheet,
            isLoading = homeUiState.isMetadataLoading,
            onDismiss = { showSheet = false },
            onReadClick = { book, bounds, resume, openAudio -> onReadClick(book, bounds, resume, openAudio) },
            onCategoryClick = { tag -> showSheet = false; homeViewModel.openCollection(tag) },
            onAddToReadingList = { book, status -> libraryViewModel.updateReadingStatus(book, status) },
            onAuthorClick = { author ->
                showSheet = false
                homeViewModel.setAuthorFilter(author)
                if (isInteractive) scope.launch { pagerState.animateScrollToPage(1, animationSpec = tween(800, easing = PremiumEasing)) }
            }
        )
    }
}

@Composable
fun ReaderOverlayManager(
    homeViewModel: HomeViewModel,
    libraryViewModel: LibraryViewModel,
    homeUiState: HomeUiState,
    readerSettingsRepo: ReaderSettingsRepository,
    onBookClick: (com.freeebookbundle.models.Book) -> Unit,
    onReadClick: (com.freeebookbundle.models.Book, InitialBounds?, resume: Boolean, openAudio: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Collection Overlays
    val currentCollection = homeUiState.currentCollection
    AnimatedVisibility(
        visible = currentCollection != null,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(600, easing = PremiumEasing)) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(600, easing = PremiumEasing)) + fadeOut()
    ) {
        if (currentCollection != null) {
            val books = homeViewModel.curatedCollectionBooks.collectAsLazyPagingItems()
            BackHandler { homeViewModel.closeCollection() }
            CollectionPage(
                collection = currentCollection,
                books = books,
                onBack = { homeViewModel.closeCollection() },
                onBookClick = onBookClick
            )
        }
    }

    val currentCurated = homeUiState.currentCuratedCollection
    AnimatedVisibility(
        visible = currentCurated != null,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(600, easing = PremiumEasing)) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(600, easing = PremiumEasing)) + fadeOut()
    ) {
        if (currentCurated != null) {
            val books = homeViewModel.curatedCollectionBooksByTag.collectAsLazyPagingItems()
            BackHandler { homeViewModel.closeCollection() }
            CollectionPage(
                title = currentCurated.title,
                description = currentCurated.description,
                accentColor = currentCurated.accentColor,
                books = books,
                onBack = { homeViewModel.closeCollection() },
                onBookClick = onBookClick
            )
        }
    }

    // 2. Transition Overlay
    val transitionBook = homeUiState.transitionBook
    if (transitionBook != null) {
        BookTransitionOverlay(
            book = transitionBook, 
            initialBounds = homeUiState.initialBounds, 
            themeColor = readerSettingsRepo.getThemeColor(), 
            textColor = readerSettingsRepo.getTextColor(), 
            onAnimationFinished = { file ->
                scope.launch {
                    val dao = (context.applicationContext as EbookApp).database.bookDao()
                    val existing = dao.getBook(transitionBook.id)
                    if (existing == null || existing.readingStatus == ReadingStatus.NONE || existing.readingStatus == ReadingStatus.WISHLIST) {
                        libraryViewModel.updateReadingStatus(transitionBook, ReadingStatus.CURRENTLY_READING)
                    } else {
                        dao.updateLastOpened(transitionBook.id, System.currentTimeMillis())
                    }
                }
                Analytics.trackBookOpened(transitionBook.id, transitionBook.categories.firstOrNull() ?: "")
                ReviewManager.onBookOpened(context)
                if (ReviewManager.shouldRequestReview(context) && context is Activity) {
                    scope.launch { ReviewManager.requestReview(context) }
                }

                val intent = Intent(context, ReaderActivity::class.java).apply {
                    putExtra("FILE_PATH", file.absolutePath)
                    putExtra("BOOK", transitionBook)
                    putExtra("OPEN_AUDIO_PLAYER", homeUiState.openAudioPlayer)
                }
                context.startActivity(intent, ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle())
            }, 
            onDismiss = { homeViewModel.endReadTransition() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailSheet(
    book: com.freeebookbundle.models.Book?,
    isVisible: Boolean,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onReadClick: (com.freeebookbundle.models.Book, InitialBounds?, resume: Boolean, openAudio: Boolean) -> Unit,
    onCategoryClick: (String) -> Unit,
    onAddToReadingList: (com.freeebookbundle.models.Book, ReadingStatus) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    if (!isVisible || book == null) return
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent, 
        contentColor = BookVerseTheme.colors.textPrimary,
        dragHandle = null 
    ) {
        BookDetailContent(
            book = book,
            isLoading = isLoading,
            onReadClick = { bounds, resume -> onReadClick(book, bounds, resume, false) },
            onCategoryClick = onCategoryClick,
            onAddToReadingList = { status -> onAddToReadingList(book, status) },
            onClose = onDismiss,
            onAuthorClick = onAuthorClick,
            onAudioClick = { b -> onReadClick(b, null, false, true) }
        )
    }
}

@Composable
fun AppNavigationBar(
    currentPage: Int,
    themeMode: AppThemeMode,
    useSpaceTouchNav: Boolean,
    isTransitioning: Boolean,
    isVisible: Boolean = true,
    isInteractive: Boolean = true,
    onTabSelected: (Int) -> Unit
) {
    AnimatedVisibility(
        visible = isVisible && !isTransitioning,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400, easing = PremiumEasing)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400, easing = PremiumEasing))
    ) {
        if (themeMode == AppThemeMode.SPACE && useSpaceTouchNav) {
            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding().zIndex(1f)) {
                SpaceEasyTouchNav(selectedIndex = currentPage, onTabSelected = { if (isInteractive) onTabSelected(it) })
            }
        } else if (themeMode == AppThemeMode.SPACE) {
            SpaceElegantNavBar(currentPage) { if (isInteractive) onTabSelected(it) }
        } else if (themeMode == AppThemeMode.DAY) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BookVerseTheme.colors.surface,
                tonalElevation = 4.dp,
                shadowElevation = 12.dp
            ) {
                ElegantNavBar(currentPage, themeMode) { if (isInteractive) onTabSelected(it) }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 32.dp).height(64.dp)) {
                ElegantNavBar(currentPage, themeMode) { if (isInteractive) onTabSelected(it) }
            }
        }
    }
}

@Composable
fun ElegantNavBar(selectedIndex: Int, themeMode: AppThemeMode, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(stringResource(R.string.nav_home), stringResource(R.string.nav_search), stringResource(R.string.nav_library))
    if (themeMode == AppThemeMode.NIGHT) {
        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(Color(0xFF111111)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)))
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                val color by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFFE50914) else Color.White.copy(alpha = 0.5f),
                    animationSpec = tween(600, easing = PremiumEasing),
                    label = "nav_color"
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = title, color = color, fontSize = 11.sp, fontWeight = if(isSelected) FontWeight.Black else FontWeight.Bold, letterSpacing = 1.sp)
                        if (isSelected) Box(Modifier.padding(top = 4.dp).size(4.dp).background(color, CircleShape))
                    }
                }
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).height(72.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                val color by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF5B4636) else Color(0xFF8D7A66).copy(alpha = 0.6f),
                    animationSpec = tween(600, easing = PremiumEasing),
                    label = "nav_color"
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = title, color = color, style = BookVerseTheme.typography.label, fontSize = 13.sp, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
                        if (isSelected) Box(Modifier.padding(top = 4.dp).width(24.dp).height(2.dp).background(color))
                    }
                }
            }
        }
    }
}

@Composable
fun SpaceElegantNavBar(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(stringResource(R.string.nav_home), stringResource(R.string.nav_search), stringResource(R.string.nav_library))

    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        StarfieldCanvas()

        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                val navColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF4A90D9) else Color.White.copy(alpha = 0.5f),
                    animationSpec = tween(600, easing = PremiumEasing),
                    label = "nav_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = title,
                            color = navColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(4.dp)
                                    .background(navColor, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StarfieldCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "space_nav_stars")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )
    
    val stars = remember {
        listOf(
            0.1f to 0.3f, 0.25f to 0.6f, 0.4f to 0.2f, 0.55f to 0.7f,
            0.7f to 0.4f, 0.85f to 0.25f, 0.15f to 0.8f, 0.6f to 0.1f,
            0.9f to 0.6f
        )
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF0A0A1A).copy(alpha = 0.95f),
                Color(0xFF0A0A1A)
            )
        )
        drawRect(brush = gradientBrush)

        stars.forEachIndexed { index, (xFrac, yFrac) ->
            val starPulse = if (index % 2 == 0) pulseAlpha else 1f - (pulseAlpha - 0.5f)
            drawCircle(
                color = Color.White.copy(alpha = starPulse * 0.8f),
                radius = (2f + (index % 3) * 0.5f).dp.toPx(),
                center = Offset(size.width * xFrac, size.height * yFrac)
            )
        }

        val arcRadius = size.width * 0.3f
        val arcCenter = Offset(size.width / 2, size.height * 1.2f)
        drawArc(
            color = Color(0xFF4A90D9).copy(alpha = 0.15f * pulseAlpha),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius),
            size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun SpaceEasyTouchNav(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    forceCollapsed: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    
    LaunchedEffect(forceCollapsed) {
        if (forceCollapsed && expanded) {
            expanded = false
        }
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val bottomNavInset = WindowInsets.navigationBars.getBottom(density)

    val buttonSizePx = with(density) { 32.dp.toPx() }
    val barWidthPx = with(density) { 320.dp.toPx() }
    val barHeightPx = with(density) { 72.dp.toPx() }
    val safeMarginPx = with(density) { 8.dp.toPx() }

    val offsetYMax = screenHeightPx - bottomNavInset

    val offsetX = remember { Animatable(screenWidthPx / 2f) }
    val offsetY = remember { Animatable(offsetYMax - buttonSizePx * 1.5f) }

    val driftAnimatableX = remember { Animatable(0f) }
    val driftAnimatableY = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            if (!expanded) {
                val targetX = (Random.nextFloat() * 0.1f - 0.05f) * screenWidthPx
                val targetY = (Random.nextFloat() * 0.1f - 0.05f) * screenHeightPx
                driftAnimatableX.animateTo(targetX, animationSpec = tween(2000, easing = LinearEasing))
                driftAnimatableY.animateTo(targetY, animationSpec = tween(2000, easing = LinearEasing))
            } else {
                driftAnimatableX.snapTo(0f)
                driftAnimatableY.snapTo(0f)
                delay(100) 
            }
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            val targetX = offsetX.value.coerceIn(
                barWidthPx / 2f + safeMarginPx,
                screenWidthPx - barWidthPx / 2f - safeMarginPx
            )
            val targetY = offsetY.value.coerceIn(
                barHeightPx / 2f + safeMarginPx,
                offsetYMax - barHeightPx / 2f - safeMarginPx
            )

            scope.launch {
                offsetX.animateTo(targetX, animationSpec = tween(400, easing = PremiumEasing))
            }
            scope.launch {
                offsetY.animateTo(targetY, animationSpec = tween(400, easing = PremiumEasing))
            }
        }
    }

    val midnightPurpleGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF6C29C0), Color(0xFF1A3BCC), Color(0xFFC0298A))
    )
    
    val currentWidthPx = if (expanded) barWidthPx else buttonSizePx
    val currentHeightPx = if (expanded) barHeightPx else buttonSizePx

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset { IntOffset(
                (offsetX.value - currentWidthPx / 2 + (if (!expanded) driftAnimatableX.value else 0f)).roundToInt(),
                (offsetY.value - currentHeightPx / 2 + (if (!expanded) driftAnimatableY.value else 0f)).roundToInt()
            )}
            .then(
                if (!expanded) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = offsetX.value.coerceIn(buttonSizePx / 2f, screenWidthPx - buttonSizePx / 2f),
                                        animationSpec = tween(durationMillis = 200)
                                    )
                                }
                                scope.launch {
                                    offsetY.animateTo(
                                        targetValue = offsetY.value.coerceIn(buttonSizePx / 2f, offsetYMax - buttonSizePx / 2f),
                                        animationSpec = tween(durationMillis = 200)
                                    )
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                driftAnimatableX.stop()
                                driftAnimatableY.stop()
                                val newX = (offsetX.value + dragAmount.x).coerceIn(buttonSizePx / 2f, screenWidthPx - buttonSizePx / 2f)
                                val newY = (offsetY.value + dragAmount.y).coerceIn(buttonSizePx / 2f, offsetYMax - buttonSizePx / 2f)
                                offsetX.snapTo(newX)
                                offsetY.snapTo(newY)
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                (fadeIn(animationSpec = tween(500, easing = PremiumEasing)) + scaleIn(initialScale = 0.8f, transformOrigin = TransformOrigin.Center))
                    .togetherWith(fadeOut(animationSpec = tween(500, easing = PremiumEasing)) + scaleOut(targetScale = 0.8f, transformOrigin = TransformOrigin.Center))
            },
            label = "nav_expansion"
        ) { isExpanded ->
            if (isExpanded) {
                Surface(
                    modifier = Modifier.widthIn(min = 300.dp).height(72.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = Color.Black.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    tonalElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EasyTouchNavItem(
                            icon = Icons.Outlined.Public,
                            label = stringResource(R.string.nav_home),
                            isSelected = selectedIndex == 0,
                            onClick = { onTabSelected(0); expanded = false }
                        )
                        EasyTouchNavItem(
                            icon = Icons.Outlined.TravelExplore,
                            label = stringResource(R.string.nav_search),
                            isSelected = selectedIndex == 1,
                            onClick = { onTabSelected(1); expanded = false }
                        )
                        EasyTouchNavItem(
                            icon = Icons.Outlined.AccountTree,
                            label = stringResource(R.string.nav_library),
                            isSelected = selectedIndex == 2,
                            onClick = { onTabSelected(2); expanded = false }
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { expanded = false }, 
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .size(buttonSizePx.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { expanded = true }
                        ),
                    shape = CircleShape,
                    color = Color(0xAA333333),
                    border = BorderStroke(2.dp, midnightPurpleGradient),
                    tonalElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EasyTouchNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (isSelected) BookVerseTheme.colors.primary else Color.White,
        animationSpec = tween(600, easing = PremiumEasing),
        label = "nav_color"
    )
    
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
