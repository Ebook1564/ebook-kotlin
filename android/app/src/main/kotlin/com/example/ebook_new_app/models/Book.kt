package com.example.ebook_new_app.models

import com.google.firebase.firestore.DocumentSnapshot

data class Book(
    val id: String = "",
    val title: String = "No Title",
    val author: String = "Unknown Author",
    val coverImageUrl: String = "",
    val description: String = "",
    val fileUrl: String? = null,
    val originalFileName: String? = null,
    val genres: List<String> = emptyList() // Added genres property
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): Book {
            val data = doc.data ?: emptyMap<String, Any>()
            
            // 1. Author Parsing
            val authorData = doc.get("author")
            val authorName = when (authorData) {
                is List<*> -> {
                    val first = authorData.firstOrNull()
                    if (first is Map<*, *>) {
                        (first["contributor"] ?: first["fileAs"] ?: "Unknown Author").toString()
                    } else {
                        "Unknown Author"
                    }
                }
                is String -> authorData
                else -> "Unknown Author"
            }

            // 2. Metadata with Case-Insensitive Fallbacks
            val url = doc.getString("fileUrl") 
                ?: doc.getString("file_url") 
                ?: doc.getString("url") 
                ?: doc.getString("fileURL")

            val asset = doc.getString("originalFileName") 
                ?: doc.getString("original_file_name") 
                ?: doc.getString("fileName") 
                ?: doc.getString("filename")

            // 3. Image with Fallbacks
            val image = doc.getString("coverImageUrl") 
                ?: doc.getString("image") 
                ?: doc.getString("cover_image") 
                ?: ""

            // 4. Genres Parsing with Fallbacks
            val genresData = doc.get("genres") ?: doc.get("tags") ?: doc.get("categories")
            val bookGenres = when (genresData) {
                is List<*> -> genresData.mapNotNull { it?.toString() }
                is String -> genresData.split(",").map { it.trim() }
                else -> emptyList()
            }

            return Book(
                id = doc.id,
                title = doc.getString("title") ?: doc.getString("name") ?: "No Title",
                author = authorName,
                coverImageUrl = image,
                description = doc.getString("description") ?: "",
                fileUrl = url,
                originalFileName = asset,
                genres = bookGenres
            )
        }
    }
}
