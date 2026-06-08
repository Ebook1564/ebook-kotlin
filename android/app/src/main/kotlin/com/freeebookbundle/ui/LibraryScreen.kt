package com.freeebookbundle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.freeebookbundle.PremiumEasing
import com.freeebookbundle.R
import com.freeebookbundle.models.Book
import com.freeebookbundle.models.ReadingStatus
import com.freeebookbundle.utils.ServerConfig
import com.freeebookbundle.viewmodel.AppThemeMode
import com.freeebookbundle.viewmodel.AppViewModel
import com.freeebookbundle.viewmodel.HomeViewModel
import com.freeebookbundle.viewmodel.LibrarySortOrder
import com.freeebookbundle.viewmodel.LibraryViewModel
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel(),
    onProfileClick: () -> Unit,
    onBookClick: (Book) -> Unit,
    onInspectStateChange: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTheme by appViewModel.themeMode
    var expandedTier by remember { mutableStateOf<String?>(null) }
    var inspectedBook by remember { mutableStateOf<Book?>(null) }

    LaunchedEffect(inspectedBook) {
        onInspectStateChange(inspectedBook != null)
    }

    if (inspectedBook != null) {
        BackHandler { inspectedBook = null }
    } else if (uiState.selectedAuthor != null || uiState.selectedTag != "All") {
        BackHandler { viewModel.selectTag("All") }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BookVerseTheme.colors.background)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.library_title),
                    style = BookVerseTheme.typography.headline,
                    color = BookVerseTheme.colors.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Row {
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BookVerseTheme.colors.surface)
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", tint = BookVerseTheme.colors.primary)
                    }
                    
                    if (showSortMenu) {
                        SortBottomSheet(
                            currentSort = uiState.sortOrder,
                            onSortSelected = { 
                                viewModel.setSortOrder(it)
                                showSortMenu = false
                            },
                            onDismiss = { showSortMenu = false }
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BookVerseTheme.colors.surface)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(R.string.library_profile), tint = BookVerseTheme.colors.primary)
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "authors_button") {
                    val isSelected = uiState.selectedAuthor != null
                    Surface(
                        onClick = { /* Open authors */ },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) BookVerseTheme.colors.primary else BookVerseTheme.colors.surface,
                        border = if (!isSelected) BorderStroke(1.dp, BookVerseTheme.colors.textSecondary.copy(alpha = 0.2f)) else null,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, null, modifier = Modifier.size(14.dp), tint = if (isSelected) Color.White else BookVerseTheme.colors.textPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.library_authors), color = if (isSelected) Color.White else BookVerseTheme.colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(uiState.dynamicTags, key = { it }) { tag ->
                    val isSelected = uiState.selectedTag == tag && uiState.selectedAuthor == null
                    Surface(
                        onClick = { homeViewModel.openCollection(tag) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) BookVerseTheme.colors.primary else BookVerseTheme.colors.surface,
                        border = if (!isSelected) BorderStroke(1.dp, BookVerseTheme.colors.textSecondary.copy(alpha = 0.2f)) else null,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = tag,
                                color = if (isSelected) Color.White else BookVerseTheme.colors.textPrimary,
                                style = BookVerseTheme.typography.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BookVerseTheme.colors.primary)
                }
            } else {
                val isFiltered = uiState.selectedAuthor != null || uiState.selectedTag != "All"
                
                if (!isFiltered) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 140.dp)
                    ) {
                        item(key = "shelf_current") {
                            ShelfTier(
                                title = stringResource(R.string.library_currently_reading),
                                books = uiState.currentlyReading,
                                themeMode = currentTheme,
                                isExpanded = expandedTier == "current",
                                onToggle = { expandedTier = if (expandedTier == "current") null else "current" },
                                onInspectBook = { inspectedBook = it },
                                onSeeAll = { viewModel.setSeeAll(ReadingStatus.CURRENTLY_READING) }
                            )
                        }
                        if (expandedTier == "current") {
                            items(uiState.currentlyReading, key = { "exp_current_${it.id}" }) { book ->
                                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    LibraryBookItem(book) { inspectedBook = it }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        
                        item(key = "spacer_1") { Spacer(modifier = Modifier.height(40.dp)) }
                        
                        item(key = "shelf_finished") {
                            ShelfTier(
                                title = stringResource(R.string.library_finished),
                                books = uiState.completed,
                                themeMode = currentTheme,
                                isExpanded = expandedTier == "finished",
                                onToggle = { expandedTier = if (expandedTier == "finished") null else "finished" },
                                onInspectBook = { inspectedBook = it },
                                onSeeAll = { viewModel.setSeeAll(ReadingStatus.COMPLETED) }
                            )
                        }
                        if (expandedTier == "finished") {
                            items(uiState.completed, key = { "exp_finished_${it.id}" }) { book ->
                                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    LibraryBookItem(book) { inspectedBook = it }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        
                        item(key = "spacer_2") { Spacer(modifier = Modifier.height(40.dp)) }
                        
                        item(key = "shelf_wishlist") {
                            ShelfTier(
                                title = stringResource(R.string.library_want_to_read),
                                books = uiState.wishlist,
                                themeMode = currentTheme,
                                isExpanded = expandedTier == "wishlist",
                                onToggle = { expandedTier = if (expandedTier == "wishlist") null else "wishlist" },
                                onInspectBook = { inspectedBook = it },
                                onSeeAll = { viewModel.setSeeAll(ReadingStatus.WISHLIST) }
                            )
                        }
                        if (expandedTier == "wishlist") {
                            items(uiState.wishlist, key = { "exp_wish_${it.id}" }) { book ->
                                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    LibraryBookItem(book) { inspectedBook = it }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                } else {
                    val filterTitle = uiState.selectedAuthor ?: uiState.selectedTag
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
                        Text(filterTitle.uppercase(), color = BookVerseTheme.colors.primary, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 14.sp)
                        Spacer(Modifier.height(24.dp))
                        
                        val filteredBooks = remember(uiState.currentlyReading, uiState.wishlist, uiState.completed, uiState.selectedAuthor, uiState.selectedTag) {
                            (uiState.currentlyReading + uiState.wishlist + uiState.completed).filter {
                                if (uiState.selectedAuthor != null) it.author == uiState.selectedAuthor
                                else it.subjects.contains(uiState.selectedTag) || it.categories.contains(uiState.selectedTag)
                            }.distinctBy { it.id }
                        }
LazyVerticalGrid(
    columns = GridCells.Fixed(3),
    verticalArrangement = Arrangement.spacedBy(24.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize()
) {
    items(filteredBooks, key = { it.id }) { book ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { inspectedBook = book }) {
            LibraryBookPoster(book, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BookVerseTheme.colors.textPrimary)
        }
    }
}
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = inspectedBook != null,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)),
            exit = fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 1.1f, animationSpec = tween(400))
        ) {
            inspectedBook?.let { book ->
                PickedBookInspection(
                    book = book, 
                    onDismiss = { inspectedBook = null },
                    onReadNow = {
                        inspectedBook = null
                        onBookClick(book)
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.seeAllStatus != null,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(500, easing = PremiumEasing)) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(500, easing = PremiumEasing)) + fadeOut()
        ) {
            uiState.seeAllStatus?.let { status ->
                val pagedBooks = viewModel.getPagedBooks(status).collectAsLazyPagingItems()
                SeeAllPage(
                    title = when(status) {
                        ReadingStatus.CURRENTLY_READING -> stringResource(R.string.library_currently_reading)
                        ReadingStatus.COMPLETED -> stringResource(R.string.library_finished_collection)
                        ReadingStatus.WISHLIST -> stringResource(R.string.library_wishlist)
                        else -> ""
                    },
                    books = pagedBooks,
                    onBack = { viewModel.setSeeAll(null) },
                    onBookClick = { 
                        viewModel.setSeeAll(null)
                        inspectedBook = it 
                    }
                )
            }
        }
    }
}

@Composable
fun LibraryBookPoster(book: Book, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.aspectRatio(0.7f),
        shape = RoundedCornerShape(4.dp),
        shadowElevation = 8.dp
    ) {
        val coverUrl = book.getBestCoverUrl()
        AsyncImage(
            model = coverUrl,
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            error = painterResource(id = R.drawable.ic_book)
        )
    }
}

@Composable
fun ShelfTier(
    title: String,
    books: List<Book>,
    themeMode: AppThemeMode,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onInspectBook: (Book) -> Unit,
    onSeeAll: () -> Unit
) {
    val cornerRadius = 2.dp
    val shelfColor = if (themeMode == AppThemeMode.SPACE) Color.Transparent else Color(0xFF3E2723) 
    val shelfWallColor = if (themeMode == AppThemeMode.SPACE) Color.Transparent else Color(0xFF2D1B18)
    
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isExpanded && themeMode == AppThemeMode.SPACE) Color(0xFF00FFFF) else Color.White.copy(alpha = 0.2f),
        animationSpec = tween(1200, easing = PremiumEasing)
    )

    val starshipProgress = remember { Animatable(0f) }

    if (themeMode == AppThemeMode.SPACE) {
        LaunchedEffect(isExpanded) {
            starshipProgress.stop()
            while (isActive) {
                starshipProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = ((1f - starshipProgress.value) * (if (isExpanded) 60000 else 20000)).toInt(),
                        easing = LinearEasing
                    )
                )
                starshipProgress.snapTo(0f)
            }
        }
    }

    val pathMeasure = remember { PathMeasure() }
    var lastSize by remember { mutableStateOf(Size.Zero) }
    val cachedPath = remember { Path() }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = BookVerseTheme.typography.label,
                color = BookVerseTheme.colors.textSecondary,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
            
            IconButton(onClick = onSeeAll, modifier = Modifier.size(24.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.library_see_all), tint = BookVerseTheme.colors.primary, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) 
                .clickable { onToggle() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp)
                    .drawWithContent {
                        drawContent()

                        if (themeMode == AppThemeMode.SPACE) {
                            if (size != lastSize) {
                                lastSize = size
                                cachedPath.rewind()
                                cachedPath.addRoundRect(
                                    RoundRect(
                                        rect = Rect(Offset.Zero, size),
                                        cornerRadius = CornerRadius(cornerRadius.toPx())
                                    )
                                )
                                pathMeasure.setPath(cachedPath, false)
                            }
                            val distance = pathMeasure.length * starshipProgress.value
                            val offset = pathMeasure.getPosition(distance)
                            
                            withTransform({
                                translate(left = offset.x, top = offset.y)
                            }) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.White, Color(0xFF87CEEB).copy(alpha = 0.5f), Color.Transparent),
                                        center = Offset.Zero,
                                        radius = 8.dp.toPx()
                                    ),
                                    radius = 8.dp.toPx(),
                                    center = Offset.Zero
                                )
                            }
                        }
                    }
                    .background(shelfWallColor, RoundedCornerShape(cornerRadius))
                    .then(
                        if (themeMode == AppThemeMode.SPACE) Modifier.border(1.dp, animatedBorderColor, RoundedCornerShape(cornerRadius))
                        else Modifier
                    )
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(40.dp)
                    .graphicsLayer {
                        rotationX = 65f
                        cameraDistance = 8f * density
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            if (themeMode == AppThemeMode.SPACE) listOf(Color.Transparent, Color.Transparent)
                            else listOf(shelfColor, Color.Black.copy(0.4f))
                        ),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                    .then(if (themeMode != AppThemeMode.SPACE) Modifier.shadow(16.dp) else Modifier)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp), 
                contentAlignment = Alignment.BottomCenter
            ) {
                if (books.isNotEmpty()) {
                    TransitioningShelfBooks(books = books, isExpanded = isExpanded, onBookClick = onInspectBook)
                }
            }
        }
    }
}

