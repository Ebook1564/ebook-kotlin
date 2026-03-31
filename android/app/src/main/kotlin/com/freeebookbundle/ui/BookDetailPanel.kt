package com.freeebookbundle.ui

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
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.freeebookbundle.R
import com.freeebookbundle.models.Book
import com.freeebookbundle.models.ReadingStatus
import com.freeebookbundle.viewmodel.CuratedCollection
import com.freeebookbundle.viewmodel.CuratedCollections
import com.freeebookbundle.viewmodel.InitialBounds

@Composable
fun BookDetailContent(
    book: Book,
    isLoading: Boolean,
    onReadClick: (InitialBounds?) -> Unit,
    onCategoryClick: (String) -> Unit,
    onAddToReadingList: (ReadingStatus) -> Unit,
    onClose: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onAudioClick: (Book) -> Unit = {}
) {
    var displayBook by remember { mutableStateOf(book) }
    
    LaunchedEffect(book) {
        displayBook = book
    }

    var coverBounds by remember { mutableStateOf<InitialBounds?>(null) }
    var coverCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val colors = BookVerseTheme.colors
    val typography = BookVerseTheme.typography
    var showStatusMenu by remember { mutableStateOf(false) }

    val isDescriptionLoading = displayBook.description.isBlank() && isLoading

    LaunchedEffect(coverCoordinates) {
        coverCoordinates?.let { coords ->
            if (coords.isAttached) {
                delay(16)
                delay(16)
                val pos = coords.positionInWindow()
                coverBounds = InitialBounds(
                    left = pos.x, top = pos.y,
                    width = coords.size.width.toFloat(), height = coords.size.height.toFloat()
                )
            }
        }
    }

    // DYNAMIC HEIGHT WRAPPER
    Surface(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = colors.surface,
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            
            // 1. BACKDROP
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                var backdropFallback by remember { mutableStateOf(false) }
                val serverCoverUrl = displayBook.getServerCoverUrl()
                val bestCoverUrl = displayBook.getBestCoverUrl()
                val backdropUrl = if (backdropFallback) serverCoverUrl else bestCoverUrl
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(60.dp).alpha(0.3f),
                    onError = {
                        backdropFallback = true
                    }
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

            // 2. CONTENT COLUMN
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 48.dp) 
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
                                coverCoordinates = coords
                            },
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 24.dp
                    ) {
                        var useFallback by remember { mutableStateOf(false) }
                        val serverCoverUrl = displayBook.getServerCoverUrl()
                        val bestCoverUrl = displayBook.getBestCoverUrl()
                        val finalCoverUrl = if (useFallback) serverCoverUrl else bestCoverUrl
                        
                        AsyncImage(
                            model = finalCoverUrl,
                            contentDescription = displayBook.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            onError = {
                                useFallback = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayBook.title,
                            style = typography.headline,
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 26.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            onClick = { onAuthorClick(displayBook.author) },
                            shape = RoundedCornerShape(16.dp),
                            color = colors.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = displayBook.author.uppercase(),
                                    style = typography.label,
                                    color = colors.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onReadClick(coverBounds) },
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = colors.primary),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.home_read_now),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            FilledTonalButton(
                                onClick = { onAudioClick(displayBook) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = colors.primary.copy(alpha = 0.15f)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Headset, 
                                    null, 
                                    modifier = Modifier.size(20.dp),
                                    tint = colors.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box {
                            OutlinedButton(
                                onClick = { showStatusMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (displayBook.readingStatus != ReadingStatus.NONE) colors.primary else colors.textPrimary
                                )
                            ) {
                                Icon(if (displayBook.readingStatus != ReadingStatus.NONE) Icons.Default.Check else Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when(displayBook.readingStatus) {
                                        ReadingStatus.CURRENTLY_READING -> stringResource(R.string.book_detail_reading)
                                        ReadingStatus.COMPLETED -> stringResource(R.string.book_detail_finished)
                                        ReadingStatus.WISHLIST -> stringResource(R.string.book_detail_wishlist)
                                        else -> stringResource(R.string.book_detail_add_to_list)
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
                                        text = { Text(
                                            when(status) { 
                                                ReadingStatus.CURRENTLY_READING -> stringResource(R.string.book_detail_currently_reading)
                                                ReadingStatus.COMPLETED -> stringResource(R.string.book_detail_completed)
                                                else -> stringResource(R.string.book_detail_want_to_read)
                                            }, 
                                            color = colors.textPrimary
                                        ) },
                                        onClick = { onAddToReadingList(status); showStatusMenu = false },
                                        leadingIcon = { Icon(if (status == ReadingStatus.CURRENTLY_READING) Icons.Default.PlayArrow else Icons.Default.Add, null, tint = if (displayBook.readingStatus == status) colors.primary else colors.textSecondary) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // CURATED COLLECTIONS (if book matches any)
                // Also check categoryCode from database as fallback
                val bookTags = remember(displayBook.subjects, displayBook.categories) {
                    displayBook.subjects + displayBook.categories
                }
                
                val matchingCollections = remember(bookTags) {
                    CuratedCollections.collections.filter { collection ->
                        collection.tags.any { tag -> 
                            bookTags.any { subject -> 
                                subject.contains(tag, ignoreCase = true) || 
                                tag.contains(subject, ignoreCase = true)
                            }
                        }
                    }
                }
                
                if (matchingCollections.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.book_detail_collections),
                            style = typography.headline,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(matchingCollections) { collection ->
                                CuratedCollectionChip(
                                    collection = collection,
                                    onClick = { onCategoryClick(collection.tags.first()) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // DIVERSIFIED TAGS (OpenLibrary Subjects)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val tags = if (displayBook.subjects.isNotEmpty()) displayBook.subjects else listOf("Public Domain", "Classic")
                    items(tags) { tag -> 
                        DetailChip(label = tag, onClick = { onCategoryClick(tag) }) 
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // DESCRIPTION
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(stringResource(R.string.book_detail_about), style = typography.headline, color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (isDescriptionLoading) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(colors.textSecondary.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                            Box(modifier = Modifier.fillMaxWidth(0.9f).height(16.dp).background(colors.textSecondary.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                            Box(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).background(colors.textSecondary.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                        }
                    } else {
                        Text(
                            text = displayBook.description,
                            style = typography.body,
                            color = colors.textSecondary.copy(alpha = 0.9f),
                            lineHeight = 22.sp,
                            fontSize = 15.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DetailChip(label: String, onClick: () -> Unit) {
    val colors = BookVerseTheme.colors
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.2f)),
        modifier = Modifier.clickable { onClick() }
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

@Composable
fun CuratedCollectionChip(collection: CuratedCollection, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = collection.accentColor,
        modifier = Modifier.clickable { onClick() },
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = collection.iconEmoji,
                fontSize = 16.sp
            )
            Column {
                Text(
                    text = collection.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = collection.subtitle,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
