package com.example.ebook_new_app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(userName: String, onBackClick: () -> Unit = {}) {
    val colors = BookVerseTheme.colors
    var activeSubScreen by remember { mutableStateOf<String?>(null) }

    if (activeSubScreen != null) {
        BackHandler { activeSubScreen = null }
    }

    val initials = if (userName.isNotEmpty()) {
        val parts = userName.split(" ")
        if (parts.size >= 2) "${parts[0].firstOrNull() ?: ""}${parts.last().firstOrNull() ?: ""}".uppercase()
        else userName.take(2).uppercase()
    } else "GU"

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(activeSubScreen ?: "PROFILE", fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = { if (activeSubScreen != null) activeSubScreen = null else onBackClick() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background, titleContentColor = colors.textPrimary)
                )
            },
            containerColor = colors.background
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = colors.primary.copy(0.1f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(initials, color = colors.primary, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(userName, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, fontWeight = FontWeight.Black)
                    Text("Bibliophile Level 1", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    ProfileOption("Reading Statistics", Icons.Default.BarChart) { activeSubScreen = "STATISTICS" }
                    ProfileOption("Settings", Icons.Default.Settings) { activeSubScreen = "SETTINGS" }
                    ProfileOption("Help & Support", Icons.AutoMirrored.Filled.HelpOutline) { activeSubScreen = "SUPPORT" }
                }

                // SUB-SCREEN CONTENT OVERLAY
                AnimatedVisibility(
                    visible = activeSubScreen != null,
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { it } + fadeOut()
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            when(activeSubScreen) {
                                "STATISTICS" -> StatisticsContent()
                                "SETTINGS" -> SettingsContent()
                                "SUPPORT" -> SupportContent()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileOption(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val colors = BookVerseTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.textSecondary.copy(0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.ChevronRight, null, tint = colors.textSecondary.copy(0.5f))
        }
    }
}

@Composable
fun StatisticsContent() {
    val colors = BookVerseTheme.colors
    Text("YOUR JOURNEY", style = MaterialTheme.typography.labelMedium, color = colors.primary, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(24.dp))
    StatRow("Total Books Read", "12")
    StatRow("Reading Time", "48h 20m")
    StatRow("Avg. Speed", "240 wpm")
    Spacer(Modifier.height(32.dp))
    Text("You're in the top 5% of readers this month!", color = colors.textSecondary, fontSize = 14.sp)
}

@Composable
fun SettingsContent() {
    val colors = BookVerseTheme.colors
    Text("PREFERENCES", style = MaterialTheme.typography.labelMedium, color = colors.primary, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(24.dp))
    Text("Notifications", color = colors.textPrimary, fontWeight = FontWeight.Bold)
    Text("Enabled", color = colors.textSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(24.dp))
    Text("Download over Wi-Fi only", color = colors.textPrimary, fontWeight = FontWeight.Bold)
    Text("Disabled", color = colors.textSecondary, fontSize = 12.sp)
}

@Composable
fun SupportContent() {
    val colors = BookVerseTheme.colors
    Text("GET HELP", style = MaterialTheme.typography.labelMedium, color = colors.primary, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(24.dp))
    Text("Frequently Asked Questions", color = colors.textPrimary, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    Text("Contact Support", color = colors.textPrimary, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    Text("Privacy Policy", color = colors.textPrimary, fontWeight = FontWeight.Bold)
}

@Composable
fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = BookVerseTheme.colors.textPrimary)
        Text(value, color = BookVerseTheme.colors.primary, fontWeight = FontWeight.Black)
    }
}
