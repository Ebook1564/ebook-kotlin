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
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
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

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BookVerseTheme.colors.primary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(uiState.currentlyReading) { book ->
                    // 3D DVD Case Cover with Progress
                    DVDCaseBook(book, progress = (30..90).random().toFloat() / 100f)
                }
            }
        }
    }
}

@Composable
fun DVDCaseBook(book: Book, progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO */ }
    ) {
        Box(
            modifier = Modifier
                .height(220.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = -15f // 3D Tilt
                    cameraDistance = 12f * density
                }
        ) {
            // THE SPINE
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .background(Color.DarkGray)
                    .graphicsLayer {
                        rotationY = -90f
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            )

            // THE COVER
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                            
                            // 1. GrayScale part
                            drawContent()
                            drawRect(
                                color = Color.White,
                                blendMode = BlendMode.Saturation,
                                alpha = 1f
                            )
                            
                            // 2. Color part
                            clipRect(top = splitY) {
                                this@drawWithContent.drawContent()
                            }
                        }
                )
                
                // 3D Highlights
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color.White.copy(0.1f), Color.Transparent, Color.Black.copy(0.1f)),
                                start = Offset(0f, 0f),
                                end = Offset(500f, 500f)
                            )
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
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
            fontSize = 11.sp
        )
    }
}
