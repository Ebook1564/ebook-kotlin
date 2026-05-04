package com.freeebookbundle.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

object GenrePreferences {
    private const val PREFS_NAME = "genre_prefs"
    private const val KEY_SELECTED_GENRES = "selected_genres"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    
    private const val MIN_GENRES_TO_SELECT = 2
    
    private var prefs: SharedPreferences? = null
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("GenrePreferences not initialized. Call initialize() first.")
    }
    
    fun getSelectedGenres(): Set<String> {
        val json = getPrefs().getString(KEY_SELECTED_GENRES, null) ?: return emptySet()
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    fun getSelectedGenresAsStringSet(): Set<String> {
        val json = getPrefs().getString(KEY_SELECTED_GENRES, null) ?: return emptySet()
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    fun setSelectedGenres(genres: Set<String>) {
        val jsonArray = JSONArray()
        genres.forEach { jsonArray.put(it) }
        getPrefs().edit().putString(KEY_SELECTED_GENRES, jsonArray.toString()).apply()
    }
    
    fun isOnboardingCompleted(): Boolean {
        return getPrefs().getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    fun setOnboardingCompleted(completed: Boolean) {
        getPrefs().edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }
    
    fun getMinGenresRequired(): Int = MIN_GENRES_TO_SELECT
    
    fun hasSelectedEnoughGenres(): Boolean {
        return getSelectedGenres().size >= MIN_GENRES_TO_SELECT
    }
}