@Composable
fun LibraryBookItem(book: Book, onClick: (Book) -> Unit) {
    val colors = BookVerseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(book) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(60.dp, 90.dp)) {
            LibraryBookPoster(book)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = book.title, style = BookVerseTheme.typography.body, color = colors.textPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = book.author, style = BookVerseTheme.typography.label, color = colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun TransitioningShelfBooks(
    books: List<Book>,
    isExpanded: Boolean,
    onBookClick: (Book) -> Unit
) {
    val expansionProgress by animateFloatAsState(if (isExpanded) 1f else 0f, animationSpec = tween(1000, easing = PremiumEasing), label = "expand")
    val needsTransform = expansionProgress > 0f
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val floatAnim by infiniteTransition.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")

    val displayCount = 7
    val displayBooks = if (books.size > displayCount) books.take(displayCount) else books
    val total = displayBooks.size

    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.BottomCenter) {
        displayBooks.forEachIndexed { index, book ->
            val relativeIndex = index - (total - 1) / 2f
            val leanAngle = when(index % 3) { 0 -> -4f; 1 -> 3f; else -> 0f }
            val currentRotationZ = lerp(leanAngle, relativeIndex * 12f, expansionProgress)
            val currentXOffset = lerp(relativeIndex * 50.dp.value, relativeIndex * 40.dp.value, expansionProgress)
            val currentYOffset = lerp(0f, -20.dp.value, expansionProgress)

            val staggeredFloat = if (expansionProgress > 0.8f) {
                (floatAnim * (index % 3 + 1) * 20f * (expansionProgress - 0.8f)).dp
            } else 0.dp

            val shadowSize = if (expansionProgress > 0.5f) 12.dp else 6.dp
            val borderAlpha = if (expansionProgress > 0.5f) 0.25f else 0.1f

            Box(
                modifier = Modifier
                    .offset(x = currentXOffset.dp, y = (currentYOffset + staggeredFloat.value).dp)
                    .width(85.dp).height(125.dp)
                    .then(if (needsTransform) Modifier.graphicsLayer {
                        rotationY = -20f; rotationZ = currentRotationZ; cameraDistance = 15f * density; transformOrigin = TransformOrigin(0.5f, 1f)
                    } else Modifier)
                    .then(if (needsTransform) Modifier.shadow(shadowSize, RoundedCornerShape(2.dp)) else Modifier)
                    .clickable(enabled = isExpanded) { onBookClick(book) }.zIndex(10f - relativeIndex.absoluteValue)
            ) {
                AsyncImage(model = book.getBestCoverUrl(), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp)).border(0.5.dp, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
fun PickedBookInspection(book: Book, onDismiss: () -> Unit, onReadNow: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "inspect")
    val autoRotation by infiniteTransition.animateFloat(initialValue = -45f, targetValue = 45f, animationSpec = infiniteRepeatable(tween(8000, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)), RepeatMode.Reverse), label = "rotate")
    val breatheAnim by infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse), label = "breathe")
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(Color.White.copy(alpha = 0.12f * breatheAnim), Color.Transparent), center = Offset.Unspecified, radius = 1400f)))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Realistic3DBook(coverUrl = book.getBestCoverUrl(), currentRotationY = autoRotation, modifier = Modifier.width(220.dp).height(330.dp))
            Spacer(modifier = Modifier.height(48.dp))
            Text(text = book.title, style = BookVerseTheme.typography.headline, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(text = book.author.uppercase(), style = BookVerseTheme.typography.label, color = BookVerseTheme.colors.primary, letterSpacing = 4.sp, textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (book.dateAdded != null) StatItem("ADDED", dateFormat.format(Date(book.dateAdded)))
                if (book.dateStarted != null) StatItem("STARTED", dateFormat.format(Date(book.dateStarted)))
                if (book.dateFinished != null) StatItem("FINISHED", dateFormat.format(Date(book.dateFinished)))
            }
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onReadNow, colors = ButtonDefaults.buttonColors(containerColor = BookVerseTheme.colors.primary), shape = RoundedCornerShape(30.dp), modifier = Modifier.height(54.dp).width(240.dp).shadow(24.dp, RoundedCornerShape(30.dp), spotColor = BookVerseTheme.colors.primary)) {
                Text(stringResource(R.string.library_continue_reading), style = BookVerseTheme.typography.label, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 13.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text(stringResource(R.string.library_tap_to_return), style = BookVerseTheme.typography.label, color = Color.White.copy(alpha = 0.3f), letterSpacing = 2.sp, fontSize = 10.sp)
        }
    }
}

