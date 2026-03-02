package com.example.ebook_new_app.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.utils.FileDownloader
import com.example.ebook_new_app.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = Color(0xFF000000)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var loadingBookId by remember { mutableStateOf<String?>(null) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    fun onReadClick(book: Book) {
        showSheet = false
        loadingBookId = book.id
        scope.launch {
            // FileDownloader.ensureCached handles both Assets and Cloud URL
            val file = FileDownloader.ensureCached(context, book)
            loadingBookId = null

            if (file != null) {
                val intent = Intent(context, com.example.ebook_new_app.reader.ReaderActivity::class.java).apply {
                    putExtra("FILE_PATH", file.absolutePath)
                    putExtra("BOOK_TITLE", book.title)
                    putExtra("BOOK_ID", book.id)
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Book crystallization failed. Check connection!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE50914))
            }
        } else if (uiState.billboardBook == null && uiState.trendingBooks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No books found", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.loadHomeData() }) {
                    Text("Retry")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                uiState.billboardBook?.let { book ->
                    item {
                        BillboardSection(
                            book = book, 
                            isLoading = loadingBookId == book.id,
                            onReadClick = { onReadClick(book) }
                        )
                    }
                }

                if (uiState.continueReading.isNotEmpty()) {
                    item {
                        CategoryRow(
                            title = "Continue Reading", 
                            books = uiState.continueReading,
                            loadingBookId = loadingBookId,
                            onBookClick = { 
                                selectedBook = it
                                showSheet = true
                            }
                        )
                    }
                }

                if (uiState.trendingBooks.isNotEmpty()) {
                    item {
                        CategoryRow(
                            title = "Trending Now", 
                            books = uiState.trendingBooks,
                            loadingBookId = loadingBookId,
                            onBookClick = { 
                                selectedBook = it
                                showSheet = true
                            }
                        )
                    }
                }

                if (uiState.newReleases.isNotEmpty()) {
                    item {
                        CategoryRow(
                            title = "New Releases", 
                            books = uiState.newReleases,
                            loadingBookId = loadingBookId,
                            onBookClick = { 
                                selectedBook = it
                                showSheet = true
                            }
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        if (showSheet && selectedBook != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                BookDetailContent(
                    book = selectedBook!!,
                    isLoading = loadingBookId == selectedBook!!.id,
                    onReadClick = { onReadClick(selectedBook!!) },
                    onClose = { showSheet = false }
                )
            }
        }
    }
}

@Composable
fun BillboardSection(book: Book, isLoading: Boolean, onReadClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
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
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black
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
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Button(
                    onClick = onReadClick,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.width(130.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            "READ NOW", 
                            color = Color.White, 
                            fontWeight = FontWeight.ExtraBold,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { /* More Info */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("MY LIST", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CategoryRow(title: String, books: List<Book>, loadingBookId: String?, onBookClick: (Book) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(books) { book ->
                BookPoster(book, isLoading = loadingBookId == book.id, onClick = { onBookClick(book) })
            }
        }
    }
}

@Composable
fun BookPoster(book: Book, isLoading: Boolean, onClick: () -> Unit) {
    val gradientBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFE50914),
                Color(0xFF00C6FF),
                Color(0xFF0072FF)
            )
        )
    }

    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable(enabled = !isLoading, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, gradientBrush, RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
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
                    tint = Color.Gray
                )
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(30.dp), color = Color(0xFFE50914))
                }
            }
        }
    }
}
