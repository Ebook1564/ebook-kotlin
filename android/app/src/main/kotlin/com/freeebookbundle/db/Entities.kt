package com.freeebookbundle.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable

@Immutable
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val scrollOffset: Int,
    val progress: Float,
    val title: String = "",
    val locator: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val color: Int,
    val annotation: String? = null,
    val locator: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
data class ReaderLocator(
    val chapterIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String = "",
    val locator: String = ""
)

@Immutable
data class BookmarkLocator(
    val chapterIndex: Int,
    val scrollOffset: Int,
    val progress: Float,
    val title: String = "",
    val locator: String = ""
)
