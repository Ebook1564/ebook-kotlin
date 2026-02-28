package com.example.ebook_new_app.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocatorDao {
    // Bookmarks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>>

    // Highlights
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight)

    @Delete
    suspend fun deleteHighlight(highlight: Highlight)

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getHighlightsForBook(bookId: String): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId")
    suspend fun getHighlightsForBookSync(bookId: String): List<Highlight>
}
