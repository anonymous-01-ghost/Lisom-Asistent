package dev.lisom.models

data class RecognizedCommand(
    val command: LisomCommand,
    val transcript: String,
    /** Null when the underlying recognizer does not provide calibrated ASR confidence. */
    val confidence: Float?,
    /** Confidence assigned by the local NLU interpreter after intent matching. */
    val nluConfidence: Float = 1.0f,
) {
    init {
        require(transcript.isNotBlank()) { "Transcript cannot be blank." }
        require(confidence == null || confidence in 0f..1f) {
            "Confidence must be null or between 0 and 1."
        }
        require(nluConfidence in 0f..1f) {
            "nluConfidence must be between 0 and 1."
        }
    }
}
