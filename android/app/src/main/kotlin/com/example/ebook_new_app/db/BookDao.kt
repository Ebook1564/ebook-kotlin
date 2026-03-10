package com.example.ebook_new_app.db

import androidx.room.*
import androidx.paging.PagingSource
import com.example.ebook_new_app.models.ReadingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    // HOME SCREEN: Fetch small batches per category
    @Query("SELECT * FROM books WHERE category_code = :categoryCode LIMIT :limit")
    suspend fun getBooksByCategoryLimit(categoryCode: String, limit: Int = 15): List<BookEntity>

    // SEARCH & SEE ALL: High-performance Paging
    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooksPaging(query: String): PagingSource<Int, BookEntity>

    @Query("SELECT * FROM books WHERE reading_status = :status ORDER BY last_opened DESC")
    fun getBooksByStatusPaging(status: ReadingStatus): PagingSource<Int, BookEntity>

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchBooksLimit(query: String): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("UPDATE books SET reading_status = :status, date_started = :dateStarted WHERE id = :bookId")
    suspend fun updateReadingStatus(bookId: String, status: ReadingStatus, dateStarted: Long?)

    @Query("UPDATE books SET last_opened = :lastOpened WHERE id = :bookId")
    suspend fun updateLastOpened(bookId: String, lastOpened: Long)

    @Query("UPDATE books SET date_finished = :dateFinished, reading_status = :status WHERE id = :bookId")
    suspend fun markAsCompleted(bookId: String, status: ReadingStatus = ReadingStatus.COMPLETED, dateFinished: Long)
}
