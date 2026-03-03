package com.example.ebook_new_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBackClick: () -> Unit = {}) {
    val theme = LocalBookTheme.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PROFILE", color = theme.onBackground, fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.background,
                    titleContentColor = theme.onBackground
                )
            )
        },
        containerColor = theme.background
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
                modifier = Modifier.size(120.dp).border(2.dp, theme.onBackground, CircleShape),
                shape = CircleShape,
                color = theme.onBackground.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("JD", color = theme.onBackground, style = MaterialTheme.typography.displayMedium, fontFamily = theme.fontFamily)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("John Doe", style = MaterialTheme.typography.headlineMedium, color = theme.onBackground, fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold)
            Text("PREMIUM MEMBER", color = theme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
            
            Spacer(modifier = Modifier.height(40.dp))
            
            ProfileOption("READING STATISTICS")
            ProfileOption("MANAGE SUBSCRIPTION")
            ProfileOption("SETTINGS")
            ProfileOption("HELP & SUPPORT")
        }
    }
}

@Composable
fun ProfileOption(title: String) {
    val theme = LocalBookTheme.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(0.5.dp, theme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(2.dp)),
        colors = CardDefaults.cardColors(containerColor = theme.onBackground.copy(alpha = 0.02f)),
        shape = RoundedCornerShape(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = theme.onBackground, fontWeight = FontWeight.Medium, fontFamily = theme.fontFamily, fontSize = 14.sp, letterSpacing = 1.sp)
        }
    }
}
