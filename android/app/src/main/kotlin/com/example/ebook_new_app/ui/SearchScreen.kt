package com.example.ebook_new_app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ebook_new_app.PremiumEasing
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: HomeViewModel = viewModel()) {
    val colors = BookVerseTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAuthors by remember { mutableStateOf(false) }

    val allCategories = listOf("Fiction", "History", "Science", "Romance", "Fantasy", "Mystery", "Biography", "Adventure")
    
    // FETCH REAL DATA
    val allBooks = remember(uiState.trendingBooks, uiState.newReleases) {
        (uiState.trendingBooks + uiState.newReleases + uiState.continueReading).distinctBy { it.id }
    }

    val filteredBooks = allBooks.filter { book ->
        val matchesQuery = book.title.contains(searchQuery, ignoreCase = true) || book.author.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || book.categories.contains(selectedCategory)
        matchesQuery && matchesCategory
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // --- HEADER ---
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

            // --- SEARCH BAR ---
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 0.dp),
                placeholder = { Text("Search titles or authors...", color = colors.textSecondary.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary) },
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

            // --- CATEGORY DRAWERS ---
            CategoryDrawerRow(
                categories = allCategories,
                selectedCategory = selectedCategory,
                onCategoryClick = { category ->
                    selectedCategory = if (selectedCategory == category) null else category
                }
            )

            // --- CONTENT AREA ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedCategory != null,
                    enter = expandVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeOut()
                ) {
                    selectedCategory?.let { category ->
                        DetailsDrawer(category = category, books = filteredBooks)
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedCategory == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (searchQuery.isEmpty()) {
                        RandomRecommendations(books = allBooks.shuffled())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredBooks) { book -> SearchResultItem(book) }
                        }
                    }
                }
            }
        }

        // --- AUTHORS OVERLAY (Circular Reveal Animation) ---
        AuthorsRevealOverlay(
            visible = showAuthors,
            allBooks = allBooks,
            onClose = { showAuthors = false }
        )
    }
}

@Composable
fun AuthorsRevealOverlay(
    visible: Boolean,
    allBooks: List<Book>,
    onClose: () -> Unit
) {
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val density = LocalDensity.current
    val maxRadius = hypot(with(density) { config.screenWidthDp.dp.toPx() }, with(density) { config.screenHeightDp.dp.toPx() })
    
    val radius = animateFloatAsState(
        targetValue = if (visible) maxRadius * 1.5f else 0f,
        animationSpec = tween(if (visible) 800 else 600, easing = PremiumEasing),
        label = "reveal_radius"
    )

    if (radius.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true
                    shape = com.example.ebook_new_app.CircularRevealShape(
                        radius.value, 
                        Offset(size.width - 40.dp.toPx(), 60.dp.toPx())
                    )
                }
                .background(BookVerseTheme.colors.background)
        ) {
            AuthorsSection(allBooks = allBooks, onClose = onClose)
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
        modifier = Modifier.fillMaxWidth().padding(start = 0.dp, top = 24.dp, end = 0.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 0.dp)
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
    
    val animatedOffsetY by animateDpAsState(
        targetValue = if (isOpen) 20.dp else 0.dp,
        animationSpec = tween(500, easing = PremiumEasing),
        label = "drawer_pull"
    )
    
    val animatedRotationX by animateFloatAsState(
        targetValue = if (isOpen) -12f else 0f,
        animationSpec = tween(500, easing = PremiumEasing),
        label = "drawer_tilt"
    )
    
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

    // OUTER CABINET HOLE
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(140.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .border(2.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // INTERNAL SHADED FRAME (Thickness depth)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.offset(0.5f), Color.Transparent, Color.Black.offset(0.3f))
                    ),
                    RoundedCornerShape(10.dp)
                )
        )

        // DRAWER FACE (Slightly smaller to sit inside frame)
        Box(
            modifier = Modifier
                .offset(y = animatedOffsetY)
                .width(134.dp)
                .height(134.dp)
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

            // Top edge bevel highlight
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.1f)))

            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = title.uppercase(),
                    style = BookVerseTheme.typography.label,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                VintageDrawerKnob()
            }
        }
    }
}

// Helper extension for darkening/lightening colors for depth
fun Color.offset(amount: Float): Color {
    return Color(
        red = (red - amount).coerceIn(0f, 1f),
        green = (green - amount).coerceIn(0f, 1f),
        blue = (blue - amount).coerceIn(0f, 1f),
        alpha = alpha
    )
}

