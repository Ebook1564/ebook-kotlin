package com.example.ebook_new_app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.ebook_new_app.PremiumEasing
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.HomeViewModel
import com.example.ebook_new_app.viewmodel.SearchFilter
import kotlinx.coroutines.launch
import kotlin.math.hypot

@Composable
fun SearchScreen(
    viewModel: HomeViewModel = viewModel(),
    onBookClick: (Book) -> Unit
) {
    val colors = BookVerseTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val pagingItems = viewModel.filterResults.collectAsLazyPagingItems()
    var showAuthors by remember { mutableStateOf(false) }

    val allCategories = listOf("Fiction", "History", "Science", "Romance", "Fantasy", "Mystery", "Biography", "Adventure")
    val hasFilter = uiState.activeFilter !is SearchFilter.None || uiState.searchQuery.isNotEmpty()
    
    if (hasFilter) {
        BackHandler { viewModel.clearFilters() }
    }

    var lastValidColor by remember { mutableStateOf(colors.primary) }
    var lastValidTitle by remember { mutableStateOf("SEARCH RESULTS") }
    
    val currentDrawerColor = when (val filter = uiState.activeFilter) {
        is SearchFilter.Category -> getDrawerColor(filter.code).also { lastValidColor = it }
        is SearchFilter.Author -> Color(0xFF2D1B18).also { lastValidColor = it }
        else -> lastValidColor
    }
    
    val currentTitleText = when (val f = uiState.activeFilter) {
        is SearchFilter.Author -> "BOOKS BY ${f.name.uppercase()}".also { lastValidTitle = it }
        is SearchFilter.Category -> f.code.uppercase().also { lastValidTitle = it }
        else -> if (uiState.searchQuery.isNotEmpty()) "SEARCH RESULTS" else lastValidTitle
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("EXPLORE", style = BookVerseTheme.typography.headline, color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
                IconButton(onClick = { showAuthors = true }) { Icon(Icons.Default.Group, null, tint = colors.primary) }
            }

            TrendingAuthorsRow(
                authors = listOf("William Shakespeare", "Charles Dickens", "Jane Austen", "Mark Twain", "Edgar Allan Poe"), 
                onAuthorClick = { viewModel.setAuthorFilter(it) }
            )

            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                placeholder = { Text("Search 60,000+ books...", color = colors.textSecondary.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.primary) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface, 
                    unfocusedContainerColor = colors.surface, 
                    focusedIndicatorColor = Color.Transparent, 
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                CategoryDrawerRow(
                    categories = allCategories,
                    selectedCategory = uiState.selectedCategoryForSearch,
                    onCategoryClick = { viewModel.navigateToCategory(it) }
                )

                Column(Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = hasFilter,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(700, easing = PremiumEasing)) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(700, easing = PremiumEasing)) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    viewModel.clearFilters()
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 100.dp)
                                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                                    .background(colors.background)
                                    .clickable(enabled = true, onClick = {}) 
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(currentDrawerColor))

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(currentTitleText, color = currentDrawerColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 14.sp)
                                    IconButton(onClick = { viewModel.clearFilters() }) {
                                        Icon(Icons.Default.Close, null, tint = colors.textSecondary)
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(
                                        count = pagingItems.itemCount,
                                        key = pagingItems.itemKey { it.id }
                                    ) { index ->
                                        pagingItems[index]?.let { book ->
                                            SearchResultItem(book, onBookClick)
                                        }
                                    }

                                    if (pagingItems.loadState.refresh is LoadState.Loading) {
                                        item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = currentDrawerColor) } }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AuthorsRevealOverlay(
            visible = showAuthors, 
            onClose = { showAuthors = false },
            onAuthorClick = { viewModel.setAuthorFilter(it) }
        )
    }
}

@Composable
fun CategoryDrawerRow(categories: List<String>, selectedCategory: String?, onCategoryClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), 
        horizontalArrangement = Arrangement.spacedBy(16.dp), 
        contentPadding = PaddingValues(horizontal = 24.dp)
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
fun DrawerFrontPanel(title: String, isOpen: Boolean, onClick: () -> Unit) {
    val animatedOffsetY by animateDpAsState(targetValue = if (isOpen) 40.dp else 0.dp, animationSpec = tween(600, easing = PremiumEasing), label = "offset")
    val animatedRotationX by animateFloatAsState(targetValue = if (isOpen) -25f else 0f, animationSpec = tween(600, easing = PremiumEasing), label = "rotation")
    val drawerColor = getDrawerColor(title)

    Box(
        modifier = Modifier.width(140.dp).height(140.dp).background(Color.Black.copy(0.2f), RoundedCornerShape(12.dp)), 
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.offset(y = animatedOffsetY).width(134.dp).height(134.dp)
                .graphicsLayer { 
                    rotationX = animatedRotationX
                    cameraDistance = 12f * density
                    transformOrigin = TransformOrigin(0.5f, 0f) 
                }
                .shadow(if (isOpen) 32.dp else 4.dp, RoundedCornerShape(10.dp))
                .background(drawerColor, RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp), 
                horizontalAlignment = Alignment.CenterHorizontally, 
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(title.uppercase(), color = Color.White.copy(0.9f), fontWeight = FontWeight.Black, fontSize = 10.sp)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.size(26.dp).background(Brush.radialGradient(listOf(Color(0xFFD4AF37), Color(0xFF8B4513))), CircleShape))
            }
        }
    }
}

