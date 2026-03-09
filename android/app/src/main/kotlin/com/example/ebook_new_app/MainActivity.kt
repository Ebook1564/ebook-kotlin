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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ebook_new_app.reader.ReaderActivity
import com.example.ebook_new_app.repository.ReaderSettingsRepository
import com.example.ebook_new_app.ui.*
import com.example.ebook_new_app.viewmodel.AppThemeMode
import com.example.ebook_new_app.viewmodel.AppViewModel
import com.example.ebook_new_app.viewmodel.HomeViewModel
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        setContent {
            val themeMode by appViewModel.themeMode
            val previousTheme by appViewModel.previousThemeMode
            val transitionTrigger by appViewModel.themeTransitionTrigger
            val isTransitioning by appViewModel.isTransitioning
            
            var showProfile by remember { mutableStateOf(false) }
            
            val view = this.window.decorView
            SideEffect {
                val window = this@MainActivity.window
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = (themeMode == AppThemeMode.DAY)
            }

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
                        StaticAppLayout(homeViewModel, appViewModel, pagerState.currentPage, dummyScrollState, onProfileClick = { showProfile = true })
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
                            pagerState, 
                            homeScrollState, 
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
                    ProfileScreen(onBackClick = { showProfile = false })
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
fun StaticAppLayout(homeViewModel: HomeViewModel, appViewModel: AppViewModel, currentPage: Int, homeScrollState: LazyListState, onProfileClick: () -> Unit) {
    val themeMode = appViewModel.themeMode.value
    Box(modifier = Modifier.fillMaxSize()) {
        if (themeMode == AppThemeMode.SPACE) {
            SpaceBackground()
        } else {
            Box(modifier = Modifier.fillMaxSize().background(BookVerseTheme.colors.background))
        }
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentPage) {
                0 -> HomeScreen(viewModel = homeViewModel, appViewModel = appViewModel, scrollState = homeScrollState)
                1 -> SearchScreen(viewModel = homeViewModel)
                2 -> LibraryScreen(onProfileClick = onProfileClick)
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter).then(if (themeMode == AppThemeMode.DAY) Modifier.background(BookVerseTheme.colors.surface) else Modifier)) {
                if (themeMode == AppThemeMode.SPACE) {
                    SpaceEasyTouchNav(currentPage) { }
                } else {
                    ElegantNavBar(currentPage, themeMode) { }
                }
            }
        }
    }
}

