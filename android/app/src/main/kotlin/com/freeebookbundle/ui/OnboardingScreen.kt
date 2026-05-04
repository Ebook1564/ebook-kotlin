package com.freeebookbundle.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.freeebookbundle.R
import com.freeebookbundle.PremiumEasing
import com.freeebookbundle.utils.ServerConfig
import androidx.compose.ui.res.stringResource
import com.freeebookbundle.viewmodel.AppThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun OnboardingScreen(
    homeViewModel: com.freeebookbundle.viewmodel.HomeViewModel? = null,
    onFinish: (String, AppThemeMode) -> Unit,
    onSyncRequired: suspend () -> Boolean = { true }
) {
    var name by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf(AppThemeMode.NIGHT) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncComplete by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var selectedGenres by remember { mutableStateOf(setOf<String>()) }
    
    // Page state: 0=Welcome, 1=Features, 2=Genre, 3=Profile
    var currentPage by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val minGenres = com.freeebookbundle.utils.GenrePreferences.getMinGenresRequired()
    
    val bgColor = when (selectedTheme) {
        AppThemeMode.DAY -> Color(0xFFF5F1E0)
        AppThemeMode.NIGHT -> Color(0xFF0A0A0A)
        AppThemeMode.SPACE -> Color(0xFF050510)
    }
    val textColor = when (selectedTheme) {
        AppThemeMode.DAY -> Color(0xFF5B4636)
        AppThemeMode.NIGHT -> Color.White
        AppThemeMode.SPACE -> Color.White
    }
    val accentColor = when (selectedTheme) {
        AppThemeMode.DAY -> Color(0xFF5B4636)
        AppThemeMode.NIGHT -> Color(0xFFE50914)
        AppThemeMode.SPACE -> Color(0xFF4A90D9)
    }
    val secondaryBgColor = when (selectedTheme) {
        AppThemeMode.DAY -> Color(0xFFE8E0D0)
        AppThemeMode.NIGHT -> Color(0xFF1A1A1A)
        AppThemeMode.SPACE -> Color(0xFF0A0A20)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "onboarding")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "logoScale"
    )
    
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "starTwinkle"
    )
    
    val cosmicRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "cosmicRotation"
    )

    LaunchedEffect(currentPage, selectedGenres) {
        // Sync logic
        if (currentPage == 2 && !isSyncing && !syncComplete) {
            isSyncing = true
            try {
                val success = onSyncRequired()
                if (success) {
                    syncComplete = true
                } else {
                    syncError = "Failed to sync with server"
                }
            } catch (e: Exception) {
                syncError = e.message ?: "Sync failed"
            } finally {
                isSyncing = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor).animateContentSize()) {
        when (selectedTheme) {
            AppThemeMode.SPACE -> SpaceOnboardingBackground(cosmicRotation, starTwinkle)
            AppThemeMode.NIGHT -> NightOnboardingBackground()
            AppThemeMode.DAY -> DayOnboardingBackground()
        }

        Box(
            modifier = Modifier.fillMaxSize().background(
                when (selectedTheme) {
                    AppThemeMode.DAY -> Brush.verticalGradient(listOf(bgColor.copy(0.6f), bgColor.copy(0.9f), bgColor))
                    AppThemeMode.NIGHT -> Brush.verticalGradient(listOf(Color.Black.copy(0.3f), Color.Black.copy(0.7f), Color.Black.copy(0.95f)))
                    AppThemeMode.SPACE -> Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0A0A1A).copy(0.7f), Color(0xFF0A0A1A)))
                }
            )
        )

        // Back button (top left) - visible when not on first page
        if (currentPage > 0) {
            IconButton(
                onClick = { if (currentPage > 0) currentPage-- },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
        }

        // Page content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (currentPage > 0) 56.dp else 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when(currentPage) {
                    0 -> WelcomeStep(logoScale, textColor, selectedTheme)
                    1 -> FeaturesStep(textColor, accentColor, selectedTheme)
                    2 -> GenreOnboardingStep(
                        selectedGenres = selectedGenres,
                        onGenresChanged = { selectedGenres = it },
                        textColor = textColor,
                        accentColor = accentColor,
                        selectedTheme = selectedTheme,
                        onContinue = { currentPage++ },
                        minGenres = minGenres
                    )
                    3 -> ProfileStep(name, { name = it }, selectedTheme, { selectedTheme = it }, textColor, accentColor, bgColor)
                }
                
                Spacer(modifier = Modifier.height(64.dp))

                val buttonText = when (currentPage) {
                    0, 1 -> stringResource(R.string.onboarding_btn_continue)
                    2 -> "" // No button on genre page - handled internally
                    3 -> stringResource(R.string.onboarding_btn_get_started)
                    else -> stringResource(R.string.onboarding_btn_continue)
                }

                if (currentPage != 2) {
                    Button(
                        onClick = {
                            when (currentPage) {
                                0, 1 -> currentPage++
                                3 -> if (name.isNotEmpty()) onFinish(name, selectedTheme)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp).shadow(24.dp, RoundedCornerShape(30.dp), spotColor = accentColor),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(30.dp),
                        enabled = true
                    ) {
                        Text(buttonText, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        if (currentPage < 3 && currentPage != 2) {
                            Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                        }
                    }
                }
            }
        }
        
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { i ->
                val color = if (currentPage == i) accentColor else textColor.copy(0.3f)
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
private fun SpaceOnboardingBackground(rotation: Float, twinkle: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF050510), Color(0xFF0A0A1A))))
        
        val stars = listOf(
            Offset(size.width * 0.1f, size.height * 0.1f),
            Offset(size.width * 0.2f, size.height * 0.3f),
            Offset(size.width * 0.35f, size.height * 0.15f),
            Offset(size.width * 0.5f, size.height * 0.25f),
            Offset(size.width * 0.65f, size.height * 0.05f),
            Offset(size.width * 0.8f, size.height * 0.2f),
            Offset(size.width * 0.9f, size.height * 0.35f),
            Offset(size.width * 0.15f, size.height * 0.5f),
            Offset(size.width * 0.4f, size.height * 0.45f),
            Offset(size.width * 0.6f, size.height * 0.55f),
            Offset(size.width * 0.85f, size.height * 0.6f),
            Offset(size.width * 0.25f, size.height * 0.7f),
            Offset(size.width * 0.5f, size.height * 0.8f),
            Offset(size.width * 0.75f, size.height * 0.75f)
        )
        stars.forEachIndexed { index, star ->
            val pulse = if (index % 3 == 0) twinkle else 1f - (twinkle - 0.3f)
            drawCircle(Color.White.copy(alpha = pulse * 0.8f), radius = (2f + (index % 4)) * 1.5f, center = star)
        }
        
        drawCircle(Color(0xFF4A90D9).copy(alpha = 0.1f), radius = size.width * 0.4f, center = Offset(size.width * 0.8f, size.height * 0.2f))
        drawCircle(Color(0xFF6B5B95).copy(alpha = 0.08f), radius = size.width * 0.3f, center = Offset(size.width * 0.2f, size.height * 0.7f))
    }
}

