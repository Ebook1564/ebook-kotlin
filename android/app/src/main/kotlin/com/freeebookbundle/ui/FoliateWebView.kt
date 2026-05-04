package com.freeebookbundle.ui

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.freeebookbundle.utils.FoliateBridge
import com.freeebookbundle.utils.RelocateEvent
import com.freeebookbundle.utils.SelectionEvent

private const val TAG = "FoliateWebView"

@Composable
fun FoliateWebView(
    bookUrl: String,
    readerUrl: String? = null,
    initialChapterIndex: Int = 0,
    initialAnchor: String? = null,
    scrollModeEnabled: Boolean = true,
    theme: ReaderTheme = ReaderTheme.LIGHT,
    onProgressChange: (chapterIndex: Int, chapterProgress: Float) -> Unit = { _, _ -> },
    onSelection: (text: String, locator: String) -> Unit = { _, _ -> },
    onBookLoaded: (title: String, author: String, direction: String) -> Unit = { _, _, _ -> },
    onError: (String) -> Unit = { },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentChapterIndex by remember { mutableIntStateOf(initialChapterIndex) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var showNewReaderBanner by remember { mutableStateOf(true) }
    
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val bridge = remember(webViewInstance) {
        webViewInstance?.let { wv ->
            FoliateBridge(wv).apply {
                this.onRelocate = { event ->
                    Log.d(TAG, "Relocate: chapter=${event.index}, fraction=${event.fraction}")
                    currentChapterIndex = event.index
                    currentProgress = event.fraction
                    onProgressChange(event.index, event.fraction)
                }
                
                this.onSelection = { event ->
                    Log.d(TAG, "Selection: ${event.text}")
                    onSelection(event.text, event.cfi)
                }
                
                this.onBookLoaded = { metadata ->
                    Log.d(TAG, "Book loaded: ${metadata.title} by ${metadata.author}")
                    onBookLoaded(metadata.title, metadata.author, metadata.direction)
                }
                
                this.onError = { error ->
                    Log.e(TAG, "Error: $error")
                    onError(error)
                }
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewInstance = this
                    // bridge is not ready yet here, will be set in LaunchedEffect or via re-composition
                }
            },
            update = { wv ->
                if (bridge != null && wv.tag != "setup_done") {
                    wv.setupWebView(bridge, readerUrl)
                    wv.tag = "setup_done"
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        LaunchedEffect(bridge, bookUrl) {
            if (bridge == null) return@LaunchedEffect
            
            // Wait for the reader to be ready
            delay(1500) 
            
            // If we didn't provide readerUrl (or it didn't have epub param), we call openBook
            if (readerUrl == null || !readerUrl.contains("epub=")) {
                Log.d(TAG, "Opening book via bridge: $bookUrl")
                bridge.openBook(bookUrl)
                // Wait for book to load after openBook
                delay(2000)
            } else {
                Log.d(TAG, "Book should be loading via readerUrl param")
                // Wait for initial load to complete
                delay(2000)
            }
            
            // Always call goToChapter to ensure it renders something
            Log.d(TAG, "Navigating to initial chapter: $initialChapterIndex")
            bridge.goToChapter(initialChapterIndex)
            
            if (scrollModeEnabled) {
                bridge.setScrollMode(true)
            }
            
            // Theme application disabled - causes issues
            // when (theme) {
            //     ReaderTheme.SEPIA -> bridge.applySepiaTheme()
            //     ReaderTheme.DARK -> bridge.applyDarkTheme()
            //     ReaderTheme.BLACK -> bridge.applyDarkTheme()
            //     ReaderTheme.LIGHT -> bridge.applyLightTheme()
            // }
        }
        
        if (showNewReaderBanner) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                color = Color(0xFF4A90D9).copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                onClick = { showNewReaderBanner = false }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "New Reader",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Ch.${currentChapterIndex + 1}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
        
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .height(4.dp)
                .fillMaxWidth(0.8f),
            color = Color(0xFF4A90D9).copy(alpha = 0.3f),
            shape = RoundedCornerShape(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(currentProgress)
                    .background(
                        Color(0xFF4A90D9),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupWebView(bridge: FoliateBridge, readerUrl: String? = null) {
    settings.apply {
        javaScriptEnabled = true
        allowFileAccess = true
        allowContentAccess = true
        domStorageEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = true
        builtInZoomControls = false
        displayZoomControls = false
        setSupportZoom(false)
        mediaPlaybackRequiresUserGesture = false
        @Suppress("DEPRECATION")
        allowFileAccessFromFileURLs = true
        @Suppress("DEPRECATION")
        allowUniversalAccessFromFileURLs = true
    }
    
    addJavascriptInterface(bridge, "FoliateBridge")
    
    webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d(TAG, "Page loaded: $url")
        }
    }
    
    // Load from local server if URL provided, otherwise from assets
    if (readerUrl != null) {
        Log.d(TAG, "Loading reader from: $readerUrl")
        loadUrl(readerUrl)
    } else {
        loadUrl("file:///android_asset/foliate-js/reader.html")
    }
}

enum class ReaderTheme {
    LIGHT, SEPIA, DARK, BLACK
}

@Composable
fun ReaderThemeColors(theme: ReaderTheme): Pair<Color, Color> {
    return when (theme) {
        ReaderTheme.LIGHT -> Color(0xFFFFFFFF) to Color(0xFF000000)
        ReaderTheme.SEPIA -> Color(0xFFF4ECD8) to Color(0xFF5B4636)
        ReaderTheme.DARK -> Color(0xFF1A1A1A) to Color(0xFFE0E0E0)
        ReaderTheme.BLACK -> Color(0xFF000000) to Color(0xFFFFFFFF)
    }
}
