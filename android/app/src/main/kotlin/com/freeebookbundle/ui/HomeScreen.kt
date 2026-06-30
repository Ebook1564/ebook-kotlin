package com.freeebookbundle.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.freeebookbundle.R
import com.freeebookbundle.models.Book
import com.freeebookbundle.viewmodel.AppThemeMode
import com.freeebookbundle.viewmodel.AppViewModel
import com.freeebookbundle.viewmodel.CuratedCollection
import com.freeebookbundle.viewmodel.HomeViewModel
import com.freeebookbundle.utils.GenrePreferences
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.cos

@Composable
fun ClassicEditionCover(title: String, author: String, modifier: Modifier = Modifier) {
    val colors = listOf(
        Color(0xFF4E342E), Color(0xFF263238), Color(0xFF37474F), Color(0xFF1B5E20), Color(0xFFB71C1C)
    )
    val bgColor = colors[title.length % colors.size]
    Box(
        modifier = modifier.background(bgColor).padding(12.dp).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.1f))))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = title.uppercase(), color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis, lineHeight = 12.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(Modifier.width(20.dp).height(1.dp).background(Color(0xFFD4AF37).copy(alpha = 0.5f)))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = author, color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
        }
        Box(modifier = Modifier.fillMaxHeight().width(6.dp).align(Alignment.CenterStart).background(Brush.horizontalGradient(listOf(Color.Black.copy(0.3f), Color.Transparent))))
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel(),
    scrollState: LazyListState = rememberLazyListState(),
    userName: String,
    isSpaceActive: Boolean = true,
    onBookClick: (Book) -> Unit,
    onReadNowClick: (Book) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val homeSections by viewModel.homeSections.collectAsStateWithLifecycle()
    val billboardBooks by viewModel.billboardBooks.collectAsStateWithLifecycle()
    val continueReadingBooks by viewModel.continueReadingBooks.collectAsStateWithLifecycle()
    val backgroundColor = BookVerseTheme.colors.background
    val themeMode by appViewModel.themeMode
    
    // Shared skeleton transition to avoid creating multiple InfiniteTransitions
    val skeletonTransition = rememberInfiniteTransition(label = "home_skeleton")

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        if (themeMode == AppThemeMode.SPACE) {
            SpaceBackground(isActive = isSpaceActive)
        }
        
        if (uiState.isLoading && homeSections.isEmpty() && continueReadingBooks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BookVerseTheme.colors.primary)
            }
        } else {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp) 
            ) {
                // 1. HEADER
                item(key = "header") { HeaderSection(appViewModel, userName) }
                
                // 2. BILLBOARD (HERO) - NOW AT TOP
                item(key = "billboard") {
                    if (billboardBooks.isNotEmpty()) {
                        if (themeMode == AppThemeMode.SPACE) {
                            OrbitalCarousel(
                                books = billboardBooks,
                                continueReadingBooks = continueReadingBooks,
                                onReadClick = onReadNowClick,
                                onInfoClick = onBookClick
                            )
                        } else {
                            BillboardCarousel(books = billboardBooks, continueReadingBooks = continueReadingBooks, onReadClick = onReadNowClick, onInfoClick = onBookClick)
                        }
                    } else if (uiState.isCatalogLoading) {
                        BillboardSkeleton(shimmerTransition = skeletonTransition)
                    }
                }

                // 3. CONTINUE READING (HIGH PRIORITY) - NOW BELOW BILLBOARD
                if (continueReadingBooks.isNotEmpty()) {
                    item(key = "continue_reading") { ContinueReadingSection(books = continueReadingBooks, onBookClick = onBookClick, onReadClick = onReadNowClick) }
                }
                
                // 4. SELECTED CATEGORIES (DYNAMICALY FILTERED BY VIEWMODEL)
                homeSections.forEach { section ->
                    item(key = "section_${section.categoryCode}") { 
                        val displayTitle = getCategoryDisplayName(section.title)
                        if (section.books.isNotEmpty()) {
                            CategoryRow(title = displayTitle, books = section.books, onBookClick = onBookClick)
                        } else {
                            CategorySkeleton(title = displayTitle, shimmerTransition = skeletonTransition)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun shimmerBrush(
    shimmerTransition: InfiniteTransition,
    showShimmer: Boolean = true, 
    targetValue: Float = 1000f
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )

        val translateAnimation = shimmerTransition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
fun BillboardSkeleton(shimmerTransition: InfiniteTransition) {
    val brush = shimmerBrush(shimmerTransition = shimmerTransition)
    val floatAnim by shimmerTransition.animateFloat(
        initialValue = -8f, 
        targetValue = 8f, 
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine), 
            repeatMode = RepeatMode.Reverse
        ), 
        label = "skeleton_float"
    )

    Box(modifier = Modifier.fillMaxWidth().height(480.dp), contentAlignment = Alignment.Center) {
        // 1. Background Gradient Placeholder
        Box(modifier = Modifier.fillMaxSize().background(brush = Brush.verticalGradient(listOf(Color.Transparent, BookVerseTheme.colors.background))))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 20.dp)) {
            // 2. 3D Book Shape Placeholder
            Box(contentAlignment = Alignment.Center) {
                // Shadow
                Box(modifier = Modifier.padding(top = 260.dp).width(180.dp).height(20.dp).graphicsLayer { 
                    rotationX = 80f; 
                    scaleX = 1.2f 
                }.background(Brush.radialGradient(listOf(Color.Black.copy(0.2f), Color.Transparent))))
                
                // The "MVP" 3D Book Shape
                Box(modifier = Modifier
                    .width(180.dp)
                    .height(270.dp)
                    .graphicsLayer { 
                        rotationY = -25f
                        rotationX = 10f
                        rotationZ = floatAnim / 4f
                        translationY = floatAnim
                        cameraDistance = 12f * density 
                    }
                    .shadow(16.dp, RoundedCornerShape(4.dp))
                    .background(brush, RoundedCornerShape(4.dp))
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 3. Title Placeholder
            Box(modifier = Modifier
                .width(200.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 4. Buttons Placeholder
            Row(modifier = Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(brush)
                )
                Box(modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(brush)
                )
            }
        }
    }
}

@Composable
fun CategorySkeleton(title: String, shimmerTransition: InfiniteTransition) {
    val brush = shimmerBrush(shimmerTransition = shimmerTransition)
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = title, style = BookVerseTheme.typography.headline, color = BookVerseTheme.colors.textPrimary, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(5) {
                Box(modifier = Modifier
                    .width(120.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
                )
            }
        }
    }
}

@Composable
fun ContinueReadingSection(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onReadClick: (Book) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = BookVerseTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_continue_reading),
                    style = BookVerseTheme.typography.headline,
                    color = BookVerseTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(books, key = { it.id }) { book ->
                ContinueReadingBookCard(
                    book = book,
                    onClick = { onBookClick(book) },
                    onReadClick = { onReadClick(book) }
                )
            }
        }
    }
}

