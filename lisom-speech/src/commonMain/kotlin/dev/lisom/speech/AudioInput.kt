package dev.lisom.speech

import kotlinx.coroutines.flow.Flow

/**
 * Platform audio adapter. frames must be a hot, multicast stream while capture is active,
 * so keyword spotting and recognition can observe one microphone source concurrently.
 */
interface AudioInput {
    val frames: Flow<PcmAudioFrame>
    suspend fun start()
    suspend fun stop()
}

data class PcmAudioFrame(
    val samples: ShortArray,
    val sampleRateHz: Int = 16_000,
    val channelCount: Int = 1,
) {
    init {
        require(samples.isNotEmpty()) { "Audio frame cannot be empty." }
        require(sampleRateHz == 16_000) { "Audio must be sampled at 16 kHz." }
        require(channelCount == 1) { "The initial pipeline supports mono audio only." }
    }
}
