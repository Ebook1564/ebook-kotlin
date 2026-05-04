package com.freeebookbundle.utils

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.freeebookbundle.ui.RatingDialog
import com.freeebookbundle.viewmodel.AppThemeMode

object RatingDialogLauncher {
    private const val TAG = "RatingDialogLauncher"
    private const val EXTRA_IS_EXIT_CONTEXT = "extra_is_exit_context"

    fun showRatingDialog(activity: Activity, themeMode: AppThemeMode, isExitContext: Boolean = false, onResult: (Int) -> Unit) {
        try {
            when (activity) {
                is FragmentActivity -> {
                    showRatingDialogFragment(activity, themeMode, isExitContext, onResult)
                }
                else -> {
                    showRatingAsActivity(activity, themeMode, isExitContext, onResult)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show rating dialog: ${e.message}", e)
            onResult(0)
        }
    }

    private fun showRatingDialogFragment(
        activity: FragmentActivity,
        themeMode: AppThemeMode,
        isExitContext: Boolean,
        onResult: (Int) -> Unit
    ) {
        val dialogFragment = RatingDialogFragment.newInstance(themeMode, isExitContext, onResult)
        dialogFragment.show(activity.supportFragmentManager, "rating_dialog")
    }

    private fun showRatingAsActivity(
        activity: Activity,
        themeMode: AppThemeMode,
        isExitContext: Boolean,
        onResult: (Int) -> Unit
    ) {
        val intent = Intent(activity, RatingDialogActivity::class.java).apply {
            putExtra(RatingDialogActivity.EXTRA_THEME_MODE, themeMode.name)
            putExtra(EXTRA_IS_EXIT_CONTEXT, isExitContext)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }

        RatingDialogActivity.setOnResult(activity) { result ->
            onResult(result)
        }

        activity.startActivity(intent)
    }
}

class RatingDialogActivity : ComponentActivity() {
    companion object {
        const val EXTRA_THEME_MODE = "extra_theme_mode"
        private const val EXTRA_IS_EXIT_CONTEXT = "extra_is_exit_context"
        private var onResultCallback: ((Int) -> Unit)? = null

        fun setOnResult(activity: Activity, callback: (Int) -> Unit) {
            onResultCallback = callback
        }

        fun dispatchResult(result: Int) {
            onResultCallback?.invoke(result)
            onResultCallback = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        val themeModeName = intent.getStringExtra(EXTRA_THEME_MODE) ?: AppThemeMode.DAY.name
        val isExitContext = intent.getBooleanExtra(EXTRA_IS_EXIT_CONTEXT, false)
        val themeMode = try {
            AppThemeMode.valueOf(themeModeName)
        } catch (e: Exception) {
            AppThemeMode.DAY
        }

        setContent {
            RatingDialog(
                themeMode = themeMode,
                isExitContext = isExitContext,
                onRating = { rating ->
                    dispatchResult(rating)
                    finish()
                },
                onDismiss = {
                    dispatchResult(0)
                    finish()
                },
                onExit = {
                    dispatchResult(-1)
                    finish()
                }
            )
        }
    }
}

class RatingDialogFragment : DialogFragment() {
    private var themeMode: AppThemeMode = AppThemeMode.DAY
    private var isExitContext: Boolean = false
    private var onResultCallback: ((Int) -> Unit)? = null

    companion object {
        fun newInstance(themeMode: AppThemeMode, isExitContext: Boolean, onResult: (Int) -> Unit): RatingDialogFragment {
            return RatingDialogFragment().apply {
                this.themeMode = themeMode
                this.isExitContext = isExitContext
                this.onResultCallback = onResult
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RatingDialog(
                    themeMode = themeMode,
                    isExitContext = isExitContext,
                    onRating = { rating ->
                        onResultCallback?.invoke(rating)
                        dismiss()
                    },
                    onDismiss = {
                        onResultCallback?.invoke(0)
                        dismiss()
                    },
                    onExit = {
                        onResultCallback?.invoke(-1)
                        dismiss()
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        onResultCallback = null
        super.onDestroyView()
    }
}