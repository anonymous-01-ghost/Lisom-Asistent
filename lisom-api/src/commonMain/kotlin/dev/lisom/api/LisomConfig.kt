package dev.lisom.api

data class LisomConfig(
    val languageTag: String = "es-PE",
    val wakeWordPhrase: String = "Oye Lisom",
    val pushToTalkEnabled: Boolean = true,
    val wakeWordEnabled: Boolean = true,
    val offlineRequired: Boolean = true,
    val sampleRateHz: Int = 16_000,
) {
    init {
        require(languageTag.isNotBlank()) { "languageTag cannot be blank." }
        require(wakeWordPhrase.isNotBlank()) { "wakeWordPhrase cannot be blank." }
        require(pushToTalkEnabled || wakeWordEnabled) { "At least one activation method must be enabled." }
        require(offlineRequired) { "Lisom Assistant only supports mandatory offline processing." }
        require(sampleRateHz == 16_000) { "The initial audio pipeline requires 16 kHz PCM input." }
    }
}
