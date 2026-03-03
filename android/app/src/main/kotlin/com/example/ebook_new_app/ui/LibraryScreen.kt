package com.example.ebook_new_app.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.reader.ReaderActivity
import com.example.ebook_new_app.utils.FileDownloader
import com.example.ebook_new_app.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    onProfileClick: () -> Unit = {}
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
                val intent = Intent(context, ReaderActivity::class.java).apply {
                    putExtra("FILE_PATH", file.absolutePath)
                    putExtra("BOOK_TITLE", book.title)
                    putExtra("BOOK_ID", book.id)
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Failed to open book", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.background)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "MY LIBRARY", 
                    color = theme.onBackground, 
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold, 
                    fontSize = 24.sp,
                    letterSpacing = 2.sp
                )
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(theme.onBackground.copy(alpha = 0.1f))
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person, 
                        contentDescription = "Profile",
                        tint = theme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        containerColor = theme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    LibraryShelf(title = "CURRENTLY READING", books = uiState.currentlyReading, loadingBookId = loadingBookId, onReadClick = ::onReadClick)
                }
                item {
                    LibraryShelf(title = "WANT TO READ", books = uiState.wishlist, loadingBookId = loadingBookId, onReadClick = ::onReadClick)
                }
                // Placeholder sections
                item {
                    // Using currentlyReading as a placeholder for 'FINISHED' for UI demonstration
                    LibraryShelf(title = "FINISHED", books = uiState.currentlyReading.take(2), loadingBookId = null, onReadClick = {})
                }
                item {
                    // Using wishlist as a placeholder for 'SIMILAR TO WHAT YOU'VE READ' for UI demonstration
                    LibraryShelf(title = "SIMILAR TO WHAT YOU'VE READ", books = uiState.wishlist.drop(2).take(3), loadingBookId = null, onReadClick = {})
                }
            }
        }
    }
}

@Composable
fun LibraryShelf(
    title: String,
    books: List<Book>,
    loadingBookId: String?,
    onReadClick: (Book) -> Unit
) {
    val theme = LocalBookTheme.current
    Column {
        Text(
            text = title,
            color = theme.onBackground,
            fontFamily = theme.fontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (books.isEmpty()) {
            Text("No books on this shelf yet.", color = theme.onBackground.copy(alpha = 0.5f), fontFamily = theme.fontFamily, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                books.forEach { book ->
                    LibraryBookItem(book, isLoading = loadingBookId == book.id) { onReadClick(book) }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun LibraryBookItem(book: Book, isLoading: Boolean, onClick: () -> Unit) {
    val theme = LocalBookTheme.current
    val progress = 0.6f // Hardcoded progress for demonstration

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.onBackground.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(4.dp),
        onClick = onClick,
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.width(80.dp).height(120.dp)) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
                )
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize().background(theme.background.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = theme.primary, strokeWidth = 2.dp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, color = theme.onBackground, fontWeight = FontWeight.Bold, fontFamily = theme.fontFamily, maxLines = 2, fontSize = 18.sp)
                Text(book.author, color = theme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = theme.fontFamily)
                Spacer(modifier = Modifier.weight(1f))

                // "Hot Bar" Progress Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = progress, 
                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = theme.primary,
                        trackColor = theme.onBackground.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = theme.onBackground.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
