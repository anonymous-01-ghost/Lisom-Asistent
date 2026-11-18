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
 * The host supplies the model directory and a keywords.txt generated for
 * "oye lisom". No model or keyword assets are downloaded at runtime.
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

        val modelConfig = OnlineModelConfig(
            transducer = OnlineTransducerModelConfig(
                encoder = encoder.absolutePath,
                decoder = decoder.absolutePath,
                joiner = joiner.absolutePath,
            ),
            tokens = tokens.absolutePath,
            numThreads = numThreads,
            provider = "cpu",
            modelType = "zipformer2",
            modelingUnit = "bpe",
            bpeVocab = bpeModel.absolutePath,
        )
        val spotter = KeywordSpotter(
            config = KeywordSpotterConfig(
                modelConfig = modelConfig,
                keywordsFile = keywords.absolutePath,
                keywordsScore = keywordsScore,
                keywordsThreshold = keywordsThreshold,
            ),
        )
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
                        // Sherpa emits the tag after @ in keywords.txt.
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
