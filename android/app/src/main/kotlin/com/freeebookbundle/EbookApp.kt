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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
    val catalogRepo by lazy { CatalogRepository(this, database.bookDao()) }

    // Custom scope for application-level initialization tasks
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        // Apply locale synchronously — affects UI immediately, must be on main thread
        LocaleManager.applySavedLocale(this)
        ServerConfig.initialize(this)
        GenrePreferences.initialize(this)

        // Initializations on IO thread
        applicationScope.launch(Dispatchers.IO) {

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

            // 2. Verify database is populated (library.db is pre-built via createFromAsset)
            try {
                val bookDao = database.bookDao()
                val count = bookDao.getBookCount()
                Log.d(TAG, "Database has $count books (pre-built from asset)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to verify database", e)
            }

            // 3. Warm catalog cache in background so home screen sections load instantly
            try {
                val startTime = System.currentTimeMillis()
                catalogRepo.getCatalog()
                Log.d(TAG, "Catalog warmed in ${System.currentTimeMillis() - startTime}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to warm catalog cache", e)
            }
        }

        // Firebase on main thread (required by SDK) but async token fetch
        applicationScope.launch(Dispatchers.Main) {
            initializeFirebase()
        }

        // Analytics + Review off main thread
        applicationScope.launch(Dispatchers.IO) {
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