@Composable
private fun NightOnboardingBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFF0A0A0A))
        for (i in 0 until 50) {
            val x = Random.nextFloat() * size.width
            val y = Random.nextFloat() * size.height
            drawCircle(Color.White.copy(alpha = Random.nextFloat() * 0.5f + 0.2f), radius = Random.nextFloat() * 1.5f + 0.5f, center = Offset(x, y))
        }
    }
}

@Composable
private fun DayOnboardingBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F1E0)))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().alpha(0.15f).blur(20.dp),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(40) { index ->
                Box(Modifier.aspectRatio(0.7f).clip(RoundedCornerShape(4.dp)).background(Color(0xFFD4C4A8)))
            }
        }
    }
}

@Composable
private fun WelcomeStep(scale: Float, textColor: Color, themeMode: AppThemeMode) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(120.dp * scale),
            contentAlignment = Alignment.Center
        ) {
            if (themeMode == AppThemeMode.SPACE) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    drawCircle(Color(0xFF4A90D9).copy(alpha = 0.2f), radius = size.minDimension / 2)
                    drawCircle(Color(0xFF4A90D9).copy(alpha = 0.3f), radius = size.minDimension / 2 * 0.7f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                    for (i in 0 until 3) {
                        val angle = (120f * i)
                        val x = center.x + (size.minDimension / 2 * 0.85f) * cos(Math.toRadians(angle.toDouble())).toFloat()
                        val y = center.y + (size.minDimension / 2 * 0.85f) * sin(Math.toRadians(angle.toDouble())).toFloat()
                        drawCircle(Color.White.copy(alpha = 0.7f), radius = 4.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = when (themeMode) {
                    AppThemeMode.SPACE -> Color(0xFF4A90D9).copy(alpha = 0.2f)
                    AppThemeMode.NIGHT -> textColor.copy(alpha = 0.1f)
                    AppThemeMode.DAY -> textColor.copy(alpha = 0.1f)
                },
                border = if (themeMode == AppThemeMode.SPACE) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A90D9).copy(alpha = 0.5f)) else null
            ) {
                AsyncImage(
                    model = R.mipmap.ic_launcher,
                    contentDescription = stringResource(R.string.cd_app_icon),
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.onboarding_title), color = textColor, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
        Text(stringResource(R.string.onboarding_tagline), color = textColor.copy(0.6f), fontSize = 14.sp)
    }
}

