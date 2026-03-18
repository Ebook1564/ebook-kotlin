package com.example.ebook_new_app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.AppThemeMode
import com.example.ebook_new_app.viewmodel.AppViewModel
import com.example.ebook_new_app.viewmodel.CuratedCollection
import com.example.ebook_new_app.viewmodel.HomeViewModel
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
    val curatedCollections = uiState.curatedCollections
    val backgroundColor = BookVerseTheme.colors.background
    
    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        if (uiState.isLoading && homeSections.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BookVerseTheme.colors.primary)
            }
        } else {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp) 
            ) {
                item { HeaderSection(appViewModel, userName) }
                if (billboardBooks.isNotEmpty()) {
                    item { BillboardCarousel(books = billboardBooks, onReadClick = onReadNowClick, onInfoClick = onBookClick) }
                }
                if (curatedCollections.isNotEmpty()) {
                    item { CuratedCollectionsRow(collections = curatedCollections, onCollectionClick = { viewModel.openCuratedCollection(it) }) }
                }
                homeSections.forEach { section ->
                    item { CategoryRow(title = section.title, books = section.books, onBookClick = onBookClick) }
                }
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
                contentDescription = "Toggle Theme",
                tint = BookVerseTheme.colors.primary
            )
        }
    }
}

@Composable
fun DynamicGreeting(userName: String) {
    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    Column {
        Text(text = "$greeting, $userName", style = BookVerseTheme.typography.headline, color = BookVerseTheme.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(text = "Ready for a new adventure?", style = BookVerseTheme.typography.body, color = BookVerseTheme.colors.textSecondary, fontSize = 13.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
fun BillboardCarousel(books: List<Book>, onReadClick: (Book) -> Unit, onInfoClick: (Book) -> Unit) {
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
            BillboardItem(book = books[page], onReadClick = { onReadClick(books[page]) }, onInfoClick = { onInfoClick(books[page]) }, pagerOffset = pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
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
fun BillboardItem(book: Book, onReadClick: () -> Unit, onInfoClick: () -> Unit, pagerOffset: Float) {
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
                    
                    // Priority: External API > Local DB
                    val imageUrl = displayBook.externalCoverUrl ?: displayBook.coverImageUrl
                    
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
                Button(onClick = onReadClick, colors = ButtonDefaults.buttonColors(containerColor = BookVerseTheme.colors.primary), shape = RoundedCornerShape(30.dp), modifier = Modifier.weight(1f).height(48.dp).shadow(12.dp, RoundedCornerShape(30.dp))) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text("READ NOW", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
                OutlinedButton(onClick = onInfoClick, shape = RoundedCornerShape(30.dp), border = BorderStroke(1.dp, BookVerseTheme.colors.primary.copy(0.5f)), modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = BookVerseTheme.colors.textPrimary)) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("MORE INFO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
fun CuratedCollectionsRow(
    collections: List<CuratedCollection>,
    onCollectionClick: (CuratedCollection) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = "Curated For You",
            style = BookVerseTheme.typography.headline,
            color = BookVerseTheme.colors.textPrimary,
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(collections) { collection ->
                CuratedCollectionCard(
                    collection = collection,
                    onClick = { onCollectionClick(collection) }
                )
            }
        }
    }
}

@Composable
fun CuratedCollectionCard(
    collection: CuratedCollection,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = collection.accentColor,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            collection.accentColor,
                            collection.accentColor.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = collection.iconEmoji,
                    fontSize = 24.sp
                )
                Column {
                    Text(
                        text = collection.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = collection.subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
            val imageUrl = displayBook.externalCoverUrl ?: displayBook.coverImageUrl
            
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
