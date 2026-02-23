package com.example.ebook_new_app.reader

import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.lifecycleScope
import com.example.ebook_new_app.EbookApp
import com.example.ebook_new_app.R
import kotlinx.coroutines.launch
import org.readium.r2.shared.util.toUrl
import java.io.File

class ReaderActivity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reader)

        val filePath = intent.getStringExtra("FILE_PATH") ?: return
        val bookId = intent.getStringExtra("BOOK_ID") ?: "unknown"
        val file = File(filePath)
        if (!file.exists()) return

        val app = application as EbookApp
        val url = file.toUrl()
        
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                Log.d("ReaderActivity", "onSingleTapUp detected")
                val fragment = supportFragmentManager.findFragmentById(R.id.reader_container) as? ReaderFragment
                if (fragment == null) {
                    Log.e("ReaderActivity", "ReaderFragment not found in reader_container")
                }
                fragment?.toggleBars()
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true // Must return true to receive subsequent events
            }
        })

        lifecycleScope.launch {
            try {
                val asset = app.assetRetriever.retrieve(url).getOrNull() ?: return@launch
                val publication = app.publicationOpener.open(asset, allowUserInteraction = true).getOrNull() ?: return@launch
                ReaderRepository.currentPublication = publication
                
                val readerFragment = ReaderFragment().apply {
                    arguments = Bundle().apply {
                        putString("BOOK_ID", bookId)
                    }
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.reader_container, readerFragment)
                    .commit()
            } catch (e: Exception) {
                e.printStackTrace()
                finish()
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        Log.d("ReaderActivity", "dispatchTouchEvent: action=${ev.action} x=${ev.x} y=${ev.y}")
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            ReaderRepository.currentPublication = null
        }
    }
}
