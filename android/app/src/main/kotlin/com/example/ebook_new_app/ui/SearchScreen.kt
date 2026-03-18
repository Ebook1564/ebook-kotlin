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
import androidx.compose.foundation.lazy.items
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
                // DAY/NIGHT: Content Area
                Spacer(modifier = Modifier.height(24.dp))
                
                DayNightSearchContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    pagingItems = pagingItems,
                    onBookClick = onBookClick,
                    currentDrawerColor = currentDrawerColor,
                    currentTitleText = currentTitleText,
                    hasFilter = hasFilter,
                    allCategories = allCategories
                )
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
private fun DayNightSearchContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    pagingItems: androidx.paging.compose.LazyPagingItems<Book>,
    onBookClick: (Book) -> Unit,
    currentDrawerColor: Color,
    currentTitleText: String,
    hasFilter: Boolean,
    allCategories: List<String>
) {
    val colors = BookVerseTheme.colors
    val selectedCategory = when (val f = uiState.activeFilter) {
        is SearchFilter.Category -> f.code
        else -> null
    }
    val drawerColor = if (selectedCategory != null) getDrawerColor(selectedCategory) else colors.primary
    val animatedColor by animateColorAsState(
        targetValue = drawerColor,
        animationSpec = tween(durationMillis = 600, easing = PremiumEasing)
    )
    
    Box(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.animation.AnimatedVisibility(
            visible = selectedCategory?.isNotEmpty() == true,
            enter = expandVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(animatedColor.copy(alpha = 0.1f))
                    .padding(16.dp)
            ) {
                Text(
                    text = "ARCHIVE: ${selectedCategory?.uppercase() ?: ""}",
                    style = BookVerseTheme.typography.label,
                    color = animatedColor,
                    modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                    letterSpacing = 2.sp
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = { index -> pagingItems[index]?.id ?: index }
                    ) { index ->
                        pagingItems[index]?.let { book ->
                            ArchiveCardItem(book, onClick = { onBookClick(book) })
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
            Column(Modifier.fillMaxSize()) {
                Text(
                    "CURATED CABINETS",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = BookVerseTheme.typography.label,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                DrawerNavigation(
                    categories = allCategories,
                    selectedCategory = selectedCategory,
                    onCategoryClick = { category ->
                        if (selectedCategory == category) {
                            viewModel.clearFilters()
                        } else {
                            viewModel.navigateToCategory(category)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "EXPLORE ALL BOOKS",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = BookVerseTheme.typography.label,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = { index -> pagingItems[index]?.id ?: index }
                    ) { index ->
                        pagingItems[index]?.let { book ->
                            SearchResultItem(book, onClick = onBookClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerNavigation(
    categories: List<String>,
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit
) {
    val colors = BookVerseTheme.colors
    
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(categories) { category ->
            val drawerColor = getDrawerColor(category)
            val isSelected = selectedCategory == category
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.width(140.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) drawerColor.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.05f)
                        )
                        .then(
                            if (isSelected) Modifier.border(2.dp, drawerColor, RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .clickable { onCategoryClick(category) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            category.uppercase(),
                            color = if (isSelected) drawerColor else colors.textSecondary,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFFD4AF37), drawerColor)))
                        )
                    }
                }
                Text(
                    text = category.uppercase(),
                    style = BookVerseTheme.typography.label,
                    color = if (isSelected) drawerColor else colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                )
            }
        }
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

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            count = pagingItems.itemCount,
                            key = { index -> pagingItems[index]?.id ?: index }
                        ) { index ->
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
            Column(Modifier.fillMaxSize()) {
                Text(
                    "EXPLORE ALL BOOKS",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = BookVerseTheme.typography.label,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = { index -> pagingItems[index]?.id ?: index }
                    ) { index ->
                        pagingItems[index]?.let { book ->
                            SearchResultItem(book, onClick = onBookClick)
                        }
                    }

                    when {
                        pagingItems.loadState.refresh is LoadState.Loading -> {
                            item { 
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color.White)
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
                    items(count = authorItems.itemCount, key = { index -> authorItems[index] ?: index }) { index ->
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
