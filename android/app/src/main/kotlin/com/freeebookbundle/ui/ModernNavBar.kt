package com.freeebookbundle.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class NavTab(val title: String) {
    HOME("HOME"),
    SEARCH("SEARCH"),
    LIBRARY("LIBRARY")
}

@Composable
fun ModernNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.entries.forEach { tab ->
                ModernNavItem(
                    tab = tab,
                    isSelected = selectedTab == tab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun ModernNavItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val transitionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "TabTransition"
    )

    // Interpolate color manually for better performance if needed, 
    // but animateColorAsState is usually well-optimized.
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.6f),
        animationSpec = tween(350),
        label = "TextColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(110.dp) // Fixed width prevents font-weight changes from jittering the Row
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = tab.title,
            fontSize = 16.sp,
            // We use a slight jump in weight, but since the container is fixed-width,
            // it won't push other elements around.
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Light,
            color = textColor,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Performance Optimization: Using graphicsLayer for the "Pea" expansion.
        // Instead of animating 'width' (Layout Pass), we animate 'scaleX' (GPU Pass).
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .graphicsLayer {
                    // 0.2f is the "dot" size, 1.0f is the "pea" size
                    scaleX = 0.2f + (transitionProgress * 0.8f)
                    alpha = 0.3f + (transitionProgress * 0.7f)
                }
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFFE50914) else Color.Gray)
        )
    }
}
