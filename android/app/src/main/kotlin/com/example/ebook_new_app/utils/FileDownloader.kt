package com.example.ebook_new_app.utils

import android.content.Context
import android.util.Log
import com.example.ebook_new_app.models.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import okio.buffer
import okio.sink

object FileDownloader {
    private const val TAG = "FileDownloader"
    
    // 10.0.2.2 is the special IP for Android Emulator to access host machine
    private const val LOCAL_SERVER_IP = "192.168.1.23"
    private const val LOCAL_SERVER_URL = "http://$LOCAL_SERVER_IP:8080"
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()

    suspend fun ensureCached(context: Context, book: Book): File? = withContext(Dispatchers.IO) {
        val cleanId = book.id.replace(Regex("[^a-zA-Z0-9]"), "")
        // Use a safe local filename
        val localFileName = "book_${cleanId}.epub"
        val destinationFile = File(context.filesDir, "epubs/$localFileName")
        
        val parent = destinationFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        if (destinationFile.exists() && destinationFile.length() > 5000) { // Tiny files are likely 404 pages
            return@withContext destinationFile
        }

        // --- 1. ASSET RECOVERY ---
        val assetNamesToTry = listOfNotNull(book.epubFileName, "$cleanId.epub")
        for (assetName in assetNamesToTry.distinct()) {
            try {
                context.assets.open("epubs/$assetName").use { input ->
                    destinationFile.outputStream().use { output -> input.copyTo(output) }
                }
                return@withContext destinationFile
            } catch (e: Exception) { }
        }

        // --- 2. NETWORK DOWNLOAD ---
        val urlsToTry = mutableListOf<String>()
        
        // Strategy A: Use the stored filename (best for exact matching)
        if (!book.epubFileName.isNullOrEmpty()) {
            val encodedName = android.net.Uri.encode(book.epubFileName)
            urlsToTry.add("$LOCAL_SERVER_URL/$encodedName")
        }
        
        // Strategy B: Use the ID fallback (triggers the Smart Server's fuzzy matching)
        urlsToTry.add("$LOCAL_SERVER_URL/fuzzy_search_${book.id}.epub")

        for (url in urlsToTry) {
            try {
                Log.d(TAG, "Downloading: $url")
                val request = Request.Builder()
                    .url(url)
                    .build()

                val result = okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            destinationFile.sink().buffer().use { sink ->
                                sink.writeAll(body.source())
                            }
                            destinationFile
                        } else null
                    } else null
                }
                
                if (result != null && result.exists() && result.length() > 5000) {
                    Log.d(TAG, "Successfully downloaded: $url")
                    return@withContext result
                } else {
                    destinationFile.delete() // Clean up failed/small downloads
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for $url: ${e.message}")
                if (destinationFile.exists()) destinationFile.delete()
            }
        }
        null
    }
}
