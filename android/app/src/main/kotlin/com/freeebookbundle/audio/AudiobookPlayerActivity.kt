package com.freeebookbundle.audio

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.freeebookbundle.models.Book
import com.freeebookbundle.ui.BookVerseTheme
import com.freeebookbundle.viewmodel.AppThemeMode
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class ChaptersResponse(val chapters: List<String>)

class AudiobookPlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val book = intent.getParcelableExtra<Book>("BOOK") ?: run {
            Toast.makeText(this, "Book data missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val audioUrl = book.audioUrl ?: run {
            Toast.makeText(this, "Audio URL missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            BookVerseTheme(themeMode = AppThemeMode.NIGHT) {
                AudiobookPlayer(
                    book = book,
                    audioUrl = audioUrl,
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun AudiobookPlayer(
    book: Book,
    audioUrl: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val player = remember {
        MediaPlayer().apply {
            setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
        }
    }

    val chapters = remember { mutableStateListOf<String>() }
    val isLoadingChapters = remember { mutableStateOf(true) }
    val currentChapterIndex = remember { mutableIntStateOf(0) }
    val isPrepared = remember { mutableStateOf(false) }
    val isPlaying = remember { mutableStateOf(false) }
    val currentPosition = remember { mutableLongStateOf(0L) }
    val chapterDuration = remember { mutableLongStateOf(0L) }
    val isSeeking = remember { mutableStateOf(false) }

    val baseUrl = audioUrl.trimEnd('/')

    fun loadChapter(index: Int) {
        if (index < 0 || index >= chapters.size) return
        currentChapterIndex.value = index
        isPrepared.value = false
        currentPosition.value = 0L
        try {
            player.reset()
            player.setDataSource("$baseUrl/${chapters[index]}")
            player.setOnPreparedListener { mp ->
                chapterDuration.value = mp.duration.toLong()
                isPrepared.value = true
                if (isPlaying.value) mp.start()
            }
            player.setOnCompletionListener {
                val next = currentChapterIndex.value + 1
                if (next < chapters.size) {
                    loadChapter(next)
                } else {
                    isPlaying.value = false
                }
            }
            player.setOnErrorListener { _, what, extra ->
                isPrepared.value = false
                android.util.Log.e("AudiobookPlayer", "MediaPlayer error: what=$what extra=$extra")
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        try {
            val url = URL("$baseUrl/chapters.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val json = conn.inputStream.bufferedReader().readText()
            val resp = Gson().fromJson(json, ChaptersResponse::class.java)
            chapters.addAll(resp.chapters)
        } catch (_: Exception) {
            val discovered = withContext(Dispatchers.IO) {
                val list = mutableListOf<String>()
                var i = 1
                while (i <= 999) {
                    val fn = "${book.id}-${i.toString().padStart(2, '0')}.m4b"
                    try {
                        val url = URL("$baseUrl/$fn")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 2000
                        conn.readTimeout = 2000
                        if (conn.responseCode == 200) {
                            list.add(fn)
                            i++
                        } else break
                    } catch (_: Exception) { break }
                }
                list
            }
            chapters.addAll(discovered)
        }
        isLoadingChapters.value = false
        if (chapters.isNotEmpty()) {
            loadChapter(0)
        } else {
            Toast.makeText(context, "No chapters found", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            if (isPlaying.value && !isSeeking.value) {
                try {
                    currentPosition.value = player.currentPosition.toLong()
                } catch (_: Exception) { }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (player.isPlaying) {
                        player.pause()
                        isPlaying.value = false
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (player.isPlaying) player.stop()
            player.release()
        }
    }

    val colors = BookVerseTheme.colors
    val coverUrl = book.getBestCoverUrl()
    val progress = if (chapterDuration.value > 0) currentPosition.value.toFloat() / chapterDuration.value.toFloat() else 0f
    val currentFormatted = formatTime(currentPosition.value)
    val totalFormatted = formatTime(chapterDuration.value)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(50.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chapters.isNotEmpty()) {
                    Text(
                        text = "Chapter ${currentChapterIndex.value + 1} of ${chapters.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (isLoadingChapters.value) {
                    Text(
                        text = "Loading...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AsyncImage(
                model = coverUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = book.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = book.author.uppercase(),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoadingChapters.value || chapters.isEmpty()) {
                CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(48.dp))
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Slider(
                        value = progress,
                        onValueChange = {
                            isSeeking.value = true
                            currentPosition.value = (it * chapterDuration.value).toLong()
                        },
                        onValueChangeFinished = {
                            try {
                                player.seekTo(currentPosition.value.toInt())
                            } catch (_: Exception) { }
                            isSeeking.value = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(20.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(currentFormatted, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Text(totalFormatted, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            if (currentChapterIndex.value > 0) {
                                isPlaying.value = true
                                loadChapter(currentChapterIndex.value - 1)
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        enabled = currentChapterIndex.value > 0
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious, "Previous chapter",
                            tint = Color.White, modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            try {
                                val pos = (player.currentPosition - 15000).coerceAtLeast(0)
                                player.seekTo(pos)
                            } catch (_: Exception) { }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.FastRewind, "Rewind 15 seconds",
                            tint = Color.White, modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                            .clickable {
                                if (!isPrepared.value) return@clickable
                                if (isPlaying.value) {
                                    player.pause()
                                    isPlaying.value = false
                                } else {
                                    player.start()
                                    isPlaying.value = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isPrepared.value) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(30.dp)
                            )
                        } else {
                            Icon(
                                if (isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (isPlaying.value) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = {
                            try {
                                val pos = (player.currentPosition + 15000).coerceAtMost(chapterDuration.value.toInt())
                                player.seekTo(pos)
                            } catch (_: Exception) { }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.FastForward, "Forward 15 seconds",
                            tint = Color.White, modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (currentChapterIndex.value + 1 < chapters.size) {
                                isPlaying.value = true
                                loadChapter(currentChapterIndex.value + 1)
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        enabled = currentChapterIndex.value + 1 < chapters.size
                    ) {
                        Icon(
                            Icons.Default.SkipNext, "Next chapter",
                            tint = Color.White, modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun formatTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%02d:%02d", minutes, seconds)
}
