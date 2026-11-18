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
 * Sherpa-ONNX Moonshine v2 offline recognizer for Windows x64/JVM.
 *
 * The host installs the Spanish model directory locally and starts the JVM
 * with -Djava.library.path pointing to the directory containing the two
 * Sherpa Windows DLLs prepared by :lisom-platform:prepareSherpaWindowsNativeRuntime.
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
        require(maxUtteranceSeconds in 1..60) {
            "maxUtteranceSeconds must be between 1 and 60."
        }
    }

    override suspend fun recognize(audio: Flow<PcmAudioFrame>): SpeechRecognitionResult =
        withContext(Dispatchers.IO) {
            check(!released) { "Speech recognizer has already been released." }

            val maxSamples = 16_000 * maxUtteranceSeconds
            val samples = ArrayList<Float>(maxSamples.coerceAtMost(16_000 * 5))

            audio.takeWhile { !it.isEndOfStream }.collect { frame ->
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

            check(samples.isNotEmpty()) {
                "No microphone audio was captured for this utterance."
            }

            recognizerMutex.withLock {
                check(!released) { "Speech recognizer has already been released." }
                val engine = recognizer ?: createRecognizer().also { recognizer = it }
                val stream = engine.createStream()
                try {
                    stream.acceptWaveform(samples.toFloatArray(), 16_000)
                    engine.decode(stream)
                    val transcript = engine.getResult(stream).text.trim()
                    SpeechRecognitionResult(
                        text = transcript,
                        languageTag = "es",
                        confidence = null,
                    )
                } finally {
                    stream.release()
                }
            }
        }

    private fun createRecognizer(): OfflineRecognizer {
        check(System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            "This adapter requires Windows."
        }
        check(System.getProperty("os.arch").let { it == "amd64" || it == "x86_64" }) {
            "This adapter currently supports Windows x64 only."
        }

        val nativeDirectories = System.getProperty("java.library.path")
            .orEmpty()
            .split(File.pathSeparator)
            .filter(String::isNotBlank)
            .map(::File)

        val nativeRuntimeReady = nativeDirectories.any { directory ->
            File(directory, "sherpa-onnx-jni.dll").isFile &&
                File(directory, "onnxruntime.dll").isFile
        }
        check(nativeRuntimeReady) {
            "Sherpa Windows DLLs were not found on java.library.path. Run " +
                ":lisom-platform:prepareSherpaWindowsNativeRuntime and launch the host JVM with " +
                "-Djava.library.path=<lisom-platform build/third-party/sherpa-onnx-win-x64 directory>."
        }

        val encoder = File(modelDirectory, "encoder_model.ort")
        val decoder = File(modelDirectory, "decoder_model_merged.ort")
        val tokens = File(modelDirectory, "tokens.txt")
        listOf(encoder, decoder, tokens).forEach { file ->
            check(file.isFile && file.canRead()) {
                "Required Spanish Moonshine model file is missing or unreadable: ${file.absolutePath}"
            }
        }

        val moonshineConfig = OfflineMoonshineModelConfig.builder()
            .setEncoder(encoder.absolutePath)
            .setMergedDecoder(decoder.absolutePath)
            .build()
        val modelConfig = OfflineModelConfig.builder()
            .setMoonshine(moonshineConfig)
            .setTokens(tokens.absolutePath)
            .setNumThreads(numThreads)
            .setProvider("cpu")
            .build()
        val recognizerConfig = OfflineRecognizerConfig.builder()
            .setOfflineModelConfig(modelConfig)
            .build()
        return OfflineRecognizer(recognizerConfig)
    }

    override suspend fun release() = recognizerMutex.withLock {
        if (released) return@withLock
        released = true
        recognizer?.release()
        recognizer = null
    }
}
