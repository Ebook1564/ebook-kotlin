package com.example.ebook_new_app.db

import androidx.room.*
import androidx.paging.PagingSource
import com.example.ebook_new_app.models.ReadingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE category_code = :categoryCode LIMIT :limit")
    suspend fun getBooksByCategoryLimit(categoryCode: String, limit: Int = 15): List<BookEntity>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooksPaging(query: String): PagingSource<Int, BookEntity>

    // IMPROVED CATEGORY SEARCH: Joins with categories table to match UI names (e.g. "Science")
    @Query("""
        SELECT b.* FROM books b
        LEFT JOIN categories c ON b.category_code = c.code
        WHERE c.name LIKE '%' || :catName || '%' 
        OR b.category_code = :catName
        OR b.subjects LIKE '%' || :catName || '%'
        ORDER BY b.title ASC
    """)
    fun getBooksByCategoryNamePaging(catName: String): PagingSource<Int, BookEntity>

    @Query("""
        SELECT * FROM books 
        WHERE author LIKE '%' || :query || '%' 
        OR title LIKE '%' || :query || '%'
        OR subjects LIKE '%' || :query || '%'
        OR category_code LIKE '%' || :query || '%'
        ORDER BY title ASC
    """)
    fun getCollectionBooksPaging(query: String): PagingSource<Int, BookEntity>

    @Query("SELECT * FROM books WHERE author LIKE '%' || :authorName || '%' ORDER BY title ASC")
    fun getBooksByAuthorPaging(authorName: String): PagingSource<Int, BookEntity>

    @Query("SELECT * FROM books WHERE reading_status = :status ORDER BY last_opened DESC")
    fun getBooksByStatusPaging(status: ReadingStatus): PagingSource<Int, BookEntity>

    @Query("SELECT * FROM books WHERE reading_status != :ignoredStatus ORDER BY date_added DESC")
    fun getLibraryBooksFlow(ignoredStatus: ReadingStatus = ReadingStatus.NONE): Flow<List<BookEntity>>

    @Query("SELECT DISTINCT author FROM books WHERE author LIKE '%' || :query || '%' ORDER BY author ASC")
    fun getAuthorsPaging(query: String = ""): PagingSource<Int, String>

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("UPDATE books SET reading_status = :status, date_added = :dateAdded WHERE id = :bookId")
    suspend fun updateReadingStatus(bookId: String, status: ReadingStatus, dateAdded: Long?)

    @Query("UPDATE books SET date_started = :dateStarted WHERE id = :bookId AND date_started IS NULL")
    suspend fun updateFirstOpened(bookId: String, dateStarted: Long)

    @Query("UPDATE books SET description = :description, subjects = :subjects, external_cover_url = :externalCoverUrl WHERE id = :bookId")
    suspend fun updateBookMetadata(bookId: String, description: String, subjects: List<String>, externalCoverUrl: String?)

    @Query("UPDATE books SET last_opened = :lastOpened WHERE id = :bookId")
    suspend fun updateLastOpened(bookId: String, lastOpened: Long)

    @Query("UPDATE books SET date_finished = :dateFinished, reading_status = :status WHERE id = :bookId")
    suspend fun markAsCompleted(bookId: String, status: ReadingStatus = ReadingStatus.COMPLETED, dateFinished: Long)
}
