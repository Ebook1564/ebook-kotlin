
 package com.example.ebookapp.ui.components

 import androidx.compose.foundation.background
 import androidx.compose.foundation.clickable
 import androidx.compose.foundation.layout.*
 import androidx.compose.foundation.shape.RoundedCornerShape
 import androidx.compose.material.icons.Icons
 import androidx.compose.material.icons.filled.Star
 import androidx.compose.material3.*
 import androidx.compose.runtime.Composable
 import androidx.compose.ui.Alignment
 import androidx.compose.ui.Modifier
 import androidx.compose.ui.draw.clip
 import androidx.compose.ui.graphics.Color
 import androidx.compose.ui.layout.ContentScale
 import androidx.compose.ui.text.font.FontWeight
 import androidx.compose.ui.unit.dp
 import androidx.compose.ui.unit.sp
 import coil.compose.AsyncImage
 import com.example.ebookapp.models.Book

 @Composable
 fun PremiumBadge() {
     Box(
         modifier = Modifier
             .clip(RoundedCornerShape(4.dp))
             .background(Color(0xFFFFD700))
             .padding(horizontal = 6.dp, vertical = 2.dp)
     ) {
         Text(
             text = "PREMIUM",
             color = Color.Black,
             fontSize = 10.sp,
             fontWeight = FontWeight.Bold
         )
     }
 }

 @Composable
 fun RatingStars(rating: Float, maxRating: Int = 5) {
     Row {
         for (i in 1..maxRating) {
             Icon(
                 imageVector = Icons.Default.Star,
                 contentDescription = null,
                 tint = if (i <= rating) Color(0xFFFFD700) else Color.Gray,
                 modifier = Modifier.size(16.dp)
             )
         }
     }
 }

 @Composable
 fun GenreChip(genre: String) {
     Box(
         modifier = Modifier
             .clip(RoundedCornerShape(12.dp))
             .background(Color.DarkGray.copy(alpha = 0.5f))
             .padding(horizontal = 12.dp, vertical = 6.dp)
     ) {
         Text(
             text = genre,
             color = Color.White,
             fontSize = 12.sp
         )
     }
 }

 @Composable
 fun BookThumbnailSmall(book: Book, onClick: () -> Unit) {
     Card(
         modifier = Modifier
             .width(100.dp)
             .clickable(onClick = onClick),
         shape = RoundedCornerShape(8.dp)
     ) {
         AsyncImage(
             model = book.coverImageUrl,
             contentDescription = book.title,
             contentScale = ContentScale.Crop,
             modifier = Modifier.fillMaxSize()
         )
     }
 }

 @Composable
 fun BookThumbnailMedium(book: Book, onClick: () -> Unit) {
     Card(
         modifier = Modifier
             .width(140.dp)
             .clickable(onClick = onClick),
         shape = RoundedCornerShape(8.dp)
     ) {
         Column {
             AsyncImage(
                 model = book.coverImageUrl,
                 contentDescription = book.title,
                 contentScale = ContentScale.Crop,
                 modifier = Modifier
                     .height(200.dp)
                     .fillMaxWidth()
             )
             Column(modifier = Modifier.padding(8.dp)) {
                 Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1)
                 Text(book.author, fontSize = 12.sp, color = Color.Gray)
             }
         }
     }
 }

 @Composable
 fun HeroBookCard(book: Book, onClick: () -> Unit) {
     Card(
         modifier = Modifier
             .fillMaxWidth()
             .height(250.dp)
             .clickable(onClick = onClick),
         shape = RoundedCornerShape(16.dp)
     ) {
         Box {
             AsyncImage(
                 model = book.coverImageUrl,
                 contentDescription = book.title,
                 contentScale = ContentScale.Crop,
                 modifier = Modifier.fillMaxSize()
             )
             Box(
                 modifier = Modifier
                     .fillMaxSize()
                     .background(
                         brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                             colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                             startY = 300f
                         )
                     )
             )
             Column(
                 modifier = Modifier
                     .align(Alignment.BottomStart)
                     .padding(16.dp)
             ) {
                 Text(book.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                 Text(book.author, fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))
             }
         }
     }
 }

 @Composable
 fun CTAButton(
     text: String,
     onClick: () -> Unit,
     modifier: Modifier = Modifier,
     enabled: Boolean = true,
     backgroundColor: Color = Color(0xFFE50914),
     contentColor: Color = Color.White
 ) {
     Button(
         onClick = onClick,
         modifier = modifier,
         enabled = enabled,
         colors = ButtonDefaults.buttonColors(
             containerColor = backgroundColor,
             contentColor = contentColor
         ),
         shape = RoundedCornerShape(8.dp)
     ) {
         Text(text, fontWeight = FontWeight.Bold)
     }
 }

 @Composable
 fun ContinueReadingProgressBar(progress: Float, modifier: Modifier = Modifier) {
     LinearProgressIndicator(
         progress = progress,
         modifier = modifier
             .height(4.dp)
             .clip(RoundedCornerShape(2.dp)),
         color = Color(0xFFE50914),
         trackColor = Color.Gray.copy(alpha = 0.3f)
     )
 }
 
