package dev.lisom.core

import dev.lisom.speech.PcmAudioFrame
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PcmUtteranceFlowTest {

    @Test
    fun closesAfterConfiguredTrailingSilenceAndKeepsPreRoll() = runBlocking {
        val speechFrame = PcmAudioFrame(ShortArray(1_600) { 2_000 })
        val silenceFrame = PcmAudioFrame(ShortArray(1_600))
        val followingAudio = PcmAudioFrame(ShortArray(1_600) { 2_000 })

        val result = flowOf(
            speechFrame,
            silenceFrame,
            silenceFrame,
            silenceFrame,
            followingAudio,
        ).untilSpeechEndpoint(
            trailingSilenceMillis = 200,
        ).toList()

        assertEquals(4, result.size)
        assertEquals(speechFrame.samples.size, result.first().samples.size)
        assertTrue(result.last().isEndOfStream)
    }

    @Test
    fun preservesAnExistingEndOfStreamMarker() = runBlocking {
        val speechFrame = PcmAudioFrame(ShortArray(1_600) { 2_000 })
        val endFrame = PcmAudioFrame(samples = shortArrayOf(), isEndOfStream = true)

        val result = flowOf(speechFrame, endFrame)
            .untilSpeechEndpoint()
            .toList()

        assertEquals(2, result.size)
        assertTrue(result.last().isEndOfStream)
    }

    @Test
    fun boundsUtteranceByMaximumDuration() = runBlocking {
        val first = PcmAudioFrame(
            samples = ShortArray(16_000) { 2_000 },
        )
        val second = PcmAudioFrame(
            samples = ShortArray(16_000) { 2_000 },
        )

        val result = flowOf(first, second)
            .untilSpeechEndpoint(
                sampleRateHz = 16_000,
                maxUtteranceSeconds = 1,
            )
            .toList()

        assertEquals(2, result.size)
        assertEquals(16_000, result.first().samples.size)
        assertTrue(result.last().isEndOfStream)
    }
}
