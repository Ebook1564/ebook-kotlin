package com.example.ebook_new_app.ui

// Assuming LocalBookTheme is available from this package based on original code.
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ebook_new_app.viewmodel.HomeViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: HomeViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val isDetailView = selectedCategory != null
    val theme = LocalBookTheme.current

    val allCategories = listOf("Fiction", "History", "Science", "Romance", "Fantasy", "Mystery", "Biography", "Adventure")

    val dummyBookData = remember {
        mapOf(
            "Fiction" to listOf("The Grand Adventure", "The Hidden Gem"),
            "History" to listOf("History of Time", "Ancient Civilizations"),
            "Science" to listOf("History of Time", "Cosmic Wonders", "Quantum Leap"),
            "Romance" to listOf("Love in Paris", "Whispers of the Heart"),
            "Fantasy" to listOf("Dragon's Quest", "Elven Prophecy"),
            "Mystery" to listOf("Shadows of the Past", "The Hidden Gem", "The Cold Case"),
            "Biography" to listOf("Biography of a Star", "Life of a Legend"),
            "Adventure" to listOf("The Grand Adventure", "Dragon's Quest")
        )
    }
    val allDummyBookTitles = remember { dummyBookData.values.flatten().distinct() }


    val categoryListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // --- HEADER AND SEARCH BAR ---
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "EXPLORE",
                color = theme.onBackground,
                fontFamily = theme.fontFamily,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search titles or authors...", color = theme.onBackground.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.onBackground.copy(alpha = 0.5f)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = theme.onBackground.copy(alpha = 0.05f),
                    unfocusedContainerColor = theme.onBackground.copy(alpha = 0.05f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = theme.onBackground,
                    unfocusedTextColor = theme.onBackground,
                    cursorColor = theme.primary
                )
            )
        }

        // --- CATEGORY ROW ---
        val booksForCategory = remember(selectedCategory) { dummyBookData[selectedCategory] ?: emptyList() }
        CategoryRow(
            categories = allCategories,
            selectedCategory = selectedCategory,
            booksForCategory = booksForCategory,
            onCategorySelected = { category, index ->
                val newSelection = if (selectedCategory == category) null else category
                selectedCategory = newSelection
                if (newSelection != null) {
                    coroutineScope.launch {
                        categoryListState.animateScrollToItem(index = index, scrollOffset = -150)
                    }
                }
            },
            listState = categoryListState
        )

        // --- DETAILS DRAWER ---
        AnimatedVisibility(
            visible = isDetailView,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            selectedCategory?.let { currentCategory ->
                DetailsDrawer(
                    category = currentCategory,
                    bookTitles = booksForCategory,
                    onBookClick = { /* TODO: Implement Read Now function */ }
                )
            }
        }

        // --- RANDOM RECOMMENDATIONS ---
        AnimatedVisibility(visible = !isDetailView) {
            RandomRecommendations(
                bookTitles = allDummyBookTitles.shuffled().take(10),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun CategoryRow(
    categories: List<String>,
    selectedCategory: String?,
    booksForCategory: List<String>,
    onCategorySelected: (String, Int) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val isDetailView = selectedCategory != null
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = if (isDetailView) Arrangement.Center else Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        userScrollEnabled = !isDetailView
    ) {
        itemsIndexed(categories) { index, category ->
            val isSelected = selectedCategory == category
            Box(contentAlignment = Alignment.Center) {
                AnimatedCategoryItem(
                    title = category,
                    isSelected = isSelected,
                    isDetailView = isDetailView,
                    onClick = { onCategorySelected(category, index) }
                )
                StatBubbles(
                    isVisible = isSelected && isDetailView,
                    totalBooks = booksForCategory.size,
                    readBooks = remember(booksForCategory) { if(booksForCategory.isNotEmpty()) (booksForCategory.size / 3..booksForCategory.size).random() else 0 }
                )
            }
        }
    }
}

@Composable
fun AnimatedCategoryItem(
    title: String,
    isSelected: Boolean,
    isDetailView: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalBookTheme.current
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f, label = "scale")
    val alpha by animateFloatAsState(targetValue = if (isDetailView && !isSelected) 0.3f else 1.0f, label = "alpha")
    val elevation by animateDpAsState(targetValue = if (isSelected) 16.dp else 8.dp, label = "elevation")

    val cardBrush = Brush.verticalGradient(
        colors = listOf(
            theme.onBackground.copy(alpha = 0.05f),
            theme.onBackground.copy(alpha = 0.15f)
        )
    )

    val handleBrush = Brush.linearGradient(
        colors = listOf(
            theme.onBackground.copy(alpha = 0.3f),
            theme.onBackground.copy(alpha = 0.5f),
            theme.onBackground.copy(alpha = 0.3f)
        )
    )

    Card(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Column(
            modifier = Modifier
                .background(cardBrush)
                .padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = theme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .background(handleBrush, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
fun StatBubbles(
    isVisible: Boolean,
    totalBooks: Int,
    readBooks: Int
) {
    val theme = LocalBookTheme.current
    Row(modifier = Modifier.offset(x = 80.dp).zIndex(1f)) {
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = theme.primary.copy(alpha = 0.9f)),
                modifier = Modifier.size(56.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "$totalBooks", fontWeight = FontWeight.Bold, color = theme.onPrimary, fontSize = 14.sp)
                    Text(text = "Total", color = theme.onPrimary.copy(alpha = 0.8f), fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut(targetScale = 0.5f) + fadeOut()
        ) {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = theme.onBackground.copy(alpha = 0.2f)),
                modifier = Modifier.size(50.dp).offset(y = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "$readBooks", fontWeight = FontWeight.Bold, color = theme.onBackground, fontSize = 13.sp)
                    Text(text = "Read", color = theme.onBackground.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        }
    }
}


@Composable
fun DetailsDrawer(
    category: String,
    bookTitles: List<String>,
    onBookClick: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val woodyBrush = if (isDark) {
        Brush.verticalGradient(colors = listOf(Color(0xFF4E342E), Color(0xFF3E2723)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFF8D6E63), Color(0xFF795548)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(woodyBrush)
            .padding(16.dp)
    ) {
        Text(
            text = "Books in $category",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(bookTitles) { bookTitle ->
                BookGridItem(bookTitle = bookTitle, onClick = { onBookClick(bookTitle) })
            }
        }
    }
}

@Composable
fun BookGridItem(bookTitle: String, onClick: () -> Unit) {
    val theme = LocalBookTheme.current
    Card(
        modifier = Modifier
            .aspectRatio(2f / 3f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = theme.onBackground.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bookTitle,
                color = theme.onBackground,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}


@Composable
fun RandomRecommendations(bookTitles: List<String>, modifier: Modifier = Modifier) {
    val theme = LocalBookTheme.current
    Column(modifier = modifier) {
        Text(
            text = "Recommendations For You",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = theme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(bookTitles) { bookTitle ->
                Column(modifier = Modifier.width(120.dp)) {
                    BookGridItem(bookTitle = bookTitle, onClick = { /*TODO*/ })
                    Text(
                        bookTitle,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.onBackground
                    )
                }
            }
        }
    }
}
