package com.freeebookbundle.voice

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger

/**
 * Orchestrator that wires together the Synthesis and Playback workers.
 */
class TtsOrchestrator(
    private val synthesizer: AndroidSystemSynthesizer
) {
    private val textInputChannel = Channel<String>(capacity = 10)
    private val playbackChannel = Channel<SynthesisResult>(capacity = 5)
    
    private val synthesizerWorker = object : BaseWorker<String, SynthesisResult>(textInputChannel, playbackChannel) {
        override suspend fun process(item: String) {
            val result = synthesizer.synthesize(item)
            playbackChannel.send(result)
        }
    }

    private val utteranceCounter = AtomicInteger(0)
    private val playbackWorker = object : BaseWorker<SynthesisResult, Unit>(playbackChannel, null) {
        override suspend fun process(item: SynthesisResult) {
            val id = "utterance_${utteranceCounter.incrementAndGet()}"
            synthesizer.speak(item.text, id)
        }
    }

    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        synthesizerWorker.start()
        playbackWorker.start()
    }

    suspend fun enqueueText(text: List<String>) {
        for (sentence in text) {
            textInputChannel.send(sentence)
        }
    }

    fun stop() {
        isRunning = false
        synthesizer.stop()
        synthesizerWorker.terminate()
        playbackWorker.terminate()
    }

    fun shutdown() {
        stop()
        synthesizer.shutdown()
    }
}
