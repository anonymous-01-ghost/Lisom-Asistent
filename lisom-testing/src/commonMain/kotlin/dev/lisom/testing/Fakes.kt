package dev.lisom.testing

import dev.lisom.speech.AudioInput
import dev.lisom.speech.PcmAudioFrame
import dev.lisom.speech.SpeechRecognitionResult
import dev.lisom.speech.SpeechRecognizer
import dev.lisom.tts.SpeechSynthesizer
import dev.lisom.wakeword.WakeWordDetector
import dev.lisom.wakeword.WakeWordEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAudioInput : AudioInput {
    private val mutableFrames = MutableSharedFlow<PcmAudioFrame>(extraBufferCapacity = 64)
    override val frames: Flow<PcmAudioFrame> = mutableFrames
    var isStarted: Boolean = false
        private set
    override suspend fun start() { isStarted = true }
    override suspend fun stop() {
        isStarted = false
        mutableFrames.emit(PcmAudioFrame(samples = shortArrayOf(), isEndOfStream = true))
    }
    fun emit(frame: PcmAudioFrame): Boolean = mutableFrames.tryEmit(frame)
}

class FakeSpeechRecognizer(
    var nextResult: SpeechRecognitionResult = SpeechRecognitionResult(
        text = "pausa la musica",
        languageTag = "es-PE",
        confidence = 0.99f,
    ),
) : SpeechRecognizer {
    override suspend fun recognize(audio: Flow<PcmAudioFrame>): SpeechRecognitionResult = nextResult
}

class FakeWakeWordDetector : WakeWordDetector {
    private val detections = MutableSharedFlow<WakeWordEvent>(extraBufferCapacity = 8)
    override fun observe(audio: Flow<PcmAudioFrame>): Flow<WakeWordEvent> = detections
    fun emit(event: WakeWordEvent): Boolean = detections.tryEmit(event)
}

class FakeSpeechSynthesizer : SpeechSynthesizer {
    val spokenTexts = mutableListOf<String>()
    var stopped: Boolean = false
        private set
    override suspend fun speak(text: String) { spokenTexts += text }
    override suspend fun stop() { stopped = true }
}
