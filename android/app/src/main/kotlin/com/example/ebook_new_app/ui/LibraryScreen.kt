package com.example.ebook_new_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BookVerseTheme.colors.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "My Library",
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

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BookVerseTheme.colors.primary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(28.dp), // Increased for thicker books
                verticalArrangement = Arrangement.spacedBy(36.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                items(uiState.currentlyReading) { book ->
                    Enhanced3DBook(book, progress = (30..90).random().toFloat() / 100f)
                }
            }
        }
    }
}

@Composable
fun Enhanced3DBook(book: Book, progress: Float) {
    val isDark = BookVerseTheme.colors.isDark
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Open Book */ }
    ) {
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            // 1. THE PAGE STACK (Thicker!)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp) // INCREASED THICKNESS
                    .graphicsLayer {
                        rotationY = -38f
                        cameraDistance = 12f * density
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                if(isDark) Color(0xFF2A2A2A) else Color(0xFFD0D0D0),
                                if(isDark) Color(0xFFF5F5F5) else Color.White,
                                if(isDark) Color(0xFFD0D0D0) else Color(0xFFE0E0E0)
                            )
                        ),
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                    )
                    .drawWithContent {
                        drawContent()
                        val lineCount = 25
                        val step = size.height / lineCount
                        for (i in 0 until lineCount) {
                            drawLine(
                                color = Color.Black.copy(alpha = 0.08f),
                                start = Offset(0f, i * step),
                                end = Offset(size.width, i * step),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
            )

            // 2. THE SPINE
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp) // Thicker spine
                    .graphicsLayer {
                        rotationY = -90f
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        cameraDistance = 12f * density
                    }
                    .background(if (isDark) Color(0xFF1A1A1A) else Color(0xFF3E2723))
            )

            // 3. THE FRONT COVER
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = -38f
                        cameraDistance = 12f * density
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        translationX = -3.dp.toPx()
                    }
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(BookVerseTheme.colors.surface)
            ) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val splitY = size.height * (1f - progress)
                            drawContent()
                            drawRect(
                                color = Color.White,
                                blendMode = BlendMode.Saturation,
                                alpha = 1f
                            )
                            clipRect(top = splitY) {
                                this@drawWithContent.drawContent()
                            }
                        }
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(0.12f), 
                                    Color.Transparent, 
                                    Color.Black.copy(0.12f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(500f, 500f)
                            )
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = book.title,
            style = BookVerseTheme.typography.body,
            color = BookVerseTheme.colors.textPrimary,
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = "${(progress * 100).toInt()}% Read",
            style = BookVerseTheme.typography.label,
            color = BookVerseTheme.colors.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}
