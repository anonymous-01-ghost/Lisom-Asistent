package dev.lisom.api

import dev.lisom.models.LisomAssistantState
import dev.lisom.models.RecognizedCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Public entry point implemented by lisom-core. All implementations must remain offline. */
interface LisomAssistant {
    val state: StateFlow<LisomAssistantState>
    val commands: Flow<RecognizedCommand>
    suspend fun initialize()
    suspend fun startPushToTalk()
    suspend fun stopPushToTalk()
    suspend fun enableWakeWord()
    suspend fun disableWakeWord()
    suspend fun speak(text: String)
    suspend fun stopSpeaking()
    suspend fun release()
}
