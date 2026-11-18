package dev.lisom.nlu

import dev.lisom.models.LisomCommand

fun interface CommandInterpreter {
    /**
     * Declared locale support. An empty set means the host interpreter accepts
     * the configured language itself.
     */
    val supportedLanguageTags: Set<String>
        get() = emptySet()

    fun interpret(text: String): InterpretationResult
}

data class InterpretationResult(
    val command: LisomCommand?,
    val normalizedText: String,
    val confidence: Float,
    val reason: String? = null,
) {
    init { require(confidence in 0f..1f) { "Confidence must be between 0 and 1." } }
    val isRecognized: Boolean get() = command != null
}
