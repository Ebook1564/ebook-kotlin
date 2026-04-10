package com.freeebookbundle.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.freeebookbundle.viewmodel.AppThemeMode
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ReviewManager {
    private const val PREFS_NAME = "review_prefs"
    private const val KEY_REVIEW_SHOWN = "review_shown"
    private const val KEY_BOOKS_COMPLETED = "books_completed"
    private const val KEY_BOOKS_OPENED = "books_opened"
    private const val KEY_BOOK_CLOSES = "book_closes"
    private const val KEY_LAST_RATING_TIME = "last_rating_time"
    private const val KEY_APP_START_TIME = "app_start_time"
    private const val KEY_LAST_RATING_VALUE = "last_rating_value"
    private const val KEY_RATING_COUNT = "rating_count"
    private const val KEY_LAST_OPEN_DATE = "last_open_date"
    private const val KEY_FIRST_TIME_PROMPT_SHOWN = "first_time_prompt_shown"
    
    private const val BOOKS_COMPLETED_THRESHOLD = 3
    private const val BOOKS_OPENED_THRESHOLD = 5
    private const val RATING_INTERVAL_AFTER_NEGATIVE = 2 * 24 * 60 * 60 * 1000L // 2 days
    private const val MAX_RATING_PROMPTS = 5
    
    private const val KEY_TIME_BASED_PROMPT_ENABLED = "time_based_prompt_enabled"

    private var reviewManager: ReviewManager? = null
    private var reviewInfo: ReviewInfo? = null
    private var prefs: SharedPreferences? = null
    
    @Volatile
    private var isRatingDialogShowing = false

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

    fun onBookClosed(context: Context) {
        prefs?.let {
            val closes = it.getInt(KEY_BOOK_CLOSES, 0)
            it.edit().putInt(KEY_BOOK_CLOSES, closes + 1).commit()  // Use commit() for immediate write
        }
    }

    fun getBookClosesCount(context: Context): Int {
        return prefs?.getInt(KEY_BOOK_CLOSES, 0) ?: 0
    }

    fun recordAppStartTime(context: Context) {
        prefs?.edit()?.putLong(KEY_APP_START_TIME, System.currentTimeMillis())?.apply()
    }

    // Simplified: Show after onboarding, then every 2 days if 1-3 stars or cancel
    fun shouldShowRatingPrompt(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Don't show if we've already shown MAX_RATING_PROMPTS times
        val ratingCount = prefs.getInt(KEY_RATING_COUNT, 0)
        if (ratingCount >= MAX_RATING_PROMPTS) {
            return false
        }

        // Check if this is the first time prompt should be shown (after onboarding)
        val firstTimeShown = prefs.getBoolean(KEY_FIRST_TIME_PROMPT_SHOWN, false)
        
        // If not shown first time yet, show now
        if (!firstTimeShown) {
            return true
        }

        // For subsequent times, check if 2 days have passed since last rating
        val lastRatingTime = prefs.getLong(KEY_LAST_RATING_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        val lastRatingValue = prefs.getInt(KEY_LAST_RATING_VALUE, 0)
        
        // Only enforce 2-day interval if last rating was 1-3 or cancel (value = 0)
        val effectiveInterval = if (lastRatingValue in 1..3) RATING_INTERVAL_AFTER_NEGATIVE else 0L
        
        // If first time was shown, we need interval check
        if (currentTime - lastRatingTime < effectiveInterval) {
            return false
        }

        return true
    }

    fun markFirstTimePromptShown(context: Context) {
        prefs?.edit()?.putBoolean(KEY_FIRST_TIME_PROMPT_SHOWN, true)?.apply()
    }

    fun onRatingShown(context: Context, ratingValue: Int) {
        prefs?.let {
            // Increment rating count
            val count = it.getInt(KEY_RATING_COUNT, 0)
            it.edit().putInt(KEY_RATING_COUNT, count + 1).commit()
            
            if (ratingValue >= 4) {
                // 4-5 stars: Never ask again
                it.edit().putBoolean(KEY_REVIEW_SHOWN, true).commit()
                it.edit().putLong(KEY_LAST_RATING_TIME, Long.MAX_VALUE).commit()
            } else {
                // 1-3 stars or cancel: Set interval to 2 days from now
                it.edit().putLong(KEY_LAST_RATING_TIME, System.currentTimeMillis() + RATING_INTERVAL_AFTER_NEGATIVE).commit()
            }
            
            // Store last rating value
            it.edit().putInt(KEY_LAST_RATING_VALUE, ratingValue).commit()
        }
    }

    fun getRatingCount(context: Context): Int {
        return prefs?.getInt(KEY_RATING_COUNT, 0) ?: 0
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

            // Do NOT mark as permanently shown - let API handle its own limits
            ReviewResult.Success
        } catch (e: Exception) {
            ReviewResult.Error(e.message ?: "Unknown error")
        }
    }

    // Removed markReviewShown - API handles its own limits internally

    fun requestRatingWithStars(activity: Activity, themeMode: AppThemeMode, onComplete: ((Int) -> Unit)? = null) {
        // Prevent multiple dialogs from showing at once
        if (isRatingDialogShowing) {
            return
        }
        activity.runOnUiThread {
            showNativeRatingDialog(activity, themeMode, onComplete)
        }
    }

    private fun showNativeRatingDialog(activity: Activity, themeMode: AppThemeMode, onComplete: ((Int) -> Unit)?) {
        // Set flag to prevent stacking
        isRatingDialogShowing = true
        
        val bgColor = when (themeMode) {
            AppThemeMode.DAY -> Color.parseColor("#FFFFF8F0")
            AppThemeMode.NIGHT -> Color.parseColor("#FF2A2A2A")
            AppThemeMode.SPACE -> Color.parseColor("#FF1A1A2E")
        }
        val textColor = when (themeMode) {
            AppThemeMode.DAY -> Color.parseColor("#FF3D2914")
            AppThemeMode.NIGHT -> Color.WHITE
            AppThemeMode.SPACE -> Color.WHITE
        }
        val starActiveColor = when (themeMode) {
            AppThemeMode.DAY -> Color.parseColor("#FF8B7355")
            AppThemeMode.NIGHT -> Color.parseColor("#FFFFD700")
            AppThemeMode.SPACE -> Color.parseColor("#FF6BB3E0")
        }
        val starInactiveColor = Color.parseColor("#FF666666")
        
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
            setBackgroundColor(bgColor)
            gravity = Gravity.CENTER
        }

        val title = TextView(activity).apply {
            text = "Enjoying E-Book Bundle?"
            textSize = 20f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }

        val subtitle = TextView(activity).apply {
            text = "Tap a star to rate"
            textSize = 14f
            setTextColor(textColor)
            alpha = 0.7f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val starRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val stars = mutableListOf<TextView>()
        val selectedRating = intArrayOf(0)

        for (i in 1..5) {
            val star = TextView(activity).apply {
                text = "☆"
                textSize = 44f
                setTextColor(starInactiveColor)
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    selectedRating[0] = i
                    stars.forEachIndexed { index, tv ->
                        if (index < i) {
                            tv.text = "★"
                            tv.setTextColor(starActiveColor)
                        } else {
                            tv.text = "☆"
                            tv.setTextColor(starInactiveColor)
                        }
                    }
                }
            }
            stars.add(star)
            starRow.addView(star)
        }

        container.addView(title)
        container.addView(subtitle)
        container.addView(starRow)

        val dialog = AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_NoActionBar)
            .setView(container)
            .setCancelable(true)
            .setNegativeButton("Cancel") { d, _ ->
                d.dismiss()
                isRatingDialogShowing = false
                onComplete?.invoke(0)
            }
            .setOnDismissListener {
                isRatingDialogShowing = false
            }
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        
        for (i in stars.indices) {
            val index = i
            stars[i].setOnClickListener {
                selectedRating[0] = index + 1
                stars.forEachIndexed { idx, tv ->
                    if (idx <= index) {
                        tv.text = "★"
                        tv.setTextColor(starActiveColor)
                    } else {
                        tv.text = "☆"
                        tv.setTextColor(starInactiveColor)
                    }
                }
                
                // Add delay to show selection before dismissing
                stars[index].postDelayed({
                    dialog.dismiss()
                    isRatingDialogShowing = false
                    handleRatingWithCallback(activity, index + 1, onComplete)
                }, 150)
            }
        }

        dialog.show()
    }

    private fun handleRatingWithCallback(activity: Activity, rating: Int, onComplete: ((Int) -> Unit)?) {
        // Track the rating before doing anything else
        onRatingShown(activity, rating)
        
        if (rating >= 4) {
            onComplete?.invoke(rating)
            // Directly open Play Store listing
            openPlayStoreListing(activity)
        } else {
            onComplete?.invoke(rating)
            Toast.makeText(activity, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPlayStoreListing(activity: Activity) {
        try {
            val appPackage = activity.packageName
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$appPackage"))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web Play Store if market app not available
            try {
                val appPackage = activity.packageName
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$appPackage"))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("ReviewManager", "Failed to open Play Store: ${e2.message}")
            }
        }
    }

    fun resetReviewState(context: Context) {
        prefs?.edit()?.apply {
            putBoolean(KEY_REVIEW_SHOWN, false)
            putInt(KEY_BOOKS_COMPLETED, 0)
            putInt(KEY_BOOKS_OPENED, 0)
            apply()
        }
    }

    sealed class ReviewResult {
        data object Success : ReviewResult()
        data class Error(val message: String) : ReviewResult()
    }
}
