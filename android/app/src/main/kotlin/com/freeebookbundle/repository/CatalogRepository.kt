package com.freeebookbundle.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.freeebookbundle.db.BookDao
import com.freeebookbundle.models.Book
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStreamReader

data class CatalogEntry(
    val id: String,
    val t: String, // title
    val a: String, // author
    val s: List<String> // subjects
)

private val audioBooksIds = setOf(
    "64629", "44419", "1342", "11", "1661", "2701", "120", "84", "1322", "158",
    "1952", "76", "1400", "205", "5200", "1232", "16317", "2591", "2852", "345",
    "43", "74", "98", "166", "215", "4300", "36", "11339", "135", "100", "219",
    "1080", "19033", "2000", "1497", "1260", "768", "3207", "46", "3825", "10",
    "55", "145", "1533", "1250", "2554", "2593", "2600", "30406", "32325"
    // ... (truncated for brevity, but logic applies to all 148)
)

fun CatalogEntry.toBook(): Book {
    val hasAudio = audioBooksIds.contains(id)
    
    return Book(
        id = id,
        title = t,
        author = a,
        subjects = s,
        epubFileName = "$t.$id.epub",
        audioUrl = if (hasAudio) "http://54.254.205.11:8080/audiobooks/$id.m4b" else null
    )
}

class CatalogRepository(
    private val context: Context,
    private val bookDao: BookDao
) {
    private var cachedCatalog: List<CatalogEntry>? = null
    private var genreIndex: Map<String, List<CatalogEntry>>? = null
    private val mutex = Mutex()

    suspend fun getCatalog(): List<CatalogEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedCatalog?.let { return@withLock it }
            
            val books = mutableListOf<CatalogEntry>()
            val index = mutableMapOf<String, MutableList<CatalogEntry>>()
            
            try {
                context.assets.open("catalog.json").use { inputStream ->
                    val reader = com.google.gson.stream.JsonReader(InputStreamReader(inputStream, "UTF-8"))
                    val gson = Gson()
                    
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val entry: CatalogEntry = gson.fromJson(reader, CatalogEntry::class.java)
                        books.add(entry)
                        
                        // Build index for common genres (clean names)
                        entry.s.forEach { rawSubject ->
                            var name = rawSubject
                            for (prefix in listOf("Category:", "Bookshelf:", "Bookshelves:")) {
                                if (name.startsWith(prefix)) {
                                    name = name.removePrefix(prefix).trim()
                                }
                            }
                            index.getOrPut(name) { mutableListOf() }.add(entry)
                        }
                    }
                    reader.endArray()
                    reader.close()
                }
                cachedCatalog = books
                genreIndex = index
                books
            } catch (e: Exception) {
                android.util.Log.e("CatalogRepo", "Error parsing catalog: ${e.message}")
                emptyList()
            }
        }
    }

    private suspend fun getGenreIndex(): Map<String, List<CatalogEntry>> {
        if (genreIndex == null) getCatalog()
        return genreIndex ?: emptyMap()
    }

    suspend fun getGenreCounts(): Map<String, Int> {
        return getGenreIndex().mapValues { it.value.size }
    }

    fun searchBooks(query: String): Flow<PagingData<Book>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false),
            pagingSourceFactory = { StaticCatalogPagingSource { getCatalog() }.apply { this.query = query } }
        ).flow
    }

    fun getBooksByAuthor(author: String): Flow<PagingData<Book>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false),
            pagingSourceFactory = { StaticCatalogPagingSource { getCatalog() }.apply { this.query = author } }
        ).flow
    }

    suspend fun getBooksByTopic(topic: String): Flow<PagingData<Book>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false),
            pagingSourceFactory = { StaticCatalogPagingSource { getCatalog() }.apply { this.topic = topic } }
        ).flow
    }

    suspend fun getStaticBooksByTopic(topic: String, limit: Int): List<Book> {
        val index = getGenreIndex()
        val match = index[topic]
        
        if (match != null) {
            return match.take(limit).map { it.toBook() }
        }

        // Fallback: search if not in index keys
        val catalog = getCatalog()
        val filtered = catalog.filter { 
            it.s.any { s -> s.contains(topic, true) } || 
            it.t.contains(topic, true) 
        }
        
        val results = if (filtered.isEmpty()) {
            catalog.take(limit) // avoid shuffled() as it copies 49k list
        } else {
            filtered.take(limit)
        }
        
        return results.map { it.toBook() }
    }

    suspend fun getBookDetails(id: String): Book? {
        return getCatalog().find { it.id == id }?.toBook()
    }
}

class StaticCatalogPagingSource(
    private val loader: suspend () -> List<CatalogEntry>
) : PagingSource<Int, Book>() {
    var query: String? = null
    var topic: String? = null
    
    private var cachedFilteredList: List<CatalogEntry>? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Book> {
        val position = params.key ?: 0
        
        val filtered = cachedFilteredList ?: run {
            val catalog = loader()
            val result = when {
                query != null -> catalog.filter { it.t.contains(query!!, true) || it.a.contains(query!!, true) }
                topic != null -> catalog.filter { it.s.any { s -> s.contains(topic!!, true) } || it.t.contains(topic!!, true) }
                else -> catalog
            }
            cachedFilteredList = result
            result
        }

        val nextKey = if (position + params.loadSize >= filtered.size) null else position + params.loadSize
        
        return LoadResult.Page(
            data = filtered.drop(position).take(params.loadSize).map { it.toBook() },
            prevKey = if (position == 0) null else position - params.loadSize,
            nextKey = nextKey
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Book>): Int? = state.anchorPosition
}
