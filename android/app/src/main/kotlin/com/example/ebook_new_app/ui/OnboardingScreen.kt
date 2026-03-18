package com.example.ebook_new_app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ebook_new_app.utils.ServerConfig
import com.example.ebook_new_app.viewmodel.AppThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinish: (String, AppThemeMode) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf(AppThemeMode.DAY) } // DEFAULT TO LIGHT
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    // Immediate Visual Feedback Colors
    val bgColor = if (selectedTheme == AppThemeMode.DAY) Color(0xFFF5F1E0) else Color.Black
    val textColor = if (selectedTheme == AppThemeMode.DAY) Color(0xFF5B4636) else Color.White
    val accentColor = Color(0xFFE50914)

    val infiniteTransition = rememberInfiniteTransition(label = "onboarding")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "logoScale"
    )

    Box(modifier = Modifier.fillMaxSize().background(bgColor).animateContentSize()) {
        AutoScrollingBackground()

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    if (selectedTheme == AppThemeMode.DAY)
                        listOf(bgColor.copy(0.6f), bgColor.copy(0.9f), bgColor)
                    else
                        listOf(Color.Black.copy(0.5f), Color.Black.copy(0.85f), Color.Black)
                )
            )
        )

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), userScrollEnabled = false) { page ->
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when(page) {
                    0 -> WelcomeStep(logoScale, textColor)
                    1 -> FeaturesStep(textColor, accentColor)
                    2 -> ProfileStep(name, { name = it }, selectedTheme, { selectedTheme = it }, textColor, accentColor)
                }
                
                Spacer(modifier = Modifier.height(64.dp))

                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else if (name.isNotEmpty()) {
                            onFinish(name, selectedTheme)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp).shadow(24.dp, RoundedCornerShape(30.dp), spotColor = accentColor),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(30.dp),
                    enabled = if (pagerState.currentPage == 2) name.isNotEmpty() else true
                ) {
                    Text(if (pagerState.currentPage < 2) "CONTINUE" else "GET STARTED", fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                }
            }
        }
        
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                val color = if (pagerState.currentPage == i) accentColor else textColor.copy(0.3f)
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
fun WelcomeStep(logoScale: Float, textColor: Color) {
    Box(
        modifier = Modifier.size(140.dp).scale(logoScale).shadow(40.dp, CircleShape, spotColor = Color(0xFFE50914)).background(Color(0xFFE50914), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("V", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black)
    }
    Spacer(Modifier.height(32.dp))
    Text("EBOOK BUNDLE", color = textColor, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
    Text("The World's Most Powerful Library", color = textColor.copy(0.6f), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

@Composable
fun FeaturesStep(textColor: Color, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("UNLIMITED CONTENT", color = accentColor, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Text("60,000+ Classic Books\n60+ Curated Genres", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 36.sp)
        Spacer(Modifier.height(32.dp))
        
        FeatureRow("🔊", "AI-Powered Text-to-Speech", textColor)
        FeatureRow("📖", "High-Fidelity 3D Reader", textColor)
        FeatureRow("🏷️", "Curated Smart Collections", textColor)
        FeatureRow("☁️", "Real-time Metadata Sync", textColor)
    }
}

@Composable
fun FeatureRow(icon: String, text: String, textColor: Color) {
    Row(modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(0.85f), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(textColor.copy(0.05f), CircleShape), contentAlignment = Alignment.Center) {
            Text(icon, fontSize = 20.sp)
        }
        Spacer(Modifier.width(16.dp))
        Text(text, color = textColor.copy(0.8f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileStep(name: String, onNameChange: (String) -> Unit, selectedTheme: AppThemeMode, onThemeChange: (AppThemeMode) -> Unit, textColor: Color, accentColor: Color) {
    Text("CUSTOMIZE YOUR EXPERIENCE", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    Spacer(Modifier.height(48.dp))
    TextField(
        value = name, onValueChange = onNameChange,
        placeholder = { Text("What should we call you?", color = textColor.copy(0.3f)) },
        modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, textColor.copy(0.1f), RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = textColor.copy(0.05f), unfocusedContainerColor = textColor.copy(0.05f),
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = textColor, unfocusedTextColor = textColor
        ),
        singleLine = true
    )
    Spacer(Modifier.height(32.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ThemeCard(label = "Day", icon = Icons.Default.LightMode, isSelected = selectedTheme == AppThemeMode.DAY, onClick = { onThemeChange(AppThemeMode.DAY) }, modifier = Modifier.weight(1f), textColor, accentColor)
        ThemeCard(label = "Night", icon = Icons.Default.Nightlight, isSelected = selectedTheme == AppThemeMode.NIGHT, onClick = { onThemeChange(AppThemeMode.NIGHT) }, modifier = Modifier.weight(1f), textColor, accentColor)
    }
}

@Composable
fun AutoScrollingBackground() {
    val coversUrl = ServerConfig.getCoversUrl()
    val scrollState = rememberLazyGridState()
    LaunchedEffect(Unit) {
        while (true) {
            scrollState.animateScrollBy(50f, tween(1000, easing = LinearEasing))
            delay(16)
        }
    }
    LazyVerticalGrid(state = scrollState, columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize().alpha(0.7f).blur(8.dp), userScrollEnabled = false, contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(100) { index ->
            val id = (index % 50) + 1
            Box(Modifier.aspectRatio(0.7f).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(0.2f))) {
                AsyncImage(model = "$coversUrl/${id}_cover_image.jpg", contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun ThemeCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier, textColor: Color, accentColor: Color) {
    Box(
        modifier = modifier.height(100.dp).clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) textColor.copy(0.05f) else Color.Transparent)
            .border(1.dp, if (isSelected) accentColor else textColor.copy(0.1f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (isSelected) accentColor else textColor.copy(0.4f))
            Spacer(Modifier.height(8.dp))
            Text(label, color = if (isSelected) textColor else textColor.copy(0.4f), fontWeight = FontWeight.Bold)
        }
    }
}
