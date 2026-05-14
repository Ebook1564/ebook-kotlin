package com.freeebookbundle.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Bookmark::class, Highlight::class, BookEntity::class, CategoryEntity::class, CatalogMetaEntity::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locatorDao(): LocatorDao
    abstract fun bookDao(): BookDao
    abstract fun catalogMetaDao(): CatalogMetaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

private fun migrateFromOldSchema(db: SupportSQLiteDatabase) {
            // Check if old schema (has 'locator' column)
            val hasLocator = try {
                db.query("SELECT * FROM bookmarks LIMIT 1").use { cursor ->
                    val index = cursor.getColumnIndex("locator")
                    index != -1
                }
            } catch (e: Exception) {
                false
            }
            
            if (hasLocator) return // Already migrated (new schema)
            
            // === BOOKMARKS TABLE ===
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS bookmarks_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    bookId TEXT NOT NULL,
                    chapterIndex INTEGER NOT NULL DEFAULT 0,
                    scrollOffset INTEGER NOT NULL DEFAULT 0,
                    progress REAL NOT NULL DEFAULT 0.0,
                    title TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL DEFAULT 0
                )
            """)
            
            db.execSQL("""
                INSERT INTO bookmarks_new (id, bookId, chapterIndex, scrollOffset, progress, title, createdAt)
                SELECT id, bookId, 0, 0, 0.0, '', createdAt FROM bookmarks
            """)
            
            db.execSQL("DROP TABLE bookmarks")
            db.execSQL("ALTER TABLE bookmarks_new RENAME TO bookmarks")

            // === HIGHLIGHTS TABLE ===
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS highlights_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    bookId TEXT NOT NULL,
                    chapterIndex INTEGER NOT NULL DEFAULT 0,
                    startOffset INTEGER NOT NULL DEFAULT 0,
                    endOffset INTEGER NOT NULL DEFAULT 0,
                    selectedText TEXT NOT NULL DEFAULT '',
                    color INTEGER NOT NULL DEFAULT 0,
                    annotation TEXT,
                    createdAt INTEGER NOT NULL DEFAULT 0
                )
            """)
            
            db.execSQL("""
                INSERT INTO highlights_new (id, bookId, chapterIndex, startOffset, endOffset, selectedText, color, annotation, createdAt)
                SELECT id, bookId, 0, 0, 50, '', color, annotation, createdAt FROM highlights
            """)
            
            db.execSQL("DROP TABLE highlights")
            db.execSQL("ALTER TABLE highlights_new RENAME TO highlights")
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateFromOldSchema(db)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "library.db"
                )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = OFF;")
                        
                        // Check if migration already ran by checking for old schema
                        // USE THE PASSED 'db' INSTANCE, NOT AppDatabase.getDatabase() to avoid recursion
                        val needsMigration = try {
                            db.query("SELECT * FROM bookmarks LIMIT 1").use { cursor ->
                                cursor.getColumnIndex("locator") != -1
                            }
                        } catch (e: Exception) {
                            false
                        }

                        if (needsMigration) {
                            migrateFromOldSchema(db)
                        }
                    }
                })
                .addMigrations(MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}