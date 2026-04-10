package com.freeebookbundle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.freeebookbundle.R
import com.freeebookbundle.db.AppDatabase
import com.freeebookbundle.models.ReadingStatus
import com.freeebookbundle.utils.ReviewManager
import kotlinx.coroutines.flow.first
import com.freeebookbundle.viewmodel.AppThemeMode
import com.freeebookbundle.viewmodel.AppViewModel
import android.app.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    appViewModel: AppViewModel = viewModel(),
    database: AppDatabase? = null,
    onBackClick: () -> Unit = {}
) {
    val colors = BookVerseTheme.colors
    val themeMode by appViewModel.themeMode
    var activeSubScreen by remember { mutableStateOf<String?>(null) }

    if (activeSubScreen != null) {
        BackHandler { activeSubScreen = null }
    }

    val initials = if (userName.isNotEmpty()) {
        val parts = userName.split(" ")
        if (parts.size >= 2) "${parts[0].firstOrNull() ?: ""}${parts.last().firstOrNull() ?: ""}".uppercase()
        else userName.take(2).uppercase()
    } else "GU"

    val bgColor = when (themeMode) {
        AppThemeMode.DAY -> Color(0xFFF5F1E0)
        AppThemeMode.NIGHT -> Color(0xFF0A0A0A)
        AppThemeMode.SPACE -> Color(0xFF050510)
    }
    val textColor = when (themeMode) {
        AppThemeMode.DAY -> Color(0xFF5B4636)
        AppThemeMode.NIGHT -> Color.White
        AppThemeMode.SPACE -> Color.White
    }
    val accentColor = when (themeMode) {
        AppThemeMode.DAY -> Color(0xFF5B4636)
        AppThemeMode.NIGHT -> Color(0xFFE50914)
        AppThemeMode.SPACE -> Color(0xFF4A90D9)
    }
    val surfaceColor = when (themeMode) {
        AppThemeMode.DAY -> Color.White
        AppThemeMode.NIGHT -> Color(0xFF1A1A1A)
        AppThemeMode.SPACE -> Color.Transparent
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        when (themeMode) {
            AppThemeMode.SPACE -> SpaceBackground()
            AppThemeMode.NIGHT -> NightProfileBackground()
            AppThemeMode.DAY -> {}
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(activeSubScreen ?: stringResource(R.string.profile_title), fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 20.sp, color = textColor) },
                    navigationIcon = {
                        IconButton(onClick = { if (activeSubScreen != null) activeSubScreen = null else onBackClick() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.profile_back), tint = textColor)
                        }
                    },
                    actions = {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(
                            onClick = { 
                                val activity = context as? Activity ?: return@IconButton
                                ReviewManager.requestRatingWithStars(activity, themeMode)
                            },
                            modifier = Modifier.clip(CircleShape).background(when (themeMode) {
                                AppThemeMode.DAY -> Color(0xFF5B4636).copy(alpha = 0.1f)
                                AppThemeMode.NIGHT -> Color.White.copy(alpha = 0.1f)
                                AppThemeMode.SPACE -> Color.White.copy(alpha = 0.1f)
                            })
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Rate app",
                                tint = accentColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = textColor)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    ProfileAvatar(initials = initials, themeMode = themeMode)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(userName, style = MaterialTheme.typography.headlineMedium, color = textColor, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.profile_level), color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    ProfileOption(stringResource(R.string.profile_reading_statistics), Icons.Default.BarChart, themeMode = themeMode, accentColor = accentColor, surfaceColor = surfaceColor, textColor = textColor) { activeSubScreen = "STATISTICS" }
                    ProfileOption(stringResource(R.string.profile_settings), Icons.Default.Settings, themeMode = themeMode, accentColor = accentColor, surfaceColor = surfaceColor, textColor = textColor) { activeSubScreen = "SETTINGS" }
                    ProfileOption(stringResource(R.string.profile_help_support), Icons.AutoMirrored.Filled.HelpOutline, themeMode = themeMode, accentColor = accentColor, surfaceColor = surfaceColor, textColor = textColor) { activeSubScreen = "SUPPORT" }
                    ProfileOption(stringResource(R.string.profile_licenses), Icons.Default.Description, themeMode = themeMode, accentColor = accentColor, surfaceColor = surfaceColor, textColor = textColor) { activeSubScreen = "LICENSES" }
                }

                AnimatedVisibility(
                    visible = activeSubScreen != null,
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { it } + fadeOut()
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = when (themeMode) {
                        AppThemeMode.DAY -> Color.White
                        AppThemeMode.NIGHT -> Color(0xFF1A1A1A)
                        AppThemeMode.SPACE -> Color.Black.copy(alpha = 0.9f)
                    }) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            when(activeSubScreen) {
                                "STATISTICS" -> StatisticsContent(themeMode = themeMode, database = database)
                                "SETTINGS" -> SettingsContent(appViewModel)
                                "SUPPORT" -> SupportContent(themeMode = themeMode)
                                "LICENSES" -> LicensesContent(themeMode = themeMode)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(initials: String, themeMode: AppThemeMode) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = androidx.compose.animation.core.LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Restart),
        label = "rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = androidx.compose.animation.core.FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse),
        label = "pulse"
    )
    
    val avatarColor = when (themeMode) {
        AppThemeMode.SPACE -> Color(0xFF4A90D9)
        else -> BookVerseTheme.colors.primary
    }
    
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        if (themeMode == AppThemeMode.SPACE) {
            Canvas(modifier = Modifier.size(120.dp)) {
                drawCircle(
                    color = avatarColor.copy(alpha = 0.1f),
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = avatarColor.copy(alpha = 0.15f),
                    radius = size.minDimension / 2 * 0.8f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = avatarColor.copy(alpha = 0.2f),
                    radius = size.minDimension / 2 * 0.6f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
                
                for (i in 0 until 3) {
                    val angle = rotation + (i * 120f)
                    val orbitRadius = size.minDimension / 2 * 0.85f
                    val x = center.x + orbitRadius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
                    val y = center.y + orbitRadius * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
                    drawCircle(
                        color = Color.White.copy(alpha = pulse * 0.7f),
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
        
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = avatarColor.copy(alpha = if (themeMode == AppThemeMode.SPACE) 0.2f else 0.1f),
            border = if (themeMode == AppThemeMode.SPACE) androidx.compose.foundation.BorderStroke(2.dp, avatarColor.copy(alpha = 0.5f)) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(initials, color = if (themeMode == AppThemeMode.SPACE) Color.White else avatarColor, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun NightProfileBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFF0A0A0A))
        for (i in 0 until 30) {
            val x = kotlin.random.Random.nextFloat() * size.width
            val y = kotlin.random.Random.nextFloat() * size.height
            drawCircle(Color.White.copy(alpha = kotlin.random.Random.nextFloat() * 0.3f + 0.1f), radius = kotlin.random.Random.nextFloat() * 1.5f + 0.5f, center = Offset(x, y))
        }
    }
}

@Composable
fun ProfileOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    themeMode: AppThemeMode,
    accentColor: Color,
    surfaceColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val bgColor = when (themeMode) {
        AppThemeMode.DAY -> surfaceColor
        AppThemeMode.NIGHT -> surfaceColor
        AppThemeMode.SPACE -> Color.White.copy(alpha = 0.05f)
    }
    val borderColor = when (themeMode) {
        AppThemeMode.DAY -> Color(0xFFE8E0D0)
        AppThemeMode.NIGHT -> Color.White.copy(alpha = 0.1f)
        AppThemeMode.SPACE -> Color.White.copy(alpha = 0.1f)
    }
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text(title, color = textColor, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.ChevronRight, null, tint = when (themeMode) {
                AppThemeMode.DAY -> Color(0xFF5B4636).copy(alpha = 0.3f)
                AppThemeMode.NIGHT -> Color.White.copy(alpha = 0.3f)
                AppThemeMode.SPACE -> Color.White.copy(alpha = 0.3f)
            })
        }
    }
}