@Composable
private fun ContinueReadingBookCard(
    book: Book,
    onClick: () -> Unit,
    onReadClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = BookVerseTheme.colors.surface,
        shadowElevation = 4.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (book.getBestCoverUrl().isNotEmpty()) {
                    AsyncImage(
                        model = book.getBestCoverUrl(),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ClassicEditionCover(
                        title = book.title,
                        author = book.author,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Progress indicator
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = BookVerseTheme.colors.primary
                ) {
                    Text(
                        text = "${book.getReadingProgressPercent()}%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = book.title,
                    style = BookVerseTheme.typography.body,
                    color = BookVerseTheme.colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author,
                    color = BookVerseTheme.colors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HeaderSection(appViewModel: AppViewModel, userName: String) {
    val themeMode = appViewModel.themeMode.value
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DynamicGreeting(userName)
        IconButton(
            onClick = { appViewModel.toggleTheme() },
            modifier = Modifier.clip(CircleShape).background(BookVerseTheme.colors.surface.copy(alpha = if(themeMode == AppThemeMode.NIGHT || themeMode == AppThemeMode.SPACE) 0.3f else 0.8f))
        ) {
            Icon(
                imageVector = when (themeMode) {
                    AppThemeMode.DAY -> Icons.Default.Nightlight
                    AppThemeMode.NIGHT -> Icons.Default.AutoAwesome
                    AppThemeMode.SPACE -> Icons.Default.LightMode
                },
                contentDescription = stringResource(R.string.home_toggle_theme),
                tint = BookVerseTheme.colors.primary
            )
        }
    }
}

@Composable
fun DynamicGreeting(userName: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..5 -> stringResource(R.string.greeting_night)
        in 6..11 -> stringResource(R.string.greeting_morning)
        in 12..16 -> stringResource(R.string.greeting_afternoon)
        in 17..20 -> stringResource(R.string.greeting_evening)
        else -> stringResource(R.string.greeting_night)
    }
    
    val punchline = remember(hour) {
        val punchlines = when (hour) {
            in 0..5 -> listOf(context.getString(R.string.punchline_quiet_world), context.getString(R.string.punchline_midnight_reading), context.getString(R.string.punchline_explore_worlds))
            in 6..8 -> listOf(context.getString(R.string.punchline_morning_story), context.getString(R.string.punchline_morning_reads), context.getString(R.string.punchline_rise_read))
            in 9..11 -> listOf(context.getString(R.string.punchline_new_adventure), context.getString(R.string.punchline_discover), context.getString(R.string.punchline_dive_book))
            in 12..14 -> listOf(context.getString(R.string.punchline_lunch_reading), context.getString(R.string.punchline_afternoon_escape), context.getString(R.string.punchline_reading_break))
            in 15..17 -> listOf(context.getString(R.string.punchline_momentum), context.getString(R.string.punchline_story_time), context.getString(R.string.punchline_chapter))
            in 18..20 -> listOf(context.getString(R.string.punchline_unwind), context.getString(R.string.punchline_evening), context.getString(R.string.punchline_perfect_stories))
            else -> listOf(context.getString(R.string.punchline_cozy), context.getString(R.string.punchline_nighttime), context.getString(R.string.punchline_transport))
        }
        punchlines.random()
    }
    
    Column {
        Text(text = "$greeting, $userName", style = BookVerseTheme.typography.headline, color = BookVerseTheme.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(text = punchline, style = BookVerseTheme.typography.body, color = BookVerseTheme.colors.textSecondary, fontSize = 13.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
fun BillboardCarousel(books: List<Book>, continueReadingBooks: List<Book>, onReadClick: (Book) -> Unit, onInfoClick: (Book) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { books.size })

    // Simplified auto-scroll logic, decoupled from scroll state
    LaunchedEffect(books.size) {
        while (books.isNotEmpty()) {
            delay(5000)
            if (!pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % books.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    val continueReadingIds = remember(continueReadingBooks) {
        continueReadingBooks.map { it.id }.toHashSet()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState, 
            modifier = Modifier.fillMaxWidth().height(480.dp), 
            contentPadding = PaddingValues(horizontal = 0.dp),
            beyondViewportPageCount = 0
        ) { page ->
            val book = books[page]
            val isContinueReading = continueReadingIds.contains(book.id)
            BillboardItem(
                book = book, 
                isContinueReading = isContinueReading, 
                onReadClick = { onReadClick(book) }, 
                onInfoClick = { onInfoClick(book) }, 
                pagerOffset = pagerState.currentPage - page + pagerState.currentPageOffsetFraction
            )
        }
        Row(Modifier.height(20.dp).fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
            repeat(books.size) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val color = if (isSelected) BookVerseTheme.colors.primary else BookVerseTheme.colors.primary.copy(alpha = 0.3f)
                val size by animateDpAsState(if (isSelected) 8.dp else 6.dp, label = "dot")
                Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(size))
            }
        }
    }
}

@Composable
fun BillboardItem(book: Book, isContinueReading: Boolean, onReadClick: () -> Unit, onInfoClick: () -> Unit, pagerOffset: Float) {
    val isCurrentPage = kotlin.math.abs(pagerOffset) < 0.5f
    val floatAnim by if (isCurrentPage) {
        rememberInfiniteTransition(label = "infinite").animateFloat(initialValue = -8f, targetValue = 8f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse), label = "float")
    } else remember { mutableStateOf(0f) }
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().background(brush = Brush.verticalGradient(listOf(Color.Transparent, BookVerseTheme.colors.background))))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 20.dp).graphicsLayer { alpha = 1f - (kotlin.math.abs(pagerOffset) * 0.5f); translationX = pagerOffset * 300f }) {
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.padding(top = 260.dp).width(180.dp).height(20.dp).graphicsLayer { rotationX = 80f; scaleX = 1.2f + (floatAnim / 40f) }.background(Brush.radialGradient(listOf(Color.Black.copy(0.4f), Color.Transparent))))
                Box(modifier = Modifier.width(180.dp).height(270.dp).graphicsLayer { rotationY = -25f + (pagerOffset * 10f); rotationX = 10f; rotationZ = floatAnim / 4f; translationY = floatAnim; cameraDistance = 12f * density }.shadow(24.dp, RoundedCornerShape(4.dp))) {
                    
                    // Priority: External API > Server cover (via getBestCoverUrl)
                    val imageUrl = book.getBestCoverUrl()
                    
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(4.dp)),
                        error = painterResource(id = R.drawable.ic_book)
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White.copy(0.1f), Color.Transparent, Color.Black.copy(0.2f)))))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = book.title, style = BookVerseTheme.typography.headline, color = BookVerseTheme.colors.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp), fontSize = 24.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onReadClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BookVerseTheme.colors.primary),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).shadow(12.dp, RoundedCornerShape(30.dp)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(if (isContinueReading) Icons.Default.PlayArrow else Icons.Default.PlayArrow, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isContinueReading) stringResource(R.string.home_continue) else stringResource(R.string.home_read_now),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onInfoClick,
                    shape = RoundedCornerShape(30.dp),
                    border = BorderStroke(1.dp, BookVerseTheme.colors.primary.copy(0.5f)),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BookVerseTheme.colors.textPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_more_info),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Orbital Carousel – space-theme billboard
