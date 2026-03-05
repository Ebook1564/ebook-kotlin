package com.example.ebook_new_app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.models.Category
import com.example.ebook_new_app.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    
    val browseCategories = remember { listOf(
        Category(id = "1", title = "Mystery", color = "#FF5722"),
        Category(id = "2", title = "Romance", color = "#E91E63"),
        Category(id = "3", title = "Fantasy", color = "#9C27B0"),
        Category(id = "4", title = "Sci-Fi", color = "#673AB7"),
        Category(id = "5", title = "Thriller", color = "#3F51B5"),
        Category(id = "6", title = "Horror", color = "#2196F3"),
        Category(id = "7", title = "Non-Fiction", color = "#03A9F4"),
        Category(id = "8", title = "Biography", color = "#00BCD4")
    ) }

    // AUTO-SELECT CATEGORY FROM EXTERNAL NAVIGATION
    LaunchedEffect(uiState.selectedCategoryForSearch) {
        uiState.selectedCategoryForSearch?.let { catTitle ->
            val match = browseCategories.find { it.title.equals(catTitle, ignoreCase = true) }
            if (match != null) {
                selectedCategory = match
            }
            // Clear navigation trigger after processing
            viewModel.clearCategoryNavigation()
        }
    }

    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }
    
    val filteredBooks = if (searchQuery.isNotEmpty()) {
        uiState.trendingBooks.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.author.contains(searchQuery, ignoreCase = true) 
        }
    } else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BookVerseTheme.colors.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it; if(it.isNotEmpty()) selectedCategory = null },
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape),
            placeholder = { Text("Search books...", color = BookVerseTheme.colors.textSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BookVerseTheme.colors.primary) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BookVerseTheme.colors.surface,
                unfocusedContainerColor = BookVerseTheme.colors.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = BookVerseTheme.colors.textPrimary,
                unfocusedTextColor = BookVerseTheme.colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (searchQuery.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clipToBounds(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (selectedCategory != null) {
                    BackgroundBookCarousel(uiState.trendingBooks)
                }

                Text(
                    text = if (selectedCategory == null) "Browse Categories" else "Selected: ${selectedCategory!!.title}",
                    style = BookVerseTheme.typography.headline,
                    color = BookVerseTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    modifier = Modifier.zIndex(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedCategory == null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(browseCategories) { category ->
                        HoneycombCategoryCard(category) {
                            selectedCategory = category
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 140.dp)
                ) {
                    item {
                        TextButton(onClick = { selectedCategory = null }) {
                            Text("← Back to categories", color = BookVerseTheme.colors.primary)
                        }
                    }
                    items(uiState.trendingBooks.shuffled().take(5)) { book ->
                        SearchResultItem(book)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                items(filteredBooks) { book ->
                    SearchResultItem(book)
                }
            }
        }
    }
}

@Composable
fun HoneycombCategoryCard(category: Category, onClick: () -> Unit) {
    val isDark = BookVerseTheme.colors.isDark
    Box(
        modifier = Modifier
            .size(150.dp)
            .clip(CircleShape)
            .background(Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.8f))
            .clickable(onClick = onClick)
            .then(
                if (isDark) Modifier.border(2.dp, Brush.linearGradient(listOf(Color.White.copy(0.5f), Color.Transparent)), CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.title,
            color = Color.White,
            style = BookVerseTheme.typography.label,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun BackgroundBookCarousel(books: List<Book>) {
    val infiniteTransition = rememberInfiniteTransition()
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = scrollOffset.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) {
            books.forEach { book ->
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(100.dp)
                        .height(60.dp)
                        .alpha(0.2f)
                        .blur(6.dp)
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(book: Book) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { /* TODO */ },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp, 90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BookVerseTheme.colors.surface)
        ) {
            AsyncImage(
                model = book.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(book.title, style = BookVerseTheme.typography.body, color = BookVerseTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
            Text(book.author, style = BookVerseTheme.typography.label, color = BookVerseTheme.colors.textSecondary)
        }
    }
}
