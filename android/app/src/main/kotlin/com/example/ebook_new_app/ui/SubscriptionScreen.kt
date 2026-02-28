package com.example.ebookapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SubscriptionPlan(
    val name: String,
    val price: String,
    val features: List<String>,
    val isPopular: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(onBackClick: () -> Unit) {
    val plans = listOf(
        SubscriptionPlan(
            name = "Basic",
            price = "$9.99/month",
            features = listOf("Ad-free reading", "Access to a limited library", "Read on one device at a time")
        ),
        SubscriptionPlan(
            name = "Standard",
            price = "$15.99/month",
            features = listOf("All features of Basic", "Access to the full library", "Read on up to 2 devices at once", "Download books for offline reading"),
            isPopular = true
        ),
        SubscriptionPlan(
            name = "Premium",
            price = "$21.99/month",
            features = listOf("All features of Standard", "Read on up to 4 devices at once", "Early access to new releases", "Listen to audiobooks")
        )
    )

    var selectedPlan by remember { mutableStateOf(plans.find { it.isPopular }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Plans", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F0F),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F0F0F)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(plans) { plan ->
                SubscriptionPlanCard(
                    plan = plan,
                    isSelected = plan == selectedPlan,
                    onSelected = { selectedPlan = plan }
                )
            }
        }
    }
}

@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isSelected) Color(0xFFE50914) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.3f)),
        onClick = onSelected
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(plan.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (plan.isPopular) {
                    Text(
                        "POPULAR",
                        color = Color.White,
                        modifier = Modifier
                            .background(Color(0xFFE50914), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(plan.price, fontSize = 20.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            plan.features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFE50914))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(feature, color = Color.White.copy(alpha = 0.8f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
