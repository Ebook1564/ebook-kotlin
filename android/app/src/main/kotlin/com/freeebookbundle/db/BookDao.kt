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

    @Query("SELECT * FROM books WHERE last_opened IS NOT NULL ORDER BY last_opened DESC LIMIT :limit")
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

    @Query("UPDATE books SET tts_spine_index = :spineIndex, tts_sentence_index = :sentenceIndex, tts_paragraph_index = :paragraphIndex WHERE id = :bookId")
    suspend fun updateTtsPosition(bookId: String, spineIndex: Int, sentenceIndex: Int, paragraphIndex: Int)

    @Query("UPDATE books SET last_spine_index = :spineIndex, chapter_progression = :progression, last_opened = :timestamp WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: String, spineIndex: Int, progression: Float, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM books WHERE reading_status = 'CURRENTLY_READING' AND last_opened IS NOT NULL ORDER BY last_opened DESC LIMIT :limit")
    fun getContinueReadingBooks(limit: Int): Flow<List<BookSummary>>

    @Query("SELECT last_spine_index AS spineIndex, chapter_progression AS progression FROM books WHERE id = :bookId")
    suspend fun getLastPosition(bookId: String): LastPosition?

    @Query("SELECT tts_spine_index AS spineIndex, tts_sentence_index AS sentenceIndex, tts_paragraph_index AS paragraphIndex FROM books WHERE id = :bookId")
    suspend fun getTtsPosition(bookId: String): TtsPosition?

    data class LastPosition(
        val spineIndex: Int,
        val progression: Float
    )

    data class TtsPosition(
        val spineIndex: Int,
        val sentenceIndex: Int,
        val paragraphIndex: Int
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBook(bookId: String): BookEntity?

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int
}
