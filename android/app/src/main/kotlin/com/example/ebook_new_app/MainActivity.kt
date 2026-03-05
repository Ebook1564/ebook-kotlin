package com.example.ebook_new_app

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
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

// Circular Reveal Shape for the punch-through effect
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
            
            val view = this.window.decorView
            SideEffect {
                val window = this@MainActivity.window
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = (themeMode == AppThemeMode.DAY)
            }

            // Shared States for the Interactive Layer
            val pagerState = rememberPagerState(pageCount = { 3 })
            val homeScrollState = rememberLazyListState()

            Box(modifier = Modifier.fillMaxSize()) {
                // LAYER 1: STATIC PREVIOUS THEME (Bottom)
                // We only render the CURRENT screen of the old theme to prevent state conflicts
                if (previousTheme != null) {
                    BookVerseTheme(previousTheme!!) {
                        StaticAppLayout(
                            homeViewModel,
                            appViewModel,
                            currentPage = pagerState.currentPage,
                            homeScrollState = homeScrollState
                        )
                    }
                }

                // LAYER 2: INTERACTIVE CURRENT THEME (Revealed via Expanding Ripple)
                val config = LocalConfiguration.current
                val density = LocalDensity.current
                val sW = with(density) { config.screenWidthDp.dp.toPx() }
                val sH = with(density) { config.screenHeightDp.dp.toPx() }
                val maxRadius = hypot(sW, sH)
                
                val radius = remember(transitionTrigger) { Animatable(if (previousTheme != null) 0f else maxRadius * 1.5f) }
                
                LaunchedEffect(transitionTrigger) {
                    if (previousTheme != null) {
                        radius.animateTo(maxRadius * 1.5f, tween(1000, easing = LinearOutSlowInEasing))
                        appViewModel.endTransition()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            clip = true
                            shape = CircularRevealShape(
                                radius = radius.value,
                                center = Offset(size.width - 40.dp.toPx(), 60.dp.toPx())
                            )
                        }
                ) {
                    BookVerseTheme(themeMode) {
                        BookVerseApp(
                            homeViewModel, 
                            appViewModel, 
                            pagerState, 
                            homeScrollState
                        )
                    }
                }

                // Block touches while the ripple is revealing to prevent mid-transition state changes
                if (isTransitioning) {
                    Box(modifier = Modifier.fillMaxSize().clickable(enabled = true, onClick = {}, interactionSource = remember { MutableInteractionSource() }, indication = null))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.endReadTransition()
    }
}

/**
 * A non-pager version of the app used for the bottom layer during theme transitions.
 * This prevents multiple Pagers from fighting over the same PagerState.
 */
@Composable
fun StaticAppLayout(
    homeViewModel: HomeViewModel,
    appViewModel: AppViewModel,
    currentPage: Int,
    homeScrollState: LazyListState
) {
    val themeMode = BookVerseTheme.colors.isDark.let { if(it) AppThemeMode.NIGHT else AppThemeMode.DAY }
    
    Box(modifier = Modifier.fillMaxSize().background(BookVerseTheme.colors.background)) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
                    // Render current screen directly without Pager
                    when (currentPage) {
                        0 -> HomeScreen(viewModel = homeViewModel, appViewModel = appViewModel, scrollState = homeScrollState)
                        1 -> SearchScreen(viewModel = homeViewModel)
                        2 -> LibraryScreen(onProfileClick = { })
                    }
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .then(if (themeMode == AppThemeMode.DAY) Modifier.background(BookVerseTheme.colors.surface) else Modifier)
                ) {
                    ElegantNavBar(currentPage, themeMode) { /* No-op in static layer */ }
                }
            }
        }
    }
}

@Composable
fun BookVerseApp(
    homeViewModel: HomeViewModel, 
    appViewModel: AppViewModel, 
    pagerState: PagerState, 
    homeScrollState: LazyListState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val themeMode = BookVerseTheme.colors.isDark.let { if(it) AppThemeMode.NIGHT else AppThemeMode.DAY }
    
    val readerSettingsRepo = remember { ReaderSettingsRepository(context) }
    val themeColor = readerSettingsRepo.getThemeColor()
    val textColor = readerSettingsRepo.getTextColor()

    Box(modifier = Modifier.fillMaxSize().background(BookVerseTheme.colors.background)) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState, 
                    modifier = Modifier.fillMaxSize(), 
                    beyondViewportPageCount = 1,
                    userScrollEnabled = true
                ) { page ->
                    when (page) {
                        0 -> HomeScreen(viewModel = homeViewModel, appViewModel = appViewModel, scrollState = homeScrollState)
                        1 -> SearchScreen(viewModel = homeViewModel)
                        2 -> LibraryScreen(onProfileClick = { })
                    }
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .then(if (themeMode == AppThemeMode.DAY) Modifier.background(BookVerseTheme.colors.surface) else Modifier)
                ) {
                    ElegantNavBar(pagerState.currentPage, themeMode) { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                }
            }
        }

        // Transition Overlay
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
                    val color by animateColorAsState(if (isSelected) Color(0xFFE50914) else Color.White.copy(alpha = 0.5f))
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
                    val color by animateColorAsState(if (isSelected) Color(0xFF5B4636) else Color(0xFF8D7A66).copy(alpha = 0.6f))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = title, color = color, style = BookVerseTheme.typography.label, fontSize = 13.sp, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isSelected) Box(Modifier.padding(top = 4.dp).width(20.dp).height(2.dp).background(color))
                        }
                    }
                }
            }
        }
    }
}
