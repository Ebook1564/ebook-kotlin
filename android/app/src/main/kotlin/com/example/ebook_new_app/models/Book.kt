package com.example.ebook_new_app.models

import android.os.Parcelable
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
    val externalCoverUrl: String? = null,
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
) : Parcelable
