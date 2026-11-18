package dev.lisom.core

import dev.lisom.speech.PcmAudioFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile

/**
 * Builds one bounded command utterance from a continuous microphone stream.
 *
 * It preserves the bounded pre-roll already held by the wake-word frame relay,
 * then emits frames until trailing silence or the maximum duration is reached.
 */
internal fun Flow<PcmAudioFrame>.untilSpeechEndpoint(
    sampleRateHz: Int = 16_000,
    trailingSilenceMillis: Int = 1_200,
    maxUtteranceSeconds: Int = 20,
    meanAbsoluteAmplitudeThreshold: Int = 500,
): Flow<PcmAudioFrame> = flow {
    require(sampleRateHz > 0) { "sampleRateHz must be greater than zero." }
    require(trailingSilenceMillis > 0) { "trailingSilenceMillis must be greater than zero." }
    require(maxUtteranceSeconds > 0) { "maxUtteranceSeconds must be greater than zero." }
    require(meanAbsoluteAmplitudeThreshold in 1..32_767) {
        "meanAbsoluteAmplitudeThreshold must be between 1 and 32767."
    }

    val maxSamples = sampleRateHz.toLong() * maxUtteranceSeconds
    val silenceSamplesRequired = sampleRateHz.toLong() * trailingSilenceMillis / 1_000L
    var totalSamples = 0L
    var trailingSilenceSamples = 0L
    var speechDetected = false
    var endpointReached = false
    var sourceEnded = false

    this@untilSpeechEndpoint.takeWhile { frame ->
        if (endpointReached) {
            false
        } else if (frame.isEndOfStream) {
            sourceEnded = true
            endpointReached = true
            true
        } else {
            val frameSamples = frame.samples
            totalSamples += frameSamples.size.toLong()

            if (frameSamples.isNotEmpty()) {
                var absoluteTotal = 0L
                for (sample in frameSamples) {
                    absoluteTotal += kotlin.math.abs(sample.toInt()).toLong()
                }
                val meanAbsoluteAmplitude = absoluteTotal / frameSamples.size
                if (meanAbsoluteAmplitude >= meanAbsoluteAmplitudeThreshold) {
                    speechDetected = true
                    trailingSilenceSamples = 0L
                } else if (speechDetected) {
                    trailingSilenceSamples += frameSamples.size.toLong()
                }
            }

            if (
                totalSamples >= maxSamples ||
                (speechDetected && trailingSilenceSamples >= silenceSamplesRequired)
            ) {
                endpointReached = true
            }
            true
        }
    }.collect { frame ->
        emit(frame)
    }

    if (!sourceEnded) {
        emit(
            PcmAudioFrame(
                samples = shortArrayOf(),
                sampleRateHz = sampleRateHz,
                isEndOfStream = true,
            ),
        )
    }
}
