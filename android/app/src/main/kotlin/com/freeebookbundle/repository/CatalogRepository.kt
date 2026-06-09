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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

data class CatalogEntry(
    val id: String,
    val t: String,
    val a: String,
    val s: List<String>,
    val d: Int = 0
)

private val audioBooksIds = setOf(
    "19504", "19505", "19506", "19508", "19513", "19514", "19515", "19516", "19517",
    "19572", "19573", "19574", "19575", "19576", "19577", "19578", "19580", "19581",
    "19582", "19583", "19616", "19617", "19618", "19619", "19620", "19621", "19622",
    "19623", "19624", "19625", "19626", "19627", "19631", "19633", "19678", "19679",
    "19682", "19683", "19684", "19685", "19686", "19782", "19783", "19784", "19785",
    "19787", "19788", "19789", "19790", "19791", "19792", "19793", "19794", "19795",
    "19796", "19797", "19836", "19837", "19838", "19839", "19840", "19841", "19842",
    "19843", "19844", "19845", "26458", "26459", "26460", "26461", "26462", "26463",
    "26464", "26465", "26466", "26467", "26468", "26469", "26470", "26471", "26573",
    "26574", "26575", "26576", "26577", "26578", "26579", "26580", "26581", "26582",
    "26583", "26584", "26585", "26586", "26587", "26588", "28731", "28732", "28733",
    "28734", "28737", "28738", "28739", "28753", "28754", "28755", "28756", "28762",
    "28781", "28782", "28792", "28793", "28794", "28795", "28797", "28798", "28799",
    "28801", "28806", "28807", "28808", "28809", "28810", "28811", "28836", "28837",
    "28838", "28839", "28840", "28907", "28908", "28909", "28911", "28912", "28913",
    "28916", "28918", "28919", "28920", "28939", "28940", "28941", "28942", "28943",
    "28944", "28945", "28946", "28947"
)

fun CatalogEntry.toBook(): Book {
    val hasAudio = audioBooksIds.contains(id)
    return Book(
        id = id,
        title = t,
        author = a,
        subjects = s,
        epubFileName = "$t.$id.epub",
        audioUrl = if (hasAudio) "http://54.254.205.11:8080/audiobooks/$id/" else null
    )
}

