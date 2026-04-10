package com.freeebookbundle.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object Analytics {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun initialize(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        if (::firebaseAnalytics.isInitialized) {
            firebaseAnalytics.logEvent(eventName, params)
        }
    }

    object Events {
        const val BOOK_OPENED = "book_opened"
        const val BOOK_COMPLETED = "book_completed"
        const val READING_SESSION = "reading_session"
        const val SEARCH_PERFORMED = "search_performed"
        const val BOOKMARK_ADDED = "bookmark_added"
        const val HIGHLIGHT_CREATED = "highlight_created"
        const val CATEGORY_VIEWED = "category_viewed"
        const val APP_OPENED = "app_opened"
        const val APP_CLOSED = "app_closed"
        const val BOOK_LOAD_FAILED = "book_load_failed"
        const val EPUB_DOWNLOAD_FAILED = "epub_download_failed"
    }

    object Params {
        const val BOOK_ID = "book_id"
        const val CATEGORY = "category"
        const val CATEGORY_CODE = "category_code"
        const val AUTHOR = "author"
        const val DURATION_SECONDS = "duration_seconds"
        const val PAGES_READ = "pages_read"
        const val QUERY = "query"
        const val RESULTS_COUNT = "results_count"
        const val LANGUAGE = "language"
        const val BOOKSHELF = "bookshelf"
        const val ERROR_TYPE = "error_type"
        const val ERROR_MESSAGE = "error_message"
        const val FILE_NAME = "file_name"
    }

    fun trackBookOpened(bookId: String, categoryCode: String? = null) {
        val params = Bundle().apply {
            putString(Params.BOOK_ID, bookId)
            categoryCode?.let { putString(Params.CATEGORY_CODE, it) }
        }
        logEvent(Events.BOOK_OPENED, params)
    }

    fun trackBookCompleted(bookId: String, durationSeconds: Long? = null) {
        val params = Bundle().apply {
            putString(Params.BOOK_ID, bookId)
            durationSeconds?.let { putLong(Params.DURATION_SECONDS, it) }
        }
        logEvent(Events.BOOK_COMPLETED, params)
    }

    fun trackReadingSession(bookId: String, durationSeconds: Long, pagesRead: Int? = null) {
        val params = Bundle().apply {
            putString(Params.BOOK_ID, bookId)
            putLong(Params.DURATION_SECONDS, durationSeconds)
            pagesRead?.let { putInt(Params.PAGES_READ, it) }
        }
        logEvent(Events.READING_SESSION, params)
    }

    fun trackSearch(query: String, resultsCount: Int) {
        val params = Bundle().apply {
            putString(Params.QUERY, query)
            putInt(Params.RESULTS_COUNT, resultsCount)
        }
        logEvent(Events.SEARCH_PERFORMED, params)
    }

    fun trackBookmarkAdded(bookId: String) {
        val params = Bundle().apply {
            putString(Params.BOOK_ID, bookId)
        }
        logEvent(Events.BOOKMARK_ADDED, params)
    }

    fun trackHighlightCreated(bookId: String) {
        val params = Bundle().apply {
            putString(Params.BOOK_ID, bookId)
        }
        logEvent(Events.HIGHLIGHT_CREATED, params)
    }

    fun trackCategoryViewed(categoryCode: String, bookshelf: String? = null) {
        val params = Bundle().apply {
            putString(Params.CATEGORY_CODE, categoryCode)
            bookshelf?.let { putString(Params.BOOKSHELF, it) }
        }
        logEvent(Events.CATEGORY_VIEWED, params)
    }

    fun trackAppOpened() {
        logEvent(Events.APP_OPENED)
    }

    fun trackAppClosed() {
        logEvent(Events.APP_CLOSED)
    }

    fun setUserProperty(name: String, value: String) {
        if (::firebaseAnalytics.isInitialized) {
            firebaseAnalytics.setUserProperty(name, value)
        }
    }

    object UserProperties {
        const val PREFERRED_CATEGORY = "preferred_category"
        const val BOOKS_COMPLETED_COUNT = "books_completed_count"
        const val TOTAL_BOOKS_READ = "total_books_read"
        const val PREFERRED_LANGUAGE = "preferred_language"
    }

    fun setPreferredCategory(category: String) {
        setUserProperty(UserProperties.PREFERRED_CATEGORY, category)
    }

    fun setBooksCompletedCount(count: Int) {
        setUserProperty(UserProperties.BOOKS_COMPLETED_COUNT, count.toString())
    }

    fun setTotalBooksRead(count: Int) {
        setUserProperty(UserProperties.TOTAL_BOOKS_READ, count.toString())
    }

    fun setPreferredLanguage(language: String) {
        setUserProperty(UserProperties.PREFERRED_LANGUAGE, language)
    }

    fun trackBookLoadFailed(bookId: String, errorType: String, errorMessage: String? = null, fileName: String? = null) {
        val params = Bundle().apply {
            putString(Params.BOOK_ID, bookId)
            putString(Params.ERROR_TYPE, errorType)
            errorMessage?.let { putString(Params.ERROR_MESSAGE, it) }
            fileName?.let { putString(Params.FILE_NAME, it) }
        }
        logEvent(Events.BOOK_LOAD_FAILED, params)
    }

    fun trackEpubDownloadFailed(bookId: String, attemptedFileName: String, errorMessage: String? = null) {
        val params = Bundle().apply {
            putString(Params.BOOK_ID, bookId)
            putString(Params.FILE_NAME, attemptedFileName)
            putString(Params.ERROR_TYPE, "download_failed")
            errorMessage?.let { putString(Params.ERROR_MESSAGE, it) }
        }
        logEvent(Events.EPUB_DOWNLOAD_FAILED, params)
    }
}
