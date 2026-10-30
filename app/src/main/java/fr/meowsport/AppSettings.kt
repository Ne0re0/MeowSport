package fr.meowsport

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode(val label: String) { SYSTEM("Système"), LIGHT("Clair"), DARK("Sombre") }

enum class Accent(val label: String, val dark: Long, val light: Long) {
    VIOLET("Violet", 0xFFB69CFF, 0xFF6D3FD8),
    LAVENDER("Lavande", 0xFFD0BCFF, 0xFF7E57C2),
    ORANGE("Orange", 0xFFFF8A3D, 0xFFB65400),
    BLUE("Bleu", 0xFF8AB4FF, 0xFF1E5BC6),
    GREEN("Vert", 0xFF7FD89B, 0xFF1F7A3E),
    PINK("Rose", 0xFFFF9FC6, 0xFFB0306A),
}

/** Every user setting of the app, with its default value. */
data class AppSettings(
    // Appearance
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.VIOLET,
    // New exercises
    val defaultSets: Int = 3,
    val defaultReps: Int = 10,
    val defaultRestSetSec: Int = 90,
    val defaultRestAfterSec: Int = 120,
    // During the session
    val keepScreenOn: Boolean = true,
    val sessionNotification: Boolean = true,
    val autoStartRest: Boolean = true,
    // End of rest
    val countdownBeeps: Boolean = true,
    val beepVolume: Int = 100,
    val duckOtherApps: Boolean = true,
    val vibrateAtEnd: Boolean = false,
    // End of session
    val updateTemplate: Boolean = true,
    val showCongrats: Boolean = true,
    // Profile stats
    val weekStartsMonday: Boolean = true,
    // Updates
    val autoCheckUpdates: Boolean = true,
    val skippedUpdate: Int = 0,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state
    val value: AppSettings get() = _state.value

    private fun read(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            theme = enumOr(prefs.getString("theme", null), d.theme),
            accent = enumOr(prefs.getString("accent", null), d.accent),
            defaultSets = prefs.getInt("defaultSets", d.defaultSets),
            defaultReps = prefs.getInt("defaultReps", d.defaultReps),
            defaultRestSetSec = prefs.getInt("defaultRestSetSec", d.defaultRestSetSec),
            defaultRestAfterSec = prefs.getInt("defaultRestAfterSec", d.defaultRestAfterSec),
            keepScreenOn = prefs.getBoolean("keepScreenOn", d.keepScreenOn),
            sessionNotification = prefs.getBoolean("sessionNotification", d.sessionNotification),
            autoStartRest = prefs.getBoolean("autoStartRest", d.autoStartRest),
            countdownBeeps = prefs.getBoolean("countdownBeeps", d.countdownBeeps),
            beepVolume = prefs.getInt("beepVolume", d.beepVolume),
            duckOtherApps = prefs.getBoolean("duckOtherApps", d.duckOtherApps),
            vibrateAtEnd = prefs.getBoolean("vibrateAtEnd", d.vibrateAtEnd),
            updateTemplate = prefs.getBoolean("updateTemplate", d.updateTemplate),
            showCongrats = prefs.getBoolean("showCongrats", d.showCongrats),
            weekStartsMonday = prefs.getBoolean("weekStartsMonday", d.weekStartsMonday),
            autoCheckUpdates = prefs.getBoolean("autoCheckUpdates", d.autoCheckUpdates),
            skippedUpdate = prefs.getInt("skippedUpdate", d.skippedUpdate),
        )
    }

    fun update(change: (AppSettings) -> AppSettings) {
        val s = change(_state.value)
        prefs.edit()
            .putString("theme", s.theme.name)
            .putString("accent", s.accent.name)
            .putInt("defaultSets", s.defaultSets)
            .putInt("defaultReps", s.defaultReps)
            .putInt("defaultRestSetSec", s.defaultRestSetSec)
            .putInt("defaultRestAfterSec", s.defaultRestAfterSec)
            .putBoolean("keepScreenOn", s.keepScreenOn)
            .putBoolean("sessionNotification", s.sessionNotification)
            .putBoolean("autoStartRest", s.autoStartRest)
            .putBoolean("countdownBeeps", s.countdownBeeps)
            .putInt("beepVolume", s.beepVolume)
            .putBoolean("duckOtherApps", s.duckOtherApps)
            .putBoolean("vibrateAtEnd", s.vibrateAtEnd)
            .putBoolean("updateTemplate", s.updateTemplate)
            .putBoolean("showCongrats", s.showCongrats)
            .putBoolean("weekStartsMonday", s.weekStartsMonday)
            .putBoolean("autoCheckUpdates", s.autoCheckUpdates)
            .putInt("skippedUpdate", s.skippedUpdate)
            .apply()
        _state.value = s
    }

    fun reset() = update { AppSettings() }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
