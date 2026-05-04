package com.freeebookbundle.utils

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"
    
    private const val KEY_DIALOG_MESSAGE = "rating_dialog_message"
    private const val KEY_EXIT_DIALOG_MESSAGE = "exit_rating_dialog_message"
    private const val KEY_RATING_TRIGGER_THRESHOLD = "rating_trigger_threshold"

    fun initializeAndFetchConfig() {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf(
            KEY_DIALOG_MESSAGE to "Enjoying the app? Please rate us!",
            KEY_EXIT_DIALOG_MESSAGE to "Before you go, would you like to rate the app?",
            KEY_RATING_TRIGGER_THRESHOLD to 3L
        ))

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d(TAG, "Config params updated: $updated")
                } else {
                    Log.d(TAG, "Config fetch failed")
                }
            }
    }

    fun getDialogMessage(): String {
        return Firebase.remoteConfig.getString(KEY_DIALOG_MESSAGE)
    }

    fun getExitDialogMessage(): String {
        return Firebase.remoteConfig.getString(KEY_EXIT_DIALOG_MESSAGE)
    }

    fun getRatingThreshold(): Long {
        return Firebase.remoteConfig.getLong(KEY_RATING_TRIGGER_THRESHOLD)
    }
}
