package dev.lisom.platform

import dev.lisom.tts.SpeechAudioOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

/** Plays normalized mono PCM through the local Windows/JVM audio device. */
class JvmPcmAudioOutput : SpeechAudioOutput {
    @Volatile
    private var currentLine: SourceDataLine? = null

    override suspend fun play(samples: FloatArray, sampleRateHz: Int) {
        require(samples.isNotEmpty()) { "Generated speech audio cannot be empty." }
        require(sampleRateHz > 0) { "Generated speech sample rate must be positive." }

        withContext(Dispatchers.IO) {
            stop()

            val format = AudioFormat(sampleRateHz.toFloat(), 16, 1, true, false)
            val line = AudioSystem.getLine(
                DataLine.Info(SourceDataLine::class.java, format),
            ) as SourceDataLine
            line.open(format)
            synchronized(this@JvmPcmAudioOutput) {
                currentLine = line
            }

            try {
                line.start()
                line.write(toPcm16LittleEndian(samples), 0, samples.size * 2)
                line.drain()
            } finally {
                synchronized(this@JvmPcmAudioOutput) {
                    if (currentLine === line) currentLine = null
                }
                runCatching { line.stop() }
                line.close()
            }
        }
    }

    override suspend fun stop() {
        val line = synchronized(this) {
            currentLine.also { currentLine = null }
        }
        if (line != null) {
            runCatching { line.stop() }
            line.close()
        }
    }

    private fun toPcm16LittleEndian(samples: FloatArray): ByteArray {
        val output = ByteArrayOutputStream(samples.size * 2)
        samples.forEach { sample ->
            val value = (sample.coerceIn(-1f, 1f) * 32767f).toInt()
            output.write(value and 0xff)
            output.write((value ushr 8) and 0xff)
        }
        return output.toByteArray()
    }
}
