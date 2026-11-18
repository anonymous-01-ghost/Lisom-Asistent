package dev.lisom.core

import dev.lisom.api.LisomAssistant
import dev.lisom.api.LisomConfig
import dev.lisom.models.LisomAssistantState
import dev.lisom.models.RecognizedCommand
import dev.lisom.nlu.CommandInterpreter
import dev.lisom.speech.AudioInput
import dev.lisom.speech.SpeechRecognizer
import dev.lisom.tts.SpeechSynthesizer
import dev.lisom.wakeword.WakeWordDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Reference coordinator. Native audio capture and offline engines are injected by adapters.
 * This class has no platform-specific code and makes no network calls.
 */
class LisomAssistantEngine(
    private val config: LisomConfig,
    private val audioInput: AudioInput,
    private val speechRecognizer: SpeechRecognizer,
    private val commandInterpreter: CommandInterpreter,
    private val wakeWordDetector: WakeWordDetector? = null,
    private val speechSynthesizer: SpeechSynthesizer? = null,
) : LisomAssistant {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow< LisomAssistantState >(LisomAssistantState.Uninitialized)
    private val mutableCommands = MutableSharedFlow<RecognizedCommand>(extraBufferCapacity = 16)

    private var pushToTalkJob: Job? = null
    private var wakeWordJob: Job? = null
    private var initialized = false
    private var released = false

    override val state = mutableState.asStateFlow()
    override val commands = mutableCommands.asSharedFlow()

    override suspend fun initialize() {
        check(!released) { "Lisom Assistant has already been released." }
        if (initialized) return
        check(config.offlineRequired) { "Online processing is not supported." }
        initialized = true
        mutableState.value = LisomAssistantState.Ready
    }

    override suspend fun startPushToTalk() {
        ensureInitialized()
        check(config.pushToTalkEnabled) { "Push-to-talk is disabled in this configuration." }
        check(wakeWordJob?.isActive != true && pushToTalkJob?.isActive != true) {
            "An audio session is already active."
        }
        audioInput.start()
        mutableState.value = LisomAssistantState.Listening
        pushToTalkJob = scope.launch {
            try {
                val result = speechRecognizer.recognize(audioInput.frames)
                processTranscript(result.text, result.confidence)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                reportFailure("speech_recognition_failed", failure)
            }
        }
    }

    override suspend fun stopPushToTalk() {
        audioInput.stop()
        pushToTalkJob?.join()
        pushToTalkJob = null
        if (!released && wakeWordJob?.isActive != true) mutableState.value = LisomAssistantState.Ready
    }

    override suspend fun enableWakeWord() {
        ensureInitialized()
        check(config.wakeWordEnabled) { "Wake-word activation is disabled in this configuration." }
        check(pushToTalkJob?.isActive != true) { "Push-to-talk is currently active." }
        if (wakeWordJob?.isActive == true) return
        val detector = requireNotNull(wakeWordDetector) {
            "No local wake-word detector has been installed."
        }
        audioInput.start()
        mutableState.value = LisomAssistantState.Listening
        wakeWordJob = scope.launch {
            try {
                detector.observe(audioInput.frames).collect {
                    mutableState.value = LisomAssistantState.Processing
                    val result = speechRecognizer.recognize(audioInput.frames)
                    processTranscript(result.text, result.confidence)
                    if (!released) mutableState.value = LisomAssistantState.Listening
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                reportFailure("wake_word_session_failed", failure)
            }
        }
    }

    override suspend fun disableWakeWord() {
        wakeWordJob?.cancel()
        wakeWordJob?.join()
        wakeWordJob = null
        audioInput.stop()
        if (!released) mutableState.value = LisomAssistantState.Ready
    }

    override suspend fun speak(text: String) {
        ensureInitialized()
        require(text.isNotBlank()) { "Speech text cannot be blank." }
        val synthesizer = requireNotNull(speechSynthesizer) {
            "No offline speech synthesizer has been installed."
        }
        mutableState.value = LisomAssistantState.Speaking
        try {
            synthesizer.speak(text)
        } finally {
            if (!released) mutableState.value = LisomAssistantState.Ready
        }
    }

    override suspend fun release() {
        if (released) return
        released = true
        pushToTalkJob?.cancel()
        wakeWordJob?.cancel()
        audioInput.release()
        speechRecognizer.release()
        speechSynthesizer?.stop()
        scope.cancel()
        mutableState.value = LisomAssistantState.Closed
    }

    private suspend fun processTranscript(transcript: String, confidence: Float?) {
        mutableState.value = LisomAssistantState.Processing
        val interpretation = commandInterpreter.interpret(transcript)
        val command = interpretation.command
        if (command != null && transcript.isNotBlank()) {
            mutableCommands.emit(
                RecognizedCommand(
                    command = command,
                    transcript = transcript,
                    confidence = confidence,
                ),
            )
        }
        if (!released) mutableState.value = LisomAssistantState.Ready
    }

    private suspend fun ensureInitialized() {
        if (!initialized) initialize()
        check(!released) { "Lisom Assistant has already been released." }
    }

    private fun reportFailure(code: String, failure: Exception) {
        if (!released) {
            mutableState.value = LisomAssistantState.Error(
                code = code,
                message = failure.message ?: "Unexpected local processing error.",
            )
        }
    }
}
