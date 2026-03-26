package com.freeebookbundle.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.freeebookbundle.PremiumEasing
import com.freeebookbundle.models.Book
import com.freeebookbundle.utils.FileDownloader
import com.freeebookbundle.viewmodel.InitialBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun BookTransitionOverlay(
    book: Book?,
    initialBounds: InitialBounds?,
    themeColor: Color = Color(0xFFFDFCF0),
    textColor: Color = Color.Black,
    onAnimationFinished: (File) -> Unit,
    onDismiss: () -> Unit
) {
    if (book == null) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val colors = BookVerseTheme.colors
    
    val pageBorderColor = if (!colors.isDark) colors.textSecondary.copy(alpha = 0.2f) else Color.Transparent
    val dimColor = if (colors.isDark) Color.Black else Color.Black.copy(alpha = 0.7f)

    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Downloading) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    BackHandler(enabled = downloadState == DownloadState.Error) {
        onDismiss()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(999f)
            .background(Color.Transparent)
            .clickable(enabled = false) {}
    ) {
        val sW = constraints.maxWidth.toFloat()
        val sH = constraints.maxHeight.toFloat()

        val startW = initialBounds?.width ?: (sW * 0.3f)
        val startH = initialBounds?.height ?: (startW * 1.5f)
        val startX = initialBounds?.left ?: (sW / 2 - startW / 2)
        val startY = initialBounds?.top ?: (sH / 2 - startH / 2)

        val centerW = sW * 0.75f
        val centerH = centerW * 1.5f
        val centerX = (sW - centerW) / 2
        val centerY = (sH - centerH) / 2
        
        val liftProgress = remember { Animatable(0f) }
        val immersionZoom = remember { Animatable(0f) }
        val bgAlpha = remember { Animatable(0f) }
        val dogEarAlpha = remember { Animatable(0f) }
        val flipRotation = remember { Animatable(0f) }
        
        var currentFlipIndex by remember { mutableIntStateOf(0) }
        val totalFlips = 6

        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(book, retryTrigger) {
            downloadState = DownloadState.Downloading
            
            coroutineScope.launch {
                liftProgress.animateTo(1f, tween(800, easing = PremiumEasing))
            }
            launch { bgAlpha.animateTo(1f, tween(600, easing = PremiumEasing)) }
            launch {
                delay(200)
                dogEarAlpha.animateTo(1f, tween(400, easing = PremiumEasing))
            }

            val file = withContext(Dispatchers.IO) { FileDownloader.ensureCached(context, book) }
            
            if (file != null) {
                downloadState = DownloadState.Ready
                delay(200)

                repeat(totalFlips) { index ->
                    currentFlipIndex = index + 1
                    flipRotation.snapTo(0f)
                    
                    val isLastFlip = index == totalFlips - 1
                    
                    if (isLastFlip) {
                        launch {
                            delay(400)
                            immersionZoom.animateTo(1f, tween(1600, easing = PremiumEasing))
                        }
                    }

                    val baseDuration = 500 - (index * 70)
                    val duration = if (isLastFlip) 1200 else max(150, baseDuration)
                    
                    flipRotation.animateTo(
                        targetValue = -180f,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = if (isLastFlip) PremiumEasing else LinearEasing
                        )
                    )
                }
                
                delay(200)
                onAnimationFinished(file)
            } else {
                downloadState = DownloadState.Error
            }
        }

        val lp = liftProgress.value
        val iz = immersionZoom.value
        
        val fillScale = max(sW / centerW, sH / centerH) * 1.25f 
        val masterScale = 1f + (fillScale - 1f) * iz

        val currentW = lerp(startW, centerW, lp)
        val currentH = lerp(startH, centerH, lp)
        val currentX = lerp(startX, centerX, lp)
        val currentY = lerp(startY, centerY, lp)

        val isError = downloadState == DownloadState.Error
        val errorOverlayAlpha by animateFloatAsState(
            targetValue = if (isError) 0.9f else 0f,
            animationSpec = tween(300),
            label = "errorAlpha"
        )
        val grayscaleAlpha by animateFloatAsState(
            targetValue = if (isError) 0.7f else 0f,
            animationSpec = tween(300),
            label = "grayscale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dimColor.copy(alpha = 0.99f * bgAlpha.value))
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                    .size(with(density) { currentW.toDp() }, with(density) { currentH.toDp() })
                    .graphicsLayer {
                        scaleX = masterScale
                        scaleY = masterScale
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
            ) {
                if (currentFlipIndex > 1 || flipRotation.value < -90f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationX = -currentW }
                            .background(themeColor)
                            .border(1.dp, pageBorderColor)
                    ) {
                        Box(modifier = Modifier.fillMaxHeight().width(30.dp).align(Alignment.CenterEnd).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(0.12f)))))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeColor)
                        .border(1.dp, pageBorderColor)
                ) {
                    Box(modifier = Modifier.fillMaxHeight().width(40.dp).align(Alignment.CenterStart).background(Brush.horizontalGradient(0.0f to Color.Black.copy(0.2f), 0.1f to Color.Black.copy(0.08f), 1.0f to Color.Transparent)))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = flipRotation.value
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            cameraDistance = 25f * density.density
                            if (flipRotation.value < 0f) {
                                rotationX = (flipRotation.value / -180f) * 3f
                                shadowElevation = 6f
                            }
                        }
                ) {
                    if (flipRotation.value > -90f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(lerp(8f, 0f, iz).dp))
                                .background(if (currentFlipIndex <= 1) Color(0xFF1A1A1A) else themeColor)
                                .graphicsLayer { alpha = 1f - grayscaleAlpha }
                                .drawWithContent {
                                    drawContent()
                                    if (currentFlipIndex <= 1 && dogEarAlpha.value > 0f) {
                                        val s = 48.dp.toPx() * dogEarAlpha.value
                                        val path = Path().apply {
                                            moveTo(size.width, 0f); lineTo(size.width - s, 0f); lineTo(size.width, s); close()
                                        }
                                        drawPath(path, Color.White.copy(0.95f))
                                        drawPath(Path().apply {
                                            moveTo(size.width - s, 0f); lineTo(size.width, s); lineTo(size.width - s, s); close()
                                        }, Color.Black.copy(0.3f))
                                    }
                                }
                        ) {
                            if (currentFlipIndex <= 1) {
                                AsyncImage(
                                    model = book.coverImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            if (downloadState == DownloadState.Downloading && liftProgress.value > 0.5f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.3f)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                        
                        if (isError) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1A1A1A).copy(alpha = errorOverlayAlpha))
                                    .clickable { retryTrigger++ },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Couldn't load book",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = { retryTrigger++ },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFE50914))
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Retry", fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f }
                                .background(themeColor)
                                .border(1.dp, pageBorderColor)
                        ) {
                            Box(modifier = Modifier.fillMaxHeight().width(40.dp).align(Alignment.CenterEnd).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(0.1f)))))
                        }
                    }
                }
            }
        }
    }
}

private enum class DownloadState {
    Downloading,
    Ready,
    Error
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
