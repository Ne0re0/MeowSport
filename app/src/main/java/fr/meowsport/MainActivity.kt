package fr.meowsport

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.meowsport.ui.MeowSportNav
import fr.meowsport.ui.theme.MeowSportTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Increased each time the session notification is tapped, so the app opens the running session. */
    private val openSessionRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handle(intent)
        setContent {
            val settings by app.settings.state.collectAsState()
            MeowSportTheme(mode = settings.theme, accent = settings.accent) {
                MeowSportNav(openSessionRequest = openSessionRequests.intValue)
                fr.meowsport.ui.UpdateDialog()
            }
        }
        // Show the session notification as long as a session is running.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(app.repo.observeActiveSession(), app.settings.state) { s, set ->
                    s?.id?.takeIf { set.sessionNotification }
                }
                    .distinctUntilChanged()
                    .collect { id -> if (id != null) SessionService.start(this@MainActivity) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        app.updater.checkOnLaunch()
        app.updater.resumePendingInstall()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(SessionService.EXTRA_OPEN_SESSION, false) == true) {
            openSessionRequests.intValue++
            intent.removeExtra(SessionService.EXTRA_OPEN_SESSION)
        }
    }
}
