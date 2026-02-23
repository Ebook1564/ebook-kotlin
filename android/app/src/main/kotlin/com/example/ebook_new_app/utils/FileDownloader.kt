package com.example.ebook_new_app.utils

import android.content.Context
import android.util.Log
import com.example.ebook_new_app.models.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object FileDownloader {
    private const val TAG = "FileDownloader"

    suspend fun ensureCached(context: Context, book: Book): File? = withContext(Dispatchers.IO) {
        val cleanId = book.id.replace(Regex("[^a-zA-Z0-9]"), "")
        val fileName = book.originalFileName ?: "$cleanId.epub"
        // Use filesDir/epubs instead of cacheDir for better compatibility with WebViewAssetLoader
        val destinationFile = File(context.filesDir, "epubs/$fileName")
        
        if (!destinationFile.parentFile.exists()) {
            destinationFile.parentFile.mkdirs()
        }

        if (destinationFile.exists()) {
            return@withContext destinationFile
        }

        // --- AGGRESSIVE ASSET RECOVERY (Mirrors Flutter) ---
        // We try both the original name AND the ID-based name
        val assetNamesToTry = listOfNotNull(book.originalFileName, "$cleanId.epub", "${book.title.replace(" ", "")}.epub")
        
        for (assetName in assetNamesToTry.distinct()) {
            try {
                Log.d(TAG, "Checking assets for: $assetName")
                context.assets.open("epubs/$assetName").use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Found in assets: $assetName")
                return@withContext destinationFile
            } catch (e: Exception) {
                // Not in assets, continue to next or cloud
            }
        }

        // --- CLOUD FALLBACK ---
        val url = book.fileUrl
        if (url.isNullOrEmpty()) {
            Log.e(TAG, "No source found for ${book.title} (No assets match, no URL)")
            return@withContext null
        }

        return@withContext try {
            Log.d(TAG, "Downloading from cloud: $url")
            val connection = URL(url).openConnection()
            connection.connectTimeout = 15000
            connection.connect()
            connection.getInputStream().use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile
        } catch (e: Exception) {
            Log.e(TAG, "Cloud download failed", e)
            null
        }
    }
}
