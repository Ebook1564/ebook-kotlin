package com.example.ebook_new_app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.ebook_new_app.PremiumEasing
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.models.ReadingStatus
import com.example.ebook_new_app.viewmodel.LibraryViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    onProfileClick: () -> Unit,
    onBookClick: (Book) -> Unit,
    onInspectStateChange: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedTier by remember { mutableStateOf<String?>(null) }
    var inspectedBook by remember { mutableStateOf<Book?>(null) }

    LaunchedEffect(inspectedBook) {
        onInspectStateChange(inspectedBook != null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BookVerseTheme.colors.background)
                .statusBarsPadding()
        ) {
            // TOP HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "My Collection",
                    style = BookVerseTheme.typography.headline,
                    color = BookVerseTheme.colors.textPrimary,
                    fontSize = 28.sp
                )
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BookVerseTheme.colors.surface)
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = BookVerseTheme.colors.primary)
                }
            }

            // GENRE PILLS
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.genres) { genre ->
                    val isSelected = uiState.selectedGenre == genre
                    Surface(
                        onClick = { viewModel.selectGenre(genre) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) BookVerseTheme.colors.primary else BookVerseTheme.colors.surface,
                        border = if (!isSelected) BorderStroke(1.dp, BookVerseTheme.colors.textSecondary.copy(alpha = 0.2f)) else null,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = genre,
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 140.dp)
                ) {
                    ShelfTier(
                        title = "Currently Reading",
                        books = uiState.currentlyReading,
                        isExpanded = expandedTier == "current",
                        onToggle = { expandedTier = if (expandedTier == "current") null else "current" },
                        onInspectBook = { inspectedBook = it },
                        onSeeAll = { viewModel.setSeeAll(ReadingStatus.CURRENTLY_READING) }
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    ShelfTier(
                        title = "Finished",
                        books = uiState.completed,
                        isExpanded = expandedTier == "finished",
                        onToggle = { expandedTier = if (expandedTier == "finished") null else "finished" },
                        onInspectBook = { inspectedBook = it },
                        onSeeAll = { viewModel.setSeeAll(ReadingStatus.COMPLETED) }
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    ShelfTier(
                        title = "Want to Read",
                        books = uiState.wishlist,
                        isExpanded = expandedTier == "wishlist",
                        onToggle = { expandedTier = if (expandedTier == "wishlist") null else "wishlist" },
                        onInspectBook = { inspectedBook = it },
                        onSeeAll = { viewModel.setSeeAll(ReadingStatus.WISHLIST) }
                    )
                }
            }
        }

        // Picking Up Animation Overlay (Resident Evil / Silent Hill Style)
        AnimatedVisibility(
            visible = inspectedBook != null,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
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

        // Dedicated See All Page (NOW USING PAGING)
        AnimatedVisibility(
            visible = uiState.seeAllStatus != null,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(500, easing = PremiumEasing)) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(500, easing = PremiumEasing)) + fadeOut()
        ) {
            uiState.seeAllStatus?.let { status ->
                val pagedBooks = viewModel.getPagedBooks(status).collectAsLazyPagingItems()
                SeeAllPage(
                    title = when(status) {
                        ReadingStatus.CURRENTLY_READING -> "Currently Reading"
                        ReadingStatus.COMPLETED -> "Finished Collection"
                        ReadingStatus.WISHLIST -> "Wishlist"
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
fun SeeAllPage(
    title: String,
    books: LazyPagingItems<Book>,
    onBack: () -> Unit,
    onBookClick: (Book) -> Unit
) {
    val colors = BookVerseTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = BookVerseTheme.typography.headline,
                color = colors.textPrimary,
                fontSize = 24.sp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(books.itemCount) { index ->
                val book = books[index]
                if (book != null) {
                    Column(
                        modifier = Modifier.clickable { onBookClick(book) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(8.dp))
                                .shadow(8.dp)
                                .background(colors.surface)
                        ) {
                            AsyncImage(
                                model = book.coverImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = book.title,
                            style = BookVerseTheme.typography.body,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = book.author,
                            style = BookVerseTheme.typography.label,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShelfTier(
    title: String,
    books: List<Book>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onInspectBook: (Book) -> Unit,
    onSeeAll: () -> Unit
) {
    val shelfColor = Color(0xFF3E2723) 
    val shelfWallColor = Color(0xFF2D1B18)
    
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
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, 
                    contentDescription = "See All", 
                    tint = BookVerseTheme.colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // THE HEAD-ON 3D SHELF
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) 
                .clickable { onToggle() }
        ) {
            // 1. BACK WALL
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp)
                    .background(shelfWallColor, RoundedCornerShape(2.dp))
            )
            
            // 2. THE FLOOR
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
                        brush = Brush.verticalGradient(listOf(shelfColor, Color.Black.copy(0.4f))),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .shadow(16.dp)
            )

            // 3. THE BOOKS / CAROUSEL
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp), 
                contentAlignment = Alignment.BottomCenter
            ) {
                if (books.isNotEmpty()) {
                    TransitioningShelfBooks(
                        books = books,
                        isExpanded = isExpanded,
                        onBookClick = onInspectBook
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                books.forEach { book ->
                    LibraryBookItem(book, onInspectBook)
                    Spacer(modifier = Modifier.height(16.dp))
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
        AsyncImage(
            model = book.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp, 90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = BookVerseTheme.typography.body,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.author,
                style = BookVerseTheme.typography.label,
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun TransitioningShelfBooks(
    books: List<Book>,
    isExpanded: Boolean,
    onBookClick: (Book) -> Unit
) {
    val expansionProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(1000, easing = PremiumEasing)
    )

    val infiniteTransition = rememberInfiniteTransition()
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val displayCount = 7
    val displayBooks = if (books.size > displayCount) books.take(displayCount) else books
    val total = displayBooks.size

    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        displayBooks.forEachIndexed { index, book ->
            val relativeIndex = index - (total - 1) / 2f
            
            // 1. FAN CALCULATIONS (OPEN STATE)
            val openRotationZ = relativeIndex * 12f
            val openXOffset = relativeIndex * 40.dp.value
            val openYOffset = -20.dp.value 
            val openRotationY = -20f
            val openRotationX = 0f

            // 2. UPRIGHT CALCULATIONS (CLOSED STATE)
            val leanAngle = when(index % 3) {
                0 -> -4f
                1 -> 3f
                else -> 0f
            }
            val closedRotationZ = leanAngle
            val closedXOffset = relativeIndex * 50.dp.value 
            val closedYOffset = 0.dp.value 
            val closedRotationY = -20f
            val closedRotationX = 0f

            // 3. INTERPOLATED VALUES
            val currentRotationZ = lerp(closedRotationZ, openRotationZ, expansionProgress)
            val currentXOffset = lerp(closedXOffset, openXOffset, expansionProgress)
            val currentYOffset = lerp(closedYOffset, openYOffset, expansionProgress)
            val currentRotationY = lerp(closedRotationY, openRotationY, expansionProgress)
            val currentRotationX = lerp(closedRotationX, openRotationX, expansionProgress)
            
            // Staggered Floating (Only when expanded)
            val staggeredFloat = if (expansionProgress > 0.8f) {
                (floatAnim * (index % 3 + 1) * 4f * (expansionProgress - 0.8f) * 5f).dp
            } else 0.dp

            Box(
                modifier = Modifier
                    .offset(x = currentXOffset.dp, y = (currentYOffset + staggeredFloat.value).dp)
                    .width(85.dp)
                    .height(125.dp)
                    .graphicsLayer {
                        rotationX = currentRotationX
                        rotationY = currentRotationY
                        rotationZ = currentRotationZ
                        cameraDistance = 15f * density
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .shadow(
                        elevation = with(androidx.compose.ui.platform.LocalDensity.current) {
                            lerp(6.dp.toPx(), 12.dp.toPx(), expansionProgress).toDp()
                        },
                        shape = RoundedCornerShape(2.dp)
                    )
                    .clickable(enabled = isExpanded) { onBookClick(book) }
                    .zIndex(10f - relativeIndex.absoluteValue)
            ) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = lerp(0.1f, 0.25f, expansionProgress)),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                
                // Dynamic Edge Lighting based on state
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f * expansionProgress))
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun PickedBookInspection(book: Book, onDismiss: () -> Unit, onReadNow: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val autoRotation by infiniteTransition.animateFloat(
        initialValue = -45f,
        targetValue = 45f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val breatheAnim by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Spotlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.12f * breatheAnim), Color.Transparent),
                        center = Offset.Unspecified,
                        radius = 1400f
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Realistic3DBook(
                coverUrl = book.coverImageUrl,
                currentRotationY = autoRotation,
                modifier = Modifier
                    .width(220.dp)
                    .height(330.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = book.title,
                style = BookVerseTheme.typography.headline,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
                fontSize = 24.sp
            )
            Text(
                text = book.author.uppercase(),
                style = BookVerseTheme.typography.label,
                color = BookVerseTheme.colors.primary,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // STATISTICS
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (book.dateStarted != null) {
                    StatItem(label = "STARTED", value = dateFormat.format(Date(book.dateStarted)))
                }
                if (book.dateFinished != null) {
                    StatItem(label = "FINISHED", value = dateFormat.format(Date(book.dateFinished)))
                } else if (book.lastOpened != null) {
                    StatItem(label = "LAST OPENED", value = dateFormat.format(Date(book.lastOpened)))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onReadNow,
                colors = ButtonDefaults.buttonColors(containerColor = BookVerseTheme.colors.primary),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.height(54.dp).width(200.dp).shadow(24.dp, RoundedCornerShape(30.dp), spotColor = BookVerseTheme.colors.primary)
            ) {
                Text("READ NOW", style = BookVerseTheme.typography.label, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 13.sp)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text("TAP ANYWHERE TO RETURN", style = BookVerseTheme.typography.label, color = Color.White.copy(alpha = 0.3f), letterSpacing = 2.sp, fontSize = 10.sp)
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
fun Realistic3DBook(
    coverUrl: String,
    currentRotationY: Float,
    modifier: Modifier = Modifier
) {
    val thickness = 40.dp 
    
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = currentRotationY
                rotationX = 12f
                cameraDistance = 20f * density
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. THE PAGES DEPTH (Right Side)
        Box(
            modifier = Modifier
                .fillMaxHeight(0.98f)
                .width(thickness)
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    this.rotationY = 90f
                    transformOrigin = TransformOrigin(1f, 0.5f)
                }
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFBDBDBD))
                    )
                )
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 2.dp.toPx()
                for (i in 0..(size.height / step).toInt()) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.08f),
                        start = Offset(0f, i * step),
                        end = Offset(size.width, i * step),
                        strokeWidth = 1f
                    )
                }
            }
        }

        // 2. THE SPINE (Left Side)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(thickness)
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    this.rotationY = -90f
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF0D0D0D), Color(0xFF3E2723), Color(0xFF0D0D0D))
                    )
                )
        ) {
            // Spine Ribs for realism
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                repeat(3) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.1f)))
                }
            }
        }

        // 3. TOP PAGES
        Box(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .height(thickness)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    rotationX = 90f
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .background(Color(0xFFF5F5F5))
        )

        // 4. BOTTOM PAGES
        Box(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .height(thickness)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    rotationX = -90f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .background(Color(0xFFD6D6D6))
        )

        // 5. FRONT COVER
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(30.dp, RoundedCornerShape(3.dp))
        ) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(3.dp))
                    .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(3.dp))
            )
            
            // Dynamic Reflection Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.15f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset.Infinite
                        )
                    )
            )
        }
    }
}
