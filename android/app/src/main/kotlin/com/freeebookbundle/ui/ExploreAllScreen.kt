package com.freeebookbundle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.freeebookbundle.EbookApp
import com.freeebookbundle.R
import com.freeebookbundle.db.CategoryEntity
import com.freeebookbundle.db.toBook
import com.freeebookbundle.models.Book
import com.freeebookbundle.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class SearchResult(
    val books: List<Book> = emptyList(),
    val authors: List<Pair<String, Int>> = emptyList(), // author name to book count
    val genres: List<Pair<String, Color>> = emptyList() // genre name to color
)

@Composable
fun ExploreAllScreen(
    onBack: () -> Unit,
    onBookClick: (Book) -> Unit,
    onGenreClick: (String) -> Unit,
    appViewModel: AppViewModel
) {
    val colors = BookVerseTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(SearchResult()) }
    var isSearching by remember { mutableStateOf(false) }

    // Real-time search — debounced, runs on IO to avoid blocking main thread
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            delay(300) // debounce
            isSearching = true
            val results = withContext(Dispatchers.IO) {
                val database = (appViewModel.getApplication() as EbookApp).database
                val bookDao = database.bookDao()

                val books = bookDao.searchBooks(searchQuery).map { it.toBook() }
                val authors = bookDao.searchAuthors(searchQuery).map { authorName ->
                    authorName to bookDao.getBookCountByAuthor(authorName)
                }
                val categories = bookDao.searchCategories(searchQuery).map { category ->
                    category.name to getCategoryColor(category.name)
                }
                SearchResult(books = books, authors = authors, genres = categories)
            }
            searchResults = results
            isSearching = false
        } else {
            searchResults = SearchResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Header with back button and search icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = "Explore All",
                style = BookVerseTheme.typography.headline,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /* Search focus handled by text field */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.textSecondary
                )
            }
        }

        // Search bar
        BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = colors.textPrimary,
                fontSize = 16.sp
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search books, authors, genres...",
                            color = colors.textSecondary
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Results
        if (searchQuery.isBlank()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Search for books, authors, or genres",
                    color = colors.textSecondary
                )
            }
        } else if (isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Genres section
                if (searchResults.genres.isNotEmpty()) {
                    item {
                        Text(
                            text = "Genres",
                            style = BookVerseTheme.typography.label,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(searchResults.genres) { (genreName, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surface)
                                .clickable { onGenreClick(genreName) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = genreName.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = genreName,
                                style = BookVerseTheme.typography.body,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                // Authors section
                if (searchResults.authors.isNotEmpty()) {
                    item {
                        Text(
                            text = "Authors",
                            style = BookVerseTheme.typography.label,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(searchResults.authors) { (authorName, bookCount) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surface)
                                .clickable {
                                    searchQuery = authorName
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = authorName.take(2).uppercase(),
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = authorName,
                                    style = BookVerseTheme.typography.body,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$bookCount books",
                                    style = BookVerseTheme.typography.label,
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Books section
                if (searchResults.books.isNotEmpty()) {
                    item {
                        Text(
                            text = "Books",
                            style = BookVerseTheme.typography.label,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(searchResults.books) { book ->
                        BookSearchResultCard(
                            book = book,
                            onClick = { onBookClick(book) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookSearchResultCard(
    book: Book,
    onClick: () -> Unit
) {
    val colors = BookVerseTheme.colors
    val serverIp = com.freeebookbundle.utils.ServerConfig.getServerIp()
    val serverPort = com.freeebookbundle.utils.ServerConfig.getServerPort()
    val coverUrl = "http://$serverIp:$serverPort/covers/${book.id}_cover_image.jpg"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = book.title,
            modifier = Modifier
                .size(60.dp, 90.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = book.title,
                style = BookVerseTheme.typography.body,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            Text(
                text = book.author,
                style = BookVerseTheme.typography.label,
                color = colors.textSecondary,
                maxLines = 1
            )
        }
    }
}

private fun getCategoryColor(categoryName: String): Color {
    return when (categoryName) {
        "Fiction", "Novels" -> Color(0xFF6366F1)
        "Romance" -> Color(0xFFEC4899)
        "Science-Fiction & Fantasy", "Science Fiction" -> Color(0xFF3B82F6)
        "Adventure" -> Color(0xFF10B981)
        "Short Stories" -> Color(0xFF06B6D4)
        "Poetry" -> Color(0xFFEF4444)
        "Children & Young Adult" -> Color(0xFFF59E0B)
        "Historical Novels" -> Color(0xFF8B5CF6)
        "Biography", "Biographies" -> Color(0xFF14B8A6)
        "Humour" -> Color(0xFF84CC16)
        else -> Color(0xFFE50914)
    }
}