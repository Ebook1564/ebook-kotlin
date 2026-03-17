package com.example.ebook_new_app

import android.app.ActivityOptions
import android.content.Intent
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.ebook_new_app.reader.ReaderActivity
import com.example.ebook_new_app.repository.ReaderSettingsRepository
import com.example.ebook_new_app.ui.*
import com.example.ebook_new_app.viewmodel.AppThemeMode
import com.example.ebook_new_app.viewmodel.AppViewModel
import com.example.ebook_new_app.viewmodel.HomeViewModel
import com.example.ebook_new_app.viewmodel.LibraryViewModel
import com.example.ebook_new_app.models.ReadingStatus
import com.example.ebook_new_app.viewmodel.InitialBounds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

// PREMIUM CONSTANTS
val PremiumEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
const val PremiumDuration = 1400

class CircularRevealShape(private val radius: Float, private val center: Offset) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            addOval(Rect(center, radius))
        }
        return Outline.Generic(path)
    }
}

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val appViewModel: AppViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appViewModel.themeMode
            val previousTheme by appViewModel.previousThemeMode
            val transitionTrigger by appViewModel.themeTransitionTrigger
            val isTransitioning by appViewModel.isTransitioning
            val userProfile by appViewModel.userProfile.collectAsState()
            
            var showProfile by remember { mutableStateOf(false) }
            
            val view = this.window.decorView
            SideEffect {
                val window = this@MainActivity.window
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = (themeMode == AppThemeMode.DAY)
            }

            if (!userProfile.isOnboarded) {
                BookVerseTheme(AppThemeMode.NIGHT) {
                    OnboardingScreen(
                        onFinish = { name, theme ->
                            appViewModel.setTheme(theme)
                            appViewModel.completeOnboarding(name)
                        }
                    )
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { 3 })
                val homeScrollState = rememberLazyListState()
                val homeUiState by homeViewModel.uiState.collectAsState()

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
                        enter = slideInHorizontally(
                            initialOffsetX = { it }, 
                            animationSpec = tween(700, easing = PremiumEasing)
                        ) + fadeIn(),
                        exit = slideOutHorizontally(
                            targetOffsetX = { it }, 
                            animationSpec = tween(700, easing = PremiumEasing)
                        ) + fadeOut()
                    ) {
                        BackHandler { showProfile = false }
                        ProfileScreen(userName = userProfile.name, onBackClick = { showProfile = false })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.endReadTransition()
    }
}

