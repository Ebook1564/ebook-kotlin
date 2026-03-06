package com.example.ebook_new_app.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.utils.FileDownloader
import com.example.ebook_new_app.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onThemeToggle: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalBookTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var loadingBookId by remember { mutableStateOf<String?>(null) }

    fun onReadClick(book: Book) {
        loadingBookId = book.id
        scope.launch {
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
                Toast.makeText(context, "Crystallization failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.primary)
            }
        } else if (uiState.billboardBook == null && uiState.trendingBooks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Library empty", color = theme.onBackground, fontFamily = theme.fontFamily)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadHomeData() },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.onBackground)
                ) {
                    Text("Retry", color = theme.background)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "EBookBundle",
                            color = theme.primary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 24.sp
                        )
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Toggle Theme",
                                tint = theme.onBackground
                            )
                        }
                    }
                }

                uiState.billboardBook?.let { book ->
                    item {
                        BillboardSection(
                            book = book, 
                            isLoading = loadingBookId == book.id,
                            onReadClick = { onReadClick(book) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
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
                                loadingBookId = loadingBookId,
                                onBookClick = { onReadClick(it) }
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun BillboardSection(book: Book, isLoading: Boolean, onReadClick: () -> Unit) {
    val theme = LocalBookTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .shadow(8.dp)
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
                            theme.background.copy(alpha = 0.4f),
                            theme.background
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
                color = theme.onBackground,
                fontFamily = theme.fontFamily,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = theme.fontFamily,
                color = theme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row {
                Button(
                    onClick = onReadClick,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.width(140.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = theme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(
                            "READ NOW", 
                            color = theme.onPrimary, 
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { /* More Info */ },
                    border = BorderStroke(1.dp, theme.onBackground),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.onBackground)
                ) {
                    Text("MY LIST", color = theme.onBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CategoryRow(title: String, books: List<Book>, loadingBookId: String?, onBookClick: (Book) -> Unit) {
    val theme = LocalBookTheme.current
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            color = theme.onBackground,
            fontFamily = theme.fontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(books) { book ->
                BookPoster(book, isLoading = loadingBookId == book.id, onClick = { onBookClick(book) })
            }
        }
    }
}

@Composable
fun BookPoster(book: Book, isLoading: Boolean, onClick: () -> Unit) {
    val theme = LocalBookTheme.current
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(enabled = !isLoading, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .border(1.dp, theme.onBackground.copy(alpha = 0.2f))
                .background(theme.onBackground.copy(alpha = 0.05f))
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
                    modifier = Modifier.align(Alignment.Center).size(40.dp),
                    tint = theme.onBackground.copy(alpha = 0.3f)
                )
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(theme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(30.dp), color = theme.primary)
                }
            }
        }
    }
}
