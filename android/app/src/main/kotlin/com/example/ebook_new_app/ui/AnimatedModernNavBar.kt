package com.example.ebook_new_app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun AnimatedModernNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    val theme = LocalBookTheme.current
    val density = LocalDensity.current
    val tabPositions = remember { mutableStateMapOf<Int, Dp>() }
    val tabWidths = remember { mutableStateMapOf<Int, Dp>() }

    val indicatorOffset by animateDpAsState(
        targetValue = tabPositions.getOrElse(selectedTab.ordinal) { 0.dp },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicatorOffset"
    )

    val indicatorWidth by animateDpAsState(
        targetValue = tabWidths.getOrElse(selectedTab.ordinal) { 0.dp },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicatorWidth"
    )

    Column {
        HorizontalDivider(thickness = 0.5.dp, color = theme.onBackground.copy(alpha = 0.1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.background)
                .padding(vertical = 8.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {

            BubbleIndicator(
                indicatorWidth = indicatorWidth,
                indicatorOffset = indicatorOffset
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEachIndexed { index, tab ->
                    ModernNavItem(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            with(density) {
                                tabPositions[index] =
                                    coordinates.positionInParent().x.toDp()
                                tabWidths[index] =
                                    coordinates.size.width.toDp()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleIndicator(
    indicatorWidth: Dp,
    indicatorOffset: Dp
) {
    val theme = LocalBookTheme.current
    Box(
        modifier = Modifier
            .offset(x = indicatorOffset)
            .width(indicatorWidth)
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.onBackground.copy(alpha = 0.1f))
            .zIndex(-1f)
    )
}

@Composable
private fun ModernNavItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val theme = LocalBookTheme.current

    val redLineWidth by animateDpAsState(
        targetValue = if (isSelected) textWidth * 0.5f else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "redLineWidth"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(110.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp)
    ) {

        Text(
            text = tab.title,
            fontSize = 14.sp,
            fontFamily = theme.fontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) theme.onBackground else theme.onBackground.copy(alpha = 0.6f),
            letterSpacing = 1.sp,
            modifier = Modifier.onGloballyPositioned {
                with(density) {
                    textWidth = it.size.width.toDp()
                }
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(redLineWidth)
                .height(2.dp)
                .background(theme.primary, shape = RoundedCornerShape(1.dp))
        )
    }
}