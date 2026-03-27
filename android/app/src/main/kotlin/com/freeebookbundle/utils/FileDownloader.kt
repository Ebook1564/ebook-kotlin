package com.freeebookbundle.utils

import android.content.Context
import android.util.Log
import com.freeebookbundle.models.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import okio.buffer
import okio.sink
import java.net.URLEncoder

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

    private suspend fun downloadFile(url: String, destinationFile: File, attemptLabel: String = ""): File? {
        return try {
            Log.d(TAG, "Downloading [$attemptLabel]: $url")
            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val isSuccess = response.isSuccessful
            val statusCode = response.code
            
            if (!isSuccess) {
                Log.w(TAG, "Download failed [$attemptLabel] - Status: $statusCode for URL: $url")
                response.close()
                return null
            }
            
            val body = response.body
            if (body == null) {
                Log.w(TAG, "Empty response body [$attemptLabel] for URL: $url")
                response.close()
                return null
            }
            
            destinationFile.sink().buffer().use { sink ->
                sink.writeAll(body.source())
            }
            response.close()
            
            if (destinationFile.exists() && destinationFile.length() > 5000) {
                Log.d(TAG, "Successfully downloaded [$attemptLabel]: ${destinationFile.name} (${destinationFile.length()} bytes)")
                destinationFile
            } else {
                Log.w(TAG, "Downloaded file too small or empty [$attemptLabel]: ${destinationFile.length()} bytes")
                destinationFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error [$attemptLabel] for $url: ${e.message}")
            if (destinationFile.exists()) destinationFile.delete()
            null
        }
    }

    private fun encodeFilenameForUrl(filename: String, usePlusForSpaces: Boolean = false): String {
        return if (usePlusForSpaces) {
            URLEncoder.encode(filename, "UTF-8")
        } else {
            android.net.Uri.encode(filename)
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

        Log.d(TAG, "Book ${book.id}: '${book.title}' - epubFileName: '${book.epubFileName}'")

        // --- 1. ASSET RECOVERY ---
        val assetNamesToTry = listOfNotNull(book.epubFileName, "$cleanId.epub")
        for (assetName in assetNamesToTry.distinct()) {
            try {
                Log.d(TAG, "Trying asset: $assetName")
                context.assets.open("epubs/$assetName").use { input ->
                    destinationFile.outputStream().use { output -> input.copyTo(output) }
                }
                Log.d(TAG, "Asset recovery successful: $assetName")
                return@withContext destinationFile
            } catch (e: Exception) {
                Log.d(TAG, "Asset not found: $assetName")
            }
        }

        // --- 2. NETWORK DOWNLOAD ---
        
        // Try 2a: URL-encoded book.epubFileName (from bundled library.db)
        if (!book.epubFileName.isNullOrEmpty()) {
            val encodedName1 = encodeFilenameForUrl(book.epubFileName, usePlusForSpaces = false)
            Log.d(TAG, "Trying URL-encoded (Uri.encode): $encodedName1")
            val result1 = downloadFile("$serverUrl/epubs/$encodedName1", destinationFile, "Uri.encode")
            if (result1 != null) return@withContext result1
            
            // Try 2b: URL-encoded filename (URLEncoder with + for spaces)
            val encodedName2 = encodeFilenameForUrl(book.epubFileName, usePlusForSpaces = true)
            Log.d(TAG, "Trying URL-encoded (URLEncoder): $encodedName2")
            val result2 = downloadFile("$serverUrl/epubs/$encodedName2", destinationFile, "URLEncoder")
            if (result2 != null) return@withContext result2
            
            // Try 2c: Raw filename (some servers handle this)
            Log.d(TAG, "Trying raw filename: ${book.epubFileName}")
            val result3 = downloadFile("$serverUrl/epubs/${book.epubFileName}", destinationFile, "raw")
            if (result3 != null) return@withContext result3
        }
        
        // Try 3: Just the book ID as filename
        Log.d(TAG, "Trying with book ID only: ${book.id}.epub")
        val result4 = downloadFile("$serverUrl/epubs/${book.id}.epub", destinationFile, "bookId")
        if (result4 != null) return@withContext result4
        
        Log.e(TAG, "All download attempts failed for book ${book.id}")
        null
    }
}
