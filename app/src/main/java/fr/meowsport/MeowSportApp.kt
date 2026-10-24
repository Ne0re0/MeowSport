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

    override fun onCreate() {
        super.onCreate()
        repo = Repository(AppDatabase.build(this))
        profile = ProfilePrefs(this)
        scope.launch { repo.seedCatalog() }
        RestAlarm(this, repo).start(scope)
    }
}

val Context.app: MeowSportApp get() = applicationContext as MeowSportApp