@Composable
fun VintageDrawerKnob() {
    Box(
        modifier = Modifier
            .size(26.dp)
            .shadow(6.dp, CircleShape)
            .background(
                Brush.radialGradient(listOf(Color(0xFFD4AF37), Color(0xFF8B4513))),
                CircleShape
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
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
                path.moveTo(size.width * 0.2f, size.height * 0.8f)
                path.lineTo(size.width * 0.2f, size.height * 0.2f)
                path.lineTo(size.width * 0.8f, size.height * 0.2f)
                path.lineTo(size.width * 0.8f, size.height * 0.8f)
                path.close()
                path.moveTo(size.width * 0.5f, size.height * 0.2f)
                path.lineTo(size.width * 0.5f, size.height * 0.8f)
            }
            "History" -> {
                path.addOval(androidx.compose.ui.geometry.Rect(center, size.width * 0.45f))
                path.moveTo(center.x, center.y - size.height * 0.3f)
                path.lineTo(center.x, center.y + size.height * 0.3f)
            }
            "Science" -> {
                path.addOval(androidx.compose.ui.geometry.Rect(center, size.width * 0.4f))
                path.addOval(androidx.compose.ui.geometry.Rect(center, size.width * 0.15f))
            }
            "Romance" -> {
                path.moveTo(size.width / 2, size.height * 0.8f)
                path.cubicTo(0f, size.height * 0.3f, size.width * 0.2f, 0f, size.width / 2, size.height * 0.2f)
                path.cubicTo(size.width * 0.8f, 0f, size.width, size.height * 0.3f, size.width / 2, size.height * 0.8f)
            }
            "Fantasy" -> {
                path.moveTo(center.x, center.y - size.height * 0.4f)
                path.lineTo(center.x + size.width * 0.1f, center.y - size.height * 0.1f)
                path.lineTo(center.x + size.width * 0.4f, center.y)
                path.lineTo(center.x + size.width * 0.1f, center.y + size.height * 0.1f)
                path.lineTo(center.x, center.y + size.height * 0.4f)
                path.lineTo(center.x - size.width * 0.1f, center.y + size.height * 0.1f)
                path.lineTo(center.x - size.width * 0.4f, center.y)
                path.lineTo(center.x - size.width * 0.1f, center.y - size.height * 0.1f)
                path.close()
            }
            else -> {
                path.moveTo(0f, 0f)
                path.lineTo(size.width, size.height)
            }
        }
        drawPath(path, color, style = stroke)
    }
}

@Composable
fun DetailsDrawer(category: String, books: List<Book>) {
    val woodyBrush = Brush.verticalGradient(listOf(Color(0xFF3E2723), Color(0xFF2D1B18)))
    
    // DRAWER BOX PERSPECTIVE
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0F0E)) // Cabinet interior color
            .padding(8.dp) // The thickness of the outer cabinet
    ) {
        // THE DRAWER BOX ITSELF
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(woodyBrush, RoundedCornerShape(4.dp))
                .border(2.dp, Color(0xFF1A0F0E), RoundedCornerShape(4.dp))
                .padding(16.dp)
        ) {
            // INNER RIM SHADOW (Creates the 'looking down into a box' effect)
            Box(modifier = Modifier.fillMaxSize()) {
                // Interior shadows
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(0.4f), Color.Transparent, Color.Black.copy(0.2f)),
                                startY = 0f,
                                endY = 100f
                            )
                        )
                )

                Column {
                    Text(
                        text = "ARCHIVE: ${category.uppercase()}",
                        style = BookVerseTheme.typography.label,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                        letterSpacing = 2.sp
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 100.dp)
                    ) {
                        items(books) { book -> ArchiveCardItem(book) }
                    }
                }
            }
        }
        
        // BOX TOP RIM HIGHLIGHT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
        )
    }
}

@Composable
fun ArchiveCardItem(book: Book) {
    Column(
        modifier = Modifier.width(100.dp).clickable { /* Inspect */ },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = book.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(4.dp))
                .shadow(8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.title,
            style = BookVerseTheme.typography.label,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
    }
}

