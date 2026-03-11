package com.example.ebook_new_app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.ebook_new_app.PremiumEasing
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: HomeViewModel = viewModel(),
    onBookClick: (Book) -> Unit
) {
    val colors = BookVerseTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val pagingItems = viewModel.searchResults.collectAsLazyPagingItems()
    
    var showAuthors by remember { mutableStateOf(false) }

    val allCategories = listOf("Fiction", "History", "Science", "Romance", "Fantasy", "Mystery", "Biography", "Adventure")
    
    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "EXPLORE",
                    style = BookVerseTheme.typography.headline,
                    color = colors.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                IconButton(onClick = { showAuthors = true }) {
                    Icon(Icons.Default.Group, contentDescription = "Authors", tint = colors.primary)
                }
            }

            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                placeholder = { Text("Search 60,000+ books...", color = colors.textSecondary.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Text("Clear", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.primary
                ),
                shape = RoundedCornerShape(16.dp)
            )

            CategoryDrawerRow(
                categories = allCategories,
                selectedCategory = uiState.selectedCategoryForSearch,
                onCategoryClick = { category ->
                    if (uiState.selectedCategoryForSearch == category) {
                        viewModel.onSearchQueryChanged("")
                        viewModel.navigateToCategory("") 
                    } else {
                        viewModel.navigateToCategory(category)
                    }
                }
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (uiState.searchQuery.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search, 
                            null, 
                            modifier = Modifier.size(64.dp).alpha(0.2f),
                            tint = colors.textPrimary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Discover your next story", 
                            color = colors.textSecondary,
                            style = BookVerseTheme.typography.label
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            count = pagingItems.itemCount,
                            key = pagingItems.itemKey { it.id }
                        ) { index ->
                            val book = pagingItems[index]
                            if (book != null) {
                                SearchResultItem(book, onBookClick = onBookClick)
                            }
                        }

                        when {
                            pagingItems.loadState.refresh is LoadState.Loading -> {
                                item { 
                                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = colors.primary)
                                    }
                                }
                            }
                            pagingItems.loadState.append is LoadState.Loading -> {
                                item { 
                                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.primary)
                                    }
                                }
                            }
                            pagingItems.itemCount == 0 && pagingItems.loadState.refresh is LoadState.NotLoading -> {
                                item {
                                    Box(Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                        Text("No books found for \"${uiState.searchQuery}\"", color = colors.textSecondary)
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
            onClose = { showAuthors = false }
        )
    }
}

