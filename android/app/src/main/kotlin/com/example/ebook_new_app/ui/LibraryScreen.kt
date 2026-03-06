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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ebook_new_app.PremiumEasing
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedTier by remember { mutableStateOf<String?>(null) }

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
                    onToggle = { expandedTier = if (expandedTier == "current") null else "current" }
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                ShelfTier(
                    title = "Finished",
                    books = uiState.completed,
                    isExpanded = expandedTier == "finished",
                    onToggle = { expandedTier = if (expandedTier == "finished") null else "finished" }
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                ShelfTier(
                    title = "Want to Read",
                    books = uiState.wishlist,
                    isExpanded = expandedTier == "wishlist",
                    onToggle = { expandedTier = if (expandedTier == "wishlist") null else "wishlist" }
                )
            }
        }
    }
}

@Composable
fun ShelfTier(
    title: String,
    books: List<Book>,
    isExpanded: Boolean,
    onToggle: () -> Unit
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

        // THE HEAD-ON 3D SHELF (Precisely grounded)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp) 
                .clickable { onToggle() }
        ) {
            // 1. BACK WALL
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp)
                    .background(shelfWallColor, RoundedCornerShape(2.dp))
            )
            
            // 2. THE FLOOR (Receding Horizontal Plane)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(32.dp)
                    .graphicsLayer {
                        rotationX = 65f // Perspective floor
                        cameraDistance = 8f * density
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(
                        brush = Brush.verticalGradient(listOf(shelfColor, Color.Black.copy(0.4f))),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .shadow(16.dp)
            )

            // 3. THE BOOKS (Upright & Anchored)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (books.isNotEmpty()) {
                    UprightBookRow(books.take(5))
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
fun UprightBookRow(books: List<Book>) {
    // Manually positioned books for perfect alignment
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val total = books.size
        books.forEachIndexed { index, book ->
            // Precise spacing calculation
            val spacing = 50.dp
            val centerOffset = (index - (total - 1) / 2f) * spacing.value
            
            // Subtle, controlled leaning
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
                        transformOrigin = TransformOrigin(0.5f, 1f) // Anchor to bottom center
                    }
                    .shadow(6.dp, RoundedCornerShape(2.dp))
            ) {
                // Book Content
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds, // FIT TO BOUNDS
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .border(0.5.dp, Color.White.copy(0.15f), RoundedCornerShape(2.dp))
                )
                
                // Spine Depth Highlight
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
