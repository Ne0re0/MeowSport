package fr.meowsport.ui.templates

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.meowsport.SettingsStore
import fr.meowsport.data.EditLine
import fr.meowsport.data.Equipment
import fr.meowsport.data.Repository
import kotlinx.coroutines.launch

/** An edited line plus a stable key for the list. */
data class UiLine(val key: Long, val data: EditLine)

class TemplateEditViewModel(
    private val repo: Repository,
    private val settings: SettingsStore,
    private val templateId: Long?,
) : ViewModel() {
    var name by mutableStateOf("")
    val lines = mutableStateListOf<UiLine>()
    var loaded by mutableStateOf(templateId == null)
        private set
    var dirty by mutableStateOf(false)
        private set
    private var nextKey = 1L

    init {
        if (templateId != null) {
            viewModelScope.launch {
                repo.template(templateId)?.let { full ->
                    name = full.template.name
                    full.sorted.forEach { l ->
                        lines += UiLine(
                            nextKey++,
                            EditLine(
                                lineId = l.line.id,
                                exercise = l.exercise,
                                sets = l.line.sets,
                                reps = l.line.reps,
                                weight = l.line.weight,
                                perHand = l.line.perHand,
                                restSetSec = l.line.restSetSec,
                                restAfterSec = l.line.restAfterSec,
                                note = l.line.note,
                            ),
                        )
                    }
                }
                loaded = true
            }
        }
    }

    fun rename(v: String) {
        name = v
        dirty = true
    }

    fun addExercises(ids: List<Long>) {
        viewModelScope.launch {
            for (id in ids) {
                val ex = repo.exercise(id) ?: continue
                lines += UiLine(
                    nextKey++,
                    EditLine(
                        lineId = null,
                        exercise = ex,
                        sets = settings.value.defaultSets,
                        reps = settings.value.defaultReps,
                        weight = 0.0,
                        perHand = ex.equipment == Equipment.DUMBBELL || ex.equipment == Equipment.KETTLEBELL,
                        restSetSec = settings.value.defaultRestSetSec,
                        restAfterSec = settings.value.defaultRestAfterSec,
                        note = "",
                    ),
                )
            }
            if (ids.isNotEmpty()) dirty = true
        }
    }

    fun update(key: Long, change: (EditLine) -> EditLine) {
        val i = lines.indexOfFirst { it.key == key }
        if (i >= 0) {
            lines[i] = lines[i].copy(data = change(lines[i].data))
            dirty = true
        }
    }

    fun remove(key: Long) {
        lines.removeAll { it.key == key }
        dirty = true
    }

    fun move(key: Long, delta: Int) {
        val i = lines.indexOfFirst { it.key == key }
        val j = i + delta
        if (i < 0 || j !in lines.indices) return
        val item = lines.removeAt(i)
        lines.add(j, item)
        dirty = true
    }

    /** Returns an error message, or null when saved. */
    fun save(onSaved: () -> Unit): String? {
        if (name.isBlank()) return "Donne un nom à ta séance."
        if (lines.isEmpty()) return "Ajoute au moins un exercice."
        viewModelScope.launch {
            repo.saveTemplate(templateId, name, lines.map { it.data })
            dirty = false
            onSaved()
        }
        return null
    }
}
