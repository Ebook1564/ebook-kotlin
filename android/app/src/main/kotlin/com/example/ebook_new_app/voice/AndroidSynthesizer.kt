package com.example.ebook_new_app.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import java.util.*

class AndroidSystemSynthesizer(context: Context) : VoiceSynthesizer, TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var isInitialized = CompletableDeferred<Boolean>()
    private var currentCallback: ((String) -> Unit)? = null

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId != null) currentCallback?.invoke(utteranceId)
            }
            override fun onError(utteranceId: String?) {}
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                // Future: Emit word highlight events back to UI
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            isInitialized.complete(true)
        } else {
            isInitialized.complete(false)
        }
    }

    override suspend fun synthesize(text: String): SynthesisResult {
        isInitialized.await()
        // For system TTS, we don't get raw audio data easily in real-time,
        // so we return a result that represents the "Intent to Speak".
        return SystemSynthesisResult(text)
    }

    /**
     * Internal speak method for the Playback Worker to call.
     */
    suspend fun speak(text: String, utteranceId: String) {
        isInitialized.await()
        val deferred = CompletableDeferred<Unit>()
        currentCallback = { id -> if (id == utteranceId) deferred.complete(Unit) }
        
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        
        deferred.await()
    }

    fun stop() {
        tts.stop()
    }

    fun shutdown() {
        tts.shutdown()
    }
}

class SystemSynthesisResult(override val text: String) : SynthesisResult {
    override val audioData: ByteArray? = null
    override fun getMessageUpTo(seconds: Float): String = text // Simplified for system TTS
}