@Composable
fun BookVerseApp(homeViewModel: HomeViewModel, appViewModel: AppViewModel, pagerState: PagerState, homeScrollState: LazyListState, isInteractive: Boolean, onProfileClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val themeMode = appViewModel.themeMode.value
    val readerSettingsRepo = remember { ReaderSettingsRepository(context) }
    val themeColor = readerSettingsRepo.getThemeColor()
    val textColor = readerSettingsRepo.getTextColor()

    Box(modifier = Modifier.fillMaxSize()) {
        if (themeMode == AppThemeMode.SPACE) {
            SpaceBackground()
        } else {
            Box(modifier = Modifier.fillMaxSize().background(BookVerseTheme.colors.background))
        }

        HorizontalPager(
            state = pagerState, 
            modifier = Modifier.fillMaxSize(), 
            beyondViewportPageCount = 1, 
            userScrollEnabled = isInteractive
        ) { page ->
            when (page) {
                0 -> HomeScreen(viewModel = homeViewModel, appViewModel = appViewModel, scrollState = homeScrollState)
                1 -> SearchScreen(viewModel = homeViewModel)
                2 -> LibraryScreen(onProfileClick = onProfileClick)
            }
        }
        
        if (themeMode == AppThemeMode.SPACE) {
            SpaceEasyTouchNav(pagerState.currentPage) { index ->
                if (isInteractive) scope.launch { pagerState.animateScrollToPage(index, animationSpec = tween(800, easing = PremiumEasing)) }
            }
        } else {
            Box(modifier = Modifier.align(Alignment.BottomCenter).then(if (themeMode == AppThemeMode.DAY) Modifier.background(BookVerseTheme.colors.surface) else Modifier)) {
                ElegantNavBar(pagerState.currentPage, themeMode) { index -> 
                    if (isInteractive) scope.launch { pagerState.animateScrollToPage(index, animationSpec = tween(800, easing = PremiumEasing)) } 
                }
            }
        }
        
        if (homeUiState.transitionBook != null) {
            BookTransitionOverlay(
                book = homeUiState.transitionBook, 
                initialBounds = homeUiState.initialBounds, 
                themeColor = themeColor, 
                textColor = textColor, 
                onAnimationFinished = { file ->
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
                        targetValue = if (isSelected) BookVerseTheme.colors.primary else Color.White.copy(alpha = 0.5f),
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

@Composable
fun SpaceEasyTouchNav(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val buttonSizePx = with(density) { 32.dp.toPx() } // Reduced button size
    val barWidthPx = with(density) { 320.dp.toPx() }
    val barHeightPx = with(density) { 72.dp.toPx() }
    val safeMarginPx = with(density) { 8.dp.toPx() }

    val offsetX = remember { Animatable(screenWidthPx / 2f) }
    val offsetY = remember { Animatable(screenHeightPx - buttonSizePx * 2.5f) }

    val driftAnimatableX = remember { Animatable(0f) }
    val driftAnimatableY = remember { Animatable(0f) }

    // Floating animation when collapsed
    LaunchedEffect(!expanded) {
        if (!expanded) {
            while (true) {
                val targetX = (Random.nextFloat() * 0.1f - 0.05f) * screenWidthPx // drift 5% of screen width
                val targetY = (Random.nextFloat() * 0.1f - 0.05f) * screenHeightPx // drift 5% of screen height
                
                driftAnimatableX.animateTo(targetX, animationSpec = tween(durationMillis = 2000, easing = LinearEasing))
                driftAnimatableY.animateTo(targetY, animationSpec = tween(durationMillis = 2000, easing = LinearEasing))
            }
        }
    }

    // Auto-shift to be visible when expanded
    LaunchedEffect(expanded) {
        if (expanded) {
            // Stop any ongoing drift animation immediately
            driftAnimatableX.stop()
            driftAnimatableY.stop()

            val targetX = offsetX.value.coerceIn(
                barWidthPx / 2f + safeMarginPx,
                screenWidthPx - barWidthPx / 2f - safeMarginPx
            )
            val targetY = offsetY.value.coerceIn(
                barHeightPx / 2f + safeMarginPx,
                screenHeightPx - barHeightPx / 2f - safeMarginPx
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
                                // Animate to current position after drag ends
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = offsetX.value.coerceIn(buttonSizePx / 2f, screenWidthPx - buttonSizePx / 2f),
                                        animationSpec = tween(durationMillis = 200) // Short animation
                                    )
                                }
                                scope.launch {
                                    offsetY.animateTo(
                                        targetValue = offsetY.value.coerceIn(buttonSizePx / 2f, screenHeightPx - buttonSizePx / 2f),
                                        animationSpec = tween(durationMillis = 200) // Short animation
                                    )
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                // Stop drift when manually dragging
                                driftAnimatableX.stop()
                                driftAnimatableY.stop()
                                val newX = (offsetX.value + dragAmount.x).coerceIn(buttonSizePx / 2f, screenWidthPx - buttonSizePx / 2f)
                                val newY = (offsetY.value + dragAmount.y).coerceIn(buttonSizePx / 2f, screenHeightPx - buttonSizePx / 2f)
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
                    modifier = Modifier.widthIn(min = 300.dp).height(72.dp), // Reverted to original dimensions, with reduced height
                    shape = RoundedCornerShape(40.dp),
                    color = Color.Black.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    tonalElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp), // Reduced horizontal padding
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly // Changed to SpaceEvenly to fit
                    ) {
                        EasyTouchNavItem(
                            icon = Icons.Outlined.Public,
                            label = "HOME",
                            isSelected = selectedIndex == 0,
                            onClick = { onTabSelected(0); expanded = false }
                        )
                        EasyTouchNavItem(
                            icon = Icons.Outlined.TravelExplore,
                            label = "SEARCH",
                            isSelected = selectedIndex == 1,
                            onClick = { onTabSelected(1); expanded = false }
                        )
                        EasyTouchNavItem(
                            icon = Icons.Outlined.AccountTree,
                            label = "LIBRARY",
                            isSelected = selectedIndex == 2,
                            onClick = { onTabSelected(2); expanded = false }
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp) // Reduced size
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { expanded = false }, 
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) // Reduced icon size
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .size(buttonSizePx.dp) // Use calculated button size here
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
                                .size(32.dp) // Reduced inner circle size
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
        animationSpec = tween(600, easing = PremiumEasing)
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
            modifier = Modifier.size(24.dp) // Reduced icon size
        )
        Spacer(modifier = Modifier.height(2.dp)) // Reduced spacer
        Text(
            text = label,
            color = color,
            fontSize = 9.sp, // Reduced font size
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}