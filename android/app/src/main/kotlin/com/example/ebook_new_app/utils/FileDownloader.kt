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
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()

    private fun getServerUrl(): String = ServerConfig.getServerUrl()

    private fun normalizeFilenameForServer(filename: String): String {
        // Server uses %EF%80%BA for colon - replace : with percent-encoded version
        // This matches Project Gutenberg's filename encoding on EC2
        return filename
            .replace(":", "%EF%80%BA")
            .replace("?", "%EF%80%BF")
    }

    private suspend fun downloadFile(url: String, destinationFile: File): File? {
        return try {
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
                result
            } else {
                destinationFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $url: ${e.message}")
            if (destinationFile.exists()) destinationFile.delete()
            null
        }
    }

    suspend fun ensureCached(context: Context, book: Book): File? = withContext(Dispatchers.IO) {
        val serverUrl = getServerUrl()
        val cleanId = book.id.replace(Regex("[^a-zA-Z0-9]"), "")
        val localFileName = "book_${cleanId}.epub"
        val destinationFile = File(context.filesDir, "epubs/$localFileName")
        
        val parent = destinationFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        if (destinationFile.exists() && destinationFile.length() > 5000) {
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
        
        // Strategy A: Use exact stored filename
        if (!book.epubFileName.isNullOrEmpty()) {
            // Try with standard URL encoding first
            val encodedName = android.net.Uri.encode(book.epubFileName, "-_.~:/?#[]@!$&'()*+,;=")
            var result = downloadFile("$serverUrl/epubs/$encodedName", destinationFile)
            if (result != null) return@withContext result
            
            // Try with original filename (no encoding)
            result = downloadFile("$serverUrl/epubs/${book.epubFileName}", destinationFile)
            if (result != null) return@withContext result
            
            // Try with Unicode colon replacement (Project Gutenberg uses U+F00A for colons)
            val unicodeFilename = normalizeFilenameForServer(book.epubFileName)
            if (unicodeFilename != book.epubFileName) {
                Log.d(TAG, "Trying Unicode filename: $unicodeFilename")
                val unicodeEncoded = android.net.Uri.encode(unicodeFilename)
                result = downloadFile("$serverUrl/epubs/$unicodeEncoded", destinationFile)
                if (result != null) return@withContext result
            }
        }
        
        // Strategy B: Try full book ID
        val result2 = downloadFile("$serverUrl/epubs/${book.id}.epub", destinationFile)
        if (result2 != null) return@withContext result2
        
        null
    }
}
