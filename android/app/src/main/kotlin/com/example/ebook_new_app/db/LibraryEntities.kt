package com.example.ebook_new_app.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverter
import com.example.ebook_new_app.models.ReadingStatus
import com.example.ebook_new_app.utils.ServerConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value == null || value == "undefined") return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            Gson().fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return Gson().toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun toReadingStatus(value: String) = enumValueOf<ReadingStatus>(value)

    @TypeConverter
    fun fromReadingStatus(status: ReadingStatus) = status.name
}

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
    @ColumnInfo(name = "date_added") val dateAdded: Long? = null,
    @ColumnInfo(name = "date_started") val dateStarted: Long? = null,
    @ColumnInfo(name = "last_opened") val lastOpened: Long? = null,
    @ColumnInfo(name = "date_finished") val dateFinished: Long? = null,
    @ColumnInfo(name = "subjects", defaultValue = "[]") val subjects: List<String> = emptyList(),
    @ColumnInfo(name = "external_cover_url") val externalCoverUrl: String? = null
)

fun BookEntity.toBook(): com.example.ebook_new_app.models.Book {
    val serverIp = ServerConfig.getServerIp()
    val serverPort = ServerConfig.getServerPort()
    val coversUrl = "http://$serverIp:$serverPort/covers/${id}_cover_image.jpg"

    return com.example.ebook_new_app.models.Book(
        id = id,
        title = title,
        author = author,
        description = description ?: "",
        coverImageUrl = coversUrl, 
        externalCoverUrl = externalCoverUrl,
        sourceFolder = sourceFolder,
        epubFileName = epubFileName,
        readingStatus = readingStatus,
        dateAdded = dateAdded,
        dateStarted = dateStarted,
        lastOpened = lastOpened,
        dateFinished = dateFinished,
        subjects = subjects
    )
}
