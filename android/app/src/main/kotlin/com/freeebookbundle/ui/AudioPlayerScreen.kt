package com.freeebookbundle.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.freeebookbundle.viewmodel.AppThemeMode
import kotlinx.coroutines.delay
import kotlin.math.sin

data class ChapterInfo(
    val title: String,
    val index: Int
)

data class TtsPlayerState(
    val bookTitle: String = "",
    val authorName: String = "",
    val coverUrl: String? = null,
    val isPlaying: Boolean = false,
    val currentProgress: Float = 0f,
    val speechRate: Float = 1f,
    val selectedVoice: String = "",
    val availableVoices: List<String> = emptyList(),
    val chapters: List<ChapterInfo> = emptyList(),
    val currentChapter: Int = 0,
    val currentParagraph: String = "",
    val currentSentenceIndex: Int = 0,
    val totalSentences: Int = 1,
    val themeMode: AppThemeMode = AppThemeMode.NIGHT
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    state: TtsPlayerState,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onVoiceChange: (String) -> Unit,
    onClose: () -> Unit,
    onChapterSelect: ((Int) -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onBookmarksClick: (() -> Unit)? = null
) {
    val backgroundColor = when (state.themeMode) {
        AppThemeMode.SPACE -> Color(0xFF0A0A1A)
        else -> Color(0xFF121212)
    }
    
    val accentColor = when (state.themeMode) {
        AppThemeMode.SPACE -> Color(0xFF4A90D9)
        else -> Color(0xFFE50914)
    }
    
    val surfaceColor = when (state.themeMode) {
        AppThemeMode.SPACE -> Color(0xFF1A1A2E)
        else -> Color(0xFF1A1A1A)
    }
    
    val textColor = Color.White
    val subTextColor = Color.White.copy(alpha = 0.6f)

    val infiniteTransition = rememberInfiniteTransition(label = "player")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isPlaying) 1.01f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    var showChapterSheet by remember { mutableStateOf(false) }
    var showVoiceMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        AnimatedBackground(
            isPlaying = state.isPlaying,
            accentColor = accentColor,
            modifier = Modifier.fillMaxSize()
        )

        state.coverUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .alpha(0.25f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = textColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Text(
                    "NOW PLAYING",
                    color = subTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(0.65f)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                if (state.isPlaying) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        rotate(rotation) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.minDimension / 2 + 8.dp.toPx()
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 16.dp
                ) {
                    if (state.coverUrl != null) {
                        AsyncImage(
                            model = state.coverUrl,
                            contentDescription = state.bookTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(surfaceColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = subTextColor,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }

                if (state.isPlaying) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp),
                        shape = CircleShape,
                        color = accentColor,
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = state.bookTitle,
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = state.authorName,
                color = subTextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (state.chapters.isNotEmpty() && state.currentChapter < state.chapters.size) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceColor
                ) {
                    Text(
                        text = state.chapters[state.currentChapter].title,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedParagraph(
                paragraph = state.currentParagraph,
                currentSentenceIndex = state.currentSentenceIndex,
                totalSentences = state.totalSentences,
                accentColor = accentColor,
                textColor = textColor,
                subTextColor = subTextColor,
                surfaceColor = surfaceColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = state.currentProgress,
                    onValueChange = onSeek,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(state.currentProgress * 100).toInt()}%",
                        color = subTextColor,
                        fontSize = 11.sp
                    )
                    Text(
                        text = state.selectedVoice.substringBefore("-", state.selectedVoice),
                        color = subTextColor,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(onClick = { showVoiceMenu = true }) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice",
                            tint = subTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showVoiceMenu,
                        onDismissRequest = { showVoiceMenu = false }
                    ) {
                        state.availableVoices.forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice, fontSize = 13.sp) },
                                onClick = {
                                    onVoiceChange(voice)
                                    showVoiceMenu = false
                                },
                                leadingIcon = {
                                    if (voice == state.selectedVoice) {
                                        Icon(Icons.Default.Check, null, tint = accentColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onSkipBackward,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "Skip back",
                        tint = textColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                FloatingActionButton(
                    onClick = onPlayPause,
                    containerColor = accentColor,
                    contentColor = Color.White,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onSkipForward,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "Skip forward",
                        tint = textColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSpeedMenu = true },
                        color = surfaceColor
                    ) {
                        Text(
                            "${state.speechRate}x",
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x", fontSize = 13.sp) },
                                onClick = {
                                    onSpeechRateChange(speed)
                                    showSpeedMenu = false
                                },
                                leadingIcon = {
                                    if (speed == state.speechRate) {
                                        Icon(Icons.Default.Check, null, tint = accentColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showChapterSheet = true },
                        enabled = state.chapters.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Chapters",
                            tint = if (state.chapters.isNotEmpty()) accentColor else subTextColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text("Chapters", color = if (state.chapters.isNotEmpty()) accentColor else subTextColor.copy(alpha = 0.4f), fontSize = 10.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showSettingsMenu = true }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = subTextColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text("Settings", color = subTextColor, fontSize = 10.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onBookmarksClick?.invoke() }
                    ) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmarks",
                            tint = subTextColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text("Bookmarks", color = subTextColor, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            AudioWaveform(
                isPlaying = state.isPlaying,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showChapterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChapterSheet = false },
                sheetState = sheetState,
                containerColor = surfaceColor
            ) {
                ChapterSelectionSheet(
                    chapters = state.chapters,
                    currentChapter = state.currentChapter,
                    onChapterSelected = { index ->
                        onChapterSelect?.invoke(index)
                        showChapterSheet = false
                    },
                    accentColor = accentColor
                )
            }
        }

        if (showSettingsMenu) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsMenu = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = surfaceColor
            ) {
                SettingsSheet(
                    onSettingsClick = {
                        onSettingsClick?.invoke()
                        showSettingsMenu = false
                    },
                    accentColor = accentColor,
                    textColor = textColor
                )
            }
        }
    }
}

@Composable
private fun AnimatedParagraph(
    paragraph: String,
    currentSentenceIndex: Int,
    totalSentences: Int,
    accentColor: Color,
    textColor: Color,
    subTextColor: Color,
    surfaceColor: Color
) {
    var previousSentenceIndex by remember { mutableIntStateOf(currentSentenceIndex) }
    val isSentenceChanged = previousSentenceIndex != currentSentenceIndex
    
    LaunchedEffect(currentSentenceIndex) {
        previousSentenceIndex = currentSentenceIndex
    }

    if (paragraph.isEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(12.dp),
            color = surfaceColor.copy(alpha = 0.5f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start reading to see the current text...",
                    color = subTextColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val sentences = remember(paragraph) {
        paragraph.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
    }

    val displayRange = remember(currentSentenceIndex, sentences.size) {
        val start = (currentSentenceIndex - 2).coerceAtLeast(0)
        val count = minOf(5, sentences.size - start)
        start until (start + count)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            sentences.forEachIndexed { index, sentence ->
                val isCurrentSentence = index == currentSentenceIndex
                val isVisible = index in displayRange
                
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(150))
                ) {
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isCurrentSentence) accentColor.copy(alpha = 0.12f) else Color.Transparent,
                        animationSpec = tween(300),
                        label = "bg"
                    )
                    
                    val textAlpha by animateFloatAsState(
                        targetValue = if (isCurrentSentence) 1f else 0.5f,
                        animationSpec = tween(300),
                        label = "alpha"
                    )
                    
                    val textColor by animateColorAsState(
                        targetValue = if (isCurrentSentence) accentColor else textColor.copy(alpha = textAlpha),
                        animationSpec = tween(300),
                        label = "color"
                    )
                    
                    val fontWeight by animateFontWeightAsState(
                        targetValue = if (isCurrentSentence) FontWeight.Medium else FontWeight.Normal,
                        animationSpec = tween(300),
                        label = "weight"
                    )
                    
                    Surface(
                        color = backgroundColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = sentence,
                            color = textColor,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = fontWeight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun animateFontWeightAsState(
    targetValue: FontWeight,
    animationSpec: AnimationSpec<Float>,
    label: String
): State<FontWeight> {
    val animatable = remember { Animatable(0f) }
    
    LaunchedEffect(targetValue) {
        animatable.animateTo(
            targetValue = when (targetValue) {
                FontWeight.Normal -> 0f
                FontWeight.SemiBold -> 1f
                FontWeight.Bold -> 1f
                else -> 0f
            },
            animationSpec = animationSpec
        )
    }
    
    return remember {
        derivedStateOf {
            when {
                animatable.value < 0.5f -> FontWeight.Normal
                else -> FontWeight.SemiBold
            }
        }
    }
}

@Composable
private fun ChapterSelectionSheet(
    chapters: List<ChapterInfo>,
    currentChapter: Int,
    onChapterSelected: (Int) -> Unit,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Jump to Chapter",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            itemsIndexed(chapters) { index, chapter ->
                val isCurrentChapter = index == currentChapter
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onChapterSelected(index) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrentChapter) accentColor.copy(alpha = 0.2f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            color = if (isCurrentChapter) accentColor else Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = chapter.title,
                            color = if (isCurrentChapter) accentColor else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrentChapter) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCurrentChapter) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Current",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSheet(
    onSettingsClick: () -> Unit,
    accentColor: Color,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Quick Settings",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettingsClick() },
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reading Settings",
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Theme, font size, scroll mode",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun AnimatedBackground(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    
    val hueShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hue"
    )

    Canvas(modifier = modifier) {
        val gradientColors = if (isPlaying) {
            listOf(
                accentColor.copy(alpha = 0.12f),
                Color.Black.copy(alpha = 0.92f),
                accentColor.copy(alpha = 0.08f)
            )
        } else {
            listOf(
                Color.Black.copy(alpha = 0.95f),
                Color(0xFF0A0A1A)
            )
        }

        drawRect(brush = Brush.verticalGradient(gradientColors))

        if (isPlaying) {
            for (i in 0 until 15) {
                val x = (size.width * (i / 15f))
                val y = size.height * (0.3f + sin(i.toFloat() + hueShift * 0.1f) * 0.15f)
                val radius = 2f + (i % 3) * 1.5f
                drawCircle(
                    color = accentColor.copy(alpha = 0.08f + (i % 4) * 0.02f),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun AudioWaveform(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform_progress"
    )

    Canvas(modifier = modifier) {
        val barCount = 40
        val barWidth = size.width / barCount
        val centerY = size.height / 2

        for (i in 0 until barCount) {
            val phase = (i.toFloat() / barCount + animationProgress) % 1f
            val amplitude: Float = if (isPlaying) {
                ((sin(phase * Math.PI * 2) * 0.5f + 0.5f) * size.height * 0.7f).toFloat()
            } else {
                (size.height * 0.15f).toFloat()
            }
            
            val alpha: Float = (0.25f + (sin(phase * Math.PI) * 0.25f)).toFloat()
            
            drawRoundRect(
                color = accentColor.copy(alpha = alpha),
                topLeft = Offset(i * barWidth + barWidth * 0.2f, centerY - amplitude * 0.5f),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.6f, amplitude.coerceAtLeast(2f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
        }
    }
}
