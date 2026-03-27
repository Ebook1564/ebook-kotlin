package com.freeebookbundle.repository

import android.content.Context
import android.util.Log
import com.freeebookbundle.db.BookDiscrepancy
import com.freeebookbundle.db.CatalogHashResponse
import com.freeebookbundle.db.CatalogMetaEntity
import com.freeebookbundle.db.DiscrepancyType
import com.freeebookbundle.db.ServerCatalogEntry
import com.freeebookbundle.db.SyncResult
import com.freeebookbundle.utils.ServerConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class LibrarySyncRepository(
    private val context: Context
) {
    private val gson = Gson()
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "LibrarySync"
    }

    suspend fun syncWithServer(): SyncResult = withContext(Dispatchers.IO) {
        val syncedAt = System.currentTimeMillis()

        try {
            Log.d(TAG, "Starting catalog hash check...")
            
            val catalogUrl = ServerConfig.getCatalogUrl()
            val request = Request.Builder()
                .url(catalogUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Catalog fetch failed: ${response.code}")
                return@withContext SyncResult(
                    success = false,
                    totalBooks = 0,
                    updatedBooks = 0,
                    discrepancies = emptyList(),
                    errors = listOf("Failed to fetch catalog: ${response.code}"),
                    syncedAt = syncedAt
                )
            }

            val body = response.body?.string()
            if (body.isNullOrEmpty()) {
                Log.e(TAG, "Empty catalog response")
                return@withContext SyncResult(
                    success = false,
                    totalBooks = 0,
                    updatedBooks = 0,
                    discrepancies = emptyList(),
                    errors = listOf("Empty catalog response"),
                    syncedAt = syncedAt
                )
            }

            val type = object : TypeToken<Map<String, Any>>() {}.type
            val catalogMap: Map<String, Any> = gson.fromJson(body, type) ?: emptyMap()

            val serverHash = catalogMap["_hash"] as? String
            val serverBookCount = (catalogMap["_bookCount"] as? Double)?.toInt() ?: 0

            Log.d(TAG, "Server catalog hash: $serverHash, book count: $serverBookCount")

            if (serverHash == null) {
                Log.e(TAG, "Server catalog missing _hash field")
                return@withContext SyncResult(
                    success = false,
                    totalBooks = 0,
                    updatedBooks = 0,
                    discrepancies = emptyList(),
                    errors = listOf("Server catalog missing hash field"),
                    syncedAt = syncedAt
                )
            }

            val bundledHash = readBundledCatalogHash()
            Log.d(TAG, "Bundled catalog hash: $bundledHash")

            if (serverHash == bundledHash) {
                Log.d(TAG, "Catalog unchanged - skipping sync")
                return@withContext SyncResult(
                    success = true,
                    skipped = true,
                    totalBooks = serverBookCount,
                    updatedBooks = 0,
                    discrepancies = emptyList(),
                    errors = emptyList(),
                    syncedAt = syncedAt
                )
            }

            Log.d(TAG, "Catalog changed - full sync required")
            return@withContext performFullSync(catalogMap, serverHash, syncedAt)

        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            return@withContext SyncResult(
                success = false,
                totalBooks = 0,
                updatedBooks = 0,
                discrepancies = emptyList(),
                errors = listOf(e.message ?: "Unknown error"),
                syncedAt = syncedAt
            )
        }
    }

    private suspend fun readBundledCatalogHash(): String? = withContext(Dispatchers.IO) {
        try {
            context.assets.open("catalog_hash.txt").bufferedReader().use { it.readText().trim() }
        } catch (e: Exception) {
            Log.d(TAG, "No bundled catalog hash found: ${e.message}")
            null
        }
    }

    private suspend fun performFullSync(catalogMap: Map<String, Any>, serverHash: String, syncedAt: Long): SyncResult = withContext(Dispatchers.IO) {
        val discrepancies = mutableListOf<BookDiscrepancy>()
        val errors = mutableListOf<String>()
        var updatedBooks = 0

        try {
            Log.d(TAG, "Performing full sync...")

            val serverCatalog = mutableMapOf<String, ServerCatalogEntry>()
            for ((key, value) in catalogMap) {
                if (!key.startsWith("_")) {
                    val entryMap = value as? Map<*, *>
                    if (entryMap != null) {
                        serverCatalog[key] = ServerCatalogEntry(
                            id = key,
                            title = entryMap["title"] as? String,
                            author = entryMap["author"] as? String,
                            epubFileName = entryMap["epubFileName"] as? String,
                            coverFileName = entryMap["coverFileName"] as? String,
                            description = entryMap["description"] as? String
                        )
                    }
                }
            }

            Log.d(TAG, "Parsed ${serverCatalog.size} entries from server catalog")

            val bookCount = serverCatalog.size
            Log.d(TAG, "Full sync completed")
            
            SyncResult(
                success = true,
                skipped = false,
                totalBooks = bookCount,
                updatedBooks = updatedBooks,
                discrepancies = discrepancies,
                errors = errors,
                syncedAt = syncedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Full sync error: ${e.message}", e)
            SyncResult(
                success = false,
                totalBooks = 0,
                updatedBooks = 0,
                discrepancies = discrepancies,
                errors = listOf(e.message ?: "Sync error"),
                syncedAt = syncedAt
            )
        }
    }

    fun logDiscrepancies(result: SyncResult) {
        if (result.skipped) {
            Log.d(TAG, "Sync skipped - catalog unchanged")
            return
        }
        
        if (result.discrepancies.isEmpty()) {
            Log.d(TAG, "No discrepancies found during sync")
            return
        }

        Log.d(TAG, "=== SYNC DISCREPANCIES REPORT ===")
        Log.d(TAG, "Total discrepancies: ${result.discrepancies.size}")
        
        result.discrepancies.forEach { discrepancy ->
            when (discrepancy.discrepancyType) {
                DiscrepancyType.FILENAME_CHANGED -> {
                    Log.d(TAG, "[FILENAME CHANGED] Book: ${discrepancy.bookId}")
                    Log.d(TAG, "  Title: ${discrepancy.title}")
                    Log.d(TAG, "  Local:  ${discrepancy.localFileName ?: "null"}")
                    Log.d(TAG, "  Server: ${discrepancy.serverFileName}")
                }
                DiscrepancyType.FILENAME_MISSING_LOCAL -> {
                    Log.d(TAG, "[MISSING LOCAL] Book: ${discrepancy.bookId}")
                    Log.d(TAG, "  Title: ${discrepancy.title}")
                    Log.d(TAG, "  Server: ${discrepancy.serverFileName}")
                }
                DiscrepancyType.BOOK_ON_SERVER_ONLY -> {
                    Log.d(TAG, "[NEW BOOK] Book: ${discrepancy.bookId}")
                    Log.d(TAG, "  Title: ${discrepancy.title}")
                    Log.d(TAG, "  Filename: ${discrepancy.serverFileName}")
                }
                DiscrepancyType.BOOK_ON_LOCAL_ONLY -> {
                    Log.d(TAG, "[LOCAL ONLY] Book: ${discrepancy.bookId}")
                    Log.d(TAG, "  Title: ${discrepancy.title}")
                }
            }
        }
        Log.d(TAG, "=== END REPORT ===")
    }
}
