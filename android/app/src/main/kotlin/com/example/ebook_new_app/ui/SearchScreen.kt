package com.example.ebook_new_app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ebook_new_app.PremiumEasing
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.AppThemeMode
import com.example.ebook_new_app.viewmodel.AppViewModel
import com.example.ebook_new_app.viewmodel.HomeViewModel
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: HomeViewModel = viewModel(), appViewModel: AppViewModel = viewModel()) {
    val colors = BookVerseTheme.colors
    val currentTheme by appViewModel.themeMode

    var searchQuery by remember { mutableStateOf("") }
    var selectedPlanet by remember { mutableStateOf<PlanetData?>(null) }
    var showAuthors by remember { mutableStateOf(false) }

    val allCategories = listOf("Fiction", "History", "Science", "Romance", "Fantasy", "Mystery", "Biography", "Adventure")
    val planetSystems = rememberPlanetSystems(allCategories)

    val dummyBooks = remember {
        listOf(
            Book(id = "1", title = "The Grand Adventure", author = "J.R.R. Tolkien", categories = listOf("Fiction", "Adventure")),
            Book(id = "2", title = "A Brief History of Time", author = "Stephen Hawking", categories = listOf("Science", "History")),
            Book(id = "3", title = "Sherlock Holmes", author = "Arthur Conan Doyle", categories = listOf("Mystery", "Fiction")),
            Book(id = "4", title = "The Hobbit", author = "J.R.R. Tolkien", categories = listOf("Fantasy", "Adventure")),
        )
    }

    val filteredBooks = dummyBooks.filter { book ->
        val matchesQuery = book.title.contains(searchQuery, ignoreCase = true) || book.author.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedPlanet == null || book.categories.contains(selectedPlanet?.name)
        matchesQuery && matchesCategory
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        if (currentTheme == AppThemeMode.SPACE) {
            SpaceBackground()
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "EXPLORE",
                    style = BookVerseTheme.typography.headline,
                    color = colors.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                IconButton(onClick = { showAuthors = true }) {
                    Icon(Icons.Default.Group, contentDescription = "Authors", tint = colors.primary)
                }
            }

            // --- SEARCH BAR ---
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 0.dp),
                placeholder = { Text("Search titles or authors...", color = colors.textSecondary.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface.copy(alpha = if (currentTheme == AppThemeMode.SPACE) 0.5f else 1f),
                    unfocusedContainerColor = colors.surface.copy(alpha = if (currentTheme == AppThemeMode.SPACE) 0.5f else 1f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.primary
                ),
                shape = RoundedCornerShape(16.dp)
            )

            if (currentTheme == AppThemeMode.SPACE) {
                CelestialNavigation(
                    planetSystems = planetSystems,
                    selectedPlanet = selectedPlanet,
                    onPlanetClick = { planet ->
                        selectedPlanet = if (selectedPlanet == planet) null else planet
                    }
                )
            } else {
                CategoryDrawerRow(
                    categories = allCategories,
                    selectedCategory = selectedPlanet?.name,
                    onCategoryClick = { category ->
                        selectedPlanet = if (selectedPlanet?.name == category) null else planetSystems.find { it.planet.name == category }?.planet
                    }
                )
            }

            // --- CONTENT AREA ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedPlanet != null,
                    enter = expandVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(600, easing = PremiumEasing)) + fadeOut()
                ) {
                    selectedPlanet?.let { planet ->
                        if (currentTheme == AppThemeMode.SPACE) {
                            DetailsDrawer(planet = planet, books = filteredBooks)
                        } else {
                            DetailsDrawer(category = planet.name, books = filteredBooks)
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedPlanet == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (searchQuery.isEmpty()) {
                        RandomRecommendations(books = dummyBooks.shuffled())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredBooks) { book -> SearchResultItem(book) }
                        }
                    }
                }
            }
        }

        // --- AUTHORS OVERLAY ---
        AnimatedVisibility(
            visible = showAuthors,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            AuthorsSection(onClose = { showAuthors = false })
        }
    }
}

