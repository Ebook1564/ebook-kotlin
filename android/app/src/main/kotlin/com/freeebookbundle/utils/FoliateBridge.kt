package com.freeebookbundle.utils

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

data class BookMetadata(
    val title: String = "",
    val author: String = "",
    val direction: String = "ltr"
)

data class RelocateEvent(
    val index: Int,
    val fraction: Float,
    val cfi: String
)

data class SelectionEvent(
    val text: String,
    val cfi: String
)

data class SearchResult(
    val cfi: String,
    val excerpt: String,
    val index: Int
)

class FoliateBridge(private val webView: WebView) {
    
    companion object {
        private const val TAG = "FoliateBridge"
    }
    
    var onRelocate: ((RelocateEvent) -> Unit)? = null
    var onSelection: ((SelectionEvent) -> Unit)? = null
    var onBookLoaded: ((BookMetadata) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    @JavascriptInterface
    fun onRelocate(data: String) {
        Log.d(TAG, "onRelocate: $data")
        try {
            val json = JSONObject(data)
            val event = RelocateEvent(
                index = json.optInt("index", 0),
                fraction = json.optDouble("fraction", 0.0).toFloat(),
                cfi = json.optString("cfi", "")
            )
            onRelocate?.invoke(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing relocate event", e)
        }
    }
    
    @JavascriptInterface
    fun onSelection(data: String) {
        Log.d(TAG, "onSelection: $data")
        try {
            val json = JSONObject(data)
            val event = SelectionEvent(
                text = json.optString("text", ""),
                cfi = json.optString("cfi", "")
            )
            onSelection?.invoke(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing selection event", e)
        }
    }
    
    @JavascriptInterface
    fun onBookLoaded(metadata: String) {
        Log.d(TAG, "onBookLoaded: $metadata")
        try {
            val json = JSONObject(metadata)
            val bookMetadata = BookMetadata(
                title = json.optString("title", ""),
                author = json.optString("author", ""),
                direction = json.optString("direction", "ltr")
            )
            onBookLoaded?.invoke(bookMetadata)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing book metadata", e)
        }
    }
    
    @JavascriptInterface
    fun onError(error: String) {
        Log.e(TAG, "Book error: $error")
        onError?.invoke(error)
    }
    
    fun openBook(bookUrl: String) {
        val escapedUrl = bookUrl.replace("'", "\\'")
        val js = "window.openBook('$escapedUrl')"
        webView.evaluateJavascript(js, null)
    }
    
    fun goToChapter(index: Int, anchor: String? = null) {
        if (anchor != null) {
            val js = "window.goTo($index, '$anchor')"
            webView.evaluateJavascript(js, null)
        } else {
            val js = "window.goTo($index)"
            webView.evaluateJavascript(js, null)
        }
    }
    
    fun goToCfi(cfi: String) {
        val js = "window.goToCfi('$cfi')"
        webView.evaluateJavascript(js, null)
    }
    
    fun setScrollMode(enabled: Boolean) {
        val flow = if (enabled) "scrolled" else "paginated"
        val js = "window.setFlow('$flow')"
        webView.evaluateJavascript(js, null)
    }
    
    fun setColumnCount(count: Int) {
        val js = "window.setColumnCount($count)"
        webView.evaluateJavascript(js, null)
    }
    
    fun applyTheme(background: String, textColor: String) {
        val css = "body { background-color: $background !important; color: $textColor !important; }"
        val js = "window.injectCss('$css')"
        webView.evaluateJavascript(js, null)
    }
    
    fun applySepiaTheme() {
        val js = "window.setTheme('sepia')"
        webView.evaluateJavascript(js, null)
    }
    
    fun applyDarkTheme() {
        val js = "window.setTheme('dark')"
        webView.evaluateJavascript(js, null)
    }
    
    fun applyLightTheme() {
        val js = "window.setTheme('light')"
        webView.evaluateJavascript(js, null)
    }
    
    fun search(query: String, onResults: (List<SearchResult>) -> Unit) {
        val escapedQuery = query.replace("'", "\\'")
        val js = "(async () => { try { return await window.search('$escapedQuery'); } catch(e) { return '[]'; } })()"
        webView.evaluateJavascript(js) { result ->
            try {
                val results = mutableListOf<SearchResult>()
                if (result != "[]" && result != "null" && result.isNotBlank()) {
                    val jsonArray = org.json.JSONArray(result)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        results.add(SearchResult(
                            cfi = obj.optString("cfi", ""),
                            excerpt = obj.optString("excerpt", ""),
                            index = obj.optInt("index", 0)
                        ))
                    }
                }
                onResults(results)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing search results", e)
                onResults(emptyList())
            }
        }
    }
    
    fun addHighlight(cfi: String, color: String, annotation: String? = null): String? {
        val annotationParam = if (annotation != null) ", '$annotation'" else ""
        val js = "window.addHighlight('$cfi', '$color'$annotationParam)"
        var highlightId: String? = null
        webView.evaluateJavascript(js) { result ->
            highlightId = result
        }
        return highlightId
    }
    
    fun removeHighlight(id: String) {
        val js = "window.removeAnnotation('$id')"
        webView.evaluateJavascript(js, null)
    }
    
    fun prev() {
        val js = "window.prev()"
        webView.evaluateJavascript(js, null)
    }
    
    fun next() {
        val js = "window.next()"
        webView.evaluateJavascript(js, null)
    }
    
    fun getCurrentLocation(onResult: (String) -> Unit) {
        val js = "window.getCurrentLocation()"
        webView.evaluateJavascript(js, onResult)
    }
    
    fun getBookInfo(onResult: (BookMetadata) -> Unit) {
        val js = "window.getMetadata()"
        webView.evaluateJavascript(js) { result ->
            try {
                val json = JSONObject(result)
                val metadata = BookMetadata(
                    title = json.optString("title", ""),
                    author = json.optString("author", ""),
                    direction = json.optString("direction", "ltr")
                )
                onResult(metadata)
            } catch (e: Exception) {
                onResult(BookMetadata())
            }
        }
    }
    
    fun getTableOfContents(onResult: (List<TocItem>) -> Unit) {
        val js = "window.getToc()"
        webView.evaluateJavascript(js) { result ->
            try {
                val items = mutableListOf<TocItem>()
                if (result != "null" && result != "[]") {
                    val jsonArray = org.json.JSONArray(result)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        items.add(TocItem(
                            label = obj.optString("label", ""),
                            href = obj.optString("href", ""),
                            index = obj.optInt("index", 0)
                        ))
                    }
                }
                onResult(items)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing TOC", e)
                onResult(emptyList())
            }
        }
    }
}

data class TocItem(
    val label: String,
    val href: String,
    val index: Int
)
