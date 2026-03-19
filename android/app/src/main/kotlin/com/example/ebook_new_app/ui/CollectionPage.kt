package com.example.ebook_new_app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
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
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.AppThemeMode
import com.example.ebook_new_app.viewmodel.AppViewModel
import com.example.ebook_new_app.viewmodel.BookCollection
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CollectionPage(
    collection: BookCollection,
    books: LazyPagingItems<Book>,
    onBack: () -> Unit,
    onBookClick: (Book) -> Unit
) {
    CollectionPageContent(
        title = collection.title,
        description = collection.description,
        accentColor = collection.accentColor,
        books = books,
        onBack = onBack,
        onBookClick = onBookClick
    )
}

@Composable
fun CollectionPage(
    title: String,
    description: String,
    accentColor: Color,
    books: LazyPagingItems<Book>,
    onBack: () -> Unit,
    onBookClick: (Book) -> Unit
) {
    CollectionPageContent(
        title = title,
        description = description,
        accentColor = accentColor,
        books = books,
        onBack = onBack,
        onBookClick = onBookClick
    )
}

@Composable
private fun CollectionPageContent(
    title: String,
    description: String,
    accentColor: Color,
    books: LazyPagingItems<Book>,
    onBack: () -> Unit,
    onBookClick: (Book) -> Unit
) {
    val colors = BookVerseTheme.colors
    val appViewModel: AppViewModel = viewModel()
    val currentTheme by appViewModel.themeMode
    val isSpaceTheme = currentTheme == AppThemeMode.SPACE
    
    // Get a backdrop from the first book if available
    val backdropUrl = remember(books.itemCount) {
        if (books.itemCount > 0) books[0]?.coverImageUrl else ""
    }

    Box(modifier = Modifier.fillMaxSize().background(if (isSpaceTheme) Color.Black else colors.background)) {
        // HERO BACKDROP
        if (!isSpaceTheme) {
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, colors.background)
                            )
                        )
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // HEADER
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
                        text = title.uppercase(),
                        style = BookVerseTheme.typography.headline,
                        color = colors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = description,
                        style = BookVerseTheme.typography.label,
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // GRID
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
                        CollectionBookItem(book, onClick = { onBookClick(book) })
                    }
                }

                // Loading States
                when {
                    books.loadState.refresh is LoadState.Loading -> {
                        item { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accentColor) } }
                    }
                    books.loadState.append is LoadState.Loading -> {
                        item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accentColor) } }
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionBookItem(book: Book, onClick: () -> Unit) {
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
            color = BookVerseTheme.colors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
