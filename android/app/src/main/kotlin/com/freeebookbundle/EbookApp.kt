package com.freeebookbundle

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import android.webkit.WebView
import com.freeebookbundle.db.AppDatabase
import com.freeebookbundle.utils.Analytics
import com.freeebookbundle.utils.GenrePreferences
import com.freeebookbundle.utils.LocaleManager
import com.freeebookbundle.utils.ReviewManager
import com.freeebookbundle.repository.CatalogRepository
import com.freeebookbundle.utils.ServerConfig
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.google.firebase.messaging.FirebaseMessaging
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

class EbookApp : Application() {

    companion object {
        private const val TAG = "EbookApp"
    }

    lateinit var assetRetriever: AssetRetriever
    lateinit var publicationOpener: PublicationOpener
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        LocaleManager.applySavedLocale(this)
        ServerConfig.initialize(this)

        // Initialize Firebase
        initializeFirebase()

        // Initialize Analytics
        Analytics.initialize(this)

        // Initialize Review Manager
        ReviewManager.initialize(this)

        // Initialize and fetch Remote Config
        com.freeebookbundle.utils.RemoteConfigManager.initializeAndFetchConfig()

        // Initialize Genre Preferences
        GenrePreferences.initialize(this)

        val httpClient = DefaultHttpClient()
        assetRetriever = AssetRetriever(
            contentResolver = contentResolver,
            httpClient = httpClient
        )
        
        val publicationParser = DefaultPublicationParser(
            context = this,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null
        )
        
        publicationOpener = PublicationOpener(
            publicationParser = publicationParser
        )

        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val bookDao = database.bookDao()
                val count = bookDao.getBookCount()
                if (count == 0) {
                    Log.d(TAG, "Database empty, preloading catalog...")
                    val catalogRepo = CatalogRepository(this@EbookApp, bookDao)
                    val catalog = catalogRepo.getCatalog()
                    // Pre-convert to entities and insert in background
                    // This prevents jank when user first searches
                    Log.d(TAG, "Preloaded ${catalog.size} books from catalog.json")
                } else {
                    Log.d(TAG, "Database already has $count books, skipping preload")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload catalog in background", e)
            }
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
                val token = task.result
                Log.d(TAG, "FCM token: $token")
                // Send token to server if needed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed", e)
        }
    }
}
