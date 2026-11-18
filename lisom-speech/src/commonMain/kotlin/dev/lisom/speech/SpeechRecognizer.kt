package dev.lisom.speech

import kotlinx.coroutines.flow.Flow

/**
 * ASR adapter contract. Implementations run inference locally and never upload audio.
 * The recognizer should finish an utterance using a local endpoint/VAD strategy.
 */
interface SpeechRecognizer {
    suspend fun recognize(audio: Flow<PcmAudioFrame>): SpeechRecognitionResult
}

data class SpeechRecognitionResult(
    val text: String,
    val languageTag: String = "es",
    val confidence: Float,
) {
    init { require(confidence in 0f..1f) { "Confidence must be between 0 and 1." } }
}
