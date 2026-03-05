package com.example.ebook_new_app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
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
import com.example.ebook_new_app.PremiumEasing
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.utils.FileDownloader
import com.example.ebook_new_app.viewmodel.InitialBounds
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

        LaunchedEffect(book) {
            // PHASE 1: Lift & Center (Premium Pacing)
            launch { bgAlpha.animateTo(1f, tween(600, easing = PremiumEasing)) }
            launch {
                delay(200)
                dogEarAlpha.animateTo(1f, tween(400, easing = PremiumEasing))
            }
            liftProgress.animateTo(1f, tween(800, easing = PremiumEasing))

            val file = withContext(Dispatchers.IO) { FileDownloader.ensureCached(context, book) }
            if (file != null) {
                delay(200)

                // PHASE 3: Accelerated Atmospheric Shuffle
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

                    // Physics-based acceleration: 500ms down to 150ms
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
                onDismiss()
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
                                .then(if (currentFlipIndex > 1) Modifier.border(1.dp, pageBorderColor) else Modifier)
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

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