@Composable
fun SearchResultItem(book: Book, onBookClick: (Book) -> Unit) {
    val colors = BookVerseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onBookClick(book) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp, 90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
        ) {
            val serverIp = "10.0.2.2"
            var fallbackStage by remember { mutableIntStateOf(0) }
            val imageUrl = when(fallbackStage) {
                0 -> "http://$serverIp:8080/${book.id}_${book.id}-cover.png"
                1 -> "http://$serverIp:8080/${book.id}_${book.id}-cover.jpg"
                2 -> "http://$serverIp:8080/${android.net.Uri.encode(book.title)}_cover.${book.id}"
                else -> ""
            }

            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    if (fallbackStage < 2) {
                        LaunchedEffect(imageUrl) { fallbackStage++ }
                        Box(Modifier.fillMaxSize().background(BookVerseTheme.colors.surface))
                    } else {
                        Box(Modifier.fillMaxSize().background(BookVerseTheme.colors.surface), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = colors.textSecondary.copy(0.3f))
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                book.title, 
                style = BookVerseTheme.typography.body, 
                color = colors.textPrimary, 
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                book.author, 
                style = BookVerseTheme.typography.label, 
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun CategoryDrawerRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit
) {
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
    val interactionSource = remember { MutableInteractionSource() }
    val animatedOffsetY by animateDpAsState(targetValue = if (isOpen) 20.dp else 0.dp, animationSpec = tween(500, easing = PremiumEasing), label = "offset")
    val animatedRotationX by animateFloatAsState(targetValue = if (isOpen) -12f else 0f, animationSpec = tween(500, easing = PremiumEasing), label = "rotation")
    
    val drawerColor = when (title) {
        "Fiction" -> Color(0xFF5D4037)
        "History" -> Color(0xFF455A64)
        "Science" -> Color(0xFF2E7D32)
        "Romance" -> Color(0xFFAD1457)
        "Fantasy" -> Color(0xFF6A1B9A)
        "Mystery" -> Color(0xFF37474F)
        "Biography" -> Color(0xFF00695C)
        "Adventure" -> Color(0xFFEF6C00)
        else -> Color(0xFF4E342E)
    }

    Box(
        modifier = Modifier.width(140.dp).height(140.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.offset(y = animatedOffsetY).width(134.dp).height(134.dp)
                .graphicsLayer {
                    rotationX = animatedRotationX
                    cameraDistance = 12f * density
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .shadow(if (isOpen) 24.dp else 4.dp, RoundedCornerShape(10.dp))
                .background(drawerColor, RoundedCornerShape(10.dp))
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
        ) {
            CategoryStencilArt(category = title, modifier = Modifier.fillMaxSize(), alpha = 0.25f)
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(title.uppercase(), color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Black, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(12.dp))
                VintageDrawerKnob()
            }
        }
    }
}

@Composable
fun VintageDrawerKnob() {
    Box(modifier = Modifier.size(26.dp).shadow(6.dp, CircleShape).background(Brush.radialGradient(listOf(Color(0xFFD4AF37), Color(0xFF8B4513))), CircleShape), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(8.dp).background(Color.Black.copy(0.2f), CircleShape))
    }
}

@Composable
fun CategoryStencilArt(category: String, modifier: Modifier = Modifier, alpha: Float = 0.15f) {
    Canvas(modifier = modifier.padding(24.dp)) {
        val path = Path()
        val color = Color.White.copy(alpha = alpha)
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (category) {
            "Fiction" -> {
                path.moveTo(size.width * 0.2f, size.height * 0.8f); path.lineTo(size.width * 0.2f, size.height * 0.2f)
                path.lineTo(size.width * 0.8f, size.height * 0.2f); path.lineTo(size.width * 0.8f, size.height * 0.8f); path.close()
                path.moveTo(size.width * 0.5f, size.height * 0.2f); path.lineTo(size.width * 0.5f, size.height * 0.8f)
            }
            "Romance" -> {
                path.moveTo(size.width / 2, size.height * 0.8f)
                path.cubicTo(0f, size.height * 0.3f, size.width * 0.2f, 0f, size.width / 2, size.height * 0.2f)
                path.cubicTo(size.width * 0.8f, 0f, size.width, size.height * 0.3f, size.width / 2, size.height * 0.8f)
            }
        }
        drawPath(path, color, style = stroke)
    }
}

@Composable
fun AuthorsRevealOverlay(visible: Boolean, onClose: () -> Unit) {
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val density = LocalDensity.current
    val maxRadius = hypot(with(density) { config.screenWidthDp.dp.toPx() }, with(density) { config.screenHeightDp.dp.toPx() })
    val radius = animateFloatAsState(targetValue = if (visible) maxRadius * 1.5f else 0f, animationSpec = tween(if (visible) 800 else 600, easing = PremiumEasing), label = "radius")

    if (radius.value > 0f) {
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                clip = true
                shape = com.example.ebook_new_app.CircularRevealShape(radius.value, Offset(size.width - 40.dp.toPx(), 60.dp.toPx()))
            }.background(BookVerseTheme.colors.background)
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("AUTHORS", style = BookVerseTheme.typography.headline, color = BookVerseTheme.colors.textPrimary)
                    TextButton(onClick = onClose) { Text("CLOSE", color = BookVerseTheme.colors.primary, fontWeight = FontWeight.Bold) }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Author Index (Paging 3 Coming Soon)", color = BookVerseTheme.colors.textSecondary)
                }
            }
        }
    }
}
