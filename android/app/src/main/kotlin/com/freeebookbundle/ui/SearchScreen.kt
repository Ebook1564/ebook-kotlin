package com.freeebookbundle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.freeebookbundle.R
import com.freeebookbundle.models.Book
import com.freeebookbundle.viewmodel.AppThemeMode
import com.freeebookbundle.viewmodel.AppViewModel
import com.freeebookbundle.viewmodel.HomeViewModel
import com.freeebookbundle.viewmodel.SearchFilter

@Composable
fun SearchScreen(
    viewModel: HomeViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel(),
    isSpaceActive: Boolean = true,
    onBookClick: (Book) -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onExploreAllClick: () -> Unit = {}
) {
    val colors = BookVerseTheme.colors
    val currentTheme by appViewModel.themeMode
    val isSpace = currentTheme == AppThemeMode.SPACE
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.filterResults.collectAsLazyPagingItems()
    var showAllGenres by remember { mutableStateOf(false) }
    var showAllAuthors by remember { mutableStateOf(false) }

    var dynamicCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var featuredAuthors by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val genres = viewModel.getTopGenres(12)
            dynamicCategories = genres.map { it.name }
            featuredAuthors = viewModel.getTopAuthors(10)
        } catch (_: Exception) {}
    }

    val allCategories = dynamicCategories.ifEmpty {
        listOf("Fiction", "Romance", "Science Fiction & Fantasy", "Adventure", "Short Stories", "Children & Young Adult", "Historical Novels", "Poetry", "Mystery", "Fantasy", "Biography", "Crime, Thrillers and Mystery")
    }

    val hasFilter = uiState.activeFilter !is SearchFilter.None || uiState.searchQuery.isNotEmpty()

    if (hasFilter) {
        BackHandler { viewModel.clearFilters() }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        if (isSpace) {
            SpaceBackground(isActive = isSpaceActive)
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // EXPLORE LABEL
            Text(
                text = stringResource(R.string.search_explore),
                style = BookVerseTheme.typography.headline,
                color = colors.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // SEARCH BAR
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.search_placeholder),
                        color = colors.textSecondary.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.primary) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Text(stringResource(R.string.search_clear), color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface.copy(alpha = if (isSpace) 0.5f else 1f),
                    unfocusedContainerColor = colors.surface.copy(alpha = if (isSpace) 0.5f else 1f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.primary
                ),
                shape = RoundedCornerShape(16.dp)
            )

            if (hasFilter) {
                // RESULTS AREA
                Text(
                    stringResource(R.string.search_explore_all),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = BookVerseTheme.typography.label,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = { index -> "${pagingItems[index]?.id ?: "null"}_$index" }
                    ) { index ->
                        pagingItems[index]?.let { book ->
                            SearchResultItem(book, onClick = onBookClick, onAuthorClick = onAuthorClick)
                        }
                    }
                    if (pagingItems.loadState.append is LoadState.Loading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            } else {
                // DEFAULT BROWSE MODE — same format for all themes
                BrowseContent(
                    allCategories = allCategories,
                    viewModel = viewModel,
                    featuredAuthors = featuredAuthors,
                    onBookClick = onBookClick,
                    onAuthorClick = { author -> viewModel.openAuthor(author) },
                    onSeeAllGenres = { showAllGenres = true },
                    onSeeAllAuthors = { showAllAuthors = true }
                )
            }
        }

        // SEE ALL GENRES OVERLAY
        if (showAllGenres) {
            AllGenresPage(
                categories = allCategories,
                onBack = { showAllGenres = false },
                onGenreClick = { genre -> showAllGenres = false; viewModel.openCollection(genre) }
            )
        }

        // SEE ALL AUTHORS OVERLAY
        if (showAllAuthors) {
            AllAuthorsPage(
                onBack = { showAllAuthors = false },
                onAuthorClick = { author -> showAllAuthors = false; viewModel.openAuthor(author) }
            )
        }
    }
}

