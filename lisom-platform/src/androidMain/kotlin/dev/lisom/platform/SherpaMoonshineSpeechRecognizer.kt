package dev.lisom.platform

import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import dev.lisom.speech.PcmAudioFrame
import dev.lisom.speech.SpeechRecognitionResult
import dev.lisom.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Sherpa-ONNX offline Moonshine v2 adapter for Android.
 *
 * The app supplies an already-installed model directory containing:
 * encoder_model.ort, decoder_model_merged.ort and tokens.txt.
 * Model files are never downloaded by this class.
 */
class SherpaMoonshineSpeechRecognizer(
    private val modelDirectory: File,
    private val numThreads: Int = 2,
    private val maxUtteranceSeconds: Int = 20,
) : SpeechRecognizer {

    private val recognizerMutex = Mutex()
    private var recognizer: OfflineRecognizer? = null
    private var released = false

    init {
        require(numThreads > 0) { "numThreads must be greater than zero." }
        require(maxUtteranceSeconds in 1..60) { "maxUtteranceSeconds must be between 1 and 60." }
    }

    override suspend fun recognize(audio: Flow<PcmAudioFrame>): SpeechRecognitionResult =
        withContext(Dispatchers.IO) {
            check(!released) { "Speech recognizer has already been released." }

            val maxSamples = 16_000 * maxUtteranceSeconds
            val samples = ArrayList<Float>(maxSamples.coerceAtMost(16_000 * 5))

            audio.takeWhile { !it.isEndOfStream }.collect { frame ->
                if (!frame.isEndOfStream) {
                    require(frame.sampleRateHz == 16_000 && frame.channelCount == 1) {
                        "Sherpa adapter requires mono 16 kHz PCM."
                    }
                    if (samples.size + frame.samples.size > maxSamples) {
                        throw IllegalStateException(
                            "Utterance exceeds the configured $maxUtteranceSeconds-second limit.",
                        )
                    }
                    frame.samples.forEach { samples += it / 32768.0f }
                }
            }

            check(samples.isNotEmpty()) { "No microphone audio was captured for this utterance." }

            recognizerMutex.withLock {
                check(!released) { "Speech recognizer has already been released." }
                val engine = recognizer ?: createRecognizer().also { recognizer = it }
                val stream = engine.createStream()
                try {
                    stream.acceptWaveform(samples.toFloatArray(), sampleRate = 16_000)
                    engine.decode(stream)
                    val transcript = engine.getResult(stream).text.trim()
                    SpeechRecognitionResult(
                        text = transcript,
                        languageTag = "es",
                        // Sherpa's OfflineRecognizerResult does not expose calibrated confidence.
                        confidence = null,
                    )
                } finally {
                    stream.release()
                }
            }
        }

    private fun createRecognizer(): OfflineRecognizer {
        val encoder = File(modelDirectory, "encoder_model.ort")
        val decoder = File(modelDirectory, "decoder_model_merged.ort")
        val tokens = File(modelDirectory, "tokens.txt")
        listOf(encoder, decoder, tokens).forEach { file ->
            check(file.isFile && file.canRead()) {
                "Required Spanish Moonshine model file is missing or unreadable: ${file.absolutePath}"
            }
        }

        val modelConfig = OfflineModelConfig(
            moonshine = OfflineMoonshineModelConfig(
                encoder = encoder.absolutePath,
                mergedDecoder = decoder.absolutePath,
            ),
            tokens = tokens.absolutePath,
            numThreads = numThreads,
            provider = "cpu",
        )
        return OfflineRecognizer(
            config = OfflineRecognizerConfig(modelConfig = modelConfig),
        )
    }

    override suspend fun release() = recognizerMutex.withLock {
        if (released) return@withLock
        released = true
        recognizer?.release()
        recognizer = null
    }
}
