package dev.lisom.platform

import dev.lisom.speech.AudioInput
import dev.lisom.speech.PcmAudioFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * Windows/JVM microphone capture using Java Sound.
 *
 * Audio is requested as mono, signed PCM16, little-endian, 16 kHz. No audio is
 * written to disk or sent over the network. The host owns UI and lifecycle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WindowsAudioInput(
    private val bufferDurationMillis: Int = 100,
) : AudioInput {
    private val sampleRateHz = 16_000
    private val format = AudioFormat(
        sampleRateHz.toFloat(),
        16,
        1,
        true,
        false,
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycleMutex = Mutex()
    private val mutableFrames = MutableSharedFlow<PcmAudioFrame>(
        replay = 1,
        extraBufferCapacity = 32,
    )

    @Volatile
    private var line: TargetDataLine? = null
    private var captureJob: Job? = null
    private var released = false

    override val frames: Flow<PcmAudioFrame> = mutableFrames.asSharedFlow()

    init {
        require(bufferDurationMillis in 20..500) {
            "bufferDurationMillis must be between 20 and 500."
        }
    }

    override suspend fun start() = lifecycleMutex.withLock {
        check(!released) { "Audio input has already been released." }
        if (captureJob?.isActive == true) return@withLock

        mutableFrames.resetReplayCache()

        val bufferBytes = sampleRateHz * 2 * bufferDurationMillis / 1_000
        val info = DataLine.Info(TargetDataLine::class.java, format, bufferBytes)
        check(AudioSystem.isLineSupported(info)) {
            "No Windows microphone supports mono PCM16 at 16 kHz through Java Sound."
        }

        val microphone = AudioSystem.getLine(info) as TargetDataLine
        try {
            microphone.open(format, bufferBytes)
            microphone.start()
        } catch (failure: Exception) {
            microphone.close()
            throw IllegalStateException(
                "Could not open the Windows microphone. Check the selected input device and Windows microphone privacy settings.",
                failure,
            )
        }

        line = microphone
        captureJob = scope.launch {
            val bytes = ByteArray(bufferBytes)
            try {
                while (true) {
                    val count = microphone.read(bytes, 0, bytes.size)
                    if (count > 0) {
                        val completeSamples = count / 2
                        if (completeSamples > 0) {
                            mutableFrames.emit(
                                PcmAudioFrame(
                                    samples = pcm16LittleEndianToShortArray(bytes, completeSamples),
                                    sampleRateHz = sampleRateHz,
                                ),
                            )
                        }
                    } else if (count < 0) {
                        throw IllegalStateException(
                            "The Windows microphone returned an audio read error ($count).",
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // Stop() closes the line and owns the end-of-stream notification.
                if (line === microphone) {
                    throw IllegalStateException("Windows microphone capture stopped unexpectedly.", failure)
                }
            }
        }
    }

    override suspend fun stop() = lifecycleMutex.withLock {
        val activeLine = line
        line = null

        if (activeLine != null) {
            runCatching { activeLine.stop() }
            activeLine.close()
        }

        captureJob?.cancelAndJoin()
        captureJob = null

        mutableFrames.emit(
            PcmAudioFrame(
                samples = shortArrayOf(),
                sampleRateHz = sampleRateHz,
                isEndOfStream = true,
            ),
        )
    }

    override suspend fun release() {
        lifecycleMutex.withLock {
            if (released) return
            released = true
            val activeLine = line
            line = null
            if (activeLine != null) {
                runCatching { activeLine.stop() }
                activeLine.close()
            }
            captureJob?.cancelAndJoin()
            captureJob = null
            mutableFrames.emit(
                PcmAudioFrame(
                    samples = shortArrayOf(),
                    sampleRateHz = sampleRateHz,
                    isEndOfStream = true,
                ),
            )
            scope.cancel()
        }
    }
}

internal fun pcm16LittleEndianToShortArray(bytes: ByteArray, sampleCount: Int): ShortArray {
    require(sampleCount >= 0 && sampleCount * 2 <= bytes.size) {
        "sampleCount must fit within the supplied PCM byte buffer."
    }
    return ShortArray(sampleCount) { index ->
        val low = bytes[index * 2].toInt() and 0xFF
        val high = bytes[index * 2 + 1].toInt()
        ((high shl 8) or low).toShort()
    }
}
