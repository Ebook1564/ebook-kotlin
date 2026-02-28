
 package com.example.ebookapp.ui

 import android.content.Intent
 import android.widget.Toast
 import androidx.compose.foundation.background
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
 import com.example.ebookapp.models.Book
 import com.example.ebookapp.ui.components.PremiumBadge
 import com.example.ebookapp.utils.FileDownloader
 import com.example.ebookapp.viewmodel.HomeViewModel
 import kotlinx.coroutines.launch

 @Composable
 fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
     val uiState by viewModel.uiState.collectAsState()
     val backgroundColor = Color(0xFF0F0F0F)
     val context = LocalContext.current
     val scope = rememberCoroutineScope()

     var loadingBookId by remember { mutableStateOf<String?>(null) }

     fun onReadClick(book: Book) {
         loadingBookId = book.id
         scope.launch {
             val file = FileDownloader.ensureCached(context, book)
             loadingBookId = null

             if (file != null) {
                 val intent = Intent(context, com.example.ebookapp.reader.ReaderActivity::class.java).apply {
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
                             onBookClick = { onReadClick(it) }
                         )
                     }
                 }

                 if (uiState.trendingBooks.isNotEmpty()) {
                     item {
                         CategoryRow(
                             title = "Trending Now",
                             books = uiState.trendingBooks,
                             loadingBookId = loadingBookId,
                             onBookClick = { onReadClick(it) }
                         )
                     }
                 }

                 if (uiState.newReleases.isNotEmpty()) {
                     item {
                         CategoryRow(
                             title = "New Releases",
                             books = uiState.newReleases,
                             loadingBookId = loadingBookId,
                             onBookClick = { onReadClick(it) }
                         )
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
     Box(
         modifier = Modifier
             .fillMaxWidth()
             .height(500.dp)
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
                             Color.Black.copy(alpha = 0.2f),
                             Color(0xFF0F0F0F)
                         ),
                         startY = 600f
                     )
                 )
         )

         Column(
             modifier = Modifier
                 .align(Alignment.BottomStart)
                 .padding(32.dp),
         ) {
             if (book.isPremium) {
                 PremiumBadge()
                 Spacer(modifier = Modifier.height(8.dp))
             }
             Text(
                 text = book.title,
                 style = MaterialTheme.typography.headlineLarge,
                 color = Color.White,
                 fontWeight = FontWeight.Bold,
             )
             Text(
                 text = "By ${book.author}",
                 style = MaterialTheme.typography.bodyMedium,
                 color = Color.White.copy(alpha = 0.7f)
             )
             Spacer(modifier = Modifier.height(16.dp))
             Row {
                 Button(
                     onClick = onReadClick,
                     enabled = !isLoading,
                     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                     shape = RoundedCornerShape(8.dp),
                 ) {
                     if (isLoading) {
                         CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                     } else {
                         Text(
                             "Read Now",
                             color = Color.White,
                             fontWeight = FontWeight.Bold,
                         )
                     }
                 }
                 Spacer(modifier = Modifier.width(12.dp))
                 OutlinedButton(
                     onClick = { /* More Info */ },
                     shape = RoundedCornerShape(8.dp),
                     colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                 ) {
                     Text("Add to Library", color = Color.White, fontWeight = FontWeight.Bold)
                 }
             }
         }
     }
 }

 @Composable
 fun CategoryRow(title: String, books: List<Book>, loadingBookId: String?, onBookClick: (Book) -> Unit) {
     Column(modifier = Modifier.padding(vertical = 16.dp)) {
         Text(
             text = title,
             color = Color.White,
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
     Card(
         modifier = Modifier
             .width(120.dp)
             .clickable(enabled = !isLoading, onClick = onClick),
         shape = RoundedCornerShape(8.dp),
         colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
     ) {
         Box(
             modifier = Modifier
                 .height(180.dp)
                 .fillMaxWidth()
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

