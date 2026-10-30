package fr.meowsport

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import fr.meowsport.data.FinishResult
import fr.meowsport.data.Session
import fr.meowsport.ui.formatDuration
import fr.meowsport.ui.formatVolume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps an ongoing notification for the whole session: session clock, rest countdown,
 * pause / resume and "finish and save". It also keeps the app alive so the rest alarm rings on time.
 */
class SessionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watcher: Job? = null
    private val repo get() = app.repo

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannels(this)
        // Must be called quickly after startForegroundService().
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            baseBuilder("Séance en cours").build(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        when (intent?.action) {
            ACTION_TOGGLE_PAUSE -> intent.getLongExtra(EXTRA_SESSION, -1).takeIf { it > 0 }?.let { id ->
                app.scope.launch { app.repo.togglePause(id) }
            }
            ACTION_FINISH -> intent.getLongExtra(EXTRA_SESSION, -1).takeIf { it > 0 }?.let { id ->
                // App scope: this service stops itself as soon as the session is over.
                val context = applicationContext
                app.scope.launch {
                    val result = runCatching { app.repo.finishSession(id) }.getOrNull()
                    if (result != null) showResult(context, result)
                }
            }
        }
        if (watcher == null) watcher = scope.launch { watch() }
        return START_STICKY
    }

    private suspend fun watch() {
        repo.observeActiveSession().collect { s ->
            if (s == null) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                notify(build(s))
            }
        }
    }

    private fun notify(n: android.app.Notification) {
        runCatching { NotificationManagerCompat.from(this).notify(NOTIF_ID, n) }
    }

    private fun build(s: Session): android.app.Notification {
        val now = System.currentTimeMillis()
        val elapsedNow = SystemClock.elapsedRealtime()
        val paused = s.pausedAt != null
        val views = RemoteViews(packageName, R.layout.notification_session)
        views.setTextViewText(R.id.title, if (paused) "⏸ En pause · ${s.name}" else s.name)
        views.setChronometer(R.id.session_clock, elapsedNow - s.elapsedMs(now), null, !paused)
        val restEnd = s.restEndsAt
        if (restEnd != null && restEnd > now) {
            views.setViewVisibility(R.id.rest_box, View.VISIBLE)
            views.setChronometer(R.id.rest_clock, elapsedNow + (restEnd - now), null, true)
            views.setChronometerCountDown(R.id.rest_clock, true)
        } else {
            views.setViewVisibility(R.id.rest_box, View.GONE)
        }
        views.setImageViewResource(R.id.btn_pause, if (paused) R.drawable.ic_notif_play else R.drawable.ic_notif_pause)
        views.setContentDescription(R.id.btn_pause, getString(if (paused) R.string.notif_resume else R.string.notif_pause))
        views.setOnClickPendingIntent(R.id.btn_pause, serviceIntent(ACTION_TOGGLE_PAUSE, s.id, 1))
        views.setOnClickPendingIntent(R.id.btn_finish, serviceIntent(ACTION_FINISH, s.id, 2))

        // Buttons live in the compact view itself, so nothing needs to be expanded.
        return baseBuilder(s.name)
            .setContentText(if (paused) "En pause" else s.name)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .build()
    }

    private fun baseBuilder(title: String) = NotificationCompat.Builder(this, CHANNEL_SESSION)
        .setSmallIcon(R.drawable.ic_stat_meow)
        .setContentTitle(title)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setShowWhen(false)
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(openAppIntent(this))

    private fun serviceIntent(action: String, sessionId: Long, code: Int): PendingIntent =
        PendingIntent.getService(
            this,
            code,
            Intent(this, SessionService::class.java).setAction(action).putExtra(EXTRA_SESSION, sessionId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_SESSION = "session"
        private const val CHANNEL_RESULT = "result"
        private const val NOTIF_ID = 1
        private const val RESULT_ID = 2
        private const val ACTION_TOGGLE_PAUSE = "fr.meowsport.TOGGLE_PAUSE"
        private const val ACTION_FINISH = "fr.meowsport.FINISH"
        private const val EXTRA_SESSION = "session_id"
        const val EXTRA_OPEN_SESSION = "open_session"

    private fun showResult(context: Context, r: FinishResult) {
        val improved = r.improvements.isNotEmpty()
        val summary = "${formatDuration(r.durationMs)} · ${r.doneSets} séries · ${formatVolume(r.volume)}"
        val text = if (improved) "$summary\n" + r.improvements.joinToString("\n") { "💪 $it" } else summary
        createChannels(context)
        val n = NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(R.drawable.ic_stat_meow)
            .setContentTitle(if (improved) "🏆 Bravo, tu as fait mieux que la dernière fois !" else "✅ Séance terminée")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(RESULT_ID, n) }
    }

        fun start(context: Context) {
            runCatching {
                androidx.core.content.ContextCompat.startForegroundService(
                    context,
                    Intent(context, SessionService::class.java),
                )
            }
        }

        private fun openAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_OPEN_SESSION, true)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun createChannels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_SESSION, "Séance en cours", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Chronomètre et commandes pendant la séance"
                    setShowBadge(false)
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_RESULT, "Fin de séance", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }
}