@Composable
private fun BrowseContent(
    allCategories: List<String>,
    viewModel: HomeViewModel,
    featuredAuthors: List<Pair<String, Int>>,
    onBookClick: (Book) -> Unit,
    onAuthorClick: (String) -> Unit,
    onSeeAllGenres: () -> Unit,
    onSeeAllAuthors: () -> Unit
) {
    val colors = BookVerseTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // GENRES SECTION
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Genres",
                    style = BookVerseTheme.typography.label,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    stringResource(R.string.search_see_all),
                    style = BookVerseTheme.typography.label,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onSeeAllGenres() }
                )
            }
        }

        // GENRES GRID — scribble art cards
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                allCategories.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { category ->
                            GenreBlock(
                                category = category,
                                onClick = { viewModel.openCollection(category) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(2 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }

        // FEATURED AUTHORS SECTION
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Featured Authors",
                    style = BookVerseTheme.typography.label,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    stringResource(R.string.search_see_all),
                    style = BookVerseTheme.typography.label,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onSeeAllAuthors() }
                )
            }
        }

        // AUTHORS LIST
        items(
            count = featuredAuthors.size,
            key = { index -> featuredAuthors[index].first }
        ) { index ->
            val (authorName, bookCount) = featuredAuthors[index]
            val authorColor = getAuthorColor(authorName)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAuthorClick(authorName) }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(authorColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authorName.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = authorName,
                        style = BookVerseTheme.typography.body,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$bookCount books",
                        style = BookVerseTheme.typography.label,
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun GenreBlock(
    category: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val genreColor = getDrawerColor(category)
    val genreFont = remember(category) { getGenreFont(category, context) }

    Box(
        modifier = modifier
            .aspectRatio(1.4f)
            .clip(RoundedCornerShape(12.dp))
            .background(genreColor.copy(alpha = 0.15f))
            .clickable { onClick() }
    ) {
        GenreScribble(category = category, color = genreColor)

        Text(
            text = getCategoryDisplayName(category),
            color = Color.White,
            fontFamily = genreFont,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        )
    }
}

@Composable
private fun GenreScribble(category: String, color: Color) {
    val scribbleColor = color.copy(alpha = 0.18f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (category) {
            "Romance" -> {
                val heartPath = Path().apply {
                    moveTo(cx, cy + h * 0.15f)
                    cubicTo(cx - w * 0.35f, cy - h * 0.15f, cx - w * 0.35f, cy - h * 0.35f, cx, cy - h * 0.1f)
                    cubicTo(cx + w * 0.35f, cy - h * 0.35f, cx + w * 0.35f, cy - h * 0.15f, cx, cy + h * 0.15f)
                }
                drawPath(heartPath, scribbleColor, style = Stroke(width = 3.dp.toPx()))
                drawCircle(scribbleColor, radius = 3.dp.toPx(), center = Offset(cx - w * 0.2f, cy - h * 0.2f))
                drawCircle(scribbleColor, radius = 2.dp.toPx(), center = Offset(cx + w * 0.25f, cy - h * 0.15f))
            }
            "Science Fiction & Fantasy", "Science Fiction" -> {
                val starPath = Path().apply {
                    val r = minOf(w, h) * 0.22f
                    for (i in 0 until 5) {
                        val angle = Math.toRadians((i * 72 - 90).toDouble())
                        val innerAngle = Math.toRadians((i * 72 + 36 - 90).toDouble())
                        val ox = cx + r * kotlin.math.cos(angle).toFloat()
                        val oy = cy + r * kotlin.math.sin(angle).toFloat()
                        val ix = cx + r * 0.45f * kotlin.math.cos(innerAngle).toFloat()
                        val iy = cy + r * 0.45f * kotlin.math.sin(innerAngle).toFloat()
                        if (i == 0) moveTo(ox, oy) else lineTo(ox, oy)
                        lineTo(ix, iy)
                    }
                    close()
                }
                drawPath(starPath, scribbleColor, style = Stroke(width = 2.5.dp.toPx()))
                drawCircle(scribbleColor, radius = minOf(w, h) * 0.32f, center = Offset(cx, cy), style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
            }
            "Fantasy" -> {
                drawArc(scribbleColor, startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(cx - w * 0.2f, cy - h * 0.25f), size = Size(w * 0.4f, h * 0.45f), style = Stroke(width = 3.dp.toPx()))
                drawCircle(scribbleColor, radius = 2.dp.toPx(), center = Offset(cx + w * 0.18f, cy - h * 0.18f))
                drawCircle(scribbleColor, radius = 1.5.dp.toPx(), center = Offset(cx - w * 0.15f, cy + h * 0.1f))
                drawCircle(scribbleColor, radius = 2.dp.toPx(), center = Offset(cx + w * 0.1f, cy + h * 0.2f))
            }
            "Adventure" -> {
                val mountainPath = Path().apply {
                    moveTo(0f, h * 0.75f)
                    lineTo(w * 0.2f, h * 0.3f)
                    lineTo(w * 0.35f, h * 0.5f)
                    lineTo(w * 0.55f, h * 0.2f)
                    lineTo(w * 0.7f, h * 0.45f)
                    lineTo(w * 0.85f, h * 0.25f)
                    lineTo(w, h * 0.6f)
                }
                drawPath(mountainPath, scribbleColor, style = Stroke(width = 2.5.dp.toPx()))
                val wavePath = Path().apply {
                    moveTo(0f, h * 0.85f)
                    for (x in 0..w.toInt() step 8) {
                        val y = h * 0.85f + kotlin.math.sin(x * 0.04).toFloat() * h * 0.04f
                        lineTo(x.toFloat(), y)
                    }
                }
                drawPath(wavePath, scribbleColor, style = Stroke(width = 1.5.dp.toPx()))
            }
            "Mystery", "Crime, Thrillers and Mystery" -> {
                for (i in 0..3) {
                    val y = cy - h * 0.15f + i * h * 0.1f
                    drawLine(scribbleColor, start = Offset(cx - w * 0.2f, y), end = Offset(cx + w * 0.2f, y), strokeWidth = 2.dp.toPx())
                    drawLine(scribbleColor, start = Offset(cx + w * 0.2f, y), end = Offset(cx + w * 0.2f, y + h * 0.08f), strokeWidth = 2.dp.toPx())
                }
                drawCircle(scribbleColor, radius = minOf(w, h) * 0.12f, center = Offset(cx + w * 0.15f, cy - h * 0.05f), style = Stroke(width = 2.dp.toPx()))
            }
            "Short Stories" -> {
                for (i in 0..4) {
                    val y = cy - h * 0.2f + i * h * 0.1f
                    val lineW = if (i % 2 == 0) w * 0.5f else w * 0.35f
                    drawLine(scribbleColor, start = Offset(cx - lineW / 2, y), end = Offset(cx - lineW / 2 + lineW, y), strokeWidth = 2.dp.toPx())
                }
            }
            "Children & Young Adult", "Humour" -> {
                for (i in 0..2) {
                    val angle = Math.toRadians((i * 120).toDouble())
                    val sx = cx + minOf(w, h) * 0.2f * kotlin.math.cos(angle).toFloat()
                    val sy = cy + minOf(w, h) * 0.2f * kotlin.math.sin(angle).toFloat()
                    drawLine(scribbleColor, start = Offset(cx, cy), end = Offset(sx, sy), strokeWidth = 2.dp.toPx())
                    drawCircle(scribbleColor, radius = 3.dp.toPx(), center = Offset(sx, sy))
                }
                val zigPath = Path().apply {
                    moveTo(cx - w * 0.25f, cy + h * 0.2f)
                    for (i in 0..4) {
                        val x = cx - w * 0.25f + i * w * 0.12f
                        val y = if (i % 2 == 0) cy + h * 0.2f else cy + h * 0.3f
                        lineTo(x, y)
                    }
                }
                drawPath(zigPath, scribbleColor, style = Stroke(width = 2.dp.toPx()))
            }
            "Historical Novels" -> {
                drawArc(scribbleColor, startAngle = 0f, sweepAngle = 270f, useCenter = false, topLeft = Offset(cx - w * 0.15f, cy - h * 0.2f), size = Size(w * 0.3f, h * 0.35f), style = Stroke(width = 2.5.dp.toPx()))
                drawLine(scribbleColor, start = Offset(cx - w * 0.15f, cy + h * 0.15f), end = Offset(cx - w * 0.15f, cy + h * 0.3f), strokeWidth = 2.dp.toPx())
                drawLine(scribbleColor, start = Offset(cx + w * 0.15f, cy + h * 0.15f), end = Offset(cx + w * 0.15f, cy + h * 0.3f), strokeWidth = 2.dp.toPx())
            }
            "Poetry" -> {
                val swooshPath = Path().apply {
                    moveTo(cx - w * 0.3f, cy)
                    cubicTo(cx - w * 0.1f, cy - h * 0.3f, cx + w * 0.1f, cy + h * 0.3f, cx + w * 0.3f, cy)
                }
                drawPath(swooshPath, scribbleColor, style = Stroke(width = 3.dp.toPx()))
                drawCircle(scribbleColor, radius = 2.dp.toPx(), center = Offset(cx - w * 0.25f, cy - h * 0.05f))
                drawCircle(scribbleColor, radius = 1.5.dp.toPx(), center = Offset(cx + w * 0.28f, cy + h * 0.05f))
            }
            "Biography", "Philosophy & Ethics" -> {
                for (i in 0..3) {
                    val x = cx - w * 0.15f + i * w * 0.1f
                    drawLine(scribbleColor, start = Offset(x, cy - h * 0.25f), end = Offset(x, cy + h * 0.25f), strokeWidth = 2.dp.toPx())
                }
                drawLine(scribbleColor, start = Offset(cx - w * 0.2f, cy), end = Offset(cx + w * 0.2f, cy), strokeWidth = 2.5.dp.toPx())
            }
            "Classics of Literature" -> {
                val quillPath = Path().apply {
                    moveTo(cx - w * 0.1f, cy + h * 0.25f)
                    cubicTo(cx, cy + h * 0.1f, cx + w * 0.05f, cy - h * 0.1f, cx + w * 0.15f, cy - h * 0.3f)
                }
                drawPath(quillPath, scribbleColor, style = Stroke(width = 2.5.dp.toPx()))
                drawLine(scribbleColor, start = Offset(cx - w * 0.2f, cy - h * 0.05f), end = Offset(cx + w * 0.2f, cy - h * 0.05f), strokeWidth = 1.5.dp.toPx())
                drawLine(scribbleColor, start = Offset(cx - w * 0.15f, cy + h * 0.05f), end = Offset(cx + w * 0.15f, cy + h * 0.05f), strokeWidth = 1.5.dp.toPx())
            }
            else -> {
                val flowPath = Path().apply {
                    moveTo(0f, cy)
                    cubicTo(w * 0.25f, cy - h * 0.2f, w * 0.5f, cy + h * 0.2f, w, cy)
                }
                drawPath(flowPath, scribbleColor, style = Stroke(width = 2.5.dp.toPx()))
                drawCircle(scribbleColor, radius = 2.dp.toPx(), center = Offset(cx - w * 0.2f, cy - h * 0.1f))
                drawCircle(scribbleColor, radius = 1.5.dp.toPx(), center = Offset(cx + w * 0.2f, cy + h * 0.1f))
            }
        }
    }
}

private fun getGenreFont(category: String, context: android.content.Context): FontFamily {
    val fontPath = when (category) {
        "Romance", "Classics of Literature" -> "fonts/PlayfairDisplay-Italic.ttf"
        "Science Fiction & Fantasy", "Science Fiction" -> "fonts/Orbitron-Bold.ttf"
        "Fantasy" -> "fonts/Cinzel-Regular.ttf"
        "Adventure" -> "fonts/Bangers-Regular.ttf"
        "Mystery", "Crime, Thrillers and Mystery" -> "fonts/Creepster-Regular.ttf"
        "Poetry" -> "fonts/Calligraffitti-Regular.ttf"
        "Children & Young Adult", "Humour" -> "fonts/FredokaOne-Regular.ttf"
        "Short Stories", "Fiction", "Novels" -> "fonts/Lora-Regular.ttf"
        "Historical Novels" -> "fonts/CinzelDecorative-Regular.ttf"
        "Biography", "Philosophy & Ethics" -> "fonts/EBGaramond-Regular.ttf"
        else -> "fonts/Lora-Regular.ttf"
    }
    return try {
        FontFamily(Font(assetManager = context.assets, path = fontPath))
    } catch (_: Exception) {
        FontFamily.SansSerif
    }
}

@Composable
private fun AllGenresPage(
    categories: List<String>,
    onBack: () -> Unit,
    onGenreClick: (String) -> Unit
) {
    val colors = BookVerseTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    val filtered = if (searchQuery.isBlank()) categories
        else categories.filter { it.contains(searchQuery, ignoreCase = true) }

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(colors.surface)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "All Genres",
                    style = BookVerseTheme.typography.headline,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Black
                )
            }

            // SEARCH
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                placeholder = { Text("Search genres...", color = colors.textSecondary.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.primary
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // GENRE LIST
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered.size) { index ->
                    val category = filtered[index]
                    val genreColor = getDrawerColor(category)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .clickable { onGenreClick(category) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(genreColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = getCategoryDisplayName(category),
                            style = BookVerseTheme.typography.body,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllAuthorsPage(
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit
) {
    val colors = BookVerseTheme.colors
    val viewModel: HomeViewModel = viewModel()
    val appViewModel: AppViewModel = viewModel()
    val currentTheme by appViewModel.themeMode
    val isSpace = currentTheme == AppThemeMode.SPACE
    val authorUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authorItems = viewModel.authorResults.collectAsLazyPagingItems()

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(colors.surface)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "All Authors",
                    style = BookVerseTheme.typography.headline,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Black
                )
            }

            // SEARCH
            TextField(
                value = authorUiState.authorSearchQuery,
                onValueChange = { viewModel.onAuthorSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                placeholder = { Text("Search authors...", color = colors.textSecondary.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.primary) },
                trailingIcon = {
                    if (authorUiState.authorSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onAuthorSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.primary
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // AUTHOR LIST
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = authorItems.itemCount,
                    key = { index -> "${authorItems[index] ?: "null"}_$index" }
                ) { index ->
                    authorItems[index]?.let { author ->
                        val authorColor = getAuthorColor(author)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surface)
                                .clickable { onAuthorClick(author) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(authorColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = author.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = author,
                                    style = BookVerseTheme.typography.body,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = getAuthorGenreHint(author),
                                    style = BookVerseTheme.typography.label,
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = colors.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (authorItems.loadState.append is LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveCardItem(book: Book, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(100.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(4.dp))
                .shadow(8.dp)
                .background(BookVerseTheme.colors.surface)
        ) {
            val serverIp = "10.0.2.2"
            var fallbackStage by remember { mutableIntStateOf(0) }
            val imageUrl = when(fallbackStage) {
                0 -> "http://$serverIp:8080/${book.id}_${book.id}-cover.png"
                1 -> "http://$serverIp:8080/${book.id}_${book.id}-cover.jpg"
                2 -> "http://$serverIp:8080/${android.net.Uri.encode(book.title)}_cover.${book.id}"
                else -> ""
            }

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    if (fallbackStage < 2) {
                        fallbackStage++
                    }
                },
                error = painterResource(id = R.drawable.ic_book)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.title,
            style = BookVerseTheme.typography.label,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
    }
}

@Composable
fun SearchResultItem(
    book: Book, 
    onClick: (Book) -> Unit,
    onAuthorClick: ((String) -> Unit)? = null
) {
    val authorColor = getAuthorColor(book.author)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(book) }
            .padding(vertical = 8.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(60.dp, 90.dp).clip(RoundedCornerShape(8.dp)).background(BookVerseTheme.colors.surface).shadow(4.dp)) {
            val coverUrl = book.getBestCoverUrl()
            AsyncImage(
                model = coverUrl, 
                contentDescription = book.title, 
                contentScale = ContentScale.Crop, 
                modifier = Modifier.fillMaxSize(),
                onError = {
                    // Will show placeholder
                }
            )
        }
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(book.title, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = BookVerseTheme.colors.textPrimary, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            if (onAuthorClick != null) {
                Text(
                    text = book.author,
                    fontSize = 15.sp,
                    color = authorColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onAuthorClick(book.author) }
                )
            } else {
                Text(book.author, fontSize = 15.sp, color = BookVerseTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun getDrawerColor(cat: String): Color {
    // Check language prefixes first
    return when {
        cat.startsWith("FR ") -> Color(0xFF0055A4)
        cat.startsWith("DE ") -> Color(0xFFFFC800)
        cat.startsWith("IT ") -> Color(0xFF009246)
        cat.startsWith("PT ") -> Color(0xFF006600)
        cat.startsWith("ES ") -> Color(0xFFAA0000)
        else -> when(cat) {
            "Fiction", "Novels" -> Color(0xFF6366F1)
            "History of the Americas" -> Color(0xFF8B5CF6)
            "Science-Fiction & Fantasy" -> Color(0xFF3B82F6)
            "Romance" -> Color(0xFFEC4899)
            "Fantasy" -> Color(0xFF6A1B9A)
            "Mystery" -> Color(0xFF37474F)
            "Biography", "Biographies" -> Color(0xFF14B8A6)
            "Adventure" -> Color(0xFF10B981)
            "Short Stories" -> Color(0xFF06B6D4)
            "Children & Young Adult" -> Color(0xFFF59E0B)
            "Poetry" -> Color(0xFFEF4444)
            "Humour" -> Color(0xFF84CC16)
            "Crime, Thrillers and Mystery" -> Color(0xFF37474F)
            "Historical Novels" -> Color(0xFF8B5CF6)
            "Science Fiction" -> Color(0xFF3B82F6)
            "Classics of Literature" -> Color(0xFF6366F1)
            "Children's Fiction" -> Color(0xFFF59E0B)
            "Philosophy & Ethics" -> Color(0xFF14B8A6)
            "American Literature" -> Color(0xFF6366F1)
            "British Literature" -> Color(0xFF6366F1)
            else -> Color(0xFFE50914)
        }
    }
}

@Composable
fun getCategoryDisplayName(category: String): String {
    val hardcoded = when (category) {
        "Fiction", "Novels", "novels" -> stringResource(R.string.category_fiction)
        "History of the Americas", "Historical Novels", "historical_novels" -> stringResource(R.string.category_history)
        "Science", "Science Fiction & Fantasy", "science_fiction_fantasy" -> stringResource(R.string.category_science)
        "Romance", "romance" -> stringResource(R.string.category_romance)
        "Fantasy", "fantasy" -> stringResource(R.string.category_fantasy)
        "Mystery", "mystery" -> stringResource(R.string.category_mystery)
        "Biography", "Biographies", "biographies" -> stringResource(R.string.category_biography)
        "Adventure", "adventure" -> stringResource(R.string.category_adventure)
        "Short Stories", "short_stories" -> stringResource(R.string.category_short_stories)
        "Children & Young Adult Reading", "children_young_adult_reading" -> stringResource(R.string.category_children)
        "Poetry", "poetry" -> stringResource(R.string.category_poetry)
        "Humour", "humour" -> stringResource(R.string.category_humour)
        else -> null
    }

    if (hardcoded != null) return hardcoded

    // High-quality humanizing fallback
    return category.replace("_", " ").split(" ")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}

fun getAuthorColor(authorName: String): Color {
    val hash = authorName.hashCode()
    val colors = listOf(
        Color(0xFF5D4037), Color(0xFF455A64), Color(0xFF2E7D32), Color(0xFFAD1457),
        Color(0xFF6A1B9A), Color(0xFF37474F), Color(0xFF00695C), Color(0xFFEF6C00),
        Color(0xFFE50914), Color(0xFF1E88E5), Color(0xFF7B1FA2), Color(0xFF00897B),
        Color(0xFFE65100), Color(0xFF6D4C41), Color(0xFF546E7A), Color(0xFF8D6E63)
    )
    return colors[Math.abs(hash) % colors.size]
}

fun getAuthorGenreHint(authorName: String): String {
    val hash = authorName.hashCode()
    val genres = listOf(
        "Fiction", "Science Fiction", "Romance", "Fantasy", "Mystery", 
        "Thriller", "Biography", "Adventure", "Historical", "Classic",
        "Drama", "Horror", "Poetry", "Philosophy", "Science", "History"
    )
    return genres[Math.abs(hash) % genres.size]
}
