package fr.meowsport

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Profile(val name: String, val weeklyGoal: Int)

class ProfilePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<Profile> = _state

    private fun read() = Profile(
        name = prefs.getString("name", null) ?: "Athlète",
        weeklyGoal = prefs.getInt("weeklyGoal", 3),
    )

    fun update(p: Profile) {
        prefs.edit().putString("name", p.name).putInt("weeklyGoal", p.weeklyGoal).apply()
        _state.value = p
    }
}
