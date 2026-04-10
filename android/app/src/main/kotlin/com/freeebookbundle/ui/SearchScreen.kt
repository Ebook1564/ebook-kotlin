package com.freeebookbundle.ui

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.freeebookbundle.PremiumEasing
import com.freeebookbundle.R
import com.freeebookbundle.models.Book
import com.freeebookbundle.utils.ServerConfig
import com.freeebookbundle.viewmodel.AppThemeMode
import com.freeebookbundle.viewmodel.AppViewModel
import com.freeebookbundle.viewmodel.HomeUiState
import com.freeebookbundle.viewmodel.HomeViewModel
import com.freeebookbundle.viewmodel.SearchFilter
import kotlinx.coroutines.launch
import kotlin.math.hypot

@Composable
fun SearchScreen(
    viewModel: HomeViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel(),
    onBookClick: (Book) -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onExploreAllClick: () -> Unit = {}
) {
    val colors = BookVerseTheme.colors
    val currentTheme by appViewModel.themeMode
    val uiState by viewModel.uiState.collectAsState()
    val pagingItems = viewModel.filterResults.collectAsLazyPagingItems()
    var showAuthors by remember { mutableStateOf(false) }

    // Dynamic categories from DB (top 8 by book count)
    var dynamicCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        try {
            val genres = viewModel.getTopGenres(8)
            dynamicCategories = genres.map { it.name }
        } catch (e: Exception) {
            // Fallback to default categories
        }
    }
    
    val allCategories = dynamicCategories.ifEmpty {
        listOf("Fiction", "Romance", "Science Fiction & Fantasy", "Adventure", "Short Stories", "Children & Young Adult", "Historical Novels", "Poetry")
    }
    
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
                    text = if (currentTheme == AppThemeMode.SPACE) stringResource(R.string.search_explore) else "E-Book Bundle",
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
                        stringResource(R.string.search_placeholder), 
                        color = colors.textSecondary.copy(alpha = 0.5f)
                    ) 
                },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.primary) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Text(stringResource(R.string.search_clear), color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Text(
                    stringResource(R.string.search_curated_genre),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = BookVerseTheme.typography.label,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
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
                    onBookClick = onBookClick,
                    allCategories = allCategories,
                    onAuthorClick = onAuthorClick
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
                    allCategories = allCategories,
                    onAuthorClick = onAuthorClick,
                    onExploreAllClick = onExploreAllClick
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
    allCategories: List<String>,
    onAuthorClick: (String) -> Unit = {},
    onExploreAllClick: () -> Unit = {}
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

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.search_curated_genre),
                    style = BookVerseTheme.typography.label,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    "Show All Books",
                    style = BookVerseTheme.typography.label,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onExploreAllClick() }
                )
            }

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

            val overlayAlpha by animateFloatAsState(
                targetValue = if (selectedCategory != null) 0.3f else 1f,
                animationSpec = tween(400),
                label = "overlay_alpha"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        stringResource(R.string.search_explore_all),
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .alpha(overlayAlpha),
                        style = BookVerseTheme.typography.label,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(overlayAlpha),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            count = pagingItems.itemCount,
                            key = { index -> pagingItems[index]?.id ?: index }
                        ) { index ->
                            pagingItems[index]?.let { book ->
                                SearchResultItem(book, onClick = onBookClick, onAuthorClick = onAuthorClick)
                            }
                        }
                        
                        if (pagingItems.loadState.append is androidx.paging.LoadState.Loading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }

                if (selectedCategory?.isNotEmpty() == true) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent() } } }
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedCategory?.isNotEmpty() == true,
                    enter = expandVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeOut()
                ) {
                    selectedCategory?.let { category ->
                        CategoryOverlay(
                            category = category,
                            bookCount = pagingItems.itemCount,
                            accentColor = animatedColor,
                            onClose = { viewModel.clearFilters() },
                            onSeeAll = { viewModel.openCollection(category) },
                            onBookClick = onBookClick,
                            viewModel = viewModel,
                            pagingItems = pagingItems,
                            categories = allCategories,
                            onCategoryChange = { newCategory ->
                                if (selectedCategory != newCategory) {
                                    viewModel.navigateToCategory(newCategory)
                                }
                            }
                        )
                    }
                }
            }
        }
    } }

