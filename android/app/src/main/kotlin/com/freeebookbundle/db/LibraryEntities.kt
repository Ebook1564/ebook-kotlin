package com.freeebookbundle.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverter
import androidx.compose.runtime.Immutable
import com.freeebookbundle.models.ReadingStatus
import com.freeebookbundle.utils.ServerConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    companion object {
        private val gson = Gson()
        private val stringListType = object : TypeToken<List<String>>() {}.type
    }

    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value == null || value == "undefined") return emptyList()
        return try {
            gson.fromJson(value, stringListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun toReadingStatus(value: String) = enumValueOf<ReadingStatus>(value)

    @TypeConverter
    fun fromReadingStatus(status: ReadingStatus) = status.name
}

@Immutable
@Entity(tableName = "catalog_meta")
data class CatalogMetaEntity(
    @PrimaryKey val id: Int = 1,
    val catalogHash: String,
    val bookCount: Int,
    val lastUpdated: Long,
    val bundledAt: Long
)

@Immutable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val code: String,
    val name: String,
    val icon: String,
    val color: String
)

@Immutable
data class BookSummary(
    val id: String,
    val title: String,
    val author: String,
    @ColumnInfo(name = "category_code") val categoryCode: String,
    @ColumnInfo(name = "reading_status") val readingStatus: ReadingStatus,
    @ColumnInfo(name = "date_added") val dateAdded: Long?,
    @ColumnInfo(name = "last_opened") val lastOpened: Long? = null,
    @ColumnInfo(name = "last_spine_index") val lastSpineIndex: Int? = 0,
    @ColumnInfo(name = "chapter_progression") val chapterProgression: Float? = 0f,
    val subjects: List<String> = emptyList(),
    @ColumnInfo(name = "external_cover_url") val externalCoverUrl: String? = null
)

fun BookSummary.toBook(): com.freeebookbundle.models.Book {
    val serverIp = ServerConfig.getServerIp()
    val serverPort = ServerConfig.getServerPort()
    val coversUrl = "http://$serverIp:$serverPort/covers/${id}_cover_image.jpg"

    return com.freeebookbundle.models.Book(
        id = id,
        title = title,
        author = author,
        coverImageUrl = coversUrl,
        externalCoverUrl = externalCoverUrl,
        readingStatus = readingStatus,
        dateAdded = dateAdded,
        lastOpened = lastOpened,
        subjects = subjects,
        lastSpineIndex = lastSpineIndex ?: 0,
        chapterProgression = chapterProgression ?: 0f
    )
}

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["title"], name = "idx_book_title"),
        Index(value = ["category_code"], name = "idx_book_category"),
        Index(value = ["reading_status"], name = "idx_book_reading_status"),
        Index(value = ["author"], name = "idx_book_author"),
        Index(value = ["last_opened"], name = "idx_book_last_opened")
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
    @ColumnInfo(name = "external_cover_url") val externalCoverUrl: String? = null,
    @ColumnInfo(name = "last_spine_index", defaultValue = "0") val lastSpineIndex: Int = 0,
    @ColumnInfo(name = "chapter_progression", defaultValue = "0.0") val chapterProgression: Float = 0f,
    @ColumnInfo(name = "tts_spine_index", defaultValue = "0") val ttsSpineIndex: Int = 0,
    @ColumnInfo(name = "tts_sentence_index", defaultValue = "0") val ttsSentenceIndex: Int = 0,
    @ColumnInfo(name = "tts_paragraph_index", defaultValue = "0") val ttsParagraphIndex: Int = 0,
    @ColumnInfo(name = "audio_url") val audioUrl: String? = null
)

@Entity(tableName = "book_progress")
data class BookProgressEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: String,
    @ColumnInfo(name = "last_spine_index", defaultValue = "0") val lastSpineIndex: Int = 0,
    @ColumnInfo(name = "chapter_progression", defaultValue = "0.0") val chapterProgression: Float = 0f,
    @ColumnInfo(name = "last_opened") val lastOpened: Long? = null,
    @ColumnInfo(name = "tts_spine_index", defaultValue = "0") val ttsSpineIndex: Int = 0,
    @ColumnInfo(name = "tts_sentence_index", defaultValue = "0") val ttsSentenceIndex: Int = 0,
    @ColumnInfo(name = "tts_paragraph_index", defaultValue = "0") val ttsParagraphIndex: Int = 0,
)

data class CategoryCount(
    @ColumnInfo(name = "category_code") val categoryCode: String,
    val count: Int
)

fun BookEntity.toBook(): com.freeebookbundle.models.Book {
    val serverIp = ServerConfig.getServerIp()
    val serverPort = ServerConfig.getServerPort()
    val coversUrl = "http://$serverIp:$serverPort/covers/${id}_cover_image.jpg"

    val categoryName = categoryCode.replace("_", " ").split(" ")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

    return com.freeebookbundle.models.Book(
        id = id,
        title = title,
        author = author,
        description = description ?: "",
        coverImageUrl = coversUrl, 
        externalCoverUrl = externalCoverUrl,
        sourceFolder = sourceFolder,
        categories = if (categoryCode.isNotEmpty()) listOf(categoryName) else emptyList(),
        epubFileName = epubFileName,
        readingStatus = readingStatus,
        dateAdded = dateAdded,
        dateStarted = dateStarted,
        lastOpened = lastOpened,
        dateFinished = dateFinished,
        subjects = subjects,
        lastSpineIndex = lastSpineIndex,
        chapterProgression = chapterProgression,
        ttsSpineIndex = ttsSpineIndex,
        ttsSentenceIndex = ttsSentenceIndex,
        ttsParagraphIndex = ttsParagraphIndex,
        audioUrl = audioUrl
    )
}
