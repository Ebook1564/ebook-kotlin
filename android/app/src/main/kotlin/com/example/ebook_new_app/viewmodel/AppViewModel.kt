package com.example.ebook_new_app.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

enum class AppThemeMode {
    DAY, NIGHT
}

class AppViewModel : ViewModel() {
    private val _themeMode = mutableStateOf(AppThemeMode.NIGHT)
    val themeMode: State<AppThemeMode> = _themeMode
    
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
        _themeMode.value = if (_themeMode.value == AppThemeMode.DAY) AppThemeMode.NIGHT else AppThemeMode.DAY
        _themeTransitionTrigger.value += 1
    }

    fun endTransition() {
        _previousThemeMode.value = null
        _isTransitioning.value = false
    }
}
