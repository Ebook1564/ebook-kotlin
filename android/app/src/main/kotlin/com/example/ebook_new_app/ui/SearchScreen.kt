package com.example.ebook_new_app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawArc
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.viewmodel.HomeViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalBookTheme.current
    var searchQuery by remember { mutableStateOf("") }
    
    val allCategories = listOf("Fiction", "History", "Science", "Romance", "Fantasy", "Mystery", "Biography")
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Calculate book counts for each category
    val categoryBookCounts = remember(uiState.trendingBooks) {
        allCategories.associateWith { category ->
            uiState.trendingBooks.count { book ->
                book.genres.any { it.contains(category, ignoreCase = true) }
            }
        }
    }
    val totalBooksInAllCategories = uiState.trendingBooks.sumOf { book ->
        allCategories.count { category ->
            book.genres.any { it.contains(category, ignoreCase = true) }
        }
    }.coerceAtLeast(1) // Avoid division by zero

    val filteredBooks = uiState.trendingBooks.filter { book ->
        val matchesSearchQuery = searchQuery.isEmpty() ||
                                 book.title.contains(searchQuery, ignoreCase = true) ||
                                 book.author.contains(searchQuery, ignoreCase = true)

        val matchesCategory = selectedCategory == null ||
                              book.genres.any { it.contains(selectedCategory!!, ignoreCase = true) }
        matchesSearchQuery && matchesCategory
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
            fontSize = 40.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search titles or authors...", color = theme.onBackground.copy(alpha = 0.5f), fontFamily = theme.fontFamily) },
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

        Spacer(modifier = Modifier.height(24.dp)) // Sufficient space to avoid overlap

        PieChartCategorySelector(
            categories = allCategories,
            selectedCategory = selectedCategory,
            categoryBookCounts = categoryBookCounts,
            totalBooksInAllCategories = totalBooksInAllCategories,
            onCategorySelected = { category ->
                selectedCategory = if (selectedCategory == category) null else category
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp) // Reduced height for the pie chart
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(filteredBooks) { book ->
                SearchResultItem(book)
            }
        }
    }
}

@Composable
fun PieChartCategorySelector(
    categories: List<String>,
    selectedCategory: String?,
    categoryBookCounts: Map<String, Int>,
    totalBooksInAllCategories: Int,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalBookTheme.current
    val colors = remember {
        listOf(
            Color(0xFF8B0000), // Dark Red
            Color(0xFFB8860B), // Dark Goldenrod
            Color(0xFF006400), // Dark Green
            Color(0xFF483D8B), // Dark Slate Blue
            Color(0xFF8B4513), // Saddle Brown
            Color(0xFF2F4F4F), // Dark Slate Gray
            Color(0xFF6A5ACD)  // Slate Blue
        ).shuffled()
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val pieChartSize = 200.dp // Size of the pie chart
        val selectedSliceRadius = 15.dp // How much selected slice "pops out"

        Canvas(modifier = Modifier
            .size(pieChartSize + selectedSliceRadius * 2) // Increase canvas size for pop-out
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val touchX = offset.x - centerX
                    val touchY = offset.y - centerY
                    val touchRadius = sqrt(touchX * touchX + touchY * touchY)
                    
                    if (touchRadius <= (pieChartSize.toPx() / 2 + selectedSliceRadius.toPx())) {
                        var touchAngle = (atan2(touchY, touchX) * 180 / Math.PI).toFloat()
                        if (touchAngle < 0) touchAngle += 360f

                        var currentStartAngle = 0f
                        categories.forEachIndexed { index, category ->
                            val count = categoryBookCounts[category] ?: 0
                            val sweepAngle = (count.toFloat() / totalBooksInAllCategories) * 360f

                            if (touchAngle >= currentStartAngle && touchAngle < currentStartAngle + sweepAngle) {
                                onCategorySelected(category)
                                return@detectTapGestures
                            }
                            currentStartAngle += sweepAngle
                        }
                    }
                    onCategorySelected(null) // Deselect if no slice is hit
                }
            }
        ) {
            var currentStartAngle = 0f

            categories.forEachIndexed { index, category ->
                val count = categoryBookCounts[category] ?: 0
                val sweepAngle = (count.toFloat() / totalBooksInAllCategories) * 360f
                val isCurrentlySelected = selectedCategory == category

                val currentRadius = if (isCurrentlySelected) {
                    (pieChartSize.toPx() / 2) + selectedSliceRadius.toPx()
                } else {
                    pieChartSize.toPx() / 2
                }

                val offset = if (isCurrentlySelected) {
                    val angleMid = currentStartAngle + sweepAngle / 2
                    val offsetX = (selectedSliceRadius.toPx() / 2) * cos(Math.toRadians(angleMid.toDouble())).toFloat()
                    val offsetY = (selectedSliceRadius.toPx() / 2) * sin(Math.toRadians(angleMid.toDouble())).toFloat()
                    Offset(offsetX, offsetY)
                } else {
                    Offset.Zero
                }

                drawArc(
                    color = colors[index % colors.size],
                    startAngle = currentStartAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(
                        (size.width - currentRadius * 2) / 2f + offset.x,
                        (size.height - currentRadius * 2) / 2f + offset.y
                    ),
                    size = Size(currentRadius * 2, currentRadius * 2)
                )

                // Draw text label on the arc
                val labelAngle = currentStartAngle + sweepAngle / 2
                val labelRadius = currentRadius * 0.7f // Position text inside slice
                val x = (size.width / 2 + labelRadius * cos(Math.toRadians(labelAngle.toDouble()))).toFloat() + offset.x
                val y = (size.height / 2 + labelRadius * sin(Math.toRadians(labelAngle.toDouble()))).toFloat() + offset.y

                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        color = Color.White.toArgb()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText(
                        category,
                        x,
                        y,
                        paint
                    )
                }

                currentStartAngle += sweepAngle
            }
        }

        // Display book count and percentage for selected category in the center
        selectedCategory?.let { category ->
            val count = categoryBookCounts[category] ?: 0
            val percentage = ((count.toFloat() / totalBooksInAllCategories) * 100).roundToInt()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    category.uppercase(),
                    color = theme.onBackground,
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    "$percentage%",
                    color = theme.onBackground.copy(alpha = 0.7f),
                    fontFamily = theme.fontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } ?: run {
             Text(
                "SELECT CATEGORY",
                color = theme.onBackground.copy(alpha = 0.8f),
                fontFamily = theme.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun SearchResultItem(book: Book) {
    val theme = LocalBookTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp, 90.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(theme.onBackground.copy(alpha = 0.1f))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(book.title, color = theme.onBackground, fontWeight = FontWeight.Bold, fontFamily = theme.fontFamily, fontSize = 18.sp)
            Text(book.author, color = theme.onBackground.copy(alpha = 0.7f), fontSize = 16.sp, fontFamily = theme.fontFamily)
        }
    }
}