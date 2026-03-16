package com.example.ebook_new_app.repository

import android.util.Log
import com.example.ebook_new_app.db.BookDao
import com.example.ebook_new_app.models.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class ExternalMetadataRepository(private val bookDao: BookDao) {
    private val client = OkHttpClient()
    private val TAG = "MetadataRepo"
    private val USER_AGENT = "EbookVerseApp/1.0 (contact: aayush@example.com)"

    suspend fun fetchAndCacheMetadata(book: Book): Book = withContext(Dispatchers.IO) {
        // Log starting
        Log.d(TAG, "Syncing: ${book.title}")

        // 1. Fetch from Google Books
        var updatedBook = fetchFromGoogleBooks(book)
        
        // 2. Fallback to OpenLibrary if description still missing
        if (updatedBook.description.contains("A classic text by")) {
            updatedBook = fetchFromOpenLibrary(updatedBook)
        }

        // 3. Persist to DB if we found ANYTHING new
        if (updatedBook.description != book.description || 
            updatedBook.externalCoverUrl != book.externalCoverUrl ||
            updatedBook.subjects != book.subjects) {
            
            bookDao.updateBookMetadata(
                bookId = book.id,
                description = updatedBook.description,
                subjects = updatedBook.subjects,
                externalCoverUrl = updatedBook.externalCoverUrl
            )
            return@withContext updatedBook
        }
        
        return@withContext book
    }

    private fun fetchFromGoogleBooks(book: Book): Book {
        try {
            val query = URLEncoder.encode("intitle:\"${book.title}\" inauthor:\"${book.author}\"", "UTF-8")
            val url = "https://www.googleapis.com/books/v1/volumes?q=$query&maxResults=1"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
                
            val response = client.newCall(request).execute()
            val jsonStr = response.body?.string() ?: ""
            val jsonObj = JSONObject(jsonStr)
            
            if (jsonObj.has("items")) {
                val item = jsonObj.getJSONArray("items").getJSONObject(0)
                val info = item.getJSONObject("volumeInfo")
                
                // DEEP IMAGE EXTRACTION
                var highResUrl: String? = null
                if (info.has("imageLinks")) {
                    val images = info.getJSONObject("imageLinks")
                    // Priority list for Google Books
                    val keys = listOf("extraLarge", "large", "medium", "small", "thumbnail")
                    for (key in keys) {
                        if (images.has(key)) {
                            highResUrl = images.getString(key)
                            break
                        }
                    }
                    
                    // High-Res Transformation
                    highResUrl = highResUrl?.replace("http://", "https://")
                        ?.replace("zoom=1", "zoom=0")
                        ?.replace("zoom=5", "zoom=0")
                        ?.replace("&edge=curl", "")
                }

                val desc = if (info.has("description")) info.getString("description") else book.description
                val subjects = mutableListOf<String>()
                if (info.has("categories")) {
                    val cats = info.getJSONArray("categories")
                    for (i in 0 until cats.length()) subjects.add(cats.getString(i))
                }

                return book.copy(
                    description = cleanDescription(desc), 
                    subjects = if (subjects.isNotEmpty()) subjects else book.subjects,
                    externalCoverUrl = highResUrl ?: book.externalCoverUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Error for ${book.title}: ${e.message}")
        }
        return book
    }

    private fun fetchFromOpenLibrary(book: Book): Book {
        try {
            val query = URLEncoder.encode("${book.title} ${book.author}", "UTF-8")
            val url = "https://openlibrary.org/search.json?q=$query&limit=1"
            val response = client.newCall(Request.Builder().url(url).header("User-Agent", USER_AGENT).build()).execute()
            val docs = JSONObject(response.body?.string() ?: "").getJSONArray("docs")
            
            if (docs.length() > 0) {
                val doc = docs.getJSONObject(0)
                var desc = book.description
                if (doc.has("first_sentence")) {
                    desc = doc.getJSONArray("first_sentence").getString(0)
                }
                
                // OpenLibrary Cover Fetch if Google failed
                var olCover = book.externalCoverUrl
                if (olCover.isNullOrEmpty() && doc.has("cover_i")) {
                    olCover = "https://covers.openlibrary.org/b/id/${doc.getInt("cover_i")}-L.jpg"
                }

                return book.copy(
                    description = cleanDescription(desc),
                    externalCoverUrl = olCover
                )
            }
        } catch (e: Exception) { }
        return book
    }

    private fun cleanDescription(desc: String): String {
        return desc.replace(Regex("<.*?>"), "") 
                   .replace(Regex("\\[.*?\\]"), "")
                   .replace(Regex("\\(.*?\\)"), "")
                   .trim()
    }
}
