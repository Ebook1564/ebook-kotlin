package com.freeebookbundle.db

import androidx.room.*
import androidx.paging.PagingSource
import com.freeebookbundle.models.ReadingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE category_code = :categoryCode LIMIT :limit")
    suspend fun getBooksByCategoryLimit(categoryCode: String, limit: Int = 15): List<BookEntity>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooksPaging(query: String): PagingSource<Int, BookEntity>

    // Unified search - search books, authors, and genres
    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchBooks(query: String, limit: Int = 50): List<BookEntity>

    @Query("SELECT DISTINCT author FROM books WHERE author LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchAuthors(query: String, limit: Int = 20): List<String>

    @Query("SELECT COUNT(*) FROM books WHERE author = :authorName")
    suspend fun getBookCountByAuthor(authorName: String): Int

    @Query("SELECT * FROM categories WHERE name LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchCategories(query: String, limit: Int = 20): List<CategoryEntity>

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
    fun getLibraryBooksFlow(ignoredStatus: ReadingStatus = ReadingStatus.NONE): Flow<List<BookSummary>>

    @Query("SELECT DISTINCT author FROM books WHERE author LIKE '%' || :query || '%' ORDER BY author ASC")
    fun getAuthorsPaging(query: String = ""): PagingSource<Int, String>

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("""
        SELECT b.* FROM books b
        INNER JOIN book_progress bp ON b.id = bp.book_id
        WHERE bp.last_opened IS NOT NULL
        ORDER BY bp.last_opened DESC LIMIT :limit
    """)
    suspend fun getRecentlyOpenedBooks(limit: Int = 10): List<BookEntity>

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

    @Query("""
        SELECT b.id, b.title, b.author, b.category_code, b.reading_status, b.date_added,
               COALESCE(bp.last_opened, b.last_opened) AS last_opened,
               bp.last_spine_index, bp.chapter_progression,
               b.subjects, b.external_cover_url
        FROM books b
        LEFT JOIN book_progress bp ON b.id = bp.book_id
        WHERE b.reading_status = 'CURRENTLY_READING' AND bp.last_opened IS NOT NULL
        ORDER BY bp.last_opened DESC LIMIT :limit
    """)
    fun getContinueReadingBooks(limit: Int): Flow<List<BookSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBook(bookId: String): BookEntity?

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Query("SELECT COUNT(*) FROM books WHERE reading_status = :status")
    suspend fun getBookCountByStatus(status: ReadingStatus): Int

    @Query("SELECT category_code, COUNT(*) AS count FROM books WHERE category_code != '' GROUP BY category_code ORDER BY count DESC LIMIT :limit")
    suspend fun getCategoryCounts(limit: Int = 5): List<CategoryCount>

    @Query("SELECT MAX(last_opened) FROM books WHERE last_opened IS NOT NULL")
    suspend fun getLastActivityTime(): Long?
}
