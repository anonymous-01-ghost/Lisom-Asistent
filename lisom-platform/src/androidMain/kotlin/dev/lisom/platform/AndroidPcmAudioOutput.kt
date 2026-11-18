package dev.lisom.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import dev.lisom.tts.SpeechAudioOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Plays normalized mono float PCM through Android's local audio output. */
class AndroidPcmAudioOutput : SpeechAudioOutput {
    @Volatile
    private var currentTrack: AudioTrack? = null

    override suspend fun play(samples: FloatArray, sampleRateHz: Int) {
        require(samples.isNotEmpty()) { "Generated speech audio cannot be empty." }
        require(sampleRateHz > 0) { "Generated speech sample rate must be positive." }

        withContext(Dispatchers.IO) {
            stop()

            val bufferBytes = AudioTrack.getMinBufferSize(
                sampleRateHz,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
            )
            check(bufferBytes > 0) { "Android could not create a valid TTS output buffer." }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRateHz)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(bufferBytes, samples.size * Float.SIZE_BYTES))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            check(track.state == AudioTrack.STATE_INITIALIZED) {
                track.release()
                "Android could not initialize the TTS audio output."
            }

            synchronized(this@AndroidPcmAudioOutput) {
                currentTrack = track
            }

            try {
                track.play()
                val written = track.write(
                    samples,
                    0,
                    samples.size,
                    AudioTrack.WRITE_BLOCKING,
                )
                check(written >= 0) {
                    "Android TTS audio output failed with code " + written + "."
                }

                while (track.playState == AudioTrack.PLAYSTATE_PLAYING &&
                    track.playbackHeadPosition < written
                ) {
                    delay(10)
                }
            } finally {
                synchronized(this@AndroidPcmAudioOutput) {
                    if (currentTrack === track) currentTrack = null
                }
                runCatching {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
                }
                track.release()
            }
        }
    }

    override suspend fun stop() {
        val track = synchronized(this) {
            currentTrack.also { currentTrack = null }
        }
        if (track != null) {
            // The play() coroutine owns final release in its finally block. Stopping
            // here unblocks writes without releasing the same AudioTrack twice.
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            }
        }
    }
}
