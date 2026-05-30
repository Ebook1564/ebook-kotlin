package com.freeebookbundle.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: BookProgressEntity)

    @Query("SELECT * FROM book_progress WHERE book_id = :bookId")
    suspend fun getProgress(bookId: String): BookProgressEntity?

    @Query("SELECT * FROM book_progress WHERE book_id = :bookId")
    fun getProgressFlow(bookId: String): Flow<BookProgressEntity?>

    @Query("SELECT last_spine_index AS spineIndex, chapter_progression AS progression FROM book_progress WHERE book_id = :bookId")
    suspend fun getLastPosition(bookId: String): LastPosition?

    @Query("SELECT tts_spine_index AS spineIndex, tts_sentence_index AS sentenceIndex, tts_paragraph_index AS paragraphIndex FROM book_progress WHERE book_id = :bookId")
    suspend fun getTtsPosition(bookId: String): TtsPosition?

    @Query("SELECT * FROM book_progress WHERE last_opened IS NOT NULL ORDER BY last_opened DESC LIMIT :limit")
    suspend fun getRecentlyOpened(limit: Int = 10): List<BookProgressEntity>

    @Query("SELECT * FROM book_progress WHERE last_opened IS NOT NULL ORDER BY last_opened DESC LIMIT :limit")
    fun getRecentlyOpenedFlow(limit: Int = 10): Flow<List<BookProgressEntity>>

    @Query("SELECT * FROM book_progress")
    fun getAllProgress(): Flow<List<BookProgressEntity>>

    @Query("SELECT * FROM book_progress")
    suspend fun getAllProgressSync(): List<BookProgressEntity>

    data class LastPosition(
        val spineIndex: Int,
        val progression: Float
    )

    data class TtsPosition(
        val spineIndex: Int,
        val sentenceIndex: Int,
        val paragraphIndex: Int
    )
}