@Composable
fun SeeAllPage(title: String, books: LazyPagingItems<Book>, onBack: () -> Unit, onBookClick: (Book) -> Unit) {
    val colors = BookVerseTheme.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.library_back), tint = colors.textPrimary) }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = BookVerseTheme.typography.headline, color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            items(books.itemCount) { index ->
                books[index]?.let { book ->
                    Column(modifier = Modifier.clickable { onBookClick(book) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        LibraryBookPoster(book, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = book.title, style = BookVerseTheme.typography.body, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(text = book.author, style = BookVerseTheme.typography.label, color = colors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Realistic3DBook(coverUrl: String, currentRotationY: Float, modifier: Modifier = Modifier) {
    val thickness = 40.dp
    Box(modifier = modifier.graphicsLayer { rotationY = currentRotationY; rotationX = 12f; cameraDistance = 20f * density; transformOrigin = TransformOrigin(0.5f, 0.5f) }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxHeight(0.98f).width(thickness).align(Alignment.CenterEnd).graphicsLayer { this.rotationY = 90f; transformOrigin = TransformOrigin(1f, 0.5f) }.background(Brush.horizontalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFBDBDBD))))) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 2.dp.toPx()
                for (i in 0..(size.height / step).toInt()) {
                    drawLine(color = Color.Black.copy(alpha = 0.08f), start = Offset(0f, i * step), end = Offset(size.width, i * step), strokeWidth = 1f)
                }
            }
        }
        Box(modifier = Modifier.fillMaxHeight().width(thickness).align(Alignment.CenterStart).graphicsLayer { this.rotationY = -90f; transformOrigin = TransformOrigin(0f, 0.5f) }.background(Brush.horizontalGradient(listOf(Color(0xFF0D0D0D), Color(0xFF3E2723), Color(0xFF0D0D0D))))) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                repeat(3) { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.1f))) }
            }
        }
        Box(modifier = Modifier.fillMaxWidth(0.98f).height(thickness).align(Alignment.TopCenter).graphicsLayer { rotationX = 90f; transformOrigin = TransformOrigin(0.5f, 0f) }.background(Color(0xFFF5F5F5)))
        Box(modifier = Modifier.fillMaxWidth(0.98f).height(thickness).align(Alignment.BottomCenter).graphicsLayer { rotationX = -90f; transformOrigin = TransformOrigin(0.5f, 1f) }.background(Color(0xFFD6D6D6)))
        Box(modifier = Modifier.fillMaxSize().shadow(30.dp, RoundedCornerShape(3.dp))) {
            AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(3.dp)).border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(3.dp)))
            Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(Color.White.copy(0.12f), Color.Transparent, Color.Black.copy(0.15f)), start = Offset(0f, 0f), end = Offset.Infinite)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: LibrarySortOrder,
    onSortSelected: (LibrarySortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BookVerseTheme.colors.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.library_sort_title),
                style = BookVerseTheme.typography.headline,
                color = BookVerseTheme.colors.textPrimary,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            val options = listOf(
                LibrarySortOrder.RECENTLY_OPENED to R.string.library_sort_recent,
                LibrarySortOrder.DATE_ADDED to R.string.library_sort_added,
                LibrarySortOrder.TITLE to R.string.library_sort_title_alpha,
                LibrarySortOrder.AUTHOR to R.string.library_sort_author,
                LibrarySortOrder.PROGRESS to R.string.library_sort_progress
            )
            
            options.forEach { (order, labelRes) ->
                val isSelected = currentSort == order
                Surface(
                    onClick = { onSortSelected(order) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isSelected) BookVerseTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = BookVerseTheme.colors.primary)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(labelRes),
                            color = if (isSelected) BookVerseTheme.colors.primary else BookVerseTheme.colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