@Composable
fun StaticAppLayout(homeViewModel: HomeViewModel, appViewModel: AppViewModel, libraryViewModel: LibraryViewModel, currentPage: Int, homeScrollState: LazyListState, userName: String, onProfileClick: () -> Unit) {
    val themeMode = BookVerseTheme.colors.isDark.let { if(it) AppThemeMode.NIGHT else AppThemeMode.DAY }
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
            Box(modifier = Modifier.align(Alignment.BottomCenter).then(if (themeMode == AppThemeMode.DAY) Modifier.background(BookVerseTheme.colors.surface) else Modifier)) {
                ElegantNavBar(currentPage, themeMode) { }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookVerseApp(homeViewModel: HomeViewModel, appViewModel: AppViewModel, libraryViewModel: LibraryViewModel, pagerState: PagerState, homeScrollState: LazyListState, userName: String, isInteractive: Boolean, onProfileClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val libraryUiState by libraryViewModel.uiState.collectAsState()
    val themeMode = BookVerseTheme.colors.isDark.let { if(it) AppThemeMode.NIGHT else AppThemeMode.DAY }
    val readerSettingsRepo = remember { ReaderSettingsRepository(context) }
    val themeColor = readerSettingsRepo.getThemeColor()
    val textColor = readerSettingsRepo.getTextColor()

    var selectedBookForSheet by remember { mutableStateOf<com.example.ebook_new_app.models.Book?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    
    var isInspectingByShelf by remember { mutableStateOf(false) }

    fun onReadClick(book: com.example.ebook_new_app.models.Book, bounds: com.example.ebook_new_app.viewmodel.InitialBounds?) {
        showSheet = false
        homeViewModel.startReadTransition(book, bounds)
    }

    fun onBookClick(book: com.example.ebook_new_app.models.Book) {
        selectedBookForSheet = book
        showSheet = true
        // TRIGGER BACKGROUND FETCH
        homeViewModel.fetchMetadataForBook(book) { updatedBook ->
            if (selectedBookForSheet?.id == updatedBook.id) {
                selectedBookForSheet = updatedBook
            }
        }
    }

    val shouldHideNavBar = isInspectingByShelf || libraryUiState.seeAllStatus != null || homeUiState.currentCollection != null || homeUiState.currentCuratedCollection != null

    Box(modifier = Modifier.fillMaxSize().background(BookVerseTheme.colors.background)) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        onReadNowClick = { book ->
                            homeViewModel.startReadTransition(book)
                        }
                    )
                    1 -> SearchScreen(
                        viewModel = homeViewModel, 
                        onBookClick = { onBookClick(it) }
                    )
                    2 -> LibraryScreen(
                        viewModel = libraryViewModel, 
                        homeViewModel = homeViewModel,
                        onProfileClick = onProfileClick, 
                        onBookClick = { onBookClick(it) },
                        onInspectStateChange = { isInspectingByShelf = it }
                    )
                }
            }
            
            AnimatedVisibility(
                visible = !shouldHideNavBar,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    initialOffsetY = { it }, 
                    animationSpec = tween(600, easing = PremiumEasing)
                ) + fadeIn(animationSpec = tween(400)),
                exit = slideOutVertically(
                    targetOffsetY = { it }, 
                    animationSpec = tween(600, easing = PremiumEasing)
                ) + fadeOut(animationSpec = tween(400))
            ) {
                Box(modifier = Modifier.then(if (themeMode == AppThemeMode.DAY) Modifier.background(BookVerseTheme.colors.surface) else Modifier)) {
                    ElegantNavBar(pagerState.currentPage, themeMode) { index -> 
                        if (isInteractive) scope.launch { pagerState.animateScrollToPage(index, animationSpec = tween(800, easing = PremiumEasing)) } 
                    }
                }
            }
        }
        
        if (showSheet && selectedBookForSheet != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = Color.Transparent, 
                contentColor = BookVerseTheme.colors.textPrimary,
                dragHandle = null 
            ) {
                BookDetailContent(
                    book = selectedBookForSheet!!,
                    isLoading = homeUiState.isMetadataLoading,
                    onReadClick = { bounds -> onReadClick(selectedBookForSheet!!, bounds) },
                    onCategoryClick = { tag ->
                        showSheet = false
                        homeViewModel.openCollection(tag)
                    },
                    onAddToReadingList = { status ->
                        libraryViewModel.updateReadingStatus(selectedBookForSheet!!, status)
                    },
                    onClose = { showSheet = false }
                )
            }
        }

        // CURATED COLLECTION PAGE OVERLAY
        AnimatedVisibility(
            visible = homeUiState.currentCollection != null,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(600, easing = PremiumEasing)) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(600, easing = PremiumEasing)) + fadeOut()
        ) {
            homeUiState.currentCollection?.let { collection ->
                val books = homeViewModel.curatedCollectionBooks.collectAsLazyPagingItems()
                BackHandler { homeViewModel.closeCollection() }
                CollectionPage(
                    collection = collection,
                    books = books,
                    onBack = { homeViewModel.closeCollection() },
                    onBookClick = { onBookClick(it) }
                )
            }
        }

        // CURATED COLLECTION (SPOTIFY-STYLE) PAGE OVERLAY
        AnimatedVisibility(
            visible = homeUiState.currentCuratedCollection != null,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(600, easing = PremiumEasing)) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(600, easing = PremiumEasing)) + fadeOut()
        ) {
            homeUiState.currentCuratedCollection?.let { collection ->
                val books = homeViewModel.curatedCollectionBooksByTag.collectAsLazyPagingItems()
                BackHandler { homeViewModel.closeCollection() }
                CollectionPage(
                    title = collection.title,
                    description = collection.description,
                    accentColor = collection.accentColor,
                    books = books,
                    onBack = { homeViewModel.closeCollection() },
                    onBookClick = { onBookClick(it) }
                )
            }
        }

        if (homeUiState.transitionBook != null) {
            BookTransitionOverlay(
                book = homeUiState.transitionBook!!, 
                initialBounds = homeUiState.initialBounds, 
                themeColor = themeColor, 
                textColor = textColor, 
                onAnimationFinished = { file ->
                    // Update First Opened Date
                    (context.applicationContext as EbookApp).database.bookDao().let { dao ->
                        scope.launch {
                            dao.updateFirstOpened(homeUiState.transitionBook!!.id, System.currentTimeMillis())
                        }
                    }

                    val intent = Intent(context, ReaderActivity::class.java).apply {
                        putExtra("FILE_PATH", file.absolutePath)
                        putExtra("BOOK_TITLE", homeUiState.transitionBook!!.title)
                        putExtra("BOOK_ID", homeUiState.transitionBook!!.id)
                    }
                    val options = ActivityOptions.makeCustomAnimation(context, 0, 0)
                    context.startActivity(intent, options.toBundle())
                }, 
                onDismiss = { homeViewModel.endReadTransition() }
            )
        }
    }
}

@Composable
fun ElegantNavBar(selectedIndex: Int, themeMode: AppThemeMode, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("HOME", "SEARCH", "LIBRARY")
    if (themeMode == AppThemeMode.NIGHT) {
        Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 32.dp).height(64.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(Color.Black.copy(alpha = 0.94f)).blur(30.dp).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)))
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedIndex == index
                    val color by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFFE50914) else Color.White.copy(alpha = 0.5f),
                        animationSpec = tween(600, easing = PremiumEasing)
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = title, color = color, fontSize = 11.sp, fontWeight = if(isSelected) FontWeight.Black else FontWeight.Bold, letterSpacing = 1.sp)
                            if (isSelected) Box(Modifier.padding(top = 4.dp).size(4.dp).background(color, CircleShape))
                        }
                    }
                }
            }
        }
    } else {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF5F1E0), tonalElevation = 4.dp, shadowElevation = 12.dp) {
            Row(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).height(72.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedIndex == index
                    val color by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF5B4636) else Color(0xFF8D7A66).copy(alpha = 0.6f),
                        animationSpec = tween(600, easing = PremiumEasing)
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
}
