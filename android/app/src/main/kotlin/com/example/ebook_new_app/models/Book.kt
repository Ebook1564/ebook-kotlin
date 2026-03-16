package com.example.ebook_new_app.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.Parcelize

enum class ReadingStatus {
    NONE,
    WISHLIST,
    CURRENTLY_READING,
    COMPLETED
}

@Parcelize
data class Book(
    val id: String = "",
    val title: String = "No Title",
    val author: String = "Unknown Author",
    val description: String = "",
    val coverImageUrl: String = "",
    val externalCoverUrl: String? = null, // Cached High-Res URL from API
    val categories: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val fileUrl: String? = null,
    val originalFileName: String? = null,
    val sourceFolder: String? = null,
    val epubFileName: String? = null,
    val readingStatus: ReadingStatus = ReadingStatus.NONE,
    val dateAdded: Long? = null,
    val dateStarted: Long? = null,
    val lastOpened: Long? = null,
    val dateFinished: Long? = null
) : Parcelable {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): Book {
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

            val cats = when (val catData = doc.get("categories") ?: doc.get("tags") ?: doc.get("genre")) {
                is List<*> -> catData.filterIsInstance<String>()
                is String -> listOf(catData)
                else -> emptyList<String>()
            }

            return Book(
                id = doc.id,
                title = doc.getString("title") ?: "No Title",
                author = authorName,
                coverImageUrl = doc.getString("coverImageUrl") ?: "",
                description = doc.getString("description") ?: "",
                categories = cats,
                fileUrl = doc.getString("fileUrl"),
                originalFileName = doc.getString("originalFileName")
            )
        }
    }
}
