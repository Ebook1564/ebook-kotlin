package com.freeebookbundle.voice

import android.content.Context
import android.content.SharedPreferences

object TtsPreferences {
    private const val PREFS_NAME = "tts_preferences"
    private const val KEY_PIPER_VOICE_ID = "piper_voice_id"
    private const val KEY_SPEECH_RATE = "speech_rate"

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getPiperVoiceId(): String {
        return prefs.getString(KEY_PIPER_VOICE_ID, "en_US-lessac-medium") ?: "en_US-lessac-medium"
    }

    fun setPiperVoiceId(voiceId: String) {
        prefs.edit().putString(KEY_PIPER_VOICE_ID, voiceId).apply()
    }

    fun getSpeechRate(): Float {
        return prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
    }

    fun setSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
    }
}
