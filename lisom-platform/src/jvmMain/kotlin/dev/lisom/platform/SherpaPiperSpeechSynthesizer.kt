package dev.lisom.platform

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import dev.lisom.tts.SpeechAudioOutput
import dev.lisom.tts.SpeechSynthesizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Offline Spanish Piper/VITS synthesizer for Windows/JVM.
 *
 * The host supplies a local model directory containing the selected .onnx voice,
 * tokens.txt, and espeak-ng-data. No model is downloaded by this class.
 */
class SherpaPiperSpeechSynthesizer(
    private val modelDirectory: File,
    private val audioOutput: SpeechAudioOutput,
    private val modelFileName: String,
    /** Optional SHA-256 of the local model file, lowercase or uppercase hex. */
    private val expectedModelSha256: String? = null,
    private val dataDirectoryName: String = "espeak-ng-data",
    private val numThreads: Int = 2,
    private val speakerId: Int = 0,
    private val speed: Float = 1.0f,
) : SpeechSynthesizer {
    private val engineMutex = Mutex()
    private var engine: OfflineTts? = null
    private var released = false

    init {
        require(modelFileName.isNotBlank()) { "modelFileName cannot be blank." }
        require(expectedModelSha256 == null || expectedModelSha256.matches(Regex("[0-9a-fA-F]{64}"))) {
            "expectedModelSha256 must be exactly 64 hexadecimal characters."
        }
        require(numThreads > 0) { "numThreads must be greater than zero." }
        require(speakerId >= 0) { "speakerId cannot be negative." }
        require(speed > 0f) { "speed must be greater than zero." }
    }

    override suspend fun speak(text: String) {
        require(text.isNotBlank()) { "Speech text cannot be blank." }

        val generated = withContext(Dispatchers.Default) {
            engineMutex.withLock {
                check(!released) { "Speech synthesizer has already been released." }
                val current = engine ?: createEngine().also { engine = it }
                current.generate(text, speakerId, speed)
            }
        }

        audioOutput.play(generated.samples, generated.sampleRate)
    }

    override suspend fun stop() {
        audioOutput.stop()
    }

    override suspend fun release() {
        engineMutex.withLock {
            if (released) return@withLock
            released = true
            engine?.release()
            engine = null
        }
        audioOutput.release()
    }

    private fun verifyModelChecksum(model: File) {
        val expected = expectedModelSha256 ?: return
        val digest = MessageDigest.getInstance("SHA-256")
        model.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        check(actual.equals(expected, ignoreCase = true)) {
            "Local Spanish TTS model SHA-256 mismatch. Expected " + expected + ", got " + actual
        }
    }

    private fun createEngine(): OfflineTts {
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
        check(nativeDirectories.any { directory ->
            File(directory, "sherpa-onnx-jni.dll").isFile &&
                File(directory, "onnxruntime.dll").isFile
        }) {
            "Sherpa Windows DLLs were not found on java.library.path."
        }

        val model = File(modelDirectory, modelFileName)
        val tokens = File(modelDirectory, "tokens.txt")
        val dataDirectory = File(modelDirectory, dataDirectoryName)
        listOf(model, tokens, dataDirectory).forEach { file ->
            check(file.exists() && file.canRead()) {
                "Required local Spanish TTS asset is missing or unreadable: " + file.absolutePath
            }
        }
        verifyModelChecksum(model)

        val vits = OfflineTtsVitsModelConfig.builder()
            .setModel(model.absolutePath)
            .setTokens(tokens.absolutePath)
            .setDataDir(dataDirectory.absolutePath)
            .build()
        val modelConfig = OfflineTtsModelConfig.builder()
            .setVits(vits)
            .setNumThreads(numThreads)
            .setProvider("cpu")
            .build()
        val config = OfflineTtsConfig.builder()
            .setModel(modelConfig)
            .setMaxNumSentences(1)
            .setSilenceScale(0.2f)
            .build()
        return OfflineTts(config)
    }
}
