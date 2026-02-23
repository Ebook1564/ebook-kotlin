package com.example.ebook_new_app.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.readium.r2.shared.publication.Locator

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val locator: String, // JSON representation of Locator
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val locator: String, // JSON representation of Locator
    val color: Int, // ARGB color
    val annotation: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
