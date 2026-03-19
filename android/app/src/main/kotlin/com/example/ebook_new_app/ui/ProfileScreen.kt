package com.example.ebook_new_app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ebook_new_app.viewmodel.AppThemeMode
import com.example.ebook_new_app.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(userName: String, appViewModel: AppViewModel = viewModel(), onBackClick: () -> Unit = {}) {
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
                    title = { Text(activeSubScreen ?: "PROFILE", fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 20.sp, color = textColor) },
                    navigationIcon = {
                        IconButton(onClick = { if (activeSubScreen != null) activeSubScreen = null else onBackClick() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { appViewModel.toggleTheme() },
                            modifier = Modifier.clip(CircleShape).background(when (themeMode) {
                                AppThemeMode.DAY -> Color(0xFF5B4636).copy(alpha = 0.1f)
                                AppThemeMode.NIGHT -> Color.White.copy(alpha = 0.1f)
                                AppThemeMode.SPACE -> Color.White.copy(alpha = 0.1f)
                            })
                        ) {
                            Icon(
                                imageVector = when (themeMode) {
                                    AppThemeMode.DAY -> Icons.Default.Nightlight
                                    AppThemeMode.NIGHT -> Icons.Filled.Star
                                    AppThemeMode.SPACE -> Icons.Default.LightMode
                                },
                                contentDescription = "Toggle Theme",
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
                    Text("Bibliophile Level 1", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    ProfileOption("Reading Statistics", Icons.Default.BarChart, themeMode = themeMode, accentColor = accentColor, surfaceColor = surfaceColor, textColor = textColor) { activeSubScreen = "STATISTICS" }
                    ProfileOption("Settings", Icons.Default.Settings, themeMode = themeMode, accentColor = accentColor, surfaceColor = surfaceColor, textColor = textColor) { activeSubScreen = "SETTINGS" }
                    ProfileOption("Help & Support", Icons.AutoMirrored.Filled.HelpOutline, themeMode = themeMode, accentColor = accentColor, surfaceColor = surfaceColor, textColor = textColor) { activeSubScreen = "SUPPORT" }
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
                                "STATISTICS" -> StatisticsContent(themeMode = themeMode)
                                "SETTINGS" -> SettingsContent(appViewModel)
                                "SUPPORT" -> SupportContent(themeMode = themeMode)
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
fun StatisticsContent(themeMode: AppThemeMode = AppThemeMode.NIGHT) {
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
    
    Column {
        Text("YOUR JOURNEY", style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(24.dp))
        StatRow("Total Books Read", "12", textColor = textColor, valueColor = labelColor)
        StatRow("Reading Time", "48h 20m", textColor = textColor, valueColor = labelColor)
        StatRow("Avg. Speed", "240 wpm", textColor = textColor, valueColor = labelColor)
        Spacer(Modifier.height(32.dp))
        Text("You're in the top 5% of readers this month!", color = subTextColor, fontSize = 14.sp)
    }
}

@Composable
fun SupportContent(themeMode: AppThemeMode = AppThemeMode.NIGHT) {
    val colors = BookVerseTheme.colors
    val labelColor = when (themeMode) {
        AppThemeMode.SPACE -> Color(0xFF4A90D9)
        else -> colors.primary
    }
    val textColor = when (themeMode) {
        AppThemeMode.SPACE -> Color.White
        else -> colors.textPrimary
    }
    
    Column {
        Text("GET HELP", style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(24.dp))
        Text("Frequently Asked Questions", color = textColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Contact Support", color = textColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Privacy Policy", color = textColor, fontWeight = FontWeight.Bold)
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
        Text("APPEARANCE", style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        
        Text("Theme", color = textColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeButton(
                label = "DAY",
                icon = Icons.Default.LightMode,
                isSelected = currentTheme == AppThemeMode.DAY,
                onClick = { appViewModel.setTheme(AppThemeMode.DAY) },
                modifier = Modifier.weight(1f)
            )
            ThemeButton(
                label = "NIGHT",
                icon = Icons.Default.Nightlight,
                isSelected = currentTheme == AppThemeMode.NIGHT,
                onClick = { appViewModel.setTheme(AppThemeMode.NIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeButton(
                label = "SPACE",
                icon = Icons.Filled.Star,
                isSelected = currentTheme == AppThemeMode.SPACE,
                onClick = { appViewModel.setTheme(AppThemeMode.SPACE) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        if (currentTheme == AppThemeMode.SPACE) {
            Text("Navigation Style", color = textColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavStyleButton(
                    label = "Touch Pad",
                    icon = Icons.Default.TouchApp,
                    isSelected = useSpaceTouchNav,
                    onClick = { appViewModel.setUseSpaceTouchNav(true) },
                    modifier = Modifier.weight(1f)
                )
                NavStyleButton(
                    label = "Classic",
                    icon = Icons.Default.Menu,
                    isSelected = !useSpaceTouchNav,
                    onClick = { appViewModel.setUseSpaceTouchNav(false) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (useSpaceTouchNav) "Floating touch pad navigation" else "Traditional bottom navigation bar",
                color = subTextColor,
                fontSize = 11.sp
            )
            
            Spacer(Modifier.height(24.dp))
        }
        
        HorizontalDivider(color = subTextColor.copy(alpha = 0.1f))
        Spacer(Modifier.height(24.dp))
        
        Text("NOTIFICATIONS", style = MaterialTheme.typography.labelMedium, color = labelColor, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Text("Push Notifications", color = textColor, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enabled", color = subTextColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(checked = true, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedTrackColor = labelColor))
        }
        Spacer(Modifier.height(16.dp))
        Text("Download over Wi-Fi only", color = textColor, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Disabled", color = subTextColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(checked = false, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedTrackColor = labelColor))
        }
    }
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
