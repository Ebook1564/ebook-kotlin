package com.example.ebook_new_app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

class EbookApp : Application() {

    lateinit var assetRetriever: AssetRetriever
    lateinit var publicationOpener: PublicationOpener

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        signInAnonymously()

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

    private fun signInAnonymously() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d("EbookApp", "Firebase: Signed in anonymously")
                }
                .addOnFailureListener { e ->
                    Log.e("EbookApp", "Firebase: Anonymous sign-in failed", e)
                }
        } else {
            Log.d("EbookApp", "Firebase: Already signed in as ${auth.currentUser?.uid}")
        }
    }
}
