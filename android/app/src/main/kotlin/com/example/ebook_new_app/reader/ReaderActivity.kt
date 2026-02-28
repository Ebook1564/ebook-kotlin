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
        // Only let the activity's gesture detector handle the touch if we are NOT in scroll mode
        // Or handle it in a way that doesn't block vertical scrolling.
        // For a reading app, it's often better to let the Fragment/Navigator handle most touches.
        
        val fragment = supportFragmentManager.findFragmentById(R.id.reader_container) as? ReaderFragment
        val isScrollMode = fragment?.isScrollMode() ?: false
        
        if (!isScrollMode) {
            gestureDetector.onTouchEvent(ev)
        } else {
            // In scroll mode, we still want to detect taps to toggle bars
            // but we must be careful not to intercept scrolls.
            // Simple approach: Only detect single taps.
            if (ev.action == MotionEvent.ACTION_UP) {
                // We might need a more refined check here if we want to support both.
                // For now, let's just use the gesture detector but return super.
                gestureDetector.onTouchEvent(ev)
            }
        }
        
        return super.dispatchTouchEvent(ev)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            ReaderRepository.currentPublication = null
        }
    }
}
