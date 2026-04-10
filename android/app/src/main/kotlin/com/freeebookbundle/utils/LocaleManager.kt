package com.freeebookbundle.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

data class SupportedLocale(
    val code: String,
    val displayName: String,
    val nativeName: String
)

object LocaleManager {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LOCALE = "selected_locale"
    
    val supportedLocales = listOf(
        SupportedLocale("en", "English", "English"),
        SupportedLocale("es", "Spanish", "Español"),
        SupportedLocale("fr", "French", "Français"),
        SupportedLocale("de", "German", "Deutsch"),
        SupportedLocale("pt", "Portuguese", "Português"),
        SupportedLocale("zh-CN", "Chinese (Simplified)", "简体中文"),
        SupportedLocale("ja", "Japanese", "日本語"),
        SupportedLocale("ru", "Russian", "Русский"),
        SupportedLocale("hi", "Hindi", "हिन्दी"),
        SupportedLocale("ar", "Arabic", "العربية")
    )
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getSavedLocale(context: Context): String? {
        return getPrefs(context).getString(KEY_LOCALE, null)
    }
    
    fun setLocale(context: Context, localeCode: String?) {
        val prefs = getPrefs(context)
        if (localeCode == null) {
            prefs.edit().remove(KEY_LOCALE).apply()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            prefs.edit().putString(KEY_LOCALE, localeCode).apply()
            val locale = LocaleListCompat.forLanguageTags(localeCode)
            AppCompatDelegate.setApplicationLocales(locale)
        }
    }
    
    fun applySavedLocale(context: Context) {
        val savedLocale = getSavedLocale(context)
        if (savedLocale != null) {
            val locale = LocaleListCompat.forLanguageTags(savedLocale)
            AppCompatDelegate.setApplicationLocales(locale)
        }
    }
    
    fun getLocaleDisplayName(localeCode: String): String {
        return supportedLocales.find { it.code == localeCode }?.displayName ?: localeCode
    }
    
    fun isRTLLocale(localeCode: String): Boolean {
        return localeCode == "ar"
    }
    
    fun isCurrentLocaleRTL(context: Context): Boolean {
        val savedLocale = getSavedLocale(context)
        return savedLocale != null && isRTLLocale(savedLocale)
    }
    
    fun getSystemLanguagePrefix(): String {
        val systemLang = Locale.getDefault().language
        return when(systemLang) {
            "fr" -> "FR "
            "de" -> "DE "
            "it" -> "IT "
            "pt" -> "PT "
            "es" -> "ES "
            else -> ""
        }
    }
    
    fun isEnglishSystem(): Boolean {
        return Locale.getDefault().language == "en"
    }
}