@Composable
private fun FeaturesStep(textColor: Color, accentColor: Color, themeMode: AppThemeMode) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.onboarding_features), color = textColor.copy(0.6f), fontSize = 12.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        FeatureRow("📚", stringResource(R.string.onboarding_feature_collections), textColor, themeMode)
        FeatureRow("✨", stringResource(R.string.onboarding_feature_recommendations), textColor, themeMode)
        FeatureRow("🚀", stringResource(R.string.onboarding_feature_statistics), textColor, themeMode)
        FeatureRow("🌙", stringResource(R.string.onboarding_feature_themes), textColor, themeMode)
    }
}

@Composable
private fun SyncStep(
    isSyncing: Boolean,
    syncComplete: Boolean,
    syncError: String?,
    textColor: Color,
    accentColor: Color,
    themeMode: AppThemeMode
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.onboarding_sync_title), color = textColor.copy(0.6f), fontSize = 12.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))
        
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            if (syncComplete) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(3.dp, accentColor)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.onboarding_sync_complete_desc),
                            tint = accentColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            } else if (isSyncing) {
                Surface(
                    modifier = Modifier.size(100.dp * pulseScale),
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.1f)
                ) {}
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = stringResource(R.string.onboarding_sync_desc),
                            tint = accentColor,
                            modifier = Modifier.size(40.dp).graphicsLayer { rotationZ = rotation }
                        )
                    }
                }
            } else if (syncError != null) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = Color.Red.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(3.dp, Color.Red)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = stringResource(R.string.onboarding_sync_error_desc),
                            tint = Color.Red,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = textColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = stringResource(R.string.onboarding_sync_start_desc),
                            tint = textColor.copy(0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        when {
            syncComplete -> {
                Text(stringResource(R.string.onboarding_sync_complete), color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.onboarding_sync_up_to_date), color = textColor.copy(0.6f), fontSize = 14.sp)
            }
            isSyncing -> {
                Text(stringResource(R.string.onboarding_sync_loading), color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.onboarding_sync_background), color = textColor.copy(0.6f), fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.onboarding_sync_continue), color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            syncError != null -> {
                Text(stringResource(R.string.onboarding_sync_failed), color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(syncError, color = textColor.copy(0.6f), fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            else -> {
                Text(stringResource(R.string.onboarding_sync_ready), color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.onboarding_sync_tap), color = textColor.copy(0.6f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: String, text: String, textColor: Color, themeMode: AppThemeMode) {
    Row(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(0.85f), verticalAlignment = Alignment.CenterVertically) {
        val bgColor = when (themeMode) {
            AppThemeMode.SPACE -> Color(0xFF4A90D9).copy(alpha = 0.1f)
            else -> textColor.copy(0.05f)
        }
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(bgColor), contentAlignment = Alignment.Center) {
            Text(icon, fontSize = 20.sp)
        }
        Spacer(Modifier.width(16.dp))
        Text(text, color = textColor.copy(0.9f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProfileStep(name: String, onNameChange: (String) -> Unit, selectedTheme: AppThemeMode, onThemeChange: (AppThemeMode) -> Unit, textColor: Color, accentColor: Color, bgColor: Color) {
    Text(stringResource(R.string.onboarding_customize_title), color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, letterSpacing = 2.sp)
    Spacer(Modifier.height(40.dp))
    
    val inputBgColor = when (selectedTheme) {
        AppThemeMode.SPACE -> Color.White.copy(alpha = 0.05f)
        AppThemeMode.NIGHT -> Color.White.copy(alpha = 0.05f)
        AppThemeMode.DAY -> Color.White.copy(alpha = 0.5f)
    }

    TextField(
        value = name, onValueChange = onNameChange,
        placeholder = { Text(stringResource(R.string.onboarding_name_placeholder), color = textColor.copy(0.3f)) },
        modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, textColor.copy(0.1f), RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = inputBgColor, unfocusedContainerColor = inputBgColor,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = textColor, unfocusedTextColor = textColor
        ),
        singleLine = true
    )
    Spacer(Modifier.height(32.dp))
    
    Text(stringResource(R.string.onboarding_theme_label), color = textColor.copy(0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(16.dp))
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OnboardingThemeCard(
            label = stringResource(R.string.onboarding_theme_day),
            icon = Icons.Default.LightMode,
            isSelected = selectedTheme == AppThemeMode.DAY,
            onClick = { onThemeChange(AppThemeMode.DAY) },
            modifier = Modifier.weight(1f),
            textColor = textColor,
            accentColor = accentColor
        )
        OnboardingThemeCard(
            label = stringResource(R.string.onboarding_theme_night),
            icon = Icons.Default.Nightlight,
            isSelected = selectedTheme == AppThemeMode.NIGHT,
            onClick = { onThemeChange(AppThemeMode.NIGHT) },
            modifier = Modifier.weight(1f),
            textColor = textColor,
            accentColor = accentColor
        )
        OnboardingThemeCard(
            label = stringResource(R.string.onboarding_theme_space),
            icon = Icons.Filled.Star,
            isSelected = selectedTheme == AppThemeMode.SPACE,
            onClick = { onThemeChange(AppThemeMode.SPACE) },
            modifier = Modifier.weight(1f),
            textColor = textColor,
            accentColor = accentColor
        )
    }
}

@Composable
private fun OnboardingThemeCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier, textColor: Color, accentColor: Color) {
    val bgColor = when {
        isSelected -> accentColor.copy(alpha = 0.15f)
        else -> textColor.copy(alpha = 0.05f)
    }
    val borderColor = if (isSelected) accentColor else textColor.copy(alpha = 0.1f)
    
    Box(
        modifier = modifier.height(90.dp).clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (isSelected) accentColor else textColor.copy(0.4f), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = if (isSelected) accentColor else textColor.copy(0.4f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GenreOnboardingStep(
    selectedGenres: Set<String>,
    onGenresChanged: (Set<String>) -> Unit,
    textColor: Color,
    accentColor: Color,
    selectedTheme: AppThemeMode,
    onContinue: () -> Unit,
    minGenres: Int
) {
    var availableGenres by remember { mutableStateOf(listOf<Pair<String, String>>()) } // code, name
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    val genreColors = mapOf(
        "novels" to Color(0xFF6366F1),
        "adventure" to Color(0xFF10B981),
        "romance" to Color(0xFFEC4899),
        "children___young_adult_reading" to Color(0xFFF59E0B),
        "historical_novels" to Color(0xFF8B5CF6),
        "short_stories" to Color(0xFF06B6D4),
        "poetry" to Color(0xFFEF4444),
        "science_fiction___fantasy" to Color(0xFF3B82F6),
        "humour" to Color(0xFF84CC16),
        "biographies" to Color(0xFF14B8A6)
    )
    
    LaunchedEffect(Unit) {
        availableGenres = listOf(
            "novels" to "Fiction",
            "adventure" to "Adventure", 
            "romance" to "Romance",
            "children___young_adult_reading" to "Children & Young Adult",
            "historical_novels" to "Historical Novels",
            "short_stories" to "Short Stories",
            "poetry" to "Poetry",
            "science_fiction___fantasy" to "Sci-Fi & Fantasy",
            "humour" to "Humour",
            "biographies" to "Biographies"
        )
        isLoading = false
    }
    
    val canProceed = selectedGenres.size >= minGenres
    
    val infiniteTransition = rememberInfiniteTransition(label = "genre_orb")
    val orbRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )
    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_genre_title),
            color = textColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.onboarding_genre_subtitle),
            color = textColor.copy(alpha = 0.5f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        if (isLoading) {
            CircularProgressIndicator(color = accentColor)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(availableGenres.size) { index ->
                    val (code, name) = availableGenres[index]
                    val isSelected = selectedGenres.contains(code)
                    val genreColor = genreColors[code] ?: accentColor
                    
                    GenreOrbChip(
                        name = name,
                        isSelected = isSelected,
                        genreColor = genreColor,
                        orbRotation = orbRotation,
                        orbPulse = orbPulse,
                        onClick = {
                            onGenresChanged(
                                if (isSelected) selectedGenres - code else selectedGenres + code
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Premium selection indicator
            SelectionCounter(
                selected = selectedGenres.size,
                required = minGenres,
                canProceed = canProceed,
                accentColor = accentColor,
                textColor = textColor
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Single continue button at bottom
            Button(
                onClick = {
                    com.freeebookbundle.utils.GenrePreferences.setSelectedGenres(selectedGenres)
                    onContinue()
                },
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = if (canProceed) 16.dp else 0.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = accentColor.copy(alpha = 0.5f)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = accentColor.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_btn_continue),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (canProceed) Color.White else Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (canProceed) Color.White else Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun GenreOrbChip(
    name: String,
    isSelected: Boolean,
    genreColor: Color,
    orbRotation: Float,
    orbPulse: Float,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(200, easing = PremiumEasing)
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            
            // Full-width bar background when selected
            if (isSelected) {
                drawRoundRect(
                    color = genreColor.copy(alpha = 0.15f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())
                )
            }
            
            // Full-width subtle background
            drawRoundRect(
                color = if (isSelected) genreColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())
            )
            
            // Subtle border on right side
            drawRoundRect(
                color = if (isSelected) genreColor.copy(alpha = 0.5f) else Color.Transparent,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
            
        Text(
            text = name,
            color = if (isSelected) genreColor else Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(genreColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionCounter(
    selected: Int,
    required: Int,
    canProceed: Boolean,
    accentColor: Color,
    textColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "counter_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Row(
        modifier = Modifier
            .graphicsLayer {
                alpha = if (canProceed) 1f else 0.6f
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (canProceed) accentColor.copy(alpha = 0.15f) 
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (canProceed) accentColor.copy(alpha = glowAlpha) 
                else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (canProceed) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        
        Text(
            text = if (canProceed) "You're all set!" else "$selected / $required genres selected",
            color = if (canProceed) accentColor else textColor.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = if (canProceed) FontWeight.Bold else FontWeight.Medium
        )
        
        if (!canProceed) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "minimum",
                color = textColor.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
    }
}
