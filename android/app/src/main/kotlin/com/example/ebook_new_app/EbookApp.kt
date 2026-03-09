package com.example.ebook_new_app

import android.app.Application
import com.example.ebook_new_app.db.AppDatabase
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

        val httpClient = DefaultHttpClient()
        assetRetriever = AssetRetriever(
            contentResolver = contentResolver,
            httpClient = httpClient
        )
        
        // In 3.1.2, DefaultPublicationParser needs context, httpClient, assetRetriever, and pdfFactory
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
