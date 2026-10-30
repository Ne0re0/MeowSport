package fr.meowsport

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * One beep per second during the last 3 seconds of a rest.
 * It asks for short audio focus with ducking, so music from other apps gets quieter meanwhile.
 *
 * The sound uses the "navigation guidance" usage, like GPS voice prompts: it follows the media
 * volume and still plays when the phone is on silent or vibrate, unlike system sounds.
 */
class CountdownSound(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** Beeps at 0 s, 1 s and 2 s, i.e. 3, 2 and 1 seconds before the end of the rest. */
    private val pcm: ShortArray by lazy {
        val buf = ShortArray((SAMPLE_RATE * 2.3).toInt())
        for (i in 0 until 3) tone(buf, startSec = i.toDouble(), lengthSec = 0.22, freq = 988.0)
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
        audio.requestAudioFocus(focus)
        try {
            val played = try {
                playTrack()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            if (!played) playFallback()
        } finally {
            audio.abandonAudioFocusRequest(focus)
        }
    }

    private suspend fun playTrack() {
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
            check(track.state == AudioTrack.STATE_INITIALIZED) { "AudioTrack not initialized" }
            val written = track.write(pcm, 0, pcm.size)
            check(written == pcm.size) { "AudioTrack write failed: $written" }
            track.setVolume(1f)
            track.play()
            delay(2_400)
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    /** Simpler beeps on the media stream, used if the generated sound cannot be played. */
    private suspend fun playFallback() {
        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        try {
            repeat(3) { i ->
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 220)
                if (i < 2) delay(1_000)
            }
            delay(400)
        } finally {
            tone.release()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
    }
}
