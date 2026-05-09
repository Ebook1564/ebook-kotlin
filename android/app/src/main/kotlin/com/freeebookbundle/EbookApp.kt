package com.freeebookbundle

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import android.webkit.WebView
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.freeebookbundle.db.AppDatabase
import com.freeebookbundle.utils.Analytics
import com.freeebookbundle.utils.GenrePreferences
import com.freeebookbundle.utils.LocaleManager
import com.freeebookbundle.utils.ReviewManager
import com.freeebookbundle.repository.CatalogRepository
import com.freeebookbundle.utils.ServerConfig
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.google.firebase.messaging.FirebaseMessaging
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

class EbookApp : Application() {

    companion object {
        private const val TAG = "EbookApp"
    }

    // Lazy so Readium init happens on first actual use, not at app startup
    val assetRetriever: AssetRetriever by lazy {
        AssetRetriever(
            contentResolver = contentResolver,
            httpClient = DefaultHttpClient()
        )
    }

    val publicationOpener: PublicationOpener by lazy {
        PublicationOpener(
            publicationParser = DefaultPublicationParser(
                context = this,
                httpClient = DefaultHttpClient(),
                assetRetriever = assetRetriever,
                pdfFactory = null
            )
        )
    }

    val database by lazy { AppDatabase.getDatabase(this) }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        // Apply locale synchronously — affects UI immediately, must be on main thread
        LocaleManager.applySavedLocale(this)
        ServerConfig.initialize(this)
        GenrePreferences.initialize(this)

        // Everything else goes to background threads
        GlobalScope.launch(Dispatchers.IO) {

            // 1. Build and WARM Coil's disk cache on IO thread.
            // DiskLruCache.initialize() is lazy — it runs on first actual cache access.
            // By touching it here on IO we guarantee it's ready before any image loads.
            val imageLoader = ImageLoader.Builder(this@EbookApp)
                .memoryCache {
                    MemoryCache.Builder(this@EbookApp)
                        .maxSizePercent(0.15) // 15% of available memory
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.02)
                        .build()
                }
                .respectCacheHeaders(false)
                .build()

            // Force DiskLruCache.initialize() to run NOW on this IO thread,
            // so it never blocks the main thread or an image-loading thread later
            runCatching {
                imageLoader.diskCache?.run {
                    openSnapshot("__warmup__")?.close()
                }
            }

            Coil.setImageLoader(imageLoader)

            // 2. Catalog preload
            try {
                val bookDao = database.bookDao()
                val count = bookDao.getBookCount()
                if (count == 0) {
                    Log.d(TAG, "Database empty, preloading catalog...")
                    val catalogRepo = CatalogRepository(this@EbookApp, bookDao)
                    val catalog = catalogRepo.getCatalog()

                    val entities = catalog.map { entry ->
                        var rawCat = entry.s.firstOrNull { it.startsWith("Category:") }
                            ?: entry.s.firstOrNull() ?: "Uncategorized"
                        for (prefix in listOf("Category:", "Bookshelf:", "Bookshelves:")) {
                            if (rawCat.startsWith(prefix)) {
                                rawCat = rawCat.removePrefix(prefix).trim()
                            }
                        }
                        val code = rawCat.lowercase()
                            .replace(Regex("[^a-z0-9]+"), "_").trim('_')

                        com.freeebookbundle.db.BookEntity(
                            id = entry.id,
                            title = entry.t,
                            author = entry.a,
                            description = "",
                            categoryCode = code,
                            sourceFolder = "",
                            coverImageName = "${entry.id}_cover_image.jpg",
                            epubFileName = "${entry.t}.${entry.id}.epub",
                            subjects = entry.s,
                            dateAdded = System.currentTimeMillis()
                        )
                    }
                    
                    // Batch insert to avoid massive DB lock and Daveys
                    entities.chunked(500).forEach { batch ->
                        bookDao.insertBooks(batch)
                    }
                    Log.d(TAG, "Preloaded ${entities.size} books from catalog.json in chunks")
                } else {
                    Log.d(TAG, "Database already has $count books, skipping preload")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload catalog", e)
            }
        }

        // Firebase on main thread (required by SDK) but async token fetch
        GlobalScope.launch(Dispatchers.Main) {
            initializeFirebase()
        }

        // Analytics + Review off main thread
        GlobalScope.launch(Dispatchers.IO) {
            Analytics.initialize(this@EbookApp)
            ReviewManager.initialize(this@EbookApp)
            com.freeebookbundle.utils.RemoteConfigManager.initializeAndFetchConfig()
        }

        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "FCM token retrieval failed", task.exception)
                    return@addOnCompleteListener
                }
                Log.d(TAG, "FCM token: ${task.result}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed", e)
        }
    }
}