@Composable
fun StatisticsContent(
    themeMode: AppThemeMode = AppThemeMode.NIGHT,
    database: AppDatabase? = null
) {
    val colors = BookVerseTheme.colors
    val labelColor = when (themeMode) {
        AppThemeMode.SPACE -> Color(0xFF4A90D9)
        AppThemeMode.NIGHT -> Color(0xFFE50914)
        AppThemeMode.DAY -> Color(0xFF5B4636)
    }
    val textColor = when (themeMode) {
        AppThemeMode.SPACE -> Color.White
        AppThemeMode.NIGHT -> colors.textPrimary
        AppThemeMode.DAY -> Color(0xFF3D2914)
    }
    val subTextColor = when (themeMode) {
        AppThemeMode.SPACE -> Color.White.copy(alpha = 0.6f)
        AppThemeMode.NIGHT -> colors.textSecondary
        AppThemeMode.DAY -> Color(0xFF8B7355)
    }
    val cardBg = when (themeMode) {
        AppThemeMode.SPACE -> Color.White.copy(alpha = 0.05f)
        AppThemeMode.NIGHT -> colors.surface
        AppThemeMode.DAY -> Color(0xFFFFF8F0)
    }
    
    var stats by remember { mutableStateOf(ReadingStats()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(database) {
        if (database != null) {
            isLoading = true
            try {
                val allBooks = database.bookDao().getAllBooks().first()
                val totalBooks = allBooks.size
                val completedBooks = allBooks.count { it.readingStatus == ReadingStatus.COMPLETED }
                val currentlyReading = allBooks.count { it.readingStatus == ReadingStatus.CURRENTLY_READING }
                val wishlistCount = allBooks.count { it.readingStatus == ReadingStatus.WISHLIST }
                
                // Get category breakdown
                val categoryCounts = allBooks
                    .groupBy { it.categoryCode }
                    .mapNotNull { (code, books) -> 
                        if (code.isNotEmpty()) CategoryStat(code, books.size) else null
                    }
                    .sortedByDescending { it.count }
                    .take(5)
                
                // Get last activity
                val lastOpened = allBooks
                    .mapNotNull { it.lastOpened }
                    .maxOrNull()
                
                stats = ReadingStats(
                    totalBooks = totalBooks,
                    completedBooks = completedBooks,
                    currentlyReading = currentlyReading,
                    wishlistCount = wishlistCount,
                    lastActivityTime = lastOpened,
                    categoryBreakdown = categoryCounts
                )
            } catch (e: Exception) {
                stats = ReadingStats()
            }
            isLoading = false
        } else {
            stats = ReadingStats()
            isLoading = false
        }
    }
    
    LazyColumn {
        item {
            Text(stringResource(R.string.profile_reading_journey), style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(24.dp))
        }
        
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = labelColor, modifier = Modifier.size(32.dp))
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.profile_stat_books),
                        value = "${stats.totalBooks}",
                        subtitle = "${stats.completedBooks} ${stringResource(R.string.profile_stat_completed)}",
                        color = labelColor,
                        bgColor = cardBg,
                        textColor = textColor,
                        subTextColor = subTextColor
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.profile_stat_reading),
                        value = "${stats.currentlyReading}",
                        subtitle = stringResource(R.string.profile_stat_in_progress),
                        color = labelColor,
                        bgColor = cardBg,
                        textColor = textColor,
                        subTextColor = subTextColor
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.profile_stat_wishlist),
                        value = "${stats.wishlistCount}",
                        subtitle = stringResource(R.string.profile_stat_on_wishlist),
                        color = labelColor,
                        bgColor = cardBg,
                        textColor = textColor,
                        subTextColor = subTextColor
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.profile_stat_last_read),
                        value = formatLastRead(stats.lastActivityTime),
                        subtitle = stringResource(R.string.profile_stat_ago),
                        color = labelColor,
                        bgColor = cardBg,
                        textColor = textColor,
                        subTextColor = subTextColor
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
            
            item {
                CircularProgressCard(
                    completed = stats.completedBooks,
                    total = stats.totalBooks,
                    labelColor = labelColor,
                    bgColor = cardBg,
                    textColor = textColor,
                    subTextColor = subTextColor
                )
                Spacer(Modifier.height(24.dp))
            }
            
            item {
                StreakCard(
                    streak = stats.streak,
                    labelColor = labelColor,
                    bgColor = cardBg,
                    textColor = textColor,
                    subTextColor = subTextColor
                )
                Spacer(Modifier.height(24.dp))
            }
            
            item {
                GenreBreakdownCard(
                    labelColor = labelColor,
                    bgColor = cardBg,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    stats = stats
                )
            }
        }
    }
}

