package dev.lisom.platform

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class Pcm16LittleEndianTest {
    @Test
    fun convertsSignedPcm16LittleEndianSamples() {
        val bytes = byteArrayOf(
            0x00,
            0x00,
            0xFF.toByte(),
            0x7F,
            0x00,
            0x80.toByte(),
        )

        assertContentEquals(
            shortArrayOf(0, 32_767, Short.MIN_VALUE),
            pcm16LittleEndianToShortArray(bytes, sampleCount = 3),
        )
    }

    @Test
    fun rejectsSampleCountOutsideInputBuffer() {
        assertFailsWith<IllegalArgumentException> {
            pcm16LittleEndianToShortArray(byteArrayOf(0, 0), sampleCount = 2)
        }
    }
}
