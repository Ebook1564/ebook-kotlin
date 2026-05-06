package com.freeebookbundle.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.freeebookbundle.PremiumEasing

data class TutorialStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun ReaderTutorialOverlay(
    onFinish: () -> Unit
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    
    val steps = listOf(
        TutorialStep(
            title = "Welcome to the Reader",
            description = "Experience your books with a minimalist, immersive interface designed for deep reading.",
            icon = Icons.Default.MenuBook,
            accentColor = Color(0xFFE50914)
        ),
        TutorialStep(
            title = "Capture Your Thoughts",
            description = "Long-press any text to highlight it. Choose a color that fits your mood.",
            icon = Icons.Default.AutoAwesome,
            accentColor = Color(0xFFFFF176)
        ),
        TutorialStep(
            title = "Powerful Actions",
            description = "Once selected, you can copy text, listen via Text-to-Speech, or look up definitions instantly.",
            icon = Icons.Default.VolumeUp,
            accentColor = Color(0xFF81D4FA)
        ),
        TutorialStep(
            title = "Stay Organized",
            description = "Use the top bar to access the Table of Contents or save your progress with Bookmarks.",
            icon = Icons.Default.Bookmark,
            accentColor = Color(0xFFAED581)
        ),
        TutorialStep(
            title = "Your Personal Style",
            description = "Adjust font sizes, line heights, and switch between Light, Dark, or Sepia themes in Settings.",
            icon = Icons.Default.Settings,
            accentColor = Color(0xFFF06292)
        )
    )

    val currentStep = steps[currentStepIndex]
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) { } // Intercept taps
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        // Animated background glow
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "glowAlpha"
        )
        
        Box(
            modifier = Modifier
                .size(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(currentStep.accentColor.copy(alpha = glowAlpha), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(500, easing = PremiumEasing)) + scaleIn(initialScale = 0.9f)).togetherWith(
                        fadeOut(animationSpec = tween(400, easing = PremiumEasing)) + scaleOut(targetScale = 1.1f)
                    )
                },
                label = "stepContent"
            ) { step ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = step.accentColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, step.accentColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = step.accentColor,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = step.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = step.description,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentStepIndex) 20.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (index == currentStepIndex) currentStep.accentColor else Color.White.copy(alpha = 0.2f))
                            .animateContentSize()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = {
                    if (currentStepIndex < steps.size - 1) {
                        currentStepIndex++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(16.dp, RoundedCornerShape(30.dp), spotColor = currentStep.accentColor),
                colors = ButtonDefaults.buttonColors(containerColor = currentStep.accentColor),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = if (currentStepIndex < steps.size - 1) "CONTINUE" else "START READING",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = if (currentStep.accentColor == Color(0xFFFFF176)) Color.Black else Color.White
                )
            }
            
            if (currentStepIndex < steps.size - 1) {
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = "Skip Tutorial",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