@Composable
fun AuthorsRevealOverlay(
    visible: Boolean, 
    onClose: () -> Unit,
    onAuthorClick: (String) -> Unit
) {
    if (visible) {
        BackHandler { onClose() }
    }

    val viewModel: HomeViewModel = viewModel()
    val authorUiState by viewModel.uiState.collectAsState()
    val authorItems = viewModel.authorResults.collectAsLazyPagingItems()
    
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val density = LocalDensity.current
    val maxRadius = hypot(with(density) { config.screenWidthDp.dp.toPx() }, with(density) { config.screenHeightDp.dp.toPx() })
    val radius by animateFloatAsState(targetValue = if (visible) maxRadius * 2f else 0f, animationSpec = tween(800), label = "radius")

    if (radius > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { 
                    clip = true
                    shape = SearchCircularRevealShape(radius, Offset(size.width - 40.dp.toPx(), 60.dp.toPx())) 
                }
                .background(BookVerseTheme.colors.background)
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AUTHORS", style = BookVerseTheme.typography.headline, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                }

                TextField(
                    value = authorUiState.authorSearchQuery,
                    onValueChange = { viewModel.onAuthorSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    placeholder = { Text("Search authors...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BookVerseTheme.colors.surface,
                        unfocusedContainerColor = BookVerseTheme.colors.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(), 
                    contentPadding = PaddingValues(24.dp), 
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        count = authorItems.itemCount,
                        key = authorItems.itemKey { it }
                    ) { index ->
                        authorItems[index]?.let { author ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { 
                                        onClose()
                                        onAuthorClick(author) 
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AuthorCircle(author = author, onClick = { onClose(); onAuthorClick(author) }, size = 48)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(author, fontWeight = FontWeight.Bold, color = BookVerseTheme.colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorCircle(author: String, onClick: (String) -> Unit, size: Int = 64) {
    val initials = remember(author) {
        val parts = author.trim().split(Regex("\\s+"))
        if (parts.size >= 2) {
            "${parts[0].firstOrNull() ?: ""}${parts.last().firstOrNull() ?: ""}".uppercase()
        } else {
            author.take(2).uppercase()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick(author) }.width(size.dp + 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF2D1B18), Color(0xFF4E342E))))
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (size / 3).sp,
                letterSpacing = 1.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = author.split(" ").last(), 
            fontSize = 10.sp, 
            color = BookVerseTheme.colors.textSecondary, 
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TrendingAuthorsRow(authors: List<String>, onAuthorClick: (String) -> Unit) {
    Column {
        Text(
            "TRENDING AUTHORS",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = BookVerseTheme.typography.label,
            color = BookVerseTheme.colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            items(authors) { author ->
                AuthorCircle(author, onAuthorClick)
            }
        }
    }
}

@Composable
fun SearchResultItem(book: Book, onClick: (Book) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick(book) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(50.dp, 75.dp).clip(RoundedCornerShape(4.dp)).background(BookVerseTheme.colors.surface)) {
            val primaryUrl = book.externalCoverUrl ?: book.coverImageUrl
            SubcomposeAsyncImage(
                model = primaryUrl, 
                contentDescription = book.title, 
                contentScale = ContentScale.Crop, 
                modifier = Modifier.fillMaxSize(),
                error = {
                    SubcomposeAsyncImage(
                        model = book.coverImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = { 
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = BookVerseTheme.colors.textSecondary.copy(0.3f))
                            }
                        }
                    )
                }
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = BookVerseTheme.colors.textPrimary)
            Text(book.author, fontSize = 12.sp, color = BookVerseTheme.colors.textSecondary)
        }
    }
}

fun getDrawerColor(cat: String): Color = when(cat) {
    "Fiction" -> Color(0xFF5D4037)
    "History" -> Color(0xFF455A64)
    "Science" -> Color(0xFF2E7D32)
    "Romance" -> Color(0xFFAD1457)
    "Fantasy" -> Color(0xFF6A1B9A)
    "Mystery" -> Color(0xFF37474F)
    "Biography" -> Color(0xFF00695C)
    "Adventure" -> Color(0xFFEF6C00)
    else -> Color(0xFFE50914)
}

class SearchCircularRevealShape(private val radius: Float, private val center: Offset) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply { addOval(Rect(center, radius)) }
        return Outline.Generic(path)
    }
}
