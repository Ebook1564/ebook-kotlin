package com.example.ebook_new_app.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
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

class AppViewModel : ViewModel() {
    private val _themeMode = mutableStateOf(AppThemeMode.NIGHT)
    val themeMode: State<AppThemeMode> = _themeMode
    
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Track the old theme for the ripple reveal
    private val _previousThemeMode = mutableStateOf<AppThemeMode?>(null)
    val previousThemeMode: State<AppThemeMode?> = _previousThemeMode

    private val _themeTransitionTrigger = mutableStateOf(0)
    val themeTransitionTrigger: State<Int> = _themeTransitionTrigger

    private val _isTransitioning = mutableStateOf(false)
    val isTransitioning: State<Boolean> = _isTransitioning

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
        if (_themeMode.value != mode) {
            toggleTheme()
        }
    }

    fun endTransition() {
        _previousThemeMode.value = null
        _isTransitioning.value = false
    }

    fun completeOnboarding(name: String) {
        _userProfile.value = UserProfile(name = name, isOnboarded = true)
    }
}
