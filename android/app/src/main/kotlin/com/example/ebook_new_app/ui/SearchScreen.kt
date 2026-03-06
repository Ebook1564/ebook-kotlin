package com.example.ebook_new_app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ebook_new_app.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: HomeViewModel = viewModel()) {
    val theme = LocalBookTheme.current
    val isDark = theme.type == ThemeType.PITCH_BLACK
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAuthors by remember { mutableStateOf(false) }

    val allCategories = listOf("Fiction", "History", "Science", "Romance", "Fantasy", "Mystery", "Biography", "Adventure")
    val dummyBookData = remember {
        mapOf(
            "Fiction" to listOf("The Grand Adventure", "The Hidden Gem", "Whispers of Fate", "Ancient Roads"),
            "History" to listOf("History of Time", "Ancient Civilizations", "Lost Empires"),
            "Science" to listOf("Cosmic Wonders", "Quantum Leap", "Nature's Code"),
            "Romance" to listOf("Love in Paris", "Whispers of the Heart", "Midnight Sun"),
            "Fantasy" to listOf("Dragon's Quest", "Elven Prophecy", "Magic Bound"),
            "Mystery" to listOf("Shadows of the Past", "The Hidden Gem", "The Cold Case"),
            "Biography" to listOf("Biography of a Star", "Life of a Legend"),
            "Adventure" to listOf("Island Quest", "Summit Peak", "Desert Wind")
        )
    }
    val allDummyBookTitles = remember { dummyBookData.values.flatten().distinct() }

    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF8F8F8) // Pitch Black vs Pale White
    val onBgColor = if (isDark) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- HEADER AND SEARCH BAR ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "EXPLORE",
                    color = onBgColor,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAuthors = true }) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = "Authors",
                        tint = if (isDark) Color(0xFFFF5252) else theme.primary
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                val searchBgColor = if (isDark) Color(0xFF1A1A1A) else Color.White
                val searchTextColor = if (isDark) Color(0xFFFF5252) else Color.Black
                val searchIndicatorColor = if (isDark) Color(0xFFFF5252) else Color.Transparent

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search titles or authors...", color = if (isDark) Color(0xFFFF5252).copy(alpha = 0.5f) else Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = if (isDark) Color(0xFFFF5252) else Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = searchBgColor,
                        unfocusedContainerColor = searchBgColor,
                        focusedIndicatorColor = searchIndicatorColor,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = searchTextColor,
                        unfocusedTextColor = searchTextColor,
                        cursorColor = if (isDark) Color(0xFFFF5252) else theme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // --- CATEGORY ROW ---
            CategoryRow(
                categories = allCategories,
                selectedCategory = selectedCategory,
                onCategoryClick = { category ->
                    selectedCategory = if (selectedCategory == category) null else category
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- CONTENT AREA ---
            Column(modifier = Modifier.weight(1f)) {
                AnimatedVisibility(
                    visible = selectedCategory != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    selectedCategory?.let { category ->
                        DetailsDrawer(
                            category = category,
                            bookTitles = dummyBookData[category] ?: emptyList()
                        )
                    }
                }

                AnimatedVisibility(
                    visible = selectedCategory == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    RandomRecommendations(
                        bookTitles = allDummyBookTitles.shuffled().take(10),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }

        // --- AUTHORS SECTION OVERLAY ---
        AnimatedVisibility(
            visible = showAuthors,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            AuthorsSection(onClose = { showAuthors = false })
        }
    }
}

@Composable
fun CategoryRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != null) {
            val index = categories.indexOf(selectedCategory)
            if (index != -1) {
                coroutineScope.launch {
                    with(density) {
                        val scrollOffsetPx = (listState.layoutInfo.viewportSize.width / 2) - 90.dp.roundToPx()
                        listState.animateScrollToItem(index = index, scrollOffset = -scrollOffsetPx)
                    }
                }
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(categories) { category ->
            DrawerFrontPanel(
                title = category,
                isOpen = selectedCategory == category,
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

@Composable
fun DrawerFrontPanel(
    title: String,
    isOpen: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val theme = LocalBookTheme.current
    val isDark = theme.type == ThemeType.PITCH_BLACK

    val animatedOffsetY by animateDpAsState(
        targetValue = if (isOpen) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "drawer_offset"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "drawer_press_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isOpen) 12.dp else 4.dp,
        label = "drawer_elevation"
    )

    // Vibrant Category Colors
    val categoryColor = remember(title) {
        when (title) {
            "Fiction" -> Color(0xFF42A5F5)
            "History" -> Color(0xFFFFA726)
            "Science" -> Color(0xFF66BB6A)
            "Romance" -> Color(0xFFEC407A)
            "Fantasy" -> Color(0xFFAB47BC)
            "Mystery" -> Color(0xFF78909C)
            "Biography" -> Color(0xFF26A69A)
            "Adventure" -> Color(0xFF29B6F6)
            else -> Color.Gray
        }
    }

    Surface(
        modifier = Modifier
            .offset(y = animatedOffsetY)
            .scale(scale)
            .width(160.dp)
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = elevation,
        shadowElevation = elevation,
        color = if (isDark) categoryColor.copy(alpha = 0.3f) else categoryColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Intricate continuous line art that fills the whole box
            CategoryContinuousLineArt(category = title, modifier = Modifier.fillMaxSize())
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                VintageDrawerKnob()
            }
        }
    }
}

@Composable
fun CategoryContinuousLineArt(category: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeWidth = 1.2.dp.toPx()
        val color = Color.White.copy(alpha = 0.4f)
        
        val path = Path()
        when (category) {
            "Fiction" -> {
                // Scribble forming multiple book outlines in one stroke
                path.moveTo(size.width * 0.1f, size.height * 0.9f)
                path.lineTo(size.width * 0.1f, size.height * 0.3f)
                path.quadraticTo(size.width * 0.3f, size.height * 0.2f, size.width * 0.5f, size.height * 0.3f)
                path.lineTo(size.width * 0.5f, size.height * 0.9f)
                path.moveTo(size.width * 0.5f, size.height * 0.3f)
                path.quadraticTo(size.width * 0.7f, size.height * 0.2f, size.width * 0.9f, size.height * 0.3f)
                path.lineTo(size.width * 0.9f, size.height * 0.9f)
                // Swirls filling space
                path.moveTo(0f, 0f)
                path.cubicTo(size.width, 0f, 0f, size.height, size.width, size.height)
            }
            "History" -> {
                path.moveTo(size.width * 0.5f, 0f)
                for (i in 0..20) {
                    val y = size.height * i / 20
                    val x = size.width * 0.5f + Math.sin(i.toDouble() * 0.5).toFloat() * size.width * 0.4f
                    path.lineTo(x, y)
                }
                path.moveTo(0f, size.height * 0.2f)
                path.lineTo(size.width, size.height * 0.8f)
            }
            "Science" -> {
                // Intricate orbit paths
                for (i in 0..2) {
                    path.reset()
                    val rx = size.width * (0.3f + i * 0.1f)
                    val ry = size.height * 0.2f
                    path.addOval(androidx.compose.ui.geometry.Rect(center = center, radius = size.minDimension / (2f + i)))
                }
                // Random scribbles
                path.moveTo(center.x, center.y)
                for (i in 0..10) {
                    path.lineTo((Math.random() * size.width).toFloat(), (Math.random() * size.height).toFloat())
                }
            }
            "Romance" -> {
                path.moveTo(size.width / 2, size.height)
                path.cubicTo(0f, size.height * 0.5f, size.width * 0.2f, 0f, size.width / 2, size.height * 0.3f)
                path.cubicTo(size.width * 0.8f, 0f, size.width, size.height * 0.5f, size.width / 2, size.height)
                // Intertwined heart
                path.moveTo(size.width * 0.3f, size.height * 0.7f)
                path.cubicTo(size.width * 0.1f, size.height * 0.4f, size.width * 0.4f, size.height * 0.2f, size.width * 0.6f, size.height * 0.4f)
            }
            "Fantasy" -> {
                // Star patterns and trails
                path.moveTo(size.width * 0.5f, size.height * 0.1f)
                for (i in 1..30) {
                    val angle = i * 137.5
                    val r = Math.sqrt(i.toDouble()) * size.width * 0.1
                    val x = (size.width * 0.5 + Math.cos(Math.toRadians(angle)) * r).toFloat()
                    val y = (size.height * 0.5 + Math.sin(Math.toRadians(angle)) * r).toFloat()
                    path.lineTo(x, y)
                }
            }
            "Mystery" -> {
                // Spiral detective vibe
                path.moveTo(size.width / 2, size.height / 2)
                for (i in 0..100) {
                    val angle = i * 0.2
                    val r = i * 0.8f
                    val x = (size.width / 2 + Math.cos(angle) * r).toFloat()
                    val y = (size.height / 2 + Math.sin(angle) * r).toFloat()
                    path.lineTo(x, y)
                }
            }
            "Biography" -> {
                path.moveTo(0f, size.height)
                path.cubicTo(size.width * 0.2f, size.height * 0.8f, size.width * 0.8f, size.height * 0.8f, size.width, size.height)
                path.moveTo(size.width * 0.5f, size.height * 0.2f)
                //path.drawCircle(center, size.width * 0.2f)
            }
            "Adventure" -> {
                path.moveTo(0f, size.height)
                for (i in 0..10) {
                    path.lineTo(size.width * i / 10, (size.height * 0.5f + Math.random() * size.height * 0.4f).toFloat())
                }
                path.lineTo(size.width, size.height)
            }
            else -> {
                path.moveTo(0f, 0f)
                path.lineTo(size.width, size.height)
            }
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round))
    }
}

@Composable
fun AuthorsSection(onClose: () -> Unit) {
    val theme = LocalBookTheme.current
    val isDark = theme.type == ThemeType.PITCH_BLACK
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color.White
    val onBgColor = if (isDark) Color.White else Color.Black

    Column(
        modifier = Modifier.fillMaxSize().background(bgColor).padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "AUTHORS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = onBgColor
            )
            TextButton(onClick = onClose) {
                Text("CLOSE", color = if (isDark) Color(0xFFFF5252) else theme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val dummyAuthors = listOf(
            AuthorProfile("A. Author", 12, 4.5f),
            AuthorProfile("B. Historian", 8, 4.2f),
            AuthorProfile("C. Scientist", 15, 4.8f),
            AuthorProfile("D. Romantic", 10, 4.0f),
            AuthorProfile("E. Fantasist", 20, 4.7f),
            AuthorProfile("F. Mystery", 6, 4.1f),
            AuthorProfile("G. Biographer", 4, 4.4f)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(dummyAuthors) { author ->
                AuthorItem(author)
            }
        }
    }
}

data class AuthorProfile(val name: String, val bookCount: Int, val rating: Float)

@Composable
fun AuthorItem(author: AuthorProfile) {
    val theme = LocalBookTheme.current
    val isDark = theme.type == ThemeType.PITCH_BLACK
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252525) else Color(0xFFF0F0F0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(author.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(author.name, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                Text("${author.bookCount} Books | Rating: ${author.rating}", style = MaterialTheme.typography.bodySmall, color = if (isDark) Color.LightGray else Color.DarkGray)
            }
        }
    }
}

@Composable
fun DetailsDrawer(
    category: String,
    bookTitles: List<String>
) {
    val theme = LocalBookTheme.current
    val isDark = theme.type == ThemeType.PITCH_BLACK
    val woodyBrush = if (isDark) {
        Brush.verticalGradient(colors = listOf(Color(0xFF2d2422), Color(0xFF1e1817)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFF8D6E63), Color(0xFF6D4C41)))
    }
    val innerShadowBrush = Brush.verticalGradient(
        colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent),
        startY = 0f,
        endY = 60f
    )

    Column(
        modifier = Modifier.fillMaxSize().background(woodyBrush).padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(innerShadowBrush))
            
            Column {
                Text(
                    text = "CATALOG: $category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(bookTitles) { bookTitle ->
                        BookGridItem(bookTitle = bookTitle, onClick = { /* TODO */ })
                    }
                }
            }
        }
    }
}

@Composable
fun BookGridItem(bookTitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.aspectRatio(2f / 3f).clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bookTitle,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RandomRecommendations(bookTitles: List<String>, modifier: Modifier = Modifier) {
    val theme = LocalBookTheme.current
    val isDark = theme.type == ThemeType.PITCH_BLACK
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Recommendations For You",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(bookTitles) { bookTitle ->
                Column(modifier = Modifier.width(120.dp)) {
                    BookGridItem(bookTitle = bookTitle, onClick = { /*TODO*/ })
                    Spacer(Modifier.height(4.dp))
                    Text(
                        bookTitle,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color.Black,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
