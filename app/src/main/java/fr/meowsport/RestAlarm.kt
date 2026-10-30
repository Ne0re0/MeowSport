package fr.meowsport

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import fr.meowsport.data.Repository
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Watches the running session's rest timer: beeps at 2 s, 1 s and 0 s (the last one louder),
 * and an optional vibration at zero, depending on the settings. A wake lock keeps the phone awake for the rest, screen off included.
 */
class RestAlarm(
    private val context: Context,
    private val repo: Repository,
    private val settings: SettingsStore,
) {
    private val sound = CountdownSound(context)
    private val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager

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
                        val lock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MeowSport:rest")
                        lock.acquire(wait + 10_000)
                        try {
                            val set = settings.value
                            coroutineScope {
                                if (set.countdownBeeps && wait > COUNTDOWN_MS) {
                                    delay(wait - COUNTDOWN_MS)
                                    // The beeps run alongside: the last one sounds right at zero.
                                    launch {
                                        // A sound problem must never stop the rest timer from working.
                                        try {
                                            sound.play(set.beepVolume / 100f, set.duckOtherApps)
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            Log.w("MeowSport", "Rest countdown sound failed", e)
                                        }
                                    }
                                }
                                delay((endsAt - System.currentTimeMillis()).coerceAtLeast(0))
                                if (set.vibrateAtEnd) vibrate()
                            }
                        } finally {
                            if (lock.isHeld) lock.release()
                        }
                    }
                    repo.setRest(sessionId, null, 0)
                }
        }
    }

    /** Plays the end-of-rest beeps once, from the settings screen. */
    fun test(scope: CoroutineScope) {
        val set = settings.value
        scope.launch { runCatching { sound.play(set.beepVolume / 100f, set.duckOtherApps) } }
    }

    private fun vibrate() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 350), -1))
        }
    }

    private companion object {
        const val COUNTDOWN_MS = 2_000L
    }
}
