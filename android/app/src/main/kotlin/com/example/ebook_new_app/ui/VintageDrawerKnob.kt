package com.example.ebook_new_app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VintageDrawerKnob() {
    Box(contentAlignment = Alignment.Center) {
        // Handle plate (Light Golden)
        Surface(
            modifier = Modifier
                .width(32.dp)
                .height(8.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFFFD700).copy(alpha = 0.3f), // Golden tint
            tonalElevation = 1.dp
        ) {}

        // Main knob (Pale Yellow)
        Surface(
            modifier = Modifier.size(20.dp),
            shape = CircleShape,
            color = Color(0xFFFFF9C4), // Pale Yellow
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)) // Golden border
        ) {}
    }
}
