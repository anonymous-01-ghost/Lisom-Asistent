package dev.lisom.models

data class RecognizedCommand(
    val command: LisomCommand,
    val transcript: String,
    /** Null when the underlying recognizer does not provide calibrated confidence. */
    val confidence: Float?,
) {
    init {
        require(transcript.isNotBlank()) { "Transcript cannot be blank." }
        require(confidence == null || confidence in 0f..1f) {
            "Confidence must be null or between 0 and 1."
        }
    }
}
