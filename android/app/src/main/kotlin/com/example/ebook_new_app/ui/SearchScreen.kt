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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.ebook_new_app.utils.ServerConfig
import com.example.ebook_new_app.viewmodel.AppThemeMode
import com.example.ebook_new_app.viewmodel.AppViewModel
import com.example.ebook_new_app.viewmodel.HomeUiState
import com.example.ebook_new_app.viewmodel.HomeViewModel
import com.example.ebook_new_app.viewmodel.SearchFilter
import kotlinx.coroutines.launch
import kotlin.math.hypot

@Composable
fun SearchScreen(
    viewModel: HomeViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel(),
    onBookClick: (Book) -> Unit
) {
    val colors = BookVerseTheme.colors
    val currentTheme by appViewModel.themeMode
    val uiState by viewModel.uiState.collectAsState()
    val pagingItems = viewModel.filterResults.collectAsLazyPagingItems()
    var showAuthors by remember { mutableStateOf(false) }

    val allCategories = listOf("Fiction", "History of the Americas", "Science", "Romance", "Fantasy", "Mystery", "Biography", "Adventure")
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
        if (currentTheme == AppThemeMode.SPACE) {
            SpaceBackground()
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentTheme == AppThemeMode.SPACE) "EXPLORE" else "EBOOK BUNDLE",
                    style = BookVerseTheme.typography.headline,
                    color = colors.textPrimary,
                    fontSize = if (currentTheme == AppThemeMode.SPACE) 28.sp else 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = if (currentTheme == AppThemeMode.SPACE) 1.sp else 0.sp
                )
                Row {
                    IconButton(
                        onClick = { appViewModel.toggleTheme() },
                        modifier = Modifier.clip(CircleShape).background(colors.surface.copy(alpha = if(currentTheme == AppThemeMode.NIGHT || currentTheme == AppThemeMode.SPACE) 0.3f else 0.8f))
                    ) {
                        Icon(
                            imageVector = when (currentTheme) {
                                AppThemeMode.DAY -> Icons.Default.Nightlight
                                AppThemeMode.NIGHT -> Icons.Default.AutoAwesome
                                AppThemeMode.SPACE -> Icons.Default.LightMode
                            },
                            contentDescription = "Toggle Theme",
                            tint = colors.primary
                        )
                    }
                    IconButton(onClick = { showAuthors = true }) { Icon(Icons.Default.Group, null, tint = colors.primary) }
                }
            }

            // SEARCH BAR
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                placeholder = { 
                    Text(
                        "Search 60,000+ books...", 
                        color = colors.textSecondary.copy(alpha = 0.5f)
                    ) 
                },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.primary) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Text("Clear", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface.copy(alpha = if (currentTheme == AppThemeMode.SPACE) 0.5f else 1f),
                    unfocusedContainerColor = colors.surface.copy(alpha = if (currentTheme == AppThemeMode.SPACE) 0.5f else 1f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.primary
                ),
                shape = RoundedCornerShape(16.dp)
            )

            // SPACE THEME: Celestial Navigation
            if (currentTheme == AppThemeMode.SPACE) {
                val planetSystems = rememberPlanetSystems(allCategories)
                val selectedCategory = when (val f = uiState.activeFilter) {
                    is SearchFilter.Category -> f.code
                    else -> null
                }
                CelestialNavigation(
                    planetSystems = planetSystems,
                    selectedCategory = selectedCategory,
                    onPlanetClick = { category ->
                        if (selectedCategory == category) {
                            viewModel.clearFilters()
                        } else {
                            viewModel.navigateToCategory(category)
                        }
                    }
                )
                
                // SPACE THEME: Content Area
                SpaceSearchContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    pagingItems = pagingItems,
                    onBookClick = onBookClick
                )
            } else {
                // DAY/NIGHT: Category Row
                Spacer(modifier = Modifier.height(24.dp))
                
                // DAY/NIGHT: Content Area
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (!hasFilter) {
                        Column(Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 140.dp)
                            ) {
                                item {
                                    Text("CURATED CABINETS",
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                        style = BookVerseTheme.typography.label,
                                        color = colors.textSecondary,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    )
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.height(500.dp).padding(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        userScrollEnabled = false
                                    ) {
                                        items(allCategories) { category ->
                                            DrawerFrontPanel(
                                                title = category,
                                                isOpen = false,
                                                onClick = { viewModel.navigateToCategory(category) }
                                            )
                                        }
                                    }
                                }

                                item {
                                    Text("EXPLORE ALL BOOKS",
                                        modifier = Modifier.padding(24.dp),
                                        style = BookVerseTheme.typography.label,
                                        color = colors.textSecondary,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    )
                                }

                                items(
                                    count = pagingItems.itemCount,
                                    key = pagingItems.itemKey { it.id }
                                ) { index ->
                                    pagingItems[index]?.let { book ->
                                        SearchResultItem(book, onClick = onBookClick)
                                    }
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = hasFilter,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(700, easing = PremiumEasing)) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(700, easing = PremiumEasing)) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
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
                                    Column {
                                        Text(currentTitleText, color = currentDrawerColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 14.sp)
                                        Text("${pagingItems.itemCount} items", color = colors.textSecondary, fontSize = 10.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { viewModel.openCollection(currentTitleText) }) {
                                            Text("VIEW ALL", color = currentDrawerColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = currentDrawerColor, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { viewModel.clearFilters() }) {
                                            Icon(Icons.Default.Close, null, tint = colors.textSecondary)
                                        }
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
                                            SearchResultItem(book, onClick = onBookClick)
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
private fun SpaceSearchContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    pagingItems: androidx.paging.compose.LazyPagingItems<Book>,
    onBookClick: (Book) -> Unit
) {
    val selectedCategory = when (val f = uiState.activeFilter) {
        is SearchFilter.Category -> f.code
        else -> null
    }

    val dummyRecommendations = remember {
        listOf(
            Book(id = "1", title = "The Grand Adventure", author = "J.R.R. Tolkien"),
            Book(id = "2", title = "A Brief History of Time", author = "Stephen Hawking"),
            Book(id = "3", title = "Sherlock Holmes", author = "Arthur Conan Doyle"),
            Book(id = "4", title = "The Hobbit", author = "J.R.R. Tolkien"),
        ).shuffled()
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.animation.AnimatedVisibility(
            visible = selectedCategory?.isNotEmpty() == true,
            enter = expandVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeOut()
        ) {
            selectedCategory?.let { category ->
                val planet = planetForCategory(category)
                val animatedColor by animateColorAsState(
                    targetValue = planet.mainColor,
                    animationSpec = tween(durationMillis = 600, easing = PremiumEasing)
                )
                val spaceBrush = Brush.verticalGradient(
                    listOf(
                        animatedColor.copy(alpha = 0.5f),
                        Color.Black
                    )
                )

                Column(
                    modifier = Modifier.fillMaxSize().background(spaceBrush).padding(16.dp)
                ) {
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
                        items(pagingItems.itemCount) { index ->
                            pagingItems[index]?.let { book ->
                                ArchiveCardItem(book, onClick = { onBookClick(book) })
                            }
                        }
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = selectedCategory.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (uiState.searchQuery.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text("DISCOVER", style = BookVerseTheme.typography.label, color = BookVerseTheme.colors.textSecondary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(dummyRecommendations) { book -> 
                            SearchResultItem(book, onClick = onBookClick) 
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = pagingItems.itemKey { it.id }
                    ) { index ->
                        val book = pagingItems[index]
                        if (book != null) {
                            SearchResultItem(book, onClick = onBookClick)
                        }
                    }

                    when {
                        pagingItems.loadState.refresh is LoadState.Loading -> {
                            item { 
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = BookVerseTheme.colors.primary)
                                }
                            }
                        }
                        pagingItems.loadState.append is LoadState.Loading -> {
                            item { 
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BookVerseTheme.colors.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveCardItem(book: Book, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(100.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(4.dp))
                .shadow(8.dp)
                .background(BookVerseTheme.colors.surface)
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
                            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = BookVerseTheme.colors.textSecondary.copy(0.3f))
                        }
                    }
                }
            )
        }
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
fun DrawerFrontPanel(title: String, isOpen: Boolean, onClick: () -> Unit) {
    val drawerColor = getDrawerColor(title)
    Box(
        modifier = Modifier.fillMaxWidth().height(110.dp).background(Color.Black.copy(0.05f), RoundedCornerShape(12.dp)), 
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize(0.95f)
                .shadow(4.dp, RoundedCornerShape(10.dp))
                .background(drawerColor, RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(10.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp), 
                horizontalAlignment = Alignment.CenterHorizontally, 
                verticalArrangement = Arrangement.Center
            ) {
                Text(title.uppercase(), color = Color.White.copy(0.9f), fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.size(18.dp).background(Brush.radialGradient(listOf(Color(0xFFD4AF37), Color(0xFF8B4513))), CircleShape))
            }
        }
    }
}

@Composable
fun SearchResultItem(book: Book, onClick: (Book) -> Unit) {
    val coversUrl = ServerConfig.getCoversUrl()
    val fallbackCoverUrl = "$coversUrl/${book.id}_cover_image.jpg"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(book) }
            .padding(vertical = 8.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(60.dp, 90.dp).clip(RoundedCornerShape(8.dp)).background(BookVerseTheme.colors.surface).shadow(4.dp)) {
            val primaryUrl = book.externalCoverUrl ?: fallbackCoverUrl
            SubcomposeAsyncImage(
                model = primaryUrl, 
                contentDescription = book.title, 
                contentScale = ContentScale.Crop, 
                modifier = Modifier.fillMaxSize(),
                error = {
                    Box(Modifier.fillMaxSize().background(BookVerseTheme.colors.surface), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = BookVerseTheme.colors.textSecondary.copy(0.3f))
                    }
                }
            )
        }
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(book.title, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = BookVerseTheme.colors.textPrimary, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(book.author, fontSize = 12.sp, color = BookVerseTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AuthorsRevealOverlay(visible: Boolean, onClose: () -> Unit, onAuthorClick: (String) -> Unit) {
    if (visible) BackHandler { onClose() }
    val viewModel: HomeViewModel = viewModel()
    val authorUiState by viewModel.uiState.collectAsState()
    val authorItems = viewModel.authorResults.collectAsLazyPagingItems()
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val density = LocalDensity.current
    val maxRadius = hypot(with(density) { config.screenWidthDp.dp.toPx() }, with(density) { config.screenHeightDp.dp.toPx() })
    val radius by animateFloatAsState(targetValue = if (visible) maxRadius * 2f else 0f, animationSpec = tween(800), label = "radius")

    if (radius > 0f) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { clip = true; shape = SearchCircularRevealShape(radius, Offset(size.width - 40.dp.toPx(), 60.dp.toPx())) }.background(BookVerseTheme.colors.background)) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                    colors = TextFieldDefaults.colors(focusedContainerColor = BookVerseTheme.colors.surface, unfocusedContainerColor = BookVerseTheme.colors.surface, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(count = authorItems.itemCount, key = authorItems.itemKey { it }) { index ->
                        authorItems[index]?.let { author ->
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClose(); onAuthorClick(author) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(BookVerseTheme.colors.primary.copy(0.1f)), contentAlignment = Alignment.Center) {
                                    Text(author.take(1), fontWeight = FontWeight.Black, color = BookVerseTheme.colors.primary)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(author, fontWeight = FontWeight.Bold, color = BookVerseTheme.colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getDrawerColor(cat: String): Color = when(cat) {
    "Fiction" -> Color(0xFF5D4037)
    "History of the Americas" -> Color(0xFF455A64)
    "Science" -> Color(0xFF2E7D32)
    "Romance" -> Color(0xFFAD1457)
    "Fantasy" -> Color(0xFF6A1B9A)
    "Mystery" -> Color(0xFF37474F)
    "Biography" -> Color(0xFF00695C)
    "Adventure" -> Color(0xFFEF6C00)
    else -> Color(0xFFE50914)
}

@Composable
fun rememberPlanetSystems(categories: List<String>): List<PlanetSystem> {
    return remember(categories) {
        categories.map { category ->
            val planetData = planetForCategory(category)
            PlanetSystem(
                planet = planetData,
                orbitingPlanets = List(3) {
                    val orbitingColor = planetData.mainColor.copy(alpha = 1f)
                    OrbitingPlanet(
                        data = PlanetData(
                            name = "",
                            mainColor = orbitingColor,
                            accentColors = listOf(orbitingColor, Color.White),
                            radius = kotlin.random.Random.nextDouble(3.0, 8.0).dp
                        ),
                        orbitRadius = kotlin.random.Random.nextDouble(planetData.radius.value + 10.0, planetData.radius.value + 20.0).dp,
                        angleOffset = kotlin.random.Random.nextFloat() * 360f,
                        speed = kotlin.random.Random.nextDouble(0.8, 1.5).toFloat()
                    )
                }
            )
        }
    }
}

@Composable
fun CelestialNavigation(
    planetSystems: List<PlanetSystem>,
    selectedCategory: String?,
    onPlanetClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(60.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        items(planetSystems) { system ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CelestialBody(
                    planet = system.planet,
                    orbitingPlanets = system.orbitingPlanets,
                    isExpanded = selectedCategory == system.planet.name,
                    onClick = { onPlanetClick(system.planet.name) }
                )
                Text(
                    text = system.planet.name.uppercase(),
                    style = BookVerseTheme.typography.label,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
            }
        }
    }
}

class SearchCircularRevealShape(private val radius: Float, private val center: Offset) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply { addOval(Rect(center, radius)) }
        return Outline.Generic(path)
    }
}
