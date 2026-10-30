package fr.meowsport

import android.app.Application
import android.content.Context
import fr.meowsport.data.AppDatabase
import fr.meowsport.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MeowSportApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var repo: Repository
        private set
    lateinit var profile: ProfilePrefs
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var restAlarm: RestAlarm
        private set
    val backup by lazy { fr.meowsport.data.Backup(repo) }

    override fun onCreate() {
        super.onCreate()
        repo = Repository(AppDatabase.build(this))
        profile = ProfilePrefs(this)
        settings = SettingsStore(this)
        scope.launch { repo.seedCatalog() }
        restAlarm = RestAlarm(this, repo, settings).also { it.start(scope) }
    }
}

val Context.app: MeowSportApp get() = applicationContext as MeowSportApp
