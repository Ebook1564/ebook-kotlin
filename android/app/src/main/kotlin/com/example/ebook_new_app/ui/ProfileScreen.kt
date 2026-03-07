package com.example.ebook_new_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBackClick: () -> Unit = {}) {
    val backgroundColor = BookVerseTheme.colors.background
    Box(modifier = Modifier.fillMaxSize()) {
        if (backgroundColor == Color.Transparent) {
            SpaceBackground()
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PROFILE", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = backgroundColor
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color(0xFF1A1A1A)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("JD", color = Color.White, style = MaterialTheme.typography.displayMedium)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("John Doe", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Premium Member", color = Color(0xFFE50914), fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(40.dp))
                
                ProfileOption("Reading Statistics")
                ProfileOption("Manage Subscription")
                ProfileOption("Settings")
                ProfileOption("Help & Support")
            }
        }
    }
}

@Composable
fun ProfileOption(title: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}
