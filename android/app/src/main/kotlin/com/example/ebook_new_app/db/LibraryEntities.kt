package com.example.ebook_new_app.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.ebook_new_app.models.ReadingStatus

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val code: String,
    val name: String,
    val icon: String,
    val color: String
)

@Entity(
    tableName = "books",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["code"],
            childColumns = ["category_code"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["title"], name = "idx_book_title"),
        Index(value = ["category_code"], name = "idx_book_category")
    ]
)
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String?,
    @ColumnInfo(name = "category_code") val categoryCode: String,
    @ColumnInfo(name = "source_folder") val sourceFolder: String,
    @ColumnInfo(name = "cover_image_name") val coverImageName: String?,
    @ColumnInfo(name = "epub_file_name") val epubFileName: String?,
    @ColumnInfo(name = "is_downloaded", defaultValue = "0") val isDownloaded: Boolean = false,
    @ColumnInfo(name = "reading_status", defaultValue = "NONE") val readingStatus: ReadingStatus = ReadingStatus.NONE,
    @ColumnInfo(name = "date_started") val dateStarted: Long? = null,
    @ColumnInfo(name = "last_opened") val lastOpened: Long? = null,
    @ColumnInfo(name = "date_finished") val dateFinished: Long? = null
)

fun BookEntity.toBook(): com.example.ebook_new_app.models.Book {
    val serverIp = "192.168.1.23"
    
    // Pattern 1: ID_ID-cover.png (Most common)
    val primaryCoverUrl = "http://$serverIp:8080/${id}_${id}-cover.png"
    
    // Pattern 2: Title_cover.ID (Found in law folder)
    val titleEncoded = android.net.Uri.encode(title)
    val secondaryCoverUrl = "http://$serverIp:8080/${titleEncoded}_cover.${id}"

    // Pattern 3: Explicit name if it exists in DB (legacy support)
    val dbCoverUrl = if (!coverImageName.isNullOrEmpty()) {
        "http://$serverIp:8080/${android.net.Uri.encode(coverImageName)}"
    } else ""

    // We'll pass the most likely one, but HomeScreen/BookPoster should handle failure
    val finalUrl = if (sourceFolder == "law") secondaryCoverUrl else primaryCoverUrl

    return com.example.ebook_new_app.models.Book(
        id = id,
        title = title,
        author = author,
        description = description ?: "",
        coverImageUrl = finalUrl, 
        sourceFolder = sourceFolder,
        epubFileName = epubFileName,
        readingStatus = readingStatus,
        dateStarted = dateStarted,
        lastOpened = lastOpened,
        dateFinished = dateFinished
    )
}
