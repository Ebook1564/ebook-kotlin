package com.example.ebook_new_app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ebook_new_app.viewmodel.AppThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.animateScrollBy

@Composable
fun OnboardingScreen(
    onFinish: (String, AppThemeMode) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf(AppThemeMode.NIGHT) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "logoScale"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AutoScrollingBackground()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(0.6f), Color.Black.copy(0.9f), Color.Black)
                    )
                )
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false 
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when(page) {
                    0 -> WelcomeStep(logoScale)
                    1 -> FeaturesStep()
                    2 -> ProfileStep(
                        name = name,
                        onNameChange = { name = it },
                        selectedTheme = selectedTheme,
                        onThemeChange = { selectedTheme = it }
                    )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(24.dp, RoundedCornerShape(30.dp), spotColor = Color(0xFFE50914)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(30.dp),
                    enabled = if (pagerState.currentPage == 2) name.isNotEmpty() else true
                ) {
                    Text(
                        if (pagerState.currentPage < 2) "CONTINUE" else "GET STARTED",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
        
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { i ->
                val color = if (pagerState.currentPage == i) Color(0xFFE50914) else Color.White.copy(0.3f)
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
fun WelcomeStep(logoScale: Float) {
    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(logoScale)
            .shadow(40.dp, CircleShape, spotColor = Color(0xFFE50914))
            .background(Color(0xFFE50914), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("V", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black)
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        "BOOKVERSE",
        color = Color.White,
        fontSize = 40.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 6.sp
    )
    Text(
        "The World's Most Powerful Library",
        color = Color.White.copy(0.6f),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
fun FeaturesStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "UNLIMITED CONTENT",
            color = Color(0xFFE50914),
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "60,000+ Classic Books\n60+ Curated Genres",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )
        Spacer(Modifier.height(32.dp))
        
        FeatureRow("🔊", "AI-Powered Text-to-Speech")
        FeatureRow("📖", "High-Fidelity 3D Reader")
        FeatureRow("🏷️", "Curated Smart Collections")
        FeatureRow("☁️", "Real-time Metadata Enrichment")
    }
}

@Composable
fun FeatureRow(icon: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(0.8f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.width(16.dp))
        Text(text = color(text = text, color = Color.White.copy(0.8f)), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun color(text: String, color: Color) = text

@Composable
fun ProfileStep(
    name: String,
    onNameChange: (String) -> Unit,
    selectedTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit
) {
    Text(
        "CUSTOMIZE YOUR EXPERIENCE",
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(48.dp))

    TextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text("What should we call you?", color = Color.White.copy(0.3f)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(0.05f),
            unfocusedContainerColor = Color.White.copy(0.05f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(32.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ThemeCard(label = "Night", icon = Icons.Default.Nightlight, isSelected = selectedTheme == AppThemeMode.NIGHT, onClick = { onThemeChange(AppThemeMode.NIGHT) }, modifier = Modifier.weight(1f))
        ThemeCard(label = "Day", icon = Icons.Default.LightMode, isSelected = selectedTheme == AppThemeMode.DAY, onClick = { onThemeChange(AppThemeMode.DAY) }, modifier = Modifier.weight(1f))
    }
}

@Composable
fun AutoScrollingBackground() {
    val scrollState = rememberLazyGridState()
    LaunchedEffect(Unit) {
        while (true) {
            scrollState.animateScrollBy(50f, tween(1000, easing = LinearEasing))
            delay(16)
        }
    }

    LazyVerticalGrid(
        state = scrollState,
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().alpha(0.25f).blur(6.dp),
        userScrollEnabled = false,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(100) { index ->
            val id = (index % 50) + 1
            Box(modifier = Modifier.aspectRatio(0.7f).clip(RoundedCornerShape(4.dp)).background(Color.DarkGray)) {
                AsyncImage(model = "http://192.168.1.23:8080/${id}.jpg", contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun ThemeCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color.White.copy(0.1f) else Color.Transparent)
            .border(1.dp, if (isSelected) Color(0xFFE50914) else Color.White.copy(0.1f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (isSelected) Color(0xFFE50914) else Color.White.copy(0.4f))
            Spacer(Modifier.height(8.dp))
            Text(label, color = if (isSelected) Color.White else Color.White.copy(0.4f), fontWeight = FontWeight.Bold)
        }
    }
}
