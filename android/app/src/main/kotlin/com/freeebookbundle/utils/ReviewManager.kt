package com.freeebookbundle.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.RuntimeExecutionException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ReviewManager {
    private const val PREFS_NAME = "review_prefs"
    private const val KEY_REVIEW_SHOWN = "review_shown"
    private const val KEY_BOOKS_COMPLETED = "books_completed"
    private const val KEY_BOOKS_OPENED = "books_opened"
    
    private const val BOOKS_COMPLETED_THRESHOLD = 3
    private const val BOOKS_OPENED_THRESHOLD = 10

    private var reviewManager: ReviewManager? = null
    private var reviewInfo: ReviewInfo? = null
    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        reviewManager = ReviewManagerFactory.create(context)
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun shouldRequestReview(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        if (prefs.getBoolean(KEY_REVIEW_SHOWN, false)) {
            return false
        }

        val completed = prefs.getInt(KEY_BOOKS_COMPLETED, 0)
        val opened = prefs.getInt(KEY_BOOKS_OPENED, 0)

        return completed >= BOOKS_COMPLETED_THRESHOLD || opened >= BOOKS_OPENED_THRESHOLD
    }

    fun onBookOpened(context: Context) {
        prefs?.let {
            val opened = it.getInt(KEY_BOOKS_OPENED, 0)
            it.edit().putInt(KEY_BOOKS_OPENED, opened + 1).apply()
        }
    }

    fun onBookCompleted(context: Context) {
        prefs?.let {
            val completed = it.getInt(KEY_BOOKS_COMPLETED, 0)
            it.edit().putInt(KEY_BOOKS_COMPLETED, completed + 1).apply()
        }
    }

    fun getCompletedBooksCount(context: Context): Int {
        return prefs?.getInt(KEY_BOOKS_COMPLETED, 0) ?: 0
    }

    fun getOpenedBooksCount(context: Context): Int {
        return prefs?.getInt(KEY_BOOKS_OPENED, 0) ?: 0
    }

    suspend fun requestReview(activity: Activity): ReviewResult {
        if (reviewManager == null) {
            return ReviewResult.Error("ReviewManager not initialized")
        }

        return try {
            val reviewInfo = suspendCancellableCoroutine<ReviewInfo> { continuation ->
                val task = reviewManager!!.requestReviewFlow()
                task.addOnCompleteListener { result ->
                    if (result.isSuccessful) {
                        continuation.resume(result.result)
                    } else {
                        continuation.resumeWithException(
                            result.exception ?: Exception("Unknown error")
                        )
                    }
                }
            }

            suspendCancellableCoroutine<Unit> { continuation ->
                val flow = reviewManager!!.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    continuation.resume(Unit)
                }
            }

            markReviewShown(activity)
            ReviewResult.Success
        } catch (e: Exception) {
            ReviewResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun markReviewShown(context: Context) {
        prefs?.edit()?.putBoolean(KEY_REVIEW_SHOWN, true)?.apply()
    }

    fun resetReviewState(context: Context) {
        prefs?.edit()?.apply {
            putBoolean(KEY_REVIEW_SHOWN, false)
            putInt(KEY_BOOKS_COMPLETED, 0)
            putInt(KEY_BOOKS_OPENED, 0)
            apply()
        }
    }

    fun requestRatingWithStarsForce(activity: android.app.Activity, themeMode: com.freeebookbundle.viewmodel.AppThemeMode) {
        // Implementation for force showing rating dialog
    }

    sealed class ReviewResult {
        data object Success : ReviewResult()
        data class Error(val message: String) : ReviewResult()
    }
}
