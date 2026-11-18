package dev.lisom.platform

import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import dev.lisom.speech.PcmAudioFrame
import dev.lisom.wakeword.WakeWordDetector
import dev.lisom.wakeword.WakeWordEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.takeWhile
import java.io.File

/**
 * Local Spanish open-vocabulary keyword detector using Sherpa-ONNX Zipformer KWS.
 *
 * Requires the Windows x64 native DLLs on java.library.path and a locally
 * installed Spanish KWS model directory containing a generated keywords.txt.
 */
class SherpaZipformerWakeWordDetector(
    private val modelDirectory: File,
    private val numThreads: Int = 1,
    private val keywordsScore: Float = 1.0f,
    private val keywordsThreshold: Float = 0.20f,
) : WakeWordDetector {

    init {
        require(numThreads > 0) { "numThreads must be greater than zero." }
        require(keywordsScore > 0f) { "keywordsScore must be greater than zero." }
        require(keywordsThreshold in 0f..1f) {
            "keywordsThreshold must be between 0 and 1."
        }
    }

    override fun observe(audio: Flow<PcmAudioFrame>): Flow<WakeWordEvent> = flow {
        check(System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            "This wake-word adapter requires Windows."
        }
        check(System.getProperty("os.arch").let { it == "amd64" || it == "x86_64" }) {
            "This wake-word adapter currently supports Windows x64 only."
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

        val encoder = File(modelDirectory, "encoder-epoch-20-avg-2-chunk-16-left-64.onnx")
        val decoder = File(modelDirectory, "decoder-epoch-20-avg-2-chunk-16-left-64.onnx")
        val joiner = File(modelDirectory, "joiner-epoch-20-avg-2-chunk-16-left-64.onnx")
        val tokens = File(modelDirectory, "tokens.txt")
        val bpeModel = File(modelDirectory, "bpe.model")
        val keywords = File(modelDirectory, "keywords.txt")
        listOf(encoder, decoder, joiner, tokens, bpeModel, keywords).forEach { file ->
            check(file.isFile && file.canRead()) {
                "Required Spanish KWS asset is missing or unreadable: ${file.absolutePath}"
            }
        }

        val transducer = OnlineTransducerModelConfig.builder()
            .setEncoder(encoder.absolutePath)
            .setDecoder(decoder.absolutePath)
            .setJoiner(joiner.absolutePath)
            .build()
        val modelConfig = OnlineModelConfig.builder()
            .setTransducer(transducer)
            .setTokens(tokens.absolutePath)
            .setNumThreads(numThreads)
            .setProvider("cpu")
            .setModelType("zipformer2")
            .setModelingUnit("bpe")
            .setBpeVocab(bpeModel.absolutePath)
            .build()
        val config = KeywordSpotterConfig.builder()
            .setOnlineModelConfig(modelConfig)
            .setKeywordsFile(keywords.absolutePath)
            .setKeywordsScore(keywordsScore)
            .setKeywordsThreshold(keywordsThreshold)
            .build()
        val spotter = KeywordSpotter(config)
        val stream = spotter.createStream()
        try {
            audio.takeWhile { !it.isEndOfStream }.collect { frame ->
                require(frame.sampleRateHz == 16_000 && frame.channelCount == 1) {
                    "Sherpa KWS requires mono 16 kHz PCM."
                }
                val samples = FloatArray(frame.samples.size) { index ->
                    frame.samples[index] / 32768.0f
                }
                stream.acceptWaveform(samples, frame.sampleRateHz)

                while (spotter.isReady(stream)) {
                    spotter.decode(stream)
                    val keyword = spotter.getResult(stream).keyword
                    if (keyword.isNotBlank()) {
                        spotter.reset(stream)
                        emit(
                            WakeWordEvent(
                                phrase = keyword.replace('_', ' '),
                                confidence = null,
                            ),
                        )
                    }
                }
            }
        } finally {
            stream.release()
            spotter.release()
        }
    }.flowOn(Dispatchers.IO)
}
