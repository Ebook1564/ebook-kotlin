package com.example.ebook_new_app.reader

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import com.example.ebook_new_app.EbookApp
import com.example.ebook_new_app.R
import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.repository.ReaderSettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.r2.shared.util.toUrl
import java.io.File

class ReaderActivity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetector
    private var lastTapTime = 0L
    private var isReadingContent = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val settingsRepo = ReaderSettingsRepository(this)
        val themeColor = settingsRepo.getThemeColor()
        window.setBackgroundDrawable(ColorDrawable(themeColor.toArgb()))
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_reader)

        val filePath = intent.getStringExtra("FILE_PATH") ?: return
        val book = intent.getParcelableExtra<Book>("BOOK") ?: return
        val openAudioPlayer = intent.getBooleanExtra("OPEN_AUDIO_PLAYER", false)
        val file = File(filePath)
        if (!file.exists()) return

        val app = application as EbookApp
        val url = file.toUrl()
        
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val fragment = supportFragmentManager.findFragmentById(R.id.reader_container) as? ReaderFragment
                fragment?.toggleBars()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val fragment = supportFragmentManager.findFragmentById(R.id.reader_container) as? ReaderFragment
                fragment?.toggleBars()
                return true
            }
        })

        lifecycleScope.launch {
            try {
                val asset = app.assetRetriever.retrieve(url).getOrNull() ?: return@launch
                val publication = app.publicationOpener.open(asset, allowUserInteraction = true).getOrNull() ?: return@launch
                ReaderRepository.currentPublication = publication
                ReaderRepository.currentBook = book
                
                val readerFragment = ReaderFragment().apply {
                    arguments = Bundle().apply {
                        putString("BOOK_ID", book.id)
                        putBoolean("OPEN_AUDIO_PLAYER", openAudioPlayer)
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
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            ReaderRepository.currentPublication = null
            ReaderRepository.currentBook = null
        }
    }
}
