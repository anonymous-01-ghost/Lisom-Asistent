package dev.lisom.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import dev.lisom.speech.AudioInput
import dev.lisom.speech.PcmAudioFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Android microphone capture: mono PCM16 at 16 kHz. The host application must
 * request RECORD_AUDIO at runtime before calling start().
 */
class AndroidAudioInput(
    private val context: Context,
) : AudioInput, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycleMutex = Mutex()
    private val mutableFrames = MutableSharedFlow<PcmAudioFrame>(
        replay = 1,
        extraBufferCapacity = 64,
    )
    private var recorder: AudioRecord? = null
    private var captureJob: Job? = null

    override val frames: Flow<PcmAudioFrame> = mutableFrames.asSharedFlow()

    override suspend fun start() = lifecycleMutex.withLock {
        if (captureJob?.isActive == true) return@withLock

        check(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        ) { "RECORD_AUDIO permission has not been granted by the host app." }

        mutableFrames.resetReplayCache()

        val sampleRate = 16_000
        val minimumBytes = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minimumBytes > 0) { "Android could not determine a valid microphone buffer size." }

        val bufferBytes = max(minimumBytes, sampleRate * 2 / 10 * 2)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferBytes,
        )
        check(audioRecord.state == AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            "Android AudioRecord could not be initialized."
        }

        try {
            audioRecord.startRecording()
        } catch (failure: Exception) {
            audioRecord.release()
            throw failure
        }

        recorder = audioRecord
        captureJob = scope.launch {
            val buffer = ShortArray(sampleRate / 10)
            try {
                while (true) {
                    val count = audioRecord.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (count > 0) {
                        mutableFrames.emit(
                            PcmAudioFrame(samples = buffer.copyOf(count), sampleRateHz = sampleRate),
                        )
                    } else if (count < 0) {
                        throw IllegalStateException("AudioRecord.read failed with code $count.")
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    override suspend fun stop() = lifecycleMutex.withLock {
        val activeRecorder = recorder
        recorder = null

        if (activeRecorder != null) {
            runCatching {
                if (activeRecorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    activeRecorder.stop()
                }
            }
        }

        captureJob?.cancelAndJoin()
        captureJob = null
        activeRecorder?.release()

        // Explicitly closes the current ASR utterance after all captured frames.
        mutableFrames.emit(
            PcmAudioFrame(
                samples = shortArrayOf(),
                sampleRateHz = 16_000,
                isEndOfStream = true,
            ),
        )
    }

    override fun close() {
        scope.cancel()
    }
}
