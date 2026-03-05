package com.example.ebook_new_app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.ebook_new_app.viewmodel.InitialBounds

@Composable
fun BookDetailContent(
    book: Book,
    isLoading: Boolean,
    onReadClick: (InitialBounds?) -> Unit,
    onClose: () -> Unit
) {
    var coverBounds by remember { mutableStateOf<InitialBounds?>(null) }
    val colors = BookVerseTheme.colors
    val typography = BookVerseTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 22.sp,
                    lineHeight = 26.sp
                )
                Text(
                    text = book.author,
                    style = typography.body,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "EPUB • High Quality",
                    style = typography.label,
                    color = colors.textSecondary.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Button(
            onClick = { onReadClick(coverBounds) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary, 
                contentColor = if(colors.isDark) Color.White else Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Read Now", fontWeight = FontWeight.ExtraBold, style = typography.label, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { /* TODO: Add to library logic */ },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add to My Library", fontWeight = FontWeight.Bold, style = typography.label)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = book.description.ifEmpty { "A captivating journey through the pages of ${book.title}. Dive into this masterpiece and explore new horizons of thought and imagination." },
            style = typography.body,
            color = colors.textPrimary.copy(alpha = 0.8f),
            lineHeight = 20.sp,
            fontSize = 14.sp
        )
    }
}
