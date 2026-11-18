package dev.lisom.models

data class RecognizedCommand(
    val command: LisomCommand,
    val transcript: String,
    val confidence: Float,
) {
    init {
        require(transcript.isNotBlank()) { "Transcript cannot be blank." }
        require(confidence in 0f..1f) { "Confidence must be between 0 and 1." }
    }
}