@Composable
fun CelestialNavigation(
    planetSystems: List<PlanetSystem>,
    selectedPlanet: PlanetData?,
    onPlanetClick: (PlanetData) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        items(planetSystems) { system ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CelestialBody(
                    planet = system.planet,
                    orbitingPlanets = system.orbitingPlanets,
                    isExpanded = selectedPlanet == system.planet,
                    onClick = { onPlanetClick(system.planet) }
                )
                Text(
                    text = system.planet.name.uppercase(),
                    style = BookVerseTheme.typography.label,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun rememberPlanetSystems(categories: List<String>): List<PlanetSystem> {
    return remember(categories) {
        categories.map { category ->
            val planetData = planetForCategory(category)
            PlanetSystem(
                planet = planetData,
                orbitingPlanets = List(3) {
                    OrbitingPlanet(
                        data = PlanetData(
                            name = "",
                            mainColor = Color.Transparent,
                            accentColors = listOf(planetData.accentColors.random(), planetData.accentColors.random()),
                            radius = Random.nextDouble(3.0, 8.0).dp
                        ),
                        orbitRadius = Random.nextDouble(planetData.radius.value + 10.0, planetData.radius.value + 20.0).dp,
                        angleOffset = Random.nextFloat() * 360f,
                        speed = Random.nextDouble(0.8, 1.5).toFloat()
                    )
                }
            )
        }
    }
}

data class PlanetSystem(
    val planet: PlanetData,
    val orbitingPlanets: List<OrbitingPlanet>
)

fun planetForCategory(category: String): PlanetData {
    return when (category) {
        "Fiction" -> PlanetData("Fiction", Color(0xFFC70039), listOf(Color(0xFFC70039), Color(0xFF900C3F)), 30.dp)
        "History" -> PlanetData("History", Color(0xFF00A8CC), listOf(Color(0xFF00A8CC), Color(0xFF007A99)), 28.dp)
        "Science" -> PlanetData("Science", Color(0xFF9B59B6), listOf(Color(0xFF9B59B6), Color(0xFF8E44AD)), 32.dp)
        "Romance" -> PlanetData("Romance", Color(0xFFE91E63), listOf(Color(0xFFE91E63), Color(0xFFC2185B)), 29.dp)
        "Fantasy" -> PlanetData("Fantasy", Color(0xFF2ECC71), listOf(Color(0xFF2ECC71), Color(0xFF27AE60)), 35.dp)
        "Mystery" -> PlanetData("Mystery", Color(0xFF34495E), listOf(Color(0xFF34495E), Color(0xFF2C3E50)), 31.dp)
        "Biography" -> PlanetData("Biography", Color(0xFFF1C40F), listOf(Color(0xFFF1C40F), Color(0xFFF39C12)), 28.dp)
        "Adventure" -> PlanetData("Adventure", Color(0xFFE67E22), listOf(Color(0xFFE67E22), Color(0xFFD35400)), 34.dp)
        else -> PlanetData("Default", Color(0xFF7F8C8D), listOf(Color(0xFF7F8C8D), Color(0xFF95A5A6)), 25.dp)
    }
}

@Composable
fun CategoryDrawerRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(start = 0.dp, top = 24.dp, end = 0.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 0.dp)
    ) {
        items(categories) { category ->
            DrawerFrontPanel(
                title = category,
                isOpen = selectedCategory == category,
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

@Composable
fun DrawerFrontPanel(title: String, isOpen: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    val animatedOffsetY by animateDpAsState(
        targetValue = if (isOpen) 15.dp else 0.dp,
        animationSpec = tween(400, easing = PremiumEasing),
        label = "drawer_pull"
    )

    val drawerColor = when (title) {
        "Fiction" -> Color(0xFF5D4037)
        "History" -> Color(0xFF4E342E)
        "Science" -> Color(0xFF3E2723)
        "Romance" -> Color(0xFF5D4037)
        "Fantasy" -> Color(0xFF4E342E)
        "Mystery" -> Color(0xFF3E2723)
        "Biography" -> Color(0xFF5D4037)
        "Adventure" -> Color(0xFF4E342E)
        else -> Color(0xFF3E2723)
    }

    Box(
        modifier = Modifier
            .offset(y = animatedOffsetY)
            .width(140.dp)
            .height(140.dp)
            .graphicsLayer {
                rotationX = if (isOpen) -8f else 0f
                cameraDistance = 10f * density
            }
            .shadow(if (isOpen) 20.dp else 4.dp, RoundedCornerShape(12.dp))
            .background(drawerColor, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        CategoryStencilArt(category = title, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = title.uppercase(),
                style = BookVerseTheme.typography.label,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            VintageDrawerKnob()
        }
    }
}

@Composable
fun VintageDrawerKnob() {
    Box(
        modifier = Modifier
            .size(26.dp)
            .shadow(6.dp, CircleShape)
            .background(
                Brush.radialGradient(listOf(Color(0xFFD4AF37), Color(0xFF8B4513))),
                CircleShape
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(8.dp).background(Color.Black.copy(0.2f), CircleShape))
    }
}

@Composable
fun CategoryStencilArt(category: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)) {
        val path = Path()
        val color = Color.White.copy(alpha = 0.15f)
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (category) {
            "Fiction" -> {
                path.moveTo(size.width * 0.2f, size.height * 0.8f)
                path.lineTo(size.width * 0.2f, size.height * 0.2f)
                path.lineTo(size.width * 0.8f, size.height * 0.2f)
                path.lineTo(size.width * 0.8f, size.height * 0.8f)
                path.close()
                path.moveTo(size.width * 0.5f, size.height * 0.2f)
                path.lineTo(size.width * 0.5f, size.height * 0.8f)
            }
            "History" -> {
                path.addOval(androidx.compose.ui.geometry.Rect(center, size.width * 0.45f))
                path.moveTo(center.x, center.y - size.height * 0.3f)
                path.lineTo(center.x, center.y + size.height * 0.3f)
            }
            "Science" -> {
                path.addOval(androidx.compose.ui.geometry.Rect(center, size.width * 0.4f))
                path.addOval(androidx.compose.ui.geometry.Rect(center, size.width * 0.15f))
            }
            "Romance" -> {
                path.moveTo(size.width / 2, size.height * 0.8f)
                path.cubicTo(0f, size.height * 0.3f, size.width * 0.2f, 0f, size.width / 2, size.height * 0.2f)
                path.cubicTo(size.width * 0.8f, 0f, size.width, size.height * 0.3f, size.width / 2, size.height * 0.8f)
            }
            else -> {
                path.moveTo(0f, 0f)
                path.lineTo(size.width, size.height)
            }
        }
        drawPath(path, color, style = stroke)
    }
}

@Composable
fun DetailsDrawer(category: String, books: List<Book>) {
    val categoryColor = when (category) {
        "Fiction" -> Color(0xFF5D4037)
        "History" -> Color(0xFF4E342E)
        "Science" -> Color(0xFF3E2723)
        "Romance" -> Color(0xFF5D4037)
        "Fantasy" -> Color(0xFF4E342E)
        "Mystery" -> Color(0xFF3E2723)
        "Biography" -> Color(0xFF5D4037)
        "Adventure" -> Color(0xFF4E342E)
        else -> Color(0xFF3E2723)
    }

    val animatedColor by animateColorAsState(
        targetValue = categoryColor,
        animationSpec = tween(durationMillis = 600, easing = PremiumEasing)
    )

    val woodyBrush = Brush.verticalGradient(listOf(animatedColor, animatedColor.copy(alpha = 0.8f)))

    Column(
        modifier = Modifier.fillMaxSize().background(woodyBrush).padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text(
            text = "ARCHIVE: ${category.uppercase()}",
            style = BookVerseTheme.typography.label,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
            letterSpacing = 2.sp
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 100.dp)
        ) {
            items(books) { book -> ArchiveCardItem(book) }
        }
    }
}

@Composable
fun DetailsDrawer(planet: PlanetData, books: List<Book>) {
    val animatedColor by animateColorAsState(
        targetValue = planet.mainColor,
        animationSpec = tween(durationMillis = 600, easing = PremiumEasing)
    )

    val spaceBrush = Brush.verticalGradient(
        listOf(
            animatedColor.copy(alpha = 0.5f),
            Color.Black
        )
    )

    Column(
        modifier = Modifier.fillMaxSize().background(spaceBrush).padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text(
            text = "ARCHIVE: ${planet.name.uppercase()}",
            style = BookVerseTheme.typography.label,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
            letterSpacing = 2.sp
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 100.dp)
        ) {
            items(books) { book -> ArchiveCardItem(book) }
        }
    }
}


@Composable
fun ArchiveCardItem(book: Book) {
    Column(
        modifier = Modifier.width(100.dp).clickable { /* Inspect */ },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = book.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(4.dp))
                .shadow(8.dp)
        )
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
fun SearchResultItem(book: Book) {
    val colors = BookVerseTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = book.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(60.dp, 90.dp).clip(RoundedCornerShape(8.dp)).background(colors.surface)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(book.title, style = BookVerseTheme.typography.body, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            Text(book.author, style = BookVerseTheme.typography.label, color = colors.textSecondary)
        }
    }
}

@Composable
fun RandomRecommendations(books: List<Book>) {
    val colors = BookVerseTheme.colors
    Column(modifier = Modifier.fillMaxSize().padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)) {
        Text("DISCOVER", style = BookVerseTheme.typography.label, color = colors.textSecondary, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(books) { book -> SearchResultItem(book) }
        }
    }
}

@Composable
fun AuthorsSection(onClose: () -> Unit) {
    val colors = BookVerseTheme.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AUTHORS", style = BookVerseTheme.typography.headline, color = colors.textPrimary)
            TextButton(onClick = onClose) { Text("CLOSE", color = colors.primary) }
        }

        val dummyAuthors = listOf("J.R.R. Tolkien", "Stephen Hawking", "Arthur Conan Doyle", "Agatha Christie", "George Orwell")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(dummyAuthors) { author ->
                AuthorItem(author)
            }
        }
    }
}

@Composable
fun AuthorItem(name: String) {
    val colors = BookVerseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(colors.primary.copy(0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), color = colors.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(name, style = BookVerseTheme.typography.body, color = colors.textPrimary, fontWeight = FontWeight.Bold)
    }
}
