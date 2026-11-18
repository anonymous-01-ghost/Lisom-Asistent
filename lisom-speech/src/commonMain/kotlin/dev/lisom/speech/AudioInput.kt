package dev.lisom.speech

import kotlinx.coroutines.flow.Flow

/**
 * Platform audio adapter. Frames are shared while capture is active so wake-word
 * detection and ASR can observe the same microphone source.
 */
interface AudioInput {
    val frames: Flow<PcmAudioFrame>
    suspend fun start()
    suspend fun stop()

    /** Releases capture resources. Default implementations stop the current session. */
    suspend fun release() = stop()
}

/**
 * Signed PCM16 samples, mono, 16 kHz. An end-of-stream frame has no samples and
 * terminates one capture/recognition session.
 */
data class PcmAudioFrame(
    val samples: ShortArray,
    val sampleRateHz: Int = 16_000,
    val channelCount: Int = 1,
    val isEndOfStream: Boolean = false,
) {
    init {
        require(samples.isNotEmpty() || isEndOfStream) { "Audio frame cannot be empty unless it ends the stream." }
        require(sampleRateHz == 16_000) { "Audio must be sampled at 16 kHz." }
        require(channelCount == 1) { "The initial pipeline supports mono audio only." }
    }
}
