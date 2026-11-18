package dev.lisom.speech

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PcmAudioFrameTest {

    @Test
    fun acceptsMonoSixteenKhzPcm() {
        val frame = PcmAudioFrame(
            samples = shortArrayOf(0, 1, -1, Short.MAX_VALUE, Short.MIN_VALUE),
        )

        assertEquals(16_000, frame.sampleRateHz)
        assertEquals(1, frame.channelCount)
        assertEquals(5, frame.samples.size)
    }

    @Test
    fun rejectsEmptyFrames() {
        assertFailsWith<IllegalArgumentException> {
            PcmAudioFrame(samples = shortArrayOf())
        }
    }

    @Test
    fun rejectsNonSixteenKhzAudio() {
        assertFailsWith<IllegalArgumentException> {
            PcmAudioFrame(samples = shortArrayOf(1), sampleRateHz = 48_000)
        }
    }

    @Test
    fun rejectsMultiChannelAudio() {
        assertFailsWith<IllegalArgumentException> {
            PcmAudioFrame(samples = shortArrayOf(1), channelCount = 2)
        }
    }
}
