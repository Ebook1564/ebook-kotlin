package com.example.ebook_new_app.ui

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.models.ReadingStatus
import com.example.ebook_new_app.viewmodel.InitialBounds

@Composable
fun BookDetailContent(
    book: Book,
    isLoading: Boolean,
    onReadClick: (InitialBounds?) -> Unit,
    onCategoryClick: (String) -> Unit,
    onAddToReadingList: (ReadingStatus) -> Unit,
    onClose: () -> Unit
) {
    var coverBounds by remember { mutableStateOf<InitialBounds?>(null) }
    val colors = BookVerseTheme.colors
    val typography = BookVerseTheme.typography
    var showStatusMenu by remember { mutableStateOf(false) }

    // DYNAMIC HEIGHT WRAPPER
    Surface(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = colors.surface,
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            
            // 1. BACKDROP (Fades out as we scroll)
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(60.dp).alpha(0.3f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, colors.surface),
                                startY = 200f
                            )
                        )
                )
            }

            // 2. CONTENT COLUMN (Uses wrapContentHeight)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 48.dp) // Base padding for bottom
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(40.dp, 4.dp).clip(CircleShape).background(colors.textSecondary.copy(alpha = 0.2f)))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // HEADER SECTION
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(120.dp, 180.dp)
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInWindow()
                                coverBounds = InitialBounds(
                                    left = pos.x, top = pos.y,
                                    width = coords.size.width.toFloat(), height = coords.size.height.toFloat()
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 24.dp
                    ) {
                        AsyncImage(
                            model = book.coverImageUrl,
                            contentDescription = book.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = typography.headline,
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 26.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.author,
                            style = typography.body,
                            color = colors.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // PRIMARY READ NOW BUTTON (Directly in header)
                        Button(
                            onClick = { onReadClick(coverBounds) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = colors.primary),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("READ NOW", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ADD TO LIST
                        Box {
                            OutlinedButton(
                                onClick = { showStatusMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (book.readingStatus != ReadingStatus.NONE) colors.primary else colors.textPrimary
                                )
                            ) {
                                Icon(if (book.readingStatus != ReadingStatus.NONE) Icons.Default.Check else Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when(book.readingStatus) {
                                        ReadingStatus.CURRENTLY_READING -> "READING"
                                        ReadingStatus.COMPLETED -> "FINISHED"
                                        ReadingStatus.WISHLIST -> "WISHLIST"
                                        else -> "ADD TO LIST"
                                    },
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showStatusMenu,
                                onDismissRequest = { showStatusMenu = false },
                                modifier = Modifier.background(colors.surface).border(1.dp, colors.textSecondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            ) {
                                ReadingStatus.entries.filter { it != ReadingStatus.NONE }.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(when(status) { ReadingStatus.CURRENTLY_READING -> "Currently Reading"; ReadingStatus.COMPLETED -> "Completed"; else -> "Want to Read" }, color = colors.textPrimary) },
                                        onClick = { onAddToReadingList(status); showStatusMenu = false },
                                        leadingIcon = { Icon(if (status == ReadingStatus.CURRENTLY_READING) Icons.Default.PlayArrow else Icons.Default.Add, null, tint = if (book.readingStatus == status) colors.primary else colors.textSecondary) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // METADATA CHIPS
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(book.categories) { DetailChip(label = it) }
                    if (book.categories.isEmpty()) {
                        item { DetailChip(label = "Legal Text") }
                        item { DetailChip(label = "Public Domain") }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // DESCRIPTION (The dynamic driver)
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("About this book", style = typography.headline, color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = book.description,
                        style = typography.body,
                        color = colors.textSecondary.copy(alpha = 0.9f),
                        lineHeight = 22.sp,
                        fontSize = 15.sp
                    )
                }
                
                // Add extra padding at the bottom so the last line of description is clear
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DetailChip(label: String) {
    val colors = BookVerseTheme.colors
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.2f))
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = colors.textSecondary
        )
    }
}
