package com.freeebookbundle.voice

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class TtsVoiceManager(private val context: Context) {
    private var piper: PiperSynthesizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingJob: Job? = null
    private var currentTempFile: File? = null

    private val _availablePiperVoices = MutableStateFlow<List<String>>(listOf("en_US-lessac-medium"))
    val availablePiperVoices: StateFlow<List<String>> = _availablePiperVoices
    
    private val _currentPiperVoice = MutableStateFlow("en_US-lessac-medium")
    val currentPiperVoice: StateFlow<String> = _currentPiperVoice
    
    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate
    
    private var onSpeakCompleteCallback: ((String) -> Unit)? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        private const val TAG = "TtsVoiceManager"
    }
    
    fun initialize(onReady: (Boolean) -> Unit) {
        TtsPreferences.initialize(context)
        _speechRate.value = TtsPreferences.getSpeechRate()

        piper = PiperSynthesizer()
        Log.d(TAG, "EC2 Piper initialized")
        
        onReady(true)
    }

    fun setPiperVoice(voiceId: String) {
        _currentPiperVoice.value = voiceId
        TtsPreferences.setPiperVoiceId(voiceId)
        Log.d(TAG, "Piper voice set to: $voiceId")
    }
    
    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate.coerceIn(0.5f, 2.0f)
        TtsPreferences.setSpeechRate(_speechRate.value)
        piper?.speechRate = _speechRate.value
    }
    
    fun speak(text: String, utteranceId: String) {
        speakWithPiper(text, utteranceId)
    }

    private fun speakWithPiper(text: String, utteranceId: String) {
        currentPlayingJob?.cancel()
        try {
            mediaPlayer?.stop()
        } catch (e: IllegalStateException) {
            // MediaPlayer not in valid state, ignore
        }
        mediaPlayer?.release()
        currentTempFile?.delete()
        
        currentPlayingJob = scope.launch {
            try {
                val tempFile = File.createTempFile("piper_tts_", ".wav", context.cacheDir)
                currentTempFile = tempFile
                
                val success = piper?.synthesizeToFile(text, tempFile) ?: false
                
                if (success && tempFile.exists() && tempFile.length() > 1000) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(tempFile.absolutePath)
                        prepare()
                        setOnCompletionListener {
                            onSpeakCompleteCallback?.invoke(utteranceId)
                            tempFile.delete()
                            currentTempFile = null
                        }
                        start()
                    }
                } else {
                    Log.e(TAG, "Piper synthesis failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Piper playback error: ${e.message}")
            }
        }
    }
    
    fun stop() {
        currentPlayingJob?.cancel()
        try {
            mediaPlayer?.stop()
        } catch (e: IllegalStateException) {
            // MediaPlayer not in valid state, ignore
        }
        mediaPlayer?.release()
        mediaPlayer = null
        currentTempFile?.delete()
        currentTempFile = null
    }
    
    fun setOnSpeakComplete(callback: (String) -> Unit) {
        onSpeakCompleteCallback = callback
    }
    
    fun shutdown() {
        scope.cancel()
        currentPlayingJob?.cancel()
        try {
            mediaPlayer?.stop()
        } catch (e: IllegalStateException) {
            // MediaPlayer not in valid state, ignore
        }
        mediaPlayer?.release()
        mediaPlayer = null
        currentTempFile?.delete()
    }
}