class CatalogRepository(
    private val context: Context,
    private val bookDao: BookDao
) {
    companion object {
        // Process-level singleton so multiple CatalogRepository instances
        // share one parsed catalog — avoids re-parsing on Activity recreate
        @Volatile private var processCache: List<CatalogEntry>? = null
        @Volatile private var processGenreIndex: Map<String, List<CatalogEntry>>? = null
        private val processMutex = Mutex()
        private val isLoading = AtomicBoolean(false)
    }

    suspend fun getCatalog(): List<CatalogEntry> {
        // Fast path — already cached, no lock needed
        processCache?.let { return it }

        // Slow path — parse once, all concurrent callers wait on the same mutex
        return processMutex.withLock {
            // Re-check inside lock — another coroutine may have finished while we waited
            processCache?.let { return@withLock it }

            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                val books = ArrayList<CatalogEntry>(80_000)
                // Pre-size the index map to avoid rehashing
                val index = HashMap<String, MutableList<CatalogEntry>>(2048)

                context.assets.open("catalog.json").use { inputStream ->
                    val reader = com.google.gson.stream.JsonReader(
                        InputStreamReader(inputStream, "UTF-8")
                    )
                    val gson = Gson()
                    val seenIds = HashSet<String>(50_000)

                    reader.beginArray()
                    while (reader.hasNext()) {
                        val entry: CatalogEntry = gson.fromJson(reader, CatalogEntry::class.java)
                        if (!seenIds.add(entry.id)) continue
                        books.add(entry)

                        entry.s.forEach { rawSubject ->
                            // Strip prefix inline — avoids building a temp string per subject
                            val name = when {
                                rawSubject.startsWith("Category:") ->
                                    rawSubject.removePrefix("Category:").trim()
                                rawSubject.startsWith("Bookshelf:") ->
                                    rawSubject.removePrefix("Bookshelf:").trim()
                                rawSubject.startsWith("Bookshelves:") ->
                                    rawSubject.removePrefix("Bookshelves:").trim()
                                else -> rawSubject
                            }
                            index.getOrPut(name) { ArrayList(64) }.add(entry)
                        }
                    }
                    reader.endArray()
                }

                android.util.Log.d("PerfTrace",
                    "Parsed ${books.size} catalog entries in ${System.currentTimeMillis() - startTime}ms")

                processCache = books
                processGenreIndex = index
                // Sort each genre list by popularity (downloads) descending
                index.forEach { (_, list) -> list.sortByDescending { it.d } }
                books
            }
        }
    }

    private suspend fun getGenreIndex(): Map<String, List<CatalogEntry>> {
        if (processGenreIndex == null) getCatalog()
        return processGenreIndex ?: emptyMap()
    }

    suspend fun getGenreCounts(): Map<String, Int> =
        getGenreIndex().mapValues { it.value.size }

    suspend fun getAuthorCounts(): Map<String, Int> =
        getCatalog().groupBy { it.a }.mapValues { it.value.size }

    suspend fun getBookDetails(id: String): Book? =
        getCatalog().find { it.id == id }?.toBook()

    suspend fun getStaticBooksByTopic(topic: String, limit: Int): List<Book> {
        val index = getGenreIndex()
        val match = index[topic]

        if (match != null) return match.take(limit).map { it.toBook() }

        val catalog = getCatalog()
        val filtered = catalog.filter {
            it.s.any { s -> s.contains(topic, ignoreCase = true) } ||
            it.t.contains(topic, ignoreCase = true)
        }
        return (if (filtered.isEmpty()) catalog.take(limit) else filtered.take(limit))
            .map { it.toBook() }
    }

    suspend fun enrichWithCachedMetadata(books: List<Book>): List<Book> {
        if (books.isEmpty()) return books
        val ids = books.map { it.id }
        val cached = bookDao.getCachedMetadata(ids)
        if (cached.isEmpty()) return books
        val cacheMap = cached.associateBy { it.id }
        return books.map { book ->
            val meta = cacheMap[book.id] ?: return@map book
            book.copy(
                description = meta.description,
                externalCoverUrl = meta.external_cover_url
            )
        }
    }

    // Filtered lists are now cached at the repository level, not inside the
    // PagingSource. PagingSource instances are short-lived and recreated on
    // invalidation — caching inside them is pointless.
    private val filterCache = HashMap<String, List<CatalogEntry>>(16)
    private val filterMutex = Mutex()

    private suspend fun getFiltered(query: String?, topic: String?): List<CatalogEntry> {
        val cacheKey = "q=$query|t=$topic"
        filterCache[cacheKey]?.let { return it }

        return filterMutex.withLock {
            filterCache[cacheKey]?.let { return@withLock it }
            val catalog = getCatalog()
            val start = System.currentTimeMillis()
            val result = when {
                query != null -> catalog.filter {
                    it.t.contains(query, ignoreCase = true) ||
                    it.a.contains(query, ignoreCase = true)
                }
                topic != null -> catalog.filter {
                    it.s.any { s -> s.contains(topic, ignoreCase = true) } ||
                    it.t.contains(topic, ignoreCase = true)
                }
                else -> catalog
            }
            android.util.Log.d("PerfTrace",
                "StaticCatalog filter took ${System.currentTimeMillis() - start}ms " +
                "for query=$query topic=$topic")
            filterCache[cacheKey] = result
            result
        }
    }

    fun searchBooks(query: String): Flow<PagingData<Book>> = Pager(
        config = PagingConfig(pageSize = 30, enablePlaceholders = false),
        pagingSourceFactory = { RepoCatalogPagingSource(query = query, topic = null) { q, t -> getFiltered(q, t) } }
    ).flow

    fun getBooksByAuthor(author: String): Flow<PagingData<Book>> = Pager(
        config = PagingConfig(pageSize = 30, enablePlaceholders = false),
        pagingSourceFactory = { RepoCatalogPagingSource(query = author, topic = null) { q, t -> getFiltered(q, t) } }
    ).flow

    suspend fun getBooksByTopic(topic: String): Flow<PagingData<Book>> = Pager(
        config = PagingConfig(pageSize = 30, enablePlaceholders = false),
        pagingSourceFactory = { RepoCatalogPagingSource(query = null, topic = topic) { q, t -> getFiltered(q, t) } }
    ).flow
}

// PagingSource is now thin — it delegates filtering to the repository cache
class RepoCatalogPagingSource(
    private val query: String?,
    private val topic: String?,
    private val filteredLoader: suspend (String?, String?) -> List<CatalogEntry>
) : PagingSource<Int, Book>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Book> {
        return try {
            val position = params.key ?: 0
            val filtered = filteredLoader(query, topic)
            val pageData = filtered.drop(position).take(params.loadSize).map { it.toBook() }
            android.util.Log.d("PerfTrace",
                "StaticCatalog loaded page at $position in ${filtered.size} results")
            LoadResult.Page(
                data = pageData,
                prevKey = if (position == 0) null else position - params.loadSize,
                nextKey = if (position + params.loadSize >= filtered.size) null
                          else position + params.loadSize
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Book>): Int? = state.anchorPosition
}
