package dev.lisom.speech

import kotlinx.coroutines.flow.Flow

/**
 * ASR adapter contract. Implementations run inference locally and never upload audio.
 * The recognizer should finish an utterance using a local endpoint/VAD strategy.
 */
interface SpeechRecognizer {
    suspend fun recognize(audio: Flow<PcmAudioFrame>): SpeechRecognitionResult

    /** Releases native/model resources. Safe to call more than once. */
    suspend fun release() = Unit
}

data class SpeechRecognitionResult(
    val text: String,
    val languageTag: String = "es",
    /** Null means the selected engine/model does not expose calibrated confidence. */
    val confidence: Float?,
) {
    init {
        require(confidence == null || confidence in 0f..1f) {
            "Confidence must be null or between 0 and 1."
        }
    }
}