@Composable
private fun DrawerNavigation(
    categories: List<String>,
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit
) {
    val colors = BookVerseTheme.colors

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(60.dp)
    ) {
        items(categories) { category ->
            val drawerColor = getDrawerColor(category)
            val isSelected = selectedCategory == category

            val animatedScale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1f,
                animationSpec = tween(400, easing = PremiumEasing)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.width(100.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp * animatedScale)
                        .clickable { onCategoryClick(category) },
                    contentAlignment = Alignment.Center
                ) {
                    ScribbleDrawer(
                        category = category,
                        color = drawerColor,
                        isSelected = isSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        category.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = getCategoryDisplayName(category).uppercase(),
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
private fun ScribbleDrawer(
    category: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scribble")
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.7f,
        animationSpec = tween(400)
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scribble_rotation"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scribble_pulse"
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 * animatedAlpha

        drawCircle(
            color = color.copy(alpha = 0.9f * pulse),
            radius = radius
        )

        drawCircle(
            color = color,
            radius = radius,
            style = Stroke(width = 3.dp.toPx())
        )

        val scribbles = when(category) {
            "Fiction" -> 8
            "Science" -> 6
            "Romance" -> 4
            "Fantasy" -> 5
            "Mystery" -> 7
            "Biography" -> 3
            "Adventure" -> 6
            "History of the Americas" -> 5
            else -> 5
        }

        for (i in 0 until scribbles) {
            val baseAngle = (360f / scribbles) * i
            val animatedAngle = baseAngle + rotation
            val startRadius = radius * 0.3f
            val endRadius = radius * 0.8f

            val startX = centerX + startRadius * kotlin.math.cos(Math.toRadians(animatedAngle.toDouble())).toFloat()
            val startY = centerY + startRadius * kotlin.math.sin(Math.toRadians(animatedAngle.toDouble())).toFloat()
            val endX = centerX + endRadius * kotlin.math.cos(Math.toRadians(animatedAngle.toDouble())).toFloat()
            val endY = centerY + endRadius * kotlin.math.sin(Math.toRadians(animatedAngle.toDouble())).toFloat()

            drawLine(
                color = Color.White.copy(alpha = (0.4f + pulse * 0.2f) * animatedAlpha),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx()
            )
        }

        if (isSelected) {
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = radius + 8.dp.toPx(),
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}

@Composable
private fun CategoryOverlay(
    category: String,
    bookCount: Int,
    accentColor: Color,
    onClose: () -> Unit,
    onSeeAll: () -> Unit,
    onBookClick: (Book) -> Unit,
    viewModel: HomeViewModel,
    pagingItems: androidx.paging.compose.LazyPagingItems<Book>,
    categories: List<String>,
    onCategoryChange: (String) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = categories.indexOf(category).coerceAtLeast(0)) { categories.size }

    LaunchedEffect(pagerState.currentPage) {
        if (categories[pagerState.currentPage] != category) {
            onCategoryChange(categories[pagerState.currentPage])
        }
    }

    LaunchedEffect(category) {
        val newPage = categories.indexOf(category)
        if (newPage >= 0 && newPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(newPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(accentColor.copy(alpha = 0.95f))
            .padding(top = 16.dp, bottom = 80.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getCategoryDisplayName(category).uppercase(),
                    style = BookVerseTheme.typography.label,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Text(
                    "$bookCount books",
                    style = BookVerseTheme.typography.label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onSeeAll,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        stringResource(R.string.search_see_all),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.search_close),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val currentCategory = categories[page]
            val currentColor = getDrawerColor(currentCategory)

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    count = pagingItems.itemCount.coerceAtMost(12),
                    key = { index -> "${currentCategory}_${pagingItems[index]?.id ?: index}" }
                ) { index ->
                    pagingItems[index]?.let { book ->
                        OverlayBookCard(
                            book = book,
                            accentColor = currentColor,
                            onClick = { onBookClick(book) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayBookCard(
    book: Book,
    accentColor: Color,
    onClick: () -> Unit
) {
    val serverCoverUrl = book.getServerCoverUrl()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.65f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .shadow(4.dp)
                .background(BookVerseTheme.colors.surface),
            contentAlignment = Alignment.Center
        ) {
            val coverUrl = book.getBestCoverUrl()
            AsyncImage(
                model = coverUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    // Fall back to server cover
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = book.title,
            style = BookVerseTheme.typography.label,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SpaceSearchContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    pagingItems: androidx.paging.compose.LazyPagingItems<Book>,
    onBookClick: (Book) -> Unit,
    allCategories: List<String>,
    onAuthorClick: (String) -> Unit = {}
) {
    val selectedCategory = when (val f = uiState.activeFilter) {
        is SearchFilter.Category -> f.code
        else -> null
    }

    val planet = if (selectedCategory != null) planetForCategory(selectedCategory) else null
    val animatedColor by animateColorAsState(
        targetValue = planet?.mainColor ?: Color.White,
        animationSpec = tween(durationMillis = 600, easing = PremiumEasing)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            val overlayAlpha by animateFloatAsState(
                targetValue = if (selectedCategory != null) 0.3f else 1f,
                animationSpec = tween(400),
                label = "space_overlay_alpha"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        stringResource(R.string.search_explore_all),
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .alpha(overlayAlpha),
                        style = BookVerseTheme.typography.label,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(overlayAlpha),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            count = pagingItems.itemCount,
                            key = { index -> pagingItems[index]?.id ?: index }
                        ) { index ->
                            pagingItems[index]?.let { book ->
                                SearchResultItem(book, onClick = onBookClick, onAuthorClick = onAuthorClick)
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
                            pagingItems.loadState.append is LoadState.Loading -> {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedCategory?.isNotEmpty() == true) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent() } } }
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedCategory?.isNotEmpty() == true,
                    enter = expandVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeOut()
                ) {
                    selectedCategory?.let { category ->
                        planet?.let { currentPlanet ->
                            SpaceCategoryOverlay(
                                category = category,
                                planet = currentPlanet,
                                bookCount = pagingItems.itemCount,
                                accentColor = animatedColor,
                                onClose = { viewModel.clearFilters() },
                                onSeeAll = { viewModel.openCollection(category) },
                                onBookClick = onBookClick,
                                pagingItems = pagingItems,
                                categories = allCategories,
                                onCategoryChange = { newCategory ->
                                    if (selectedCategory != newCategory) {
                                        viewModel.navigateToCategory(newCategory)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceCategoryOverlay(
    category: String,
    planet: PlanetData,
    bookCount: Int,
    accentColor: Color,
    onClose: () -> Unit,
    onSeeAll: () -> Unit,
    onBookClick: (Book) -> Unit,
    pagingItems: androidx.paging.compose.LazyPagingItems<Book>,
    categories: List<String>,
    onCategoryChange: (String) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = categories.indexOf(category).coerceAtLeast(0)) { categories.size }

    LaunchedEffect(pagerState.currentPage) {
        if (categories[pagerState.currentPage] != category) {
            onCategoryChange(categories[pagerState.currentPage])
        }
    }

    LaunchedEffect(category) {
        val newPage = categories.indexOf(category)
        if (newPage >= 0 && newPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(newPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(accentColor.copy(alpha = 0.95f))
            .padding(top = 16.dp, bottom = 80.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getCategoryDisplayName(category).uppercase(),
                    style = BookVerseTheme.typography.label,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Text(
                    "$bookCount books",
                    style = BookVerseTheme.typography.label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onSeeAll,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        stringResource(R.string.search_see_all),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.search_close),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val currentCategory = categories[page]
            val currentPlanet = planetForCategory(currentCategory)
            val currentColor = currentPlanet.mainColor

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    count = pagingItems.itemCount.coerceAtMost(12),
                    key = { index -> "${currentCategory}_${pagingItems[index]?.id ?: index}" }
                ) { index ->
                    pagingItems[index]?.let { book ->
                        SpaceOverlayBookCard(
                            book = book,
                            accentColor = currentColor,
                            onClick = { onBookClick(book) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceOverlayBookCard(
    book: Book,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.65f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .shadow(4.dp),
            contentAlignment = Alignment.Center
        ) {
            val coverUrl = book.getBestCoverUrl()
            AsyncImage(
                model = coverUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    // Will show placeholder
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = book.title,
            style = BookVerseTheme.typography.label,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
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
                loading = {
                    Box(Modifier.fillMaxSize().background(BookVerseTheme.colors.surface))
                },
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
fun SearchResultItem(
    book: Book, 
    onClick: (Book) -> Unit,
    onAuthorClick: ((String) -> Unit)? = null
) {
    val authorColor = getAuthorColor(book.author)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(book) }
            .padding(vertical = 8.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(60.dp, 90.dp).clip(RoundedCornerShape(8.dp)).background(BookVerseTheme.colors.surface).shadow(4.dp)) {
            val coverUrl = book.getBestCoverUrl()
            AsyncImage(
                model = coverUrl, 
                contentDescription = book.title, 
                contentScale = ContentScale.Crop, 
                modifier = Modifier.fillMaxSize(),
                onError = {
                    // Will show placeholder
                }
            )
        }
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(book.title, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = BookVerseTheme.colors.textPrimary, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            if (onAuthorClick != null) {
                Text(
                    text = book.author,
                    fontSize = 15.sp,
                    color = authorColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onAuthorClick(book.author) }
                )
            } else {
                Text(book.author, fontSize = 15.sp, color = BookVerseTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AuthorsRevealOverlay(visible: Boolean, onClose: () -> Unit, onAuthorClick: (String) -> Unit) {
    if (visible) BackHandler { onClose() }
    val viewModel: HomeViewModel = viewModel()
    val appViewModel: AppViewModel = viewModel()
    val currentTheme by appViewModel.themeMode
    val isSpaceTheme = currentTheme == AppThemeMode.SPACE
    val authorUiState by viewModel.uiState.collectAsState()
    val authorItems = viewModel.authorResults.collectAsLazyPagingItems()
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val density = LocalDensity.current
    val maxRadius = hypot(with(density) { config.screenWidthDp.dp.toPx() }, with(density) { config.screenHeightDp.dp.toPx() })
    val radius by animateFloatAsState(targetValue = if (visible) maxRadius * 2f else 0f, animationSpec = tween(800), label = "radius")

    if (radius > 0f) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { clip = true; shape = SearchCircularRevealShape(radius, Offset(size.width - 40.dp.toPx(), 60.dp.toPx())) }.background(if (isSpaceTheme) Color.Black else BookVerseTheme.colors.background)) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.search_authors), style = BookVerseTheme.typography.headline, fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (isSpaceTheme) Color.White else BookVerseTheme.colors.textPrimary)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = if (isSpaceTheme) Color.White else BookVerseTheme.colors.textPrimary) }
                }
                TextField(
                    value = authorUiState.authorSearchQuery,
                    onValueChange = { viewModel.onAuthorSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    placeholder = { Text(stringResource(R.string.search_authors_placeholder), color = if (isSpaceTheme) Color.White.copy(alpha = 0.5f) else BookVerseTheme.colors.textSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = if (isSpaceTheme) Color.White else BookVerseTheme.colors.primary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = if (isSpaceTheme) Color.White.copy(alpha = 0.1f) else BookVerseTheme.colors.surface,
                        unfocusedContainerColor = if (isSpaceTheme) Color.White.copy(alpha = 0.1f) else BookVerseTheme.colors.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = if (isSpaceTheme) Color.White else BookVerseTheme.colors.primary
                    )
                )
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(count = authorItems.itemCount, key = { index -> authorItems[index] ?: index }) { index ->
                        authorItems[index]?.let { author ->
                            val authorColor = getAuthorColor(author)
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (isSpaceTheme) Color.White.copy(alpha = 0.05f) else Color.Transparent).clickable { onClose(); onAuthorClick(author) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                ScribbleAvatar(
                                    authorName = author,
                                    modifier = Modifier.size(50.dp),
                                    accentColor = if (isSpaceTheme) authorColor.copy(alpha = 0.8f) else authorColor
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(author, fontWeight = FontWeight.Bold, color = if (isSpaceTheme) Color.White else BookVerseTheme.colors.textPrimary, fontSize = 16.sp)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = getAuthorGenreHint(author),
                                        style = BookVerseTheme.typography.label,
                                        color = if (isSpaceTheme) Color.White.copy(alpha = 0.6f) else authorColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getDrawerColor(cat: String): Color {
    // Check language prefixes first
    return when {
        cat.startsWith("FR ") -> Color(0xFF0055A4)
        cat.startsWith("DE ") -> Color(0xFFFFC800)
        cat.startsWith("IT ") -> Color(0xFF009246)
        cat.startsWith("PT ") -> Color(0xFF006600)
        cat.startsWith("ES ") -> Color(0xFFAA0000)
        else -> when(cat) {
            "Fiction", "Novels" -> Color(0xFF6366F1)
            "History of the Americas" -> Color(0xFF8B5CF6)
            "Science-Fiction & Fantasy" -> Color(0xFF3B82F6)
            "Romance" -> Color(0xFFEC4899)
            "Fantasy" -> Color(0xFF6A1B9A)
            "Mystery" -> Color(0xFF37474F)
            "Biography", "Biographies" -> Color(0xFF14B8A6)
            "Adventure" -> Color(0xFF10B981)
            "Short Stories" -> Color(0xFF06B6D4)
            "Children & Young Adult" -> Color(0xFFF59E0B)
            "Poetry" -> Color(0xFFEF4444)
            "Humour" -> Color(0xFF84CC16)
            "Crime, Thrillers and Mystery" -> Color(0xFF37474F)
            "Historical Novels" -> Color(0xFF8B5CF6)
            "Science Fiction" -> Color(0xFF3B82F6)
            "Classics of Literature" -> Color(0xFF6366F1)
            "Children's Fiction" -> Color(0xFFF59E0B)
            "Philosophy & Ethics" -> Color(0xFF14B8A6)
            "American Literature" -> Color(0xFF6366F1)
            "British Literature" -> Color(0xFF6366F1)
            else -> Color(0xFFE50914)
        }
    }
}

@Composable
fun getCategoryDisplayName(category: String): String {
    return when (category) {
        "Fiction", "Novels", "category__novels" -> stringResource(R.string.category_fiction)
        "History of the Americas", "Historical Novels", "category__historical_novels" -> stringResource(R.string.category_history)
        "Science", "Science Fiction & Fantasy", "category__science_fiction___fantasy" -> stringResource(R.string.category_science)
        "Romance", "category__romance" -> stringResource(R.string.category_romance)
        "Fantasy" -> stringResource(R.string.category_fantasy)
        "Mystery" -> stringResource(R.string.category_mystery)
        "Biography", "Biographies", "category__biographies" -> stringResource(R.string.category_biography)
        "Adventure", "category__adventure" -> stringResource(R.string.category_adventure)
        "Short Stories", "category__short_stories" -> stringResource(R.string.category_short_stories)
        "Children & Young Adult", "category__children___young_adult_reading" -> stringResource(R.string.category_children)
        "Poetry", "category__poetry" -> stringResource(R.string.category_poetry)
        "Humour", "category__humour" -> stringResource(R.string.category_humour)
        else -> category
    }
}

fun getAuthorColor(authorName: String): Color {
    val hash = authorName.hashCode()
    val colors = listOf(
        Color(0xFF5D4037), Color(0xFF455A64), Color(0xFF2E7D32), Color(0xFFAD1457),
        Color(0xFF6A1B9A), Color(0xFF37474F), Color(0xFF00695C), Color(0xFFEF6C00),
        Color(0xFFE50914), Color(0xFF1E88E5), Color(0xFF7B1FA2), Color(0xFF00897B),
        Color(0xFFE65100), Color(0xFF6D4C41), Color(0xFF546E7A), Color(0xFF8D6E63)
    )
    return colors[Math.abs(hash) % colors.size]
}

fun getAuthorGenreHint(authorName: String): String {
    val hash = authorName.hashCode()
    val genres = listOf(
        "Fiction", "Science Fiction", "Romance", "Fantasy", "Mystery", 
        "Thriller", "Biography", "Adventure", "Historical", "Classic",
        "Drama", "Horror", "Poetry", "Philosophy", "Science", "History"
    )
    return genres[Math.abs(hash) % genres.size]
}

@Composable
fun ScribbleAvatar(
    authorName: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    accentColor: Color? = null
) {
    val color = accentColor ?: getAuthorColor(authorName)
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(300, easing = PremiumEasing)
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "scribble_animation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scribble_rotation"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scribble_pulse"
    )
    
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(50.dp * animatedScale)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2
            
            drawCircle(
                color = color.copy(alpha = 0.08f),
                radius = radius + 8.dp.toPx()
            )
            
            drawCircle(
                color = color.copy(alpha = 0.85f),
                radius = radius
            )
            
            drawCircle(
                color = color.copy(alpha = 0.6f),
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )
            
            val seed = authorName.hashCode().toLong()
            val random = kotlin.random.Random(seed)
            val scribbles = 6 + Math.abs(seed) % 4
            
            for (i in 0 until scribbles) {
                val baseAngle = (360f / scribbles) * i
                val animatedAngle = baseAngle + rotation
                val startRadius = radius * (0.3f + random.nextFloat() * 0.1f)
                val endRadius = radius * (0.6f + random.nextFloat() * 0.15f)
                
                val startX = centerX + startRadius * kotlin.math.cos(Math.toRadians(animatedAngle.toDouble())).toFloat()
                val startY = centerY + startRadius * kotlin.math.sin(Math.toRadians(animatedAngle.toDouble())).toFloat()
                val endX = centerX + endRadius * kotlin.math.cos(Math.toRadians(animatedAngle.toDouble())).toFloat()
                val endY = centerY + endRadius * kotlin.math.sin(Math.toRadians(animatedAngle.toDouble())).toFloat()
                
                drawLine(
                    color = Color.White.copy(alpha = (0.25f + random.nextFloat() * 0.15f) * pulse),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = (1f + random.nextFloat() * 0.5f).dp.toPx()
                )
            }
            
            for (i in 0 until 3) {
                val baseAngle = random.nextFloat() * 360f
                val animatedAngle = baseAngle + orbitRotation
                val r = radius * (0.45f + random.nextFloat() * 0.25f)
                val dotX = centerX + r * kotlin.math.cos(Math.toRadians(animatedAngle.toDouble())).toFloat()
                val dotY = centerY + r * kotlin.math.sin(Math.toRadians(animatedAngle.toDouble())).toFloat()
                
                drawCircle(
                    color = Color.White.copy(alpha = (0.2f + random.nextFloat() * 0.2f) * pulse),
                    radius = (1.5f + random.nextFloat() * 1.5f).dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
            
            for (i in 0 until 2) {
                val orbitAngle = (orbitRotation * 1.5f) + (i * 180f)
                val orbitR = radius * 0.82f
                val satX = centerX + orbitR * kotlin.math.cos(Math.toRadians(orbitAngle.toDouble())).toFloat()
                val satY = centerY + orbitR * kotlin.math.sin(Math.toRadians(orbitAngle.toDouble())).toFloat()
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f * pulse),
                    radius = 2.5.dp.toPx(),
                    center = Offset(satX, satY)
                )
            }
            
            if (isSelected) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = radius + 6.dp.toPx(),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
        
        Text(
            text = authorName.take(2).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.sp
        )
    }
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
