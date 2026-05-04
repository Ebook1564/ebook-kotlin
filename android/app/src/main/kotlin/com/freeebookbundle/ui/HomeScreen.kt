package com.freeebookbundle.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.yield

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
    onBookClick: (Book) -> Unit,
    onReadNowClick: (Book) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val homeSections by viewModel.homeSections.collectAsState()
    val billboardBooks by viewModel.billboardBooks.collectAsState()
    val continueReadingBooks by viewModel.continueReadingBooks.collectAsState()
    val backgroundColor = BookVerseTheme.colors.background
    val themeMode by appViewModel.themeMode
    
    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        if (themeMode == AppThemeMode.SPACE) {
            SpaceBackground()
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
                item { HeaderSection(appViewModel, userName) }
                
                // 2. BILLBOARD (HERO) - NOW AT TOP
                item {
                    if (billboardBooks.isNotEmpty()) {
                        BillboardCarousel(books = billboardBooks, continueReadingBooks = continueReadingBooks, onReadClick = onReadNowClick, onInfoClick = onBookClick)
                    } else if (uiState.isCatalogLoading) {
                        BillboardSkeleton()
                    }
                }

                // 3. CONTINUE READING (HIGH PRIORITY) - NOW BELOW BILLBOARD
                if (continueReadingBooks.isNotEmpty()) {
                    item { ContinueReadingSection(books = continueReadingBooks, onBookClick = onBookClick, onReadClick = onReadNowClick) }
                }
                
                // 4. SELECTED CATEGORIES (DYNAMICALY FILTERED BY VIEWMODEL)
                homeSections.forEach { section ->
                    item { 
                        if (section.books.isNotEmpty()) {
                            CategoryRow(title = section.title, books = section.books, onBookClick = onBookClick)
                        } else {
                            CategorySkeleton(title = section.title)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
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
fun BillboardSkeleton() {
    val brush = shimmerBrush()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .background(brush)
    )
}

@Composable
fun CategorySkeleton(title: String) {
    val brush = shimmerBrush()
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
            items(books.size) { index ->
                ContinueReadingBookCard(
                    book = books[index],
                    onClick = { onBookClick(books[index]) },
                    onReadClick = { onReadClick(books[index]) }
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
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..5 -> stringResource(R.string.greeting_night)
        in 6..11 -> stringResource(R.string.greeting_morning)
        in 12..16 -> stringResource(R.string.greeting_afternoon)
        in 17..20 -> stringResource(R.string.greeting_evening)
        else -> stringResource(R.string.greeting_night)
    }
    
    val punchlines = when (hour) {
        in 0..5 -> listOf(stringResource(R.string.punchline_quiet_world), stringResource(R.string.punchline_midnight_reading), stringResource(R.string.punchline_explore_worlds))
        in 6..8 -> listOf(stringResource(R.string.punchline_morning_story), stringResource(R.string.punchline_morning_reads), stringResource(R.string.punchline_rise_read))
        in 9..11 -> listOf(stringResource(R.string.punchline_new_adventure), stringResource(R.string.punchline_discover), stringResource(R.string.punchline_dive_book))
        in 12..14 -> listOf(stringResource(R.string.punchline_lunch_reading), stringResource(R.string.punchline_afternoon_escape), stringResource(R.string.punchline_reading_break))
        in 15..17 -> listOf(stringResource(R.string.punchline_momentum), stringResource(R.string.punchline_story_time), stringResource(R.string.punchline_chapter))
        in 18..20 -> listOf(stringResource(R.string.punchline_unwind), stringResource(R.string.punchline_evening), stringResource(R.string.punchline_perfect_stories))
        else -> listOf(stringResource(R.string.punchline_cozy), stringResource(R.string.punchline_nighttime), stringResource(R.string.punchline_transport))
    }
    
    val punchline = punchlines.random()
    
    Column {
        Text(text = "$greeting, $userName", style = BookVerseTheme.typography.headline, color = BookVerseTheme.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(text = punchline, style = BookVerseTheme.typography.body, color = BookVerseTheme.colors.textSecondary, fontSize = 13.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
fun BillboardCarousel(books: List<Book>, continueReadingBooks: List<Book>, onReadClick: (Book) -> Unit, onInfoClick: (Book) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { books.size })
    LaunchedEffect(Unit) {
        while (true) {
            yield()
            delay(5000)
            if (pagerState.pageCount > 0) {
                val nextNextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(nextNextPage)
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(480.dp), contentPadding = PaddingValues(horizontal = 0.dp)) { page ->
            val book = books[page]
            val isContinueReading = continueReadingBooks.any { it.id == book.id }
            BillboardItem(book = book, isContinueReading = isContinueReading, onReadClick = { onReadClick(book) }, onInfoClick = { onReadClick(book) }, pagerOffset = pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
        }
        Row(Modifier.height(20.dp).fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
            repeat(books.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) BookVerseTheme.colors.primary else BookVerseTheme.colors.primary.copy(alpha = 0.3f)
                val size by animateDpAsState(if (pagerState.currentPage == iteration) 8.dp else 6.dp, label = "dot")
                Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(size))
            }
        }
    }
}

@Composable
fun BillboardItem(book: Book, isContinueReading: Boolean, onReadClick: () -> Unit, onInfoClick: () -> Unit, pagerOffset: Float) {
    var displayBook by remember { mutableStateOf(book) }
    LaunchedEffect(book) { 
        if (displayBook.externalCoverUrl != book.externalCoverUrl || displayBook.description != book.description) {
            displayBook = book 
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val floatAnim by infiniteTransition.animateFloat(initialValue = -8f, targetValue = 8f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse), label = "float")
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().background(brush = Brush.verticalGradient(listOf(Color.Transparent, BookVerseTheme.colors.background))))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 20.dp).graphicsLayer { alpha = 1f - (kotlin.math.abs(pagerOffset) * 0.5f); translationX = pagerOffset * 300f }) {
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.padding(top = 260.dp).width(180.dp).height(20.dp).graphicsLayer { rotationX = 80f; scaleX = 1.2f + (floatAnim / 40f) }.background(Brush.radialGradient(listOf(Color.Black.copy(0.4f), Color.Transparent))))
                Box(modifier = Modifier.width(180.dp).height(270.dp).graphicsLayer { rotationY = -25f + (pagerOffset * 10f); rotationX = 10f; rotationZ = floatAnim / 4f; translationY = floatAnim; cameraDistance = 12f * density }.shadow(24.dp, RoundedCornerShape(4.dp))) {
                    
                    // Priority: External API > Server cover (via getBestCoverUrl)
                    val imageUrl = displayBook.getBestCoverUrl()
                    
                    // BLOOM ANIMATION
                    AnimatedContent(
                        targetState = imageUrl,
                        transitionSpec = { fadeIn(tween(1000)) + scaleIn(initialScale = 0.95f) togetherWith fadeOut(tween(600)) },
                        label = "bloom"
                    ) { targetUrl ->
                        SubcomposeAsyncImage(
                            model = targetUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(4.dp)),
                            error = {
                                SubcomposeAsyncImage(
                                    model = displayBook.coverImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    error = { ClassicEditionCover(displayBook.title, displayBook.author, Modifier.fillMaxSize()) }
                                )
                            }
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White.copy(0.1f), Color.Transparent, Color.Black.copy(0.2f)))))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = displayBook.title, style = BookVerseTheme.typography.headline, color = BookVerseTheme.colors.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp), fontSize = 24.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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

@Composable
fun CategoryRow(title: String, books: List<Book>, onBookClick: (Book) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = title, style = BookVerseTheme.typography.headline, color = BookVerseTheme.colors.textPrimary, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(books) { book -> BookPoster(book, onClick = { onBookClick(book) }) }
        }
    }
}

@Composable
fun BookPoster(book: Book, onClick: () -> Unit) {
    var displayBook by remember { mutableStateOf(book) }
    LaunchedEffect(book) { 
        if (displayBook.externalCoverUrl != book.externalCoverUrl) {
            displayBook = book 
        }
    }

    val isDark = BookVerseTheme.colors.isDark
    
    Column(modifier = Modifier.width(120.dp).clickable(onClick = onClick)) {
        Box(modifier = Modifier.height(180.dp).fillMaxWidth().then(if (!isDark) Modifier.shadow(8.dp, RoundedCornerShape(8.dp)) else Modifier).clip(RoundedCornerShape(8.dp)).then(if (!isDark) Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp)) else Modifier).background(BookVerseTheme.colors.surface)) {
            val imageUrl = displayBook.getBestCoverUrl()
            
            // BLOOM ANIMATION
            AnimatedContent(
                targetState = imageUrl,
                transitionSpec = { fadeIn(tween(1000)) + scaleIn(initialScale = 0.95f) togetherWith fadeOut(tween(600)) },
                label = "bloom"
            ) { targetUrl ->
                SubcomposeAsyncImage(
                    model = targetUrl, 
                    contentDescription = displayBook.title, 
                    contentScale = ContentScale.Crop, 
                    modifier = Modifier.fillMaxSize(),
                    error = {
                        SubcomposeAsyncImage(
                            model = displayBook.coverImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = { ClassicEditionCover(displayBook.title, displayBook.author, Modifier.fillMaxSize()) }
                        )
                    }
                )
            }
        }
    }
}
