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
                    .padding(padding)
            ) {
                item {
                    Text(
                        "CURRENTLY READING",
                        color = theme.onBackground,
                        fontFamily = theme.fontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                if (uiState.currentlyReading.isEmpty()) {
                    item {
                        Text("No books here yet.", color = theme.onBackground.copy(alpha = 0.5f), fontFamily = theme.fontFamily, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    items(uiState.currentlyReading) { book ->
                        LibraryBookItem(book, isLoading = loadingBookId == book.id) { onReadClick(book) }
                    }
                }

                item {
                    Text(
                        "WISHLIST",
                        color = theme.onBackground,
                        fontFamily = theme.fontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (uiState.wishlist.isEmpty()) {
                    item {
                        Text("Your wishlist is empty.", color = theme.onBackground.copy(alpha = 0.5f), fontFamily = theme.fontFamily, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    items(uiState.wishlist) { book ->
                        LibraryBookItem(book, isLoading = loadingBookId == book.id) { onReadClick(book) }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryBookItem(book: Book, isLoading: Boolean, onClick: () -> Unit) {
    val theme = LocalBookTheme.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(0.5.dp, theme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(2.dp)),
        colors = CardDefaults.cardColors(containerColor = theme.onBackground.copy(alpha = 0.02f)),
        shape = RoundedCornerShape(2.dp),
        onClick = onClick,
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(110.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(75.dp).fillMaxHeight()) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp)).border(0.5.dp, theme.onBackground.copy(alpha = 0.1f))
                )
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize().background(theme.background.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = theme.primary, strokeWidth = 2.dp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, color = theme.onBackground, fontWeight = FontWeight.Bold, fontFamily = theme.fontFamily, maxLines = 2)
                Text(book.author, color = theme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = theme.fontFamily)
                Spacer(modifier = Modifier.weight(1f))
                LinearProgressIndicator(
                    progress = 0.4f, 
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = theme.primary,
                    trackColor = theme.onBackground.copy(alpha = 0.1f)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Button(
                onClick = onClick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text("READ", color = theme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
