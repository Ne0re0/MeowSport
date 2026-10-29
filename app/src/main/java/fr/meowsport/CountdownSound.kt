package fr.meowsport

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * "Tut, tut, tuuuut" played during the last 3 seconds of a rest.
 * It asks for short audio focus with ducking, so music from other apps gets quieter meanwhile.
 */
class CountdownSound(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** 3 seconds of sound: short beep at 0 s and 1 s, long higher beep from 2 s to 3 s. */
    private val pcm: ShortArray by lazy {
        val buf = ShortArray(SAMPLE_RATE * 3)
        tone(buf, startSec = 0.0, lengthSec = 0.18, freq = 880.0)
        tone(buf, startSec = 1.0, lengthSec = 0.18, freq = 880.0)
        tone(buf, startSec = 2.0, lengthSec = 0.9, freq = 1320.0)
        buf
    }

    private fun tone(buf: ShortArray, startSec: Double, lengthSec: Double, freq: Double) {
        val start = (startSec * SAMPLE_RATE).toInt()
        val n = (lengthSec * SAMPLE_RATE).toInt()
        val fade = (0.01 * SAMPLE_RATE).toInt()
        for (i in 0 until n) {
            val env = min(1.0, min(i, n - i).toDouble() / fade)
            val v = sin(2 * PI * freq * i / SAMPLE_RATE) * env * 0.8
            buf[start + i] = (v * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Plays the countdown and suspends until it is over. Stops early if the coroutine is cancelled. */
    suspend fun play() {
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            audio.requestAudioFocus(focus)
            track.write(pcm, 0, pcm.size)
            track.play()
            delay(3_100)
        } finally {
            runCatching { track.stop() }
            track.release()
            audio.abandonAudioFocusRequest(focus)
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
    }
}