//  5 books on an elliptical orbit, centre = sun
// ─────────────────────────────────────────────
private const val ORBITAL_VISIBLE_COUNT = 5
private const val ORBITAL_AUTO_SCROLL_MS = 8000L
private const val ORBITAL_STEP_DEGREES = 360f / ORBITAL_VISIBLE_COUNT // 72°
private val OrbitalEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private fun orbitalNormalize(deg: Float): Float {
    var a = deg % 360f
    if (a > 180f) a -= 360f
    if (a < -180f) a += 360f
    return a
}

@Composable
fun OrbitalCarousel(
    books: List<Book>,
    continueReadingBooks: List<Book>,
    onReadClick: (Book) -> Unit,
    onInfoClick: (Book) -> Unit
) {
    val displayBooks = remember(books) {
        if (books.size >= ORBITAL_VISIBLE_COUNT) books.take(ORBITAL_VISIBLE_COUNT) else books
    }
    if (displayBooks.isEmpty()) return

    val continueReadingIds = remember(continueReadingBooks) {
        continueReadingBooks.map { it.id }.toHashSet()
    }

    // ── rotation state ──────────────────────
    var rotationStep by remember { mutableIntStateOf(0) }
    var dragAngle by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // single animated value: snap during drag, smooth tween on release
    val displayedAngle by animateFloatAsState(
        targetValue = if (isDragging) dragAngle else rotationStep * ORBITAL_STEP_DEGREES,
        animationSpec = if (isDragging) snap<Float>() else tween(750, easing = OrbitalEasing),
        label = "orbital_angle"
    )

    // auto-rotate every 8 s
    LaunchedEffect(displayBooks.size) {
        while (true) {
            delay(ORBITAL_AUTO_SCROLL_MS)
            rotationStep--
        }
    }

    // ── infinite transitions ─────────────────
    val inf = rememberInfiniteTransition(label = "orbital_inf")

    val centerFloat by inf.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "center_float"
    )

    val ringPulse by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "ring_pulse"
    )

    // ── density helpers ──────────────────────
    val localDensity = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val arcRadiusXPx: Float = with(localDensity) { (screenWidthDp * 0.44f).dp.roundToPx().toFloat() }
    val arcRadiusYPx: Float = with(localDensity) { 100.dp.roundToPx().toFloat() }
    val arcCenterYPx: Float = with(localDensity) { 30.dp.roundToPx().toFloat() }

    // ── centre book (stable during animation) ─
    val centerBookIndex = remember(rotationStep, displayBooks.size) {
        var bestIdx = 0
        var bestDist = Float.MAX_VALUE
        val targetAngle = rotationStep * ORBITAL_STEP_DEGREES
        displayBooks.forEachIndexed { i, _ ->
            val norm = orbitalNormalize(i * ORBITAL_STEP_DEGREES + targetAngle)
            val d = kotlin.math.abs(norm)
            if (d < bestDist) { bestDist = d; bestIdx = i }
        }
        bestIdx
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
            .pointerInput(displayBooks.size) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragAngle = displayedAngle
                    },
                    onDragEnd = {
                        rotationStep = Math.round(dragAngle / ORBITAL_STEP_DEGREES)
                        isDragging = false
                    },
                    onDragCancel = {
                        rotationStep = Math.round(dragAngle / ORBITAL_STEP_DEGREES)
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val degPerPx = 180f / (3.14159265f * arcRadiusXPx)
                        dragAngle += dragAmount * degPerPx
                    }
                )
            }
    ) {
        // ── orbit ring with gaps at book positions ───
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.42f
            val rx = size.width * 0.44f
            val ry = size.height * 0.20f
            val ovalTopLeft = Offset(cx - rx, cy - ry)
            val ovalSize = Size(rx * 2, ry * 2)

            val bookCanvasAngles = displayBooks.mapIndexed { i, _ ->
                val ourAngle = orbitalNormalize(i * ORBITAL_STEP_DEGREES + displayedAngle)
                90f - ourAngle
            }.sorted()

            val gapHalf = 10f
            for (j in bookCanvasAngles.indices) {
                val a0 = bookCanvasAngles[j]
                var a1 = bookCanvasAngles[(j + 1) % bookCanvasAngles.size]
                var sweep = a1 - a0
                if (sweep <= 0f) sweep += 360f
                val arcSweep = sweep - 2 * gapHalf
                if (arcSweep > 0f) {
                    drawArc(
                        color = SpaceGlowColors.GlowCyan.copy(alpha = 0.06f + ringPulse * 0.08f),
                        startAngle = a0 + gapHalf,
                        sweepAngle = arcSweep,
                        useCenter = false,
                        topLeft = ovalTopLeft,
                        size = ovalSize,
                        style = Stroke(width = 8.dp.toPx())
                    )
                    drawArc(
                        color = SpaceGlowColors.GlowCyan.copy(alpha = 0.18f + ringPulse * 0.17f),
                        startAngle = a0 + gapHalf,
                        sweepAngle = arcSweep,
                        useCenter = false,
                        topLeft = ovalTopLeft,
                        size = ovalSize,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }

        // ── books: sorted by depth (far first, close last) ──
        //    Single pass – smooth continuous scale, no instantaneous swap
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val sortedIndices = displayBooks.indices.sortedBy { i ->
                val norm = orbitalNormalize(i * ORBITAL_STEP_DEGREES + displayedAngle)
                val angleRad = Math.toRadians(norm.toDouble()).toFloat()
                arcCenterYPx - arcRadiusYPx * (1f - cos(angleRad))
            }

            sortedIndices.forEach { i ->
                val book = displayBooks[i]
                val norm = orbitalNormalize(i * ORBITAL_STEP_DEGREES + displayedAngle)
                val dist = kotlin.math.abs(norm)
                val t = (dist / 180f).coerceIn(0f, 1f)
                val angleRad = Math.toRadians(norm.toDouble()).toFloat()
                val rawPx = arcRadiusXPx * sin(angleRad)
                val rawPy = arcCenterYPx - arcRadiusYPx * (1f - cos(angleRad))
                val isCenter = i == centerBookIndex
                val floatY = if (isCenter) with(localDensity) { centerFloat.dp.roundToPx().toFloat() } else 0f

                // smooth continuous scale – no jump at centre swap
                val s = 1f - t * 0.55f

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = rawPx
                            translationY = rawPy + floatY
                            scaleX = s; scaleY = s
                            alpha = 1f - t * 0.5f
                            if (isCenter) rotationZ = centerFloat / 5f
                            cameraDistance = 12f * density
                        },
                    contentAlignment = Alignment.Center
                ) {
                    OrbitalBookItem(book = book)
                }
            }
        }

        // ── static title + buttons (no AnimatedContent, no flicker) ──
        val centerBook = displayBooks.getOrNull(centerBookIndex)
        if (centerBook != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(PaddingValues(start = 24.dp, end = 24.dp, bottom = 8.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = centerBook.title,
                    style = BookVerseTheme.typography.headline,
                    color = BookVerseTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val isContinueReading = continueReadingIds.contains(centerBook.id)
                    Button(
                        onClick = { onReadClick(centerBook) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BookVerseTheme.colors.primary
                        ),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .shadow(10.dp, RoundedCornerShape(30.dp)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isContinueReading) stringResource(R.string.home_continue) else stringResource(R.string.home_read_now),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { onInfoClick(centerBook) },
                        shape = RoundedCornerShape(30.dp),
                        border = BorderStroke(1.dp, BookVerseTheme.colors.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BookVerseTheme.colors.textPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_more_info),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrbitalBookItem(book: Book, coverWidth: Dp = 130.dp, coverHeight: Dp = 195.dp) {
    Box(
        modifier = Modifier
            .width(coverWidth)
            .height(coverHeight)
            .shadow(20.dp, RoundedCornerShape(4.dp))
            .background(Color(0xFF0D0D15), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = book.getBestCoverUrl(),
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
            error = painterResource(id = R.drawable.ic_book)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f)
                        )
                    )
                )
        )
    }
}

@Composable
fun CategoryRow(title: String, books: List<Book>, onBookClick: (Book) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = title, style = BookVerseTheme.typography.headline, color = BookVerseTheme.colors.textPrimary, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(books, key = { it.id }) { book -> BookPoster(book, onClick = { onBookClick(book) }) }
        }
    }
}

@Composable
fun BookPoster(book: Book, onClick: () -> Unit) {
    val isDark = BookVerseTheme.colors.isDark
    
    Column(modifier = Modifier.width(120.dp).clickable(onClick = onClick)) {
        Box(modifier = Modifier.height(180.dp).fillMaxWidth().then(if (!isDark) Modifier.shadow(8.dp, RoundedCornerShape(8.dp)) else Modifier).clip(RoundedCornerShape(8.dp)).then(if (!isDark) Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp)) else Modifier).background(BookVerseTheme.colors.surface)) {
            val imageUrl = book.getBestCoverUrl()
            
            AsyncImage(
                model = imageUrl, 
                contentDescription = book.title, 
                contentScale = ContentScale.Crop, 
                modifier = Modifier.fillMaxSize(),
                error = painterResource(id = R.drawable.ic_book)
            )
        }
    }
}
