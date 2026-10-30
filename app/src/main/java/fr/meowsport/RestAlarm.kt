package fr.meowsport

import android.content.Context
import android.os.PowerManager
import fr.meowsport.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Watches the running session's rest timer: one beep per second during the last 3 seconds. A wake lock keeps the phone awake for the rest, screen off included.
 */
class RestAlarm(private val context: Context, private val repo: Repository) {
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
                            if (wait > COUNTDOWN_MS) {
                                delay(wait - COUNTDOWN_MS)
                                sound.play()
                            }
                            delay((endsAt - System.currentTimeMillis()).coerceAtLeast(0))
                        } finally {
                            if (lock.isHeld) lock.release()
                        }
                    }
                    repo.setRest(sessionId, null, 0)
                }
        }
    }

    private companion object {
        const val COUNTDOWN_MS = 3_000L
    }
}
