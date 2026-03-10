package com.example.ebook_new_app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // Book Cover
            Box(
                modifier = Modifier
                    .size(110.dp, 160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.background)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInWindow()
                        coverBounds = InitialBounds(
                            left = pos.x,
                            top = pos.y,
                            width = coords.size.width.toFloat(),
                            height = coords.size.height.toFloat()
                        )
                    }
            ) {
                if (book.coverImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = book.coverImageUrl,
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = typography.headline,
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "by ${book.author}",
                    style = typography.body,
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add to Reading List Button
                Box {
                    Button(
                        onClick = { showStatusMenu = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (book.readingStatus != ReadingStatus.NONE) colors.primary.copy(alpha = 0.1f) else colors.surface,
                            contentColor = if (book.readingStatus != ReadingStatus.NONE) colors.primary else colors.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            if (book.readingStatus != ReadingStatus.NONE) Icons.Default.Check else Icons.Default.Add, 
                            contentDescription = null, 
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when(book.readingStatus) {
                                ReadingStatus.CURRENTLY_READING -> "READING"
                                ReadingStatus.COMPLETED -> "FINISHED"
                                ReadingStatus.WISHLIST -> "WISHLIST"
                                else -> "ADD TO LIST"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        ReadingStatus.values().filter { it != ReadingStatus.NONE }.forEach { status ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        when(status) {
                                            ReadingStatus.CURRENTLY_READING -> "Currently Reading"
                                            ReadingStatus.COMPLETED -> "Completed"
                                            ReadingStatus.WISHLIST -> "Want to Read"
                                            else -> ""
                                        },
                                        color = colors.textPrimary
                                    ) 
                                },
                                onClick = {
                                    onAddToReadingList(status)
                                    showStatusMenu = false
                                },
                                trailingIcon = {
                                    if (book.readingStatus == status) Icon(Icons.Default.Check, null, tint = colors.primary)
                                }
                            )
                        }
                        if (book.readingStatus != ReadingStatus.NONE) {
                            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.2f))
                            DropdownMenuItem(
                                text = { Text("Remove from List", color = Color.Red.copy(alpha = 0.7f)) },
                                onClick = {
                                    onAddToReadingList(ReadingStatus.NONE)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Description",
            style = typography.headline,
            color = colors.textPrimary,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.description,
            style = typography.body,
            color = colors.textSecondary,
            lineHeight = 20.sp,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Read Now Button
        Button(
            onClick = { onReadClick(coverBounds) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("READ NOW", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
