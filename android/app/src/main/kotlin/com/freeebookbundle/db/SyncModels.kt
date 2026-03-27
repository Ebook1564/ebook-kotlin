package com.freeebookbundle.db

import com.google.gson.annotations.SerializedName

data class ServerCatalogEntry(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("epubFileName") val epubFileName: String? = null,
    @SerializedName("coverFileName") val coverFileName: String? = null,
    @SerializedName("description") val description: String? = null
)

data class ServerCatalog(
    val entries: Map<String, ServerCatalogEntry>
)

data class CatalogHashResponse(
    @SerializedName("_hash") val hash: String?,
    @SerializedName("_bookCount") val bookCount: Int?,
    @SerializedName("_generated") val generatedAt: String?
)

data class BookDiscrepancy(
    val bookId: String,
    val title: String,
    val localFileName: String?,
    val serverFileName: String?,
    val discrepancyType: DiscrepancyType
)

enum class DiscrepancyType {
    FILENAME_CHANGED,
    FILENAME_MISSING_LOCAL,
    BOOK_ON_SERVER_ONLY,
    BOOK_ON_LOCAL_ONLY
}

data class SyncResult(
    val success: Boolean,
    val skipped: Boolean = false,
    val totalBooks: Int,
    val updatedBooks: Int,
    val discrepancies: List<BookDiscrepancy>,
    val errors: List<String>,
    val syncedAt: Long
)