data class ReadingStats(
    val totalBooks: Int = 0,
    val completedBooks: Int = 0,
    val currentlyReading: Int = 0,
    val wishlistCount: Int = 0,
    val lastActivityTime: Long? = null,
    val categoryBreakdown: List<CategoryStat> = emptyList(),
    val streak: Int = 0
)

data class CategoryStat(val name: String, val count: Int)

@Composable
private fun formatLastRead(timestamp: Long?): String {
    if (timestamp == null) return "-"
    
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 604800_000 -> "${diff / 86400_000}d"
        else -> "${diff / 604800_000}w"
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    bgColor: Color,
    textColor: Color,
    subTextColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = subTextColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(value, color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = subTextColor, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CircularProgressCard(
    completed: Int,
    total: Int,
    labelColor: Color,
    bgColor: Color,
    textColor: Color,
    subTextColor: Color
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    
                    drawCircle(
                        color = subTextColor.copy(alpha = 0.2f),
                        radius = radius,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    drawArc(
                        color = labelColor,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    color = textColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(stringResource(R.string.profile_completion_rate), color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("$completed of $total books", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.profile_books_finished), color = subTextColor, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StreakCard(
    streak: Int,
    labelColor: Color,
    bgColor: Color,
    textColor: Color,
    subTextColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = labelColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = labelColor, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.profile_reading_streak), color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.profile_streak_days, streak), color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.profile_streak_message), color = subTextColor, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GenreBreakdownCard(
    labelColor: Color,
    bgColor: Color,
    textColor: Color,
    subTextColor: Color,
    stats: ReadingStats = ReadingStats()
) {
    // Default categories for when no real data is available
    val defaultGenres = listOf(
        GenreStat("Fiction", 0.35f, Color(0xFF5D4037)),
        GenreStat("Science", 0.25f, Color(0xFF2E7D32)),
        GenreStat("Romance", 0.20f, Color(0xFFAD1457)),
        GenreStat("History", 0.12f, Color(0xFF455A64)),
        GenreStat("Fantasy", 0.08f, Color(0xFF6A1B9A))
    )
    
    // Use real category data if available, otherwise fall back to defaults
    val genres = if (stats.categoryBreakdown.isNotEmpty() && stats.totalBooks > 0) {
        stats.categoryBreakdown.take(5).mapIndexed { index, cat ->
            val colors = listOf(
                Color(0xFF5D4037), Color(0xFF2E7D32), Color(0xFFAD1457),
                Color(0xFF455A64), Color(0xFF6A1B9A)
            )
            GenreStat(
                cat.name.replaceFirstChar { it.uppercase() }.take(20),
                cat.count.toFloat() / stats.totalBooks,
                colors.getOrElse(index) { colors.last() }
            )
        }
    } else {
        defaultGenres
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.profile_genre_breakdown), color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            
            genres.forEach { genre ->
                GenreBar(
                    name = genre.name,
                    progress = genre.progress,
                    color = genre.color,
                    textColor = textColor,
                    subTextColor = subTextColor
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

data class GenreStat(val name: String, val progress: Float, val color: Color)

@Composable
private fun GenreBar(
    name: String,
    progress: Float,
    color: Color,
    textColor: Color,
    subTextColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, color = textColor, fontSize = 12.sp)
            Text("${(progress * 100).toInt()}%", color = subTextColor, fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(subTextColor.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun SupportContent(themeMode: AppThemeMode = AppThemeMode.NIGHT) {
    val colors = BookVerseTheme.colors
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val labelColor = when (themeMode) {
        AppThemeMode.SPACE -> Color(0xFF4A90D9)
        else -> colors.primary
    }
    val textColor = when (themeMode) {
        AppThemeMode.SPACE -> Color.White
        else -> colors.textPrimary
    }
    val subTextColor = when (themeMode) {
        AppThemeMode.SPACE -> Color.White.copy(alpha = 0.6f)
        else -> colors.textSecondary
    }
    val cardBg = when (themeMode) {
        AppThemeMode.SPACE -> Color.White.copy(alpha = 0.05f)
        else -> colors.surface
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.profile_help_title), style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        
        SupportLinkCard(
            icon = Icons.Default.Email,
            title = stringResource(R.string.profile_contact_support),
            subtitle = stringResource(R.string.profile_send_email),
            themeMode = themeMode,
            cardBg = cardBg,
            textColor = textColor,
            subTextColor = subTextColor,
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:myebookbundle@gmail.com")
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Free Ebook Bundle - Support Request")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
        )
        
        Spacer(Modifier.height(12.dp))
        
        SupportLinkCard(
            icon = Icons.Default.Description,
            title = stringResource(R.string.profile_privacy_policy),
            subtitle = stringResource(R.string.profile_privacy_desc),
            themeMode = themeMode,
            cardBg = cardBg,
            textColor = textColor,
            subTextColor = subTextColor,
            onClick = { showPrivacyPolicy = true }
        )
        
        Spacer(Modifier.height(32.dp))
        
        Text(stringResource(R.string.profile_about_title), style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.profile_about_description),
            color = subTextColor,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(stringResource(R.string.profile_version_title) + " 1.0.0", color = subTextColor, fontSize = 12.sp)
    }
    
    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
    }
}

@Composable
private fun SupportLinkCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    themeMode: AppThemeMode,
    cardBg: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = subTextColor, fontSize = 12.sp)
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = subTextColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LicensesContent(themeMode: AppThemeMode = AppThemeMode.NIGHT) {
    val colors = BookVerseTheme.colors
    val labelColor = when (themeMode) {
        AppThemeMode.SPACE -> Color(0xFF4A90D9)
        else -> colors.primary
    }
    val textColor = when (themeMode) {
        AppThemeMode.SPACE -> Color.White
        else -> colors.textPrimary
    }
    val subTextColor = when (themeMode) {
        AppThemeMode.SPACE -> Color.White.copy(alpha = 0.6f)
        else -> colors.textSecondary
    }
    
    LazyColumn {
        item {
            Text(stringResource(R.string.profile_open_source), style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
        }
        
        item {
            LicenseItem("Readium Kotlin", "Apache 2.0", "EPUB rendering engine for reading EPUB files on mobile platforms with support for pagination, scroll mode, and decorations", textColor, subTextColor)
            LicenseItem("Jetpack Compose", "Apache 2.0", "Google's modern toolkit for building native Android UI with declarative patterns", textColor, subTextColor)
            LicenseItem("Coil", "Apache 2.0", "Kotlin-first image loading library with coroutines support for efficient cover image loading", textColor, subTextColor)
            LicenseItem("Room Database", "Apache 2.0", "Android persistence library providing an abstraction layer over SQLite for storing books and reading progress", textColor, subTextColor)
            LicenseItem("OkHttp", "Apache 2.0", "HTTP client for making network requests to OpenLibrary and Google Books APIs", textColor, subTextColor)
            LicenseItem("Material 3", "Apache 2.0", "Material Design 3 components for modern, accessible UI elements", textColor, subTextColor)
            LicenseItem("Navigation Compose", "Apache 2.0", "Type-safe navigation library for Compose with support for animations and deep linking", textColor, subTextColor)
            LicenseItem("Paging 3", "Apache 2.0", "Pagination library for loading large datasets efficiently with infinite scrolling support", textColor, subTextColor)
            LicenseItem("Jsoup", "MIT", "Java HTML parser for extracting text content from EPUB chapters for text-to-speech", textColor, subTextColor)
            LicenseItem("Piper TTS", "Apache 2.0", "On-device neural text-to-speech engine providing natural-sounding audiobook narration", textColor, subTextColor)
            LicenseItem("Kotlin Coroutines", "Apache 2.0", "Asynchronous programming library for managing TTS playback and network operations", textColor, subTextColor)
            LicenseItem("Gson", "Apache 2.0", "JSON serialization/deserialization for Room database type converters", textColor, subTextColor)
        }
        
        item {
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.profile_privacy_title), style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.profile_privacy_statement), color = subTextColor, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(24.dp))
        }
        
        item {
            Text(stringResource(R.string.profile_attribution_title), style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.profile_attribution_text), color = subTextColor, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.profile_copyright), color = subTextColor, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(24.dp))
        }
        
        item {
            Text(stringResource(R.string.profile_version_title), style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            Text("Ebook Bundle v1.0", color = textColor, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.profile_built_with), color = subTextColor, fontSize = 12.sp)
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun LicenseItem(name: String, license: String, description: String, textColor: Color, subTextColor: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, color = textColor, fontWeight = FontWeight.Bold)
            Text(license, color = subTextColor, fontSize = 11.sp)
        }
        Text(description, color = subTextColor, fontSize = 11.sp)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = subTextColor.copy(alpha = 0.1f))
    }
}

@Composable
fun SettingsContent(appViewModel: AppViewModel) {
    val colors = BookVerseTheme.colors
    val currentTheme by appViewModel.themeMode
    val useSpaceTouchNav by appViewModel.useSpaceTouchNav
    
    val textColor = when (currentTheme) {
        AppThemeMode.SPACE -> Color.White
        AppThemeMode.NIGHT -> Color.White
        AppThemeMode.DAY -> Color(0xFF5B4636)
    }
    val subTextColor = when (currentTheme) {
        AppThemeMode.SPACE -> Color.White.copy(alpha = 0.6f)
        AppThemeMode.NIGHT -> Color.White.copy(alpha = 0.6f)
        AppThemeMode.DAY -> Color(0xFF8B7355)
    }
    val labelColor = when (currentTheme) {
        AppThemeMode.SPACE -> Color(0xFF4A90D9)
        AppThemeMode.NIGHT -> Color(0xFFE50914)
        AppThemeMode.DAY -> Color(0xFF5B4636)
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.profile_appearance_title), style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        
        Text(stringResource(R.string.profile_theme_label), color = textColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeButton(
                label = stringResource(R.string.onboarding_theme_day),
                icon = Icons.Default.LightMode,
                isSelected = currentTheme == AppThemeMode.DAY,
                onClick = { appViewModel.setTheme(AppThemeMode.DAY) },
                modifier = Modifier.weight(1f)
            )
            ThemeButton(
                label = stringResource(R.string.onboarding_theme_night),
                icon = Icons.Default.Nightlight,
                isSelected = currentTheme == AppThemeMode.NIGHT,
                onClick = { appViewModel.setTheme(AppThemeMode.NIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeButton(
                label = stringResource(R.string.onboarding_theme_space),
                icon = Icons.Filled.Star,
                isSelected = currentTheme == AppThemeMode.SPACE,
                onClick = { appViewModel.setTheme(AppThemeMode.SPACE) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        if (currentTheme == AppThemeMode.SPACE) {
            Text(stringResource(R.string.profile_nav_style), color = textColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavStyleButton(
                    label = stringResource(R.string.profile_touch_pad),
                    icon = Icons.Default.TouchApp,
                    isSelected = useSpaceTouchNav,
                    onClick = { appViewModel.setUseSpaceTouchNav(true) },
                    modifier = Modifier.weight(1f)
                )
                NavStyleButton(
                    label = stringResource(R.string.profile_classic),
                    icon = Icons.Default.Menu,
                    isSelected = !useSpaceTouchNav,
                    onClick = { appViewModel.setUseSpaceTouchNav(false) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (useSpaceTouchNav) stringResource(R.string.profile_touch_pad_desc) else stringResource(R.string.profile_classic_desc),
                color = subTextColor,
                fontSize = 11.sp
            )
            
            Spacer(Modifier.height(24.dp))
        }
        
        HorizontalDivider(color = subTextColor.copy(alpha = 0.1f))
        Spacer(Modifier.height(24.dp))
        
        Text(stringResource(R.string.profile_notifications_title), style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.profile_push_notifications), color = textColor, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.profile_enabled), color = subTextColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(checked = true, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedTrackColor = labelColor))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.profile_wifi_only), color = textColor, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.profile_disabled), color = subTextColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(checked = false, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedTrackColor = labelColor))
        }
    }
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val colors = BookVerseTheme.colors
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.profile_privacy_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        text = stringResource(R.string.profile_privacy_last_updated),
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                }
                
                item {
                    Text(
                        text = "Tara Applications (\"we,\" \"us,\" or \"our\") operates the Free Ebook Bundle mobile application (hereinafter referred to as the \"Service\").",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
                
                item {
                    Text(
                        text = "INFORMATION WE COLLECT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "1. Information You Provide\n   • User profile name (stored locally on your device)\n   • Reading preferences and theme settings (stored locally on your device)\n\n2. Information Collected Automatically\n   • Book reading progress (stored locally on your device)\n   • Reading history and recently opened books (stored locally on your device)\n   • Anonymous usage analytics via Firebase (app interactions, crash reports) - no personally identifiable information",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
                
                item {
                    Text(
                        text = "THIRD-PARTY SERVICES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "We use the following third-party services:\n   • Google Books API\n   • OpenLibrary API\n   • Firebase (for anonymous analytics only)",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
                
                item {
                    Text(
                        text = "DATA STORAGE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "All personal data is stored locally on your device using the Room database. This data is NOT stored on our servers and remains under your device's control. We use Firebase Analytics for anonymous usage data collection to improve the app experience.",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
                
                item {
                    Text(
                        text = "PERMISSIONS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "• Internet Access: Required to download ebooks and fetch book metadata\n• Storage Access: Required to cache downloaded ebook files",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
                
                item {
                    Text(
                        text = "CHILDREN'S PRIVACY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Our Service is not intended for use by children under the age of 13. We do not knowingly collect personally identifiable information from children under 13.",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
                
                item {
                    Text(
                        text = "SECURITY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "The security of your data is important to us. We strive to use commercially acceptable means to protect your personal data.",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
                
                item {
                    Text(
                        text = "YOUR DATA RIGHTS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "• Access your personal data stored locally\n• Delete all data by uninstalling the app\n• Reset reading progress by clearing app data",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
                
                item {
                    Text(
                        text = "CONTACT US",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "• Company: Tara Applications\n• Email: myebookbundle@gmail.com\n• Website: https://tarapplications.com",
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_close))
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary
    )
}

@Composable
private fun ThemeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentColors = BookVerseTheme.colors
    val isDarkCurrent = currentColors.isDark
    
    val bgColor = when {
        isSelected && label == "DAY" -> Color(0xFF5B4636)
        isSelected && label == "NIGHT" -> Color(0xFFE50914)
        isSelected && label == "SPACE" -> Color(0xFF4A90D9)
        !isDarkCurrent -> Color(0xFFFFF8E7)
        else -> Color(0xFF2A2A2A)
    }
    val textColor = when {
        isSelected -> Color.White
        !isDarkCurrent -> Color(0xFF5B4636)
        else -> Color.White
    }
    val borderColor = when {
        !isDarkCurrent -> Color(0xFFD4C4A8)
        else -> Color(0xFF444444)
    }
    
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = if (!isDarkCurrent && !isSelected) 3.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NavStyleButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(0xFF4A90D9)
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
    val borderColor = if (isSelected) accentColor else Color.White.copy(alpha = 0.1f)
    val iconColor = if (isSelected) accentColor else Color.White.copy(alpha = 0.7f)
    
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = iconColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String, textColor: Color = BookVerseTheme.colors.textPrimary, valueColor: Color = BookVerseTheme.colors.primary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = textColor)
        Text(value, color = valueColor, fontWeight = FontWeight.Black)
    }
}
