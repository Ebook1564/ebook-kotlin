package com.example.ebook_new_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ebook_new_app.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalBookTheme.current
    var searchQuery by remember { mutableStateOf("") }
    
    val categories = listOf("Fiction", "Non-Fiction", "Romance", "Science", "History", "Fantasy", "Mystery")
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val filteredBooks = uiState.trendingBooks.filter { 
        (it.title.contains(searchQuery, ignoreCase = true) || 
        it.author.contains(searchQuery, ignoreCase = true)) &&
        (selectedCategory == null || it.author.contains(selectedCategory!!, ignoreCase = true)) // Dummy filter for categories
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(16.dp)
    ) {
        Text(
            "EXPLORE",
            color = theme.onBackground,
            fontFamily = theme.fontFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.onBackground.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp)),
            placeholder = { Text("Titles or authors...", color = theme.onBackground.copy(alpha = 0.5f), fontFamily = theme.fontFamily) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.onBackground.copy(alpha = 0.5f)) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = theme.onBackground.copy(alpha = 0.05f),
                unfocusedContainerColor = theme.onBackground.copy(alpha = 0.05f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = theme.onBackground,
                unfocusedTextColor = theme.onBackground,
                cursorColor = theme.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Categories
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = if (isSelected) null else category },
                    label = { Text(category, fontFamily = theme.fontFamily) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = theme.primary,
                        selectedLabelColor = theme.onPrimary,
                        labelColor = theme.onBackground,
                        containerColor = theme.onBackground.copy(alpha = 0.1f)
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (searchQuery.isEmpty() && selectedCategory == null) {
            Text("TRENDING SEARCHES", color = theme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = theme.fontFamily)
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredBooks) { book ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, theme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp, 75.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(theme.onBackground.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(book.title, color = theme.onBackground, fontWeight = FontWeight.Bold, fontFamily = theme.fontFamily)
                        Text(book.author, color = theme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = theme.fontFamily)
                    }
                }
            }
        }
    }
}
