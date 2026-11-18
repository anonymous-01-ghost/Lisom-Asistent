package dev.lisom.wakeword

import dev.lisom.speech.PcmAudioFrame
import kotlinx.coroutines.flow.Flow

/** Local keyword spotter. Model loading and inference belong to platform adapters. */
interface WakeWordDetector {
    fun observe(audio: Flow<PcmAudioFrame>): Flow<WakeWordEvent>
}

data class WakeWordEvent(val phrase: String, val confidence: Float?) {
    init {
        require(phrase.isNotBlank()) { "Detected phrase cannot be blank." }
        require(confidence == null || confidence in 0f..1f) { "Confidence must be null or between 0 and 1." }
    }
}
