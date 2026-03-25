package com.example.ebook_new_app.voice

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseWorker<I, O>(
    inputChannel: Channel<I>,
    outputChannel: Channel<O>?
) {
    private val channel: Channel<I> = inputChannel
    private val out: Channel<O>? = outputChannel
    protected val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    private val activeFlag = AtomicBoolean(false)

    fun start() {
        if (activeFlag.getAndSet(true)) return
        job = scope.launch {
            try {
                for (item in channel) {
                    if (!activeFlag.get()) break
                    process(item)
                }
            } catch (e: Exception) {
                // Channel closed or cancelled
            }
        }
    }

    abstract suspend fun process(item: I)

    open fun terminate() {
        activeFlag.set(false)
        job?.cancel()
        channel.close()
    }
}

interface SynthesisResult {
    val text: String
    val audioData: ByteArray?
    fun getMessageUpTo(seconds: Float): String
}

interface VoiceSynthesizer {
    suspend fun synthesize(text: String): SynthesisResult
}
