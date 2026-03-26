package com.freeebookbundle.models

import android.os.Parcelable
import com.freeebookbundle.utils.ServerConfig
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
) : Parcelable {
    fun getServerCoverUrl(): String {
        return "${ServerConfig.getCoversUrl()}/${id}_cover_image.jpg"
    }
    
    fun isExternalCoverPlaceholder(): Boolean {
        val url = externalCoverUrl ?: return true
        
        val lowerUrl = url.lowercase()
        
        val placeholderPatterns = listOf(
            "image not available",
            "no image",
            "no cover",
            "placeholder",
            "nopicture",
            "noplaceholder",
            "blank",
            "generic",
            "default",
            "default_cover",
            "default_book",
            "missing",
            "not_available",
            "printsec=frontcover&w=",
            "books/content?id=nopicture",
            "books/content?id=no",
            "g/generic",
            "/noCover",
            "/no_cover",
            "/missing",
            "/placeholder",
            "icon?",
            "1x1.gif",
            "spacer.gif",
            "pixel",
            "base64",
            "data:image"
        )
        
        if (placeholderPatterns.any { lowerUrl.contains(it) }) {
            return true
        }
        
        if (lowerUrl.contains("googleapis.com") || lowerUrl.contains("books.google.com")) {
            if (lowerUrl.contains("printsec=frontcover") && lowerUrl.contains("img=1")) {
                return true
            }
            if (lowerUrl.contains("&zoom=") && !lowerUrl.contains("zoom=0") && !lowerUrl.contains("zoom=1")) {
                val zoomMatch = Regex("zoom=(\\d+)").find(lowerUrl)
                val zoomLevel = zoomMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                if (zoomLevel > 2) return true
            }
        }
        
        if (lowerUrl.contains("openlibrary.org") || lowerUrl.contains("covers.openlibrary.org")) {
            if (lowerUrl.contains("/b/id/") && lowerUrl.contains("-M.jpg")) {
                val idPattern = Regex("/b/id/(\\d+)").find(lowerUrl)
                val coverId = idPattern?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                if (coverId == 0 || coverId == -1) return true
            }
            if (lowerUrl.contains("/b/cover/")) return true
        }
        
        return false
    }
    
    fun getBestCoverUrl(): String {
        if (!isExternalCoverPlaceholder()) {
            return externalCoverUrl ?: getServerCoverUrl()
        }
        return getServerCoverUrl()
    }
    
    fun hasStartedReading(): Boolean {
        return dateStarted != null
    }
}