package com.freeebookbundle.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import com.freeebookbundle.models.Book

@Composable
fun AuthorPage(
    authorName: String,
    authorColor: Color,
    books: LazyPagingItems<Book>,
    onBack: () -> Unit,
    onBookClick: (Book) -> Unit
) {
    val colors = BookVerseTheme.colors

    val backdropUrl = remember(books.itemCount) {
        if (books.itemCount > 0) books[0]?.getBestCoverUrl() else ""
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
            if (!backdropUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(80.dp).alpha(0.4f)
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, colors.background))
                )
            )
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(colors.surface.copy(0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = authorName.uppercase(),
                        style = BookVerseTheme.typography.headline,
                        color = colors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${books.itemCount} books",
                        style = BookVerseTheme.typography.label,
                        color = authorColor,
                        fontSize = 12.sp
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(books.itemCount) { index ->
                    val book = books[index]
                    if (book != null) {
                        AuthorBookItem(book, onClick = { onBookClick(book) })
                    }
                }

                when {
                    books.loadState.refresh is LoadState.Loading -> {
                        item {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = authorColor)
                            }
                        }
                    }
                    books.loadState.append is LoadState.Loading -> {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = authorColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorBookItem(book: Book, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(100.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.aspectRatio(0.7f).fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            shadowElevation = 8.dp
        ) {
            AsyncImage(
                model = book.getBestCoverUrl(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.title,
            style = BookVerseTheme.typography.body,
            color = BookVerseTheme.colors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
