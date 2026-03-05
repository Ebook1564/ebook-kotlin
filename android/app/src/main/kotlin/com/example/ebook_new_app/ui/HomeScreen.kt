package com.example.ebook_new_app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.AppThemeMode
import com.example.ebook_new_app.viewmodel.AppViewModel
import com.example.ebook_new_app.viewmodel.HomeViewModel
import com.example.ebook_new_app.viewmodel.InitialBounds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = BookVerseTheme.colors.background
    
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    fun onReadClick(book: Book, bounds: InitialBounds?) {
        showSheet = false
        viewModel.startReadTransition(book, bounds)
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BookVerseTheme.colors.primary)
            }
        } else {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize()
            ) {
                // MOVE HEADER SECTION HERE (inside Home Only)
                item {
                    HeaderSection(appViewModel, uiState.userName)
                }

                uiState.billboardBook?.let { book ->
                    item {
                        BillboardSection(
                            book = book, 
                            onReadClick = { onReadClick(book, null) }
                        )
                    }
                }

                val categories = listOf(
                    "Continue Reading" to uiState.continueReading,
                    "Trending Now" to uiState.trendingBooks,
                    "New Releases" to uiState.newReleases
                )

                categories.forEach { (title, books) ->
                    if (books.isNotEmpty()) {
                        item {
                            CategoryRow(
                                title = title, 
                                books = books,
                                onBookClick = { 
                                    selectedBook = it
                                    showSheet = true
                                }
                            )
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }

        if (showSheet && selectedBook != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = BookVerseTheme.colors.surface,
                contentColor = BookVerseTheme.colors.textPrimary,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                BookDetailContent(
                    book = selectedBook!!,
                    isLoading = false,
                    onReadClick = { bounds -> onReadClick(selectedBook!!, bounds) },
                    onClose = { showSheet = false }
                )
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
            .statusBarsPadding() // Correct padding from top
            .padding(horizontal = 24.dp, vertical = 20.dp),
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
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Column {
        Text(
            text = "$greeting, Aayush", // Fixed name as requested or dynamic
            style = BookVerseTheme.typography.headline,
            color = BookVerseTheme.colors.textPrimary,
            fontSize = 22.sp, // Refined size
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
fun BillboardSection(book: Book, onReadClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        AsyncImage(
            model = book.coverImageUrl.ifEmpty { "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?q=80&w=1000" },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BookVerseTheme.colors.background.copy(alpha = 0.5f),
                            BookVerseTheme.colors.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = book.title,
                style = BookVerseTheme.typography.headline,
                color = BookVerseTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
                fontSize = 32.sp
            )
            Text(
                text = book.author,
                style = BookVerseTheme.typography.body,
                color = BookVerseTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onReadClick,
                colors = ButtonDefaults.buttonColors(containerColor = BookVerseTheme.colors.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(160.dp).height(48.dp)
            ) {
                Text("READ NOW", color = Color.White, fontWeight = FontWeight.ExtraBold)
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
    
    val gradientBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFE50914),
                Color(0xFF00C6FF)
            )
        )
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
                    if (isDark) Modifier.border(1.dp, gradientBrush, RoundedCornerShape(8.dp))
                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                )
                .background(BookVerseTheme.colors.surface)
        ) {
            if (book.coverImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = BookVerseTheme.colors.textSecondary
                )
            }
        }
    }
}
