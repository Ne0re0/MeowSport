package fr.meowsport

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Profile(
    val name: String,
    val weeklyGoal: Int,
    /** Body weight in kg, or null when not set. */
    val bodyWeight: Double? = null,
    val female: Boolean = false,
)

class ProfilePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<Profile> = _state

    private fun read() = Profile(
        name = prefs.getString("name", null) ?: "Athlète",
        weeklyGoal = prefs.getInt("weeklyGoal", 3),
        bodyWeight = prefs.getFloat("bodyWeight", 0f).takeIf { it > 0f }?.toDouble(),
        female = prefs.getBoolean("female", false),
    )

    fun update(p: Profile) {
        prefs.edit()
            .putString("name", p.name)
            .putInt("weeklyGoal", p.weeklyGoal)
            .putFloat("bodyWeight", (p.bodyWeight ?: 0.0).toFloat())
            .putBoolean("female", p.female)
            .apply()
        _state.value = p
    }
}
