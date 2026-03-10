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
    
    // CHANGE THIS TO YOUR PC'S IP FOR PHYSICAL DEVICE (e.g., "192.168.1.15")
    private const val LOCAL_SERVER_IP = "10.0.2.2"
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
        val fileName = book.epubFileName ?: book.originalFileName ?: "$cleanId.epub"
        val destinationFile = File(context.filesDir, "epubs/$fileName")
        
        val parent = destinationFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        if (destinationFile.exists() && destinationFile.length() > 0) {
            return@withContext destinationFile
        }

        // --- 1. ASSET RECOVERY ---
        val assetNamesToTry = listOfNotNull(book.epubFileName, book.originalFileName, "$cleanId.epub")
        for (assetName in assetNamesToTry.distinct()) {
            try {
                Log.d(TAG, "Checking assets for: $assetName")
                context.assets.open("epubs/$assetName").use { input ->
                    destinationFile.outputStream().use { output -> input.copyTo(output) }
                }
                Log.d(TAG, "Restored from assets: $assetName")
                return@withContext destinationFile
            } catch (e: Exception) { }
        }

        // --- 2. NETWORK DOWNLOAD ---
        val urlsToTry = mutableListOf<String>()
        book.fileUrl?.let { urlsToTry.add(it) }
        
        if (!book.epubFileName.isNullOrEmpty()) {
            val rawName = book.epubFileName
            val encodedName = android.net.Uri.encode(rawName)
            urlsToTry.add("$LOCAL_SERVER_URL/$encodedName")
            urlsToTry.add("$LOCAL_SERVER_URL/$rawName")
        }

        for (url in urlsToTry) {
            try {
                Log.d(TAG, "Attempting: $url")
                val request = Request.Builder()
                    .url(url)
                    .header("Connection", "close") // Force non-persistent to avoid early termination
                    .header("Accept-Encoding", "identity") // No GZIP
                    .build()

                val result = okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            val expectedSize = body.contentLength()
                            Log.d(TAG, "Starting stream for $url (Expected: $expectedSize)")
                            
                            try {
                                destinationFile.sink().buffer().use { sink ->
                                    sink.writeAll(body.source())
                                }
                            } catch (e: Exception) {
                                // If we read most of it, it might still be valid (EPUB is a ZIP)
                                if (destinationFile.length() > 0 && expectedSize > 0) {
                                    val percent = (destinationFile.length().toFloat() / expectedSize) * 100
                                    if (percent > 95) {
                                        Log.w(TAG, "Stream interrupted at $percent%, attempting to use partial file")
                                        return@use destinationFile
                                    }
                                }
                                throw e
                            }
                            destinationFile
                        } else null
                    } else null
                }
                
                if (result != null && result.exists() && result.length() > 0) {
                    Log.d(TAG, "Success! Final size: ${result.length()} bytes")
                    return@withContext result
                }
            } catch (e: Exception) {
                Log.e(TAG, "Stream failure for $url: ${e.message}")
                // If it failed but we have a substantial file, don't delete yet
                if (destinationFile.exists() && destinationFile.length() < 1000) {
                    destinationFile.delete()
                }
            }
        }
        null
    }
}