@Composable
fun SearchResultItem(book: Book) {
    val colors = BookVerseTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable { }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = book.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(60.dp, 90.dp).clip(RoundedCornerShape(8.dp)).background(colors.surface)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(book.title, style = BookVerseTheme.typography.body, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            Text(book.author, style = BookVerseTheme.typography.label, color = colors.textSecondary)
        }
    }
}

@Composable
fun RandomRecommendations(books: List<Book>) {
    val colors = BookVerseTheme.colors
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("DISCOVER", style = BookVerseTheme.typography.label, color = colors.textSecondary, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(books) { book -> SearchResultItem(book) }
        }
    }
}

@Composable
fun AuthorsSection(allBooks: List<Book>, onClose: () -> Unit) {
    val colors = BookVerseTheme.colors
    var authorSearchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val authors = remember(allBooks) {
        allBooks.map { it.author }.distinct().sorted()
    }
    
    val filteredAuthors = authors.filter { it.contains(authorSearchQuery, ignoreCase = true) }
    
    val alphabet = ('A'..'Z').toList()

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Header stays full width at the top
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AUTHORS", style = BookVerseTheme.typography.headline, color = colors.textPrimary)
            TextButton(onClick = onClose) { 
                Text("CLOSE", color = colors.primary, fontWeight = FontWeight.Bold) 
            }
        }

        // Row containing the search/list and the scroller starting below the header
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                TextField(
                    value = authorSearchQuery,
                    onValueChange = { authorSearchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    placeholder = { Text("Search authors...", color = colors.textSecondary.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAuthors) { name ->
                        AuthorItem(name)
                    }
                }
            }

            // Scroller starts here, parallel with the search bar
            AlphabetScroller(
                alphabet = alphabet,
                onLetterSelected = { letter ->
                    val index = filteredAuthors.indexOfFirst { it.startsWith(letter, ignoreCase = true) }
                    if (index != -1) {
                        scope.launch { listState.scrollToItem(index) }
                    }
                }
            )
        }
    }
}

@Composable
fun AuthorItem(name: String) {
    val colors = BookVerseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(colors.primary.copy(0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), color = colors.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(name, style = BookVerseTheme.typography.body, color = colors.textPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AlphabetScroller(
    alphabet: List<Char>,
    onLetterSelected: (Char) -> Unit
) {
    val colors = BookVerseTheme.colors
    val haptic = LocalHapticFeedback.current
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(56.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        dragOffsetY = offset.y
                    },
                    onDragEnd = { selectedLetter = null },
                    onDragCancel = { selectedLetter = null },
                    onVerticalDrag = { change, _ ->
                        dragOffsetY = change.position.y
                    }
                )
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            alphabet.forEachIndexed { index, letter ->
                val itemHeightPx = with(density) { 20.dp.toPx() }
                val itemCenterY = index * itemHeightPx + with(density) { 32.dp.toPx() }
                val distance = abs(dragOffsetY - itemCenterY)
                
                val scale by animateFloatAsState(
                    targetValue = if (selectedLetter != null) {
                        (1.5f - (distance / 300f)).coerceIn(1f, 1.5f)
                    } else 1f,
                    label = "letter_scale"
                )
                
                val offsetX by animateDpAsState(
                    targetValue = if (selectedLetter != null) {
                        (-(20 * (1f - (distance / 400f).coerceIn(0f, 1f))).dp)
                    } else 0.dp,
                    label = "letter_offset"
                )

                LaunchedEffect(dragOffsetY) {
                    if (distance < itemHeightPx / 2) {
                        if (selectedLetter != letter) {
                            selectedLetter = letter
                            onLetterSelected(letter)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }

                Text(
                    text = letter.toString(),
                    style = BookVerseTheme.typography.label,
                    color = if (selectedLetter == letter) colors.primary else colors.textSecondary.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = if (selectedLetter == letter) FontWeight.Black else FontWeight.Bold,
                    modifier = Modifier
                        .offset(x = offsetX)
                        .scale(scale)
                        .padding(vertical = 1.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = selectedLetter != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.offset { 
                IntOffset(
                    x = (-60.dp.toPx()).toInt(), 
                    y = dragOffsetY.toInt() - 150.dp.roundToPx()
                ) 
            }
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(12.dp, CircleShape)
                    .background(colors.primary, CircleShape)
                    .border(2.dp, Color.White.copy(0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedLetter?.toString() ?: "",
                    style = BookVerseTheme.typography.headline,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
