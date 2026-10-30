package fr.meowsport

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Release(val number: Int, val name: String, val notes: String, val apkUrl: String, val size: Long)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    /** Only shown after a manual check. */
    data object UpToDate : UpdateState
    /** Only shown after a manual check. */
    data object Unreachable : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class Failed(val release: Release) : UpdateState
}

/**
 * Looks for a newer GitHub release and installs it when the user agrees.
 * Every network error is swallowed: no GitHub, no update, no fuss.
 */
class Updater(private val context: Context, private val settings: SettingsStore, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state
    private var checkedThisLaunch = false
    private var job: Job? = null

    val currentNumber: Int by lazy {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else @Suppress("DEPRECATION") info.versionCode
        }.getOrDefault(0)
    }

    val currentName: String by lazy {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    /** Automatic check, once per app launch, if enabled. Silent unless a new version exists. */
    fun checkOnLaunch() {
        if (checkedThisLaunch || !settings.value.autoCheckUpdates) return
        checkedThisLaunch = true
        check(manual = false)
    }

    fun check(manual: Boolean) {
        if (job?.isActive == true) return
        job = scope.launch {
            if (manual) _state.value = UpdateState.Checking
            val release = fetchLatest()
            _state.value = when {
                release == null -> if (manual) UpdateState.Unreachable else UpdateState.Idle
                release.number <= currentNumber -> if (manual) UpdateState.UpToDate else UpdateState.Idle
                !manual && release.number == settings.value.skippedUpdate -> UpdateState.Idle
                else -> UpdateState.Available(release)
            }
        }
    }

    fun dismiss() {
        if (_state.value !is UpdateState.Downloading) _state.value = UpdateState.Idle
    }

    fun skip(release: Release) {
        settings.update { it.copy(skippedUpdate = release.number) }
        _state.value = UpdateState.Idle
    }

    fun download(release: Release) {
        if (job?.isActive == true) return
        job = scope.launch {
            _state.value = UpdateState.Downloading(release, 0f)
            val file = runCatching { downloadApk(release) }.getOrNull()
            if (file == null) {
                _state.value = UpdateState.Failed(release)
            } else {
                _state.value = UpdateState.Idle
                install(file)
            }
        }
    }

    private suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "MeowSport")
            }
            try {
                if (conn.responseCode != 200) return@runCatching null
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val tag = json.getString("tag_name")
                val number = tag.substringAfterLast('.').toIntOrNull() ?: return@runCatching null
                val assets = json.getJSONArray("assets")
                val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                    .firstOrNull { it.getString("name").endsWith(".apk") } ?: return@runCatching null
                Release(
                    number = number,
                    name = json.optString("name").ifBlank { tag },
                    notes = json.optString("body").trim(),
                    apkUrl = apk.getString("browser_download_url"),
                    size = apk.optLong("size"),
                )
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private suspend fun downloadApk(release: Release): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "MeowSport-${release.number}.apk")
        var url = URL(release.apkUrl)
        var conn: HttpURLConnection
        // Follow GitHub's redirect to its download host by hand (HTTPS to HTTPS).
        var hops = 0
        while (true) {
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MeowSport")
            }
            val code = conn.responseCode
            if (code in 300..399 && hops++ < 5) {
                url = URL(url, conn.getHeaderField("Location"))
                conn.disconnect()
                continue
            }
            if (code != 200) error("HTTP $code")
            break
        }
        try {
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.size
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var lastShown = 0f
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) {
                            val p = (done.toFloat() / total).coerceIn(0f, 1f)
                            if (p - lastShown >= 0.01f) {
                                lastShown = p
                                _state.value = UpdateState.Downloading(release, p)
                            }
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        file
    }

    private fun install(file: File) {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            // Android asks once to allow installs from MeowSport; the user taps "Mettre à jour" again after.
            pendingFile = file
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private var pendingFile: File? = null

    /** Called when the app comes back to the foreground: resumes an install that waited for permission. */
    fun resumePendingInstall() {
        val file = pendingFile ?: return
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) return
        pendingFile = null
        if (file.exists()) runCatching { install(file) }
    }

    companion object {
        private const val LATEST_URL = "https://api.github.com/repos/Ne0re0/MeowSport/releases/latest"
    }
}
