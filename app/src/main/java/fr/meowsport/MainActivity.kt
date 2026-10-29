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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Increased each time the session notification is tapped, so the app opens the running session. */
    private val openSessionRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handle(intent)
        setContent {
            MeowSportTheme {
                MeowSportNav(openSessionRequest = openSessionRequests.intValue)
            }
        }
        // Show the session notification as long as a session is running.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.repo.observeActiveSession()
                    .map { it?.id }
                    .distinctUntilChanged()
                    .collect { id -> if (id != null) SessionService.start(this@MainActivity) }
            }
        }
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
