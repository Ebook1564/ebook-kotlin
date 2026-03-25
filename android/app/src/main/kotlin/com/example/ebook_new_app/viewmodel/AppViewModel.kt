package com.example.ebook_new_app.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    DAY, NIGHT, SPACE
}

data class UserProfile(
    val name: String = "Guest",
    val isOnboarded: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    private val _themeMode = mutableStateOf(loadThemeFromPrefs())
    val themeMode: State<AppThemeMode> = _themeMode
    
    private fun loadThemeFromPrefs(): AppThemeMode {
        val themeName = prefs.getString("theme_mode", AppThemeMode.NIGHT.name)
        return try {
            AppThemeMode.valueOf(themeName ?: AppThemeMode.NIGHT.name)
        } catch (e: Exception) {
            AppThemeMode.NIGHT
        }
    }
    
    private val _useSpaceTouchNav = mutableStateOf(false)
    val useSpaceTouchNav: State<Boolean> = _useSpaceTouchNav
    
    private val _userProfile = MutableStateFlow(
        UserProfile(
            name = prefs.getString("user_name", "Guest") ?: "Guest",
            isOnboarded = prefs.getBoolean("is_onboarded", false)
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _previousThemeMode = mutableStateOf<AppThemeMode?>(null)
    val previousThemeMode: State<AppThemeMode?> = _previousThemeMode

    private val _themeTransitionTrigger = mutableStateOf(0)
    val themeTransitionTrigger: State<Int> = _themeTransitionTrigger

    private val _isTransitioning = mutableStateOf(false)
    val isTransitioning: State<Boolean> = _isTransitioning

    init {
        _useSpaceTouchNav.value = prefs.getBoolean("use_space_touch_nav", false)
    }

    fun toggleTheme() {
        if (_isTransitioning.value) return
        _isTransitioning.value = true
        _previousThemeMode.value = _themeMode.value
        _themeMode.value = when (_themeMode.value) {
            AppThemeMode.DAY -> AppThemeMode.NIGHT
            AppThemeMode.NIGHT -> AppThemeMode.SPACE
            AppThemeMode.SPACE -> AppThemeMode.DAY
        }
        _themeTransitionTrigger.value += 1
    }

    fun setTheme(mode: AppThemeMode) {
        if (_themeMode.value == mode) return
        _isTransitioning.value = true
        _previousThemeMode.value = _themeMode.value
        _themeMode.value = mode
        _themeTransitionTrigger.value += 1
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun endTransition() {
        _previousThemeMode.value = null
        _isTransitioning.value = false
    }

    fun setUseSpaceTouchNav(use: Boolean) {
        _useSpaceTouchNav.value = use
        prefs.edit().putBoolean("use_space_touch_nav", use).apply()
    }

    fun completeOnboarding(name: String) {
        _userProfile.value = UserProfile(name = name, isOnboarded = true)
        prefs.edit()
            .putString("user_name", name)
            .putBoolean("is_onboarded", true)
            .apply()
    }
}
