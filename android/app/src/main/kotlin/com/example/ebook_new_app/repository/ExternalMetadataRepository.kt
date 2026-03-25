package com.example.ebook_new_app.repository

import android.util.Log
import com.example.ebook_new_app.db.BookDao
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.utils.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class ExternalMetadataRepository(private val bookDao: BookDao) {
    private val client = OkHttpClient()
    private val TAG = "MetadataRepo"
    private val USER_AGENT = "EbookReaderApp/1.0"

    private fun isPlaceholderCover(url: String?): Boolean {
        if (url.isNullOrEmpty()) return true
        
        val lowerUrl = url.lowercase()
        
        val placeholderPatterns = listOf(
            "image not available",
            "no image",
            "no cover",
            "placeholder",
            "nopicture",
            "noplaceholder",
            "blank",
            "generic",
            "default",
            "default_cover",
            "default_book",
            "missing",
            "not_available",
            "printsec=frontcover&w=",
            "books/content?id=nopicture",
            "books/content?id=no",
            "g/generic",
            "/noCover",
            "/no_cover",
            "/missing",
            "/placeholder",
            "icon?",
            "1x1.gif",
            "spacer.gif",
            "pixel",
            "base64",
            "data:image"
        )
        
        if (placeholderPatterns.any { lowerUrl.contains(it) }) {
            return true
        }
        
        if (lowerUrl.contains("googleapis.com") || lowerUrl.contains("books.google.com")) {
            if (lowerUrl.contains("printsec=frontcover") && lowerUrl.contains("img=1")) {
                return true
            }
        }
        
        if (lowerUrl.contains("openlibrary.org") || lowerUrl.contains("covers.openlibrary.org")) {
            if (lowerUrl.contains("/b/id/")) {
                val idPattern = Regex("/b/id/(-?\\d+)").find(lowerUrl)
                val coverId = idPattern?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                if (coverId <= 0) return true
            }
            if (lowerUrl.contains("/b/cover/")) return true
        }
        
        return false
    }

    private fun getServerCoverUrl(bookId: String): String {
        return "${ServerConfig.getCoversUrl()}/${bookId}_cover_image.jpg"
    }

    suspend fun fetchAndCacheMetadata(book: Book): Book = withContext(Dispatchers.IO) {
        Log.d(TAG, "Syncing: ${book.title}")

        var updatedBook = fetchFromGoogleBooks(book)
        
        if (updatedBook.description.contains("A classic text by")) {
            updatedBook = fetchFromOpenLibrary(updatedBook)
        }

        if (updatedBook.description != book.description || 
            updatedBook.externalCoverUrl != book.externalCoverUrl ||
            updatedBook.subjects != book.subjects) {
            
            val coverToStore = if (isPlaceholderCover(updatedBook.externalCoverUrl)) {
                null
            } else {
                updatedBook.externalCoverUrl
            }
            
            bookDao.updateBookMetadata(
                bookId = book.id,
                description = updatedBook.description,
                subjects = updatedBook.subjects,
                externalCoverUrl = coverToStore
            )
            
            val finalBook = if (isPlaceholderCover(updatedBook.externalCoverUrl)) {
                updatedBook.copy(externalCoverUrl = null)
            } else {
                updatedBook
            }
            return@withContext finalBook
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
                
                var coverUrl: String? = null
                if (info.has("imageLinks")) {
                    val images = info.getJSONObject("imageLinks")
                    val keys = listOf("extraLarge", "large", "medium", "small", "thumbnail")
                    for (key in keys) {
                        if (images.has(key)) {
                            coverUrl = images.getString(key)
                            break
                        }
                    }
                    
                    coverUrl = coverUrl?.replace("http://", "https://")
                        ?.replace("zoom=1", "zoom=0")
                        ?.replace("zoom=5", "zoom=0")
                        ?.replace("&edge=curl", "")
                }

                val desc = if (info.has("description")) info.getString("description") else book.description
                val apiSubjects = mutableListOf<String>()
                if (info.has("categories")) {
                    val cats = info.getJSONArray("categories")
                    for (i in 0 until cats.length()) apiSubjects.add(cats.getString(i))
                }

                val mergedSubjects = (book.subjects + apiSubjects).distinct()

                return book.copy(
                    description = cleanDescription(desc), 
                    subjects = mergedSubjects,
                    externalCoverUrl = coverUrl
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
                
                var olCover: String? = book.externalCoverUrl
                if (doc.has("cover_i")) {
                    val coverId = doc.getInt("cover_i")
                    if (coverId > 0) {
                        olCover = "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
                    }
                }

                val olSubjects = mutableListOf<String>()
                if (doc.has("subject")) {
                    val subj = doc.getJSONArray("subject")
                    for (i in 0 until minOf(subj.length(), 10)) {
                        olSubjects.add(subj.getString(i))
                    }
                }
                val mergedSubjects = (book.subjects + olSubjects).distinct()

                return book.copy(
                    description = cleanDescription(desc),
                    subjects = mergedSubjects,
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
