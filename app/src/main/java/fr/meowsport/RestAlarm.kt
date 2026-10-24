package fr.meowsport

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import fr.meowsport.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Watches the running session and buzzes when its rest timer reaches zero. */
class RestAlarm(private val context: Context, private val repo: Repository) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            repo.observeActiveSession()
                .map { s -> s?.let { it.id to it.restEndsAt } }
                .distinctUntilChanged()
                .collectLatest { pair ->
                    val (sessionId, endsAt) = pair ?: return@collectLatest
                    if (endsAt == null) return@collectLatest
                    val wait = endsAt - System.currentTimeMillis()
                    if (wait > 0) {
                        delay(wait)
                        alert()
                    }
                    repo.setRest(sessionId, null, 0)
                }
        }
    }

    private fun alert() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 350, 150, 600), -1))
        }
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 800)
        }
    }
}
