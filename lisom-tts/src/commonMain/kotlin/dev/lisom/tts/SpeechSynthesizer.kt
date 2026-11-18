package dev.lisom.tts

/** Local speech synthesis adapter. Implementations must not call a remote TTS service. */
interface SpeechSynthesizer {
    suspend fun speak(text: String)
    suspend fun stop()
}
