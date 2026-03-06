package com.example.ebook_new_app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
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
import coil.compose.AsyncImage
import com.example.ebook_new_app.PremiumEasing
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.LibraryViewModel
import kotlin.math.absoluteValue

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedTier by remember { mutableStateOf<String?>(null) }
    var inspectedBook by remember { mutableStateOf<Book?>(null) }

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
                        onInspectBook = { inspectedBook = it }
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    ShelfTier(
                        title = "Finished",
                        books = uiState.completed,
                        isExpanded = expandedTier == "finished",
                        onToggle = { expandedTier = if (expandedTier == "finished") null else "finished" },
                        onInspectBook = { inspectedBook = it }
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    ShelfTier(
                        title = "Want to Read",
                        books = uiState.wishlist,
                        isExpanded = expandedTier == "wishlist",
                        onToggle = { expandedTier = if (expandedTier == "wishlist") null else "wishlist" },
                        onInspectBook = { inspectedBook = it }
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
                PickedBookInspection(book = book, onDismiss = { inspectedBook = null })
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
    onInspectBook: (Book) -> Unit
) {
    val shelfColor = Color(0xFF3E2723) 
    val shelfWallColor = Color(0xFF2D1B18)
    
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(
            text = title.uppercase(),
            style = BookVerseTheme.typography.label,
            color = BookVerseTheme.colors.textSecondary,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp
        )

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
                    .padding(bottom = 32.dp), 
                contentAlignment = Alignment.BottomCenter
            ) {
                if (books.isNotEmpty()) {
                    if (isExpanded) {
                        HandOfCardsCarousel(books, onBookClick = onInspectBook)
                    } else {
                        UprightBookRow(books.take(5))
                    }
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
                    SearchResultItem(book)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun HandOfCardsCarousel(books: List<Book>, onBookClick: (Book) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val displayCount = 7
        val displayBooks = if (books.size > displayCount) books.take(displayCount) else books
        val total = displayBooks.size
        
        displayBooks.forEachIndexed { index, book ->
            val relativeIndex = index - (total - 1) / 2f
            val arcRotation = relativeIndex * 12f
            val arcXOffset = relativeIndex * 40.dp.value
            val arcYOffset = relativeIndex.absoluteValue * 8.dp.value
            
            // Back and forth periodic movement
            val staggeredFloat = (floatAnim * (index % 3 + 1) * 4f).dp
            
            Box(
                modifier = Modifier
                    .offset(x = arcXOffset.dp, y = (arcYOffset + staggeredFloat.value).dp)
                    .width(85.dp)
                    .height(125.dp)
                    .graphicsLayer {
                        rotationZ = arcRotation
                        rotationY = -20f
                        cameraDistance = 12f * density
                    }
                    .shadow(12.dp, RoundedCornerShape(2.dp))
                    .clickable { onBookClick(book) }
                    .zIndex(10f - relativeIndex.absoluteValue)
            ) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(2.dp))
                )
                
                // Thickness highlight
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.3f))
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun PickedBookInspection(book: Book, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val autoRotation by infiniteTransition.animateFloat(
        initialValue = -35f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Spotlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset.Unspecified,
                        radius = 1200f
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
                    .width(230.dp)
                    .height(345.dp)
            )
            
            Spacer(modifier = Modifier.height(56.dp))
            
            Text(
                text = book.title,
                style = BookVerseTheme.typography.headline,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
            Text(
                text = book.author.uppercase(),
                style = BookVerseTheme.typography.label,
                color = BookVerseTheme.colors.primary,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = BookVerseTheme.colors.primary),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.height(60.dp).width(220.dp).shadow(20.dp, RoundedCornerShape(30.dp), spotColor = BookVerseTheme.colors.primary)
            ) {
                Text("READ NOW", style = BookVerseTheme.typography.label, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text("TAP ANYWHERE TO RETURN", style = BookVerseTheme.typography.label, color = Color.White.copy(alpha = 0.3f), letterSpacing = 2.sp, fontSize = 11.sp)
        }
    }
}

@Composable
fun Realistic3DBook(
    coverUrl: String,
    currentRotationY: Float,
    modifier: Modifier = Modifier
) {
    val thickness = 40.dp // INCREASED THICKNESS
    
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = currentRotationY
                rotationX = 15f
                cameraDistance = 20f * density
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. THE PAGES DEPTH (Right Side)
        Box(
            modifier = Modifier
                .fillMaxHeight(0.99f)
                .width(thickness)
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    this.rotationY = 90f
                    transformOrigin = TransformOrigin(1f, 0.5f)
                }
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFE8E8E8), Color(0xFFF9F9F9), Color(0xFFDCDCDC))
                    )
                )
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 1.5.dp.toPx()
                for (i in 0..(size.height / step).toInt()) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.1f),
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
                        listOf(Color(0xFF150B0A), Color(0xFF3E2723), Color(0xFF150B0A))
                    )
                )
        )

        // 3. TOP PAGES
        Box(
            modifier = Modifier
                .fillMaxWidth(0.99f)
                .height(thickness)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    rotationX = 90f
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .background(Color(0xFFF9F9F9))
        )

        // 4. BOTTOM PAGES
        Box(
            modifier = Modifier
                .fillMaxWidth(0.99f)
                .height(thickness)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    rotationX = -90f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .background(Color(0xFFE0E0E0))
        )

        // 5. FRONT COVER
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(40.dp, RoundedCornerShape(4.dp))
        ) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(4.dp))
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.08f), Color.Transparent, Color.Black.copy(alpha = 0.15f)),
                            start = Offset(0f, 0f),
                            end = Offset.Infinite
                        )
                    )
            )
        }
    }
}

@Composable
fun UprightBookRow(books: List<Book>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val total = books.size
        books.forEachIndexed { index, book ->
            val spacing = 50.dp
            val centerOffset = (index - (total - 1) / 2f) * spacing.value
            
            val leanAngle = when(index % 3) {
                0 -> -4f
                1 -> 3f
                else -> 0f
            }
            
            Box(
                modifier = Modifier
                    .offset(x = centerOffset.dp)
                    .width(75.dp)
                    .height(125.dp)
                    .graphicsLayer {
                        rotationY = -20f 
                        rotationZ = leanAngle
                        cameraDistance = 15f * density
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .shadow(6.dp, RoundedCornerShape(2.dp))
            ) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .border(0.5.dp, Color.White.copy(0.15f), RoundedCornerShape(2.dp))
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(8.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Black.copy(0.45f), Color.Transparent)
                            )
                        )
                )
            }
        }
    }
}
