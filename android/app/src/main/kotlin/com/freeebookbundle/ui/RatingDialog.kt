package com.freeebookbundle.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.freeebookbundle.utils.RemoteConfigManager
import com.freeebookbundle.viewmodel.AppThemeMode

@Composable
fun RatingDialog(
    themeMode: AppThemeMode,
    isExitContext: Boolean = false,
    message: String = if (isExitContext) RemoteConfigManager.getExitDialogMessage() else RemoteConfigManager.getDialogMessage(),
    onRating: (Int) -> Unit,
    onDismiss: () -> Unit,
    onExit: (() -> Unit)? = null
) {
    var selectedRating by remember { mutableIntStateOf(0) }

    val colors = getRatingDialogColors(themeMode)

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            color = colors.background,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enjoying E-Book Bundle?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in 1..5) {
                        RatingStar(
                            star = i,
                            isSelected = i <= selectedRating,
                            activeColor = colors.starActive,
                            inactiveColor = colors.starInactive,
                            onStarClick = { selectedRating = i }
                        )
                        if (i < 5) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (selectedRating > 0) {
                    Button(
                        onClick = { onRating(selectedRating) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (selectedRating >= 4) "Submit Review" else "Send Feedback",
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isExitContext && onExit != null) {
                    Button(
                        onClick = { onExit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Exit Now",
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                TextButton(
                    onClick = if (isExitContext && onExit != null) onDismiss else onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedRating > 0) "Cancel" else "Remind Me Later",
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingStar(
    star: Int,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onStarClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "star_scale"
    )

    val tint by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "star_color"
    )

    Icon(
        imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.StarOutline,
        contentDescription = "Star $star",
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onStarClick
            ),
        tint = tint
    )
}

private data class RatingDialogColors(
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primary: Color,
    val starActive: Color,
    val starInactive: Color
)

@Composable
private fun getRatingDialogColors(themeMode: AppThemeMode): RatingDialogColors {
    return when (themeMode) {
        AppThemeMode.DAY -> RatingDialogColors(
            background = Color(0xFFFFF8F5),
            textPrimary = Color(0xFF3D2914),
            textSecondary = Color(0xFF8D7A66),
            primary = Color(0xFF8B7355),
            starActive = Color(0xFF8B7355),
            starInactive = Color(0xFFCCC0B8)
        )
        AppThemeMode.NIGHT -> RatingDialogColors(
            background = Color(0xFF2A2A2A),
            textPrimary = Color.White,
            textSecondary = Color(0xFFB0B0B0),
            primary = Color(0xFFFFD700),
            starActive = Color(0xFFFFD700),
            starInactive = Color(0xFF555555)
        )
        AppThemeMode.SPACE -> RatingDialogColors(
            background = Color(0xFF1A1A2E),
            textPrimary = Color.White,
            textSecondary = Color(0xFFB0B0C0),
            primary = Color(0xFF6BB3E0),
            starActive = Color(0xFF6BB3E0),
            starInactive = Color(0xFF3D3D5C)
        )
    }
}