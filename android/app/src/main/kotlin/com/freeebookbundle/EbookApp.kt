package com.freeebookbundle

import android.app.Application
import com.freeebookbundle.db.AppDatabase
import com.freeebookbundle.utils.Analytics
import com.freeebookbundle.utils.GenrePreferences
import com.freeebookbundle.utils.LocaleManager
import com.freeebookbundle.utils.ReviewManager
import com.freeebookbundle.utils.ServerConfig
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

class EbookApp : Application() {

    lateinit var assetRetriever: AssetRetriever
    lateinit var publicationOpener: PublicationOpener
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        LocaleManager.applySavedLocale(this)
        ServerConfig.initialize(this)

        // Initialize Analytics
        Analytics.initialize(this)

        // Initialize Review Manager
        ReviewManager.initialize(this)

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
    }
}
