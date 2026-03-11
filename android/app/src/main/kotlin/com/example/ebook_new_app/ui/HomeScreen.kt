package com.example.ebook_new_app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.AppThemeMode
import com.example.ebook_new_app.viewmodel.AppViewModel
import com.example.ebook_new_app.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel(),
    scrollState: LazyListState = rememberLazyListState(),
    onBookClick: (Book) -> Unit,
    onReadNowClick: (Book) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = BookVerseTheme.colors.background
    
    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BookVerseTheme.colors.primary)
            }
        } else {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp) 
            ) {
                item { HeaderSection(appViewModel, uiState.userName) }

                if (uiState.billboardBooks.isNotEmpty()) {
                    item {
                        BillboardCarousel(
                            books = uiState.billboardBooks,
                            onReadClick = onReadNowClick,
                            onInfoClick = onBookClick
                        )
                    }
                }

                uiState.sections.forEach { section ->
                    item {
                        CategoryRow(
                            title = section.title, 
                            books = section.books,
                            onBookClick = onBookClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(appViewModel: AppViewModel, userName: String) {
    val themeMode = appViewModel.themeMode.value
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DynamicGreeting(userName)
        
        IconButton(
            onClick = { appViewModel.toggleTheme() },
            modifier = Modifier
                .clip(CircleShape)
                .background(BookVerseTheme.colors.surface.copy(alpha = if(themeMode == AppThemeMode.NIGHT) 0.3f else 0.8f))
        ) {
            Icon(
                imageVector = if (themeMode == AppThemeMode.NIGHT) Icons.Default.LightMode else Icons.Default.Nightlight,
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
        Text(
            text = "$greeting, Aayush",
            style = BookVerseTheme.typography.headline,
            color = BookVerseTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Ready for a new adventure?",
            style = BookVerseTheme.typography.body,
            color = BookVerseTheme.colors.textSecondary,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun BillboardCarousel(
    books: List<Book>,
    onReadClick: (Book) -> Unit,
    onInfoClick: (Book) -> Unit
) {
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) { page ->
            BillboardItem(
                book = books[page],
                onReadClick = { onReadClick(books[page]) },
                onInfoClick = { onInfoClick(books[page]) },
                pagerOffset = pagerState.currentPage - page + pagerState.currentPageOffsetFraction
            )
        }
        
        Row(
            Modifier
                .height(20.dp)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(books.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) BookVerseTheme.colors.primary else BookVerseTheme.colors.primary.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(if (pagerState.currentPage == iteration) 8.dp else 6.dp)
                )
            }
        }
    }
}

@Composable
fun BillboardItem(
    book: Book,
    onReadClick: () -> Unit,
    onInfoClick: () -> Unit,
    pagerOffset: Float
) {
    val infiniteTransition = rememberInfiniteTransition()
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, BookVerseTheme.colors.background)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(bottom = 20.dp)
                .graphicsLayer {
                    alpha = 1f - (kotlin.math.abs(pagerOffset) * 0.5f)
                    translationX = pagerOffset * 300f
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .padding(top = 260.dp)
                        .width(180.dp)
                        .height(20.dp)
                        .graphicsLayer {
                            rotationX = 80f
                            scaleX = 1.2f + (floatAnim / 40f)
                        }
                        .background(
                            Brush.radialGradient(
                                listOf(Color.Black.copy(0.4f), Color.Transparent)
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(270.dp)
                        .graphicsLayer {
                            rotationY = -25f + (pagerOffset * 10f)
                            rotationX = 10f
                            rotationZ = floatAnim / 4f
                            translationY = floatAnim
                            cameraDistance = 12f * density
                        }
                        .shadow(24.dp, RoundedCornerShape(4.dp))
                ) {
                    val serverIp = "10.0.2.2"
                    var fallbackStage by remember { mutableIntStateOf(0) }
                    
                    // START with the URL generated by toBook() which handles the law folder pattern
                    val imageUrl = when(fallbackStage) {
                        0 -> book.coverImageUrl
                        1 -> if (book.coverImageUrl.contains("cover.")) "http://$serverIp:8080/${book.id}_${book.id}-cover.png" else "http://$serverIp:8080/${android.net.Uri.encode(book.title)}_cover.${book.id}"
                        2 -> "http://$serverIp:8080/${book.id}_${book.id}-cover.jpg"
                        else -> ""
                    }

                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(4.dp)),
                        error = {
                            if (fallbackStage < 2) {
                                LaunchedEffect(imageUrl) { fallbackStage++ }
                                Box(Modifier.fillMaxSize().background(BookVerseTheme.colors.surface))
                            } else {
                                Box(Modifier.fillMaxSize().background(BookVerseTheme.colors.surface), contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = BookVerseTheme.colors.textSecondary)
                                }
                            }
                        }
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color.White.copy(0.1f), Color.Transparent, Color.Black.copy(0.2f))
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = book.title,
                style = BookVerseTheme.typography.headline,
                color = BookVerseTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
                fontSize = 24.sp,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onReadClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BookVerseTheme.colors.primary),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.weight(1f).height(48.dp).shadow(12.dp, RoundedCornerShape(30.dp))
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("READ NOW", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }

                OutlinedButton(
                    onClick = onInfoClick,
                    shape = RoundedCornerShape(30.dp),
                    border = BorderStroke(1.dp, BookVerseTheme.colors.primary.copy(0.5f)),
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BookVerseTheme.colors.textPrimary)
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MORE INFO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CategoryRow(title: String, books: List<Book>, onBookClick: (Book) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = BookVerseTheme.typography.headline,
            color = BookVerseTheme.colors.textPrimary,
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(books) { book ->
                BookPoster(book, onClick = { onBookClick(book) })
            }
        }
    }
}

@Composable
fun BookPoster(book: Book, onClick: () -> Unit) {
    val isDark = BookVerseTheme.colors.isDark
    val serverIp = "10.0.2.2"
    var fallbackStage by remember { mutableIntStateOf(0) }
    
    val imageUrl = when(fallbackStage) {
        0 -> book.coverImageUrl
        1 -> if (book.coverImageUrl.contains("cover.")) "http://$serverIp:8080/${book.id}_${book.id}-cover.png" else "http://$serverIp:8080/${android.net.Uri.encode(book.title)}_cover.${book.id}"
        2 -> "http://$serverIp:8080/${book.id}_${book.id}-cover.jpg"
        else -> ""
    }
    
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
                .then(
                    if (!isDark) Modifier.shadow(8.dp, RoundedCornerShape(8.dp))
                    else Modifier
                )
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (!isDark) Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    else Modifier
                )
                .background(BookVerseTheme.colors.surface)
        ) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    if (fallbackStage < 2) {
                        LaunchedEffect(imageUrl) { fallbackStage++ }
                        Box(Modifier.fillMaxSize().background(BookVerseTheme.colors.surface))
                    } else {
                        Box(Modifier.fillMaxSize().background(BookVerseTheme.colors.surface), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = BookVerseTheme.colors.textSecondary)
                        }
                    }
                }
            )
        }
    }
}
