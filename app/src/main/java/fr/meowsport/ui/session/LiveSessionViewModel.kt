package fr.meowsport.ui.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.meowsport.data.FinishResult
import fr.meowsport.data.Repository
import fr.meowsport.data.SessionFull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LiveSessionViewModel(private val repo: Repository, val sessionId: Long) : ViewModel() {

    val session: StateFlow<SessionFull?> =
        repo.observeSession(sessionId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Current time, refreshed several times per second for the clocks. */
    val now: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(250)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    var result by mutableStateOf<FinishResult?>(null)
        private set
    var finishing by mutableStateOf(false)
        private set

    fun toggleSet(setId: Long) = viewModelScope.launch {
        val rest = repo.toggleSet(setId)
        if (rest != null && rest > 0) {
            repo.setRest(sessionId, System.currentTimeMillis() + rest * 1000L, rest)
        }
    }

    fun updateSet(setId: Long, weight: Double, reps: Int) = viewModelScope.launch {
        repo.updateSetValues(setId, weight, reps)
    }

    fun addSet(sessionExerciseId: Long) = viewModelScope.launch { repo.addSet(sessionExerciseId) }
    fun removeLastSet(sessionExerciseId: Long) = viewModelScope.launch { repo.removeLastSet(sessionExerciseId) }
    fun removeExercise(sessionExerciseId: Long) = viewModelScope.launch { repo.removeSessionExercise(sessionExerciseId) }
    fun addExercises(ids: List<Long>) = viewModelScope.launch { repo.addExercisesToSession(sessionId, ids) }
    fun exerciseNote(sessionExerciseId: Long, note: String) = viewModelScope.launch { repo.updateExerciseNote(sessionExerciseId, note) }
    fun sessionNote(note: String) = viewModelScope.launch { repo.updateSessionNote(sessionId, note) }

    fun adjustRest(deltaSec: Int) = viewModelScope.launch {
        val s = session.value?.session ?: return@launch
        val end = s.restEndsAt ?: return@launch
        val newEnd = end + deltaSec * 1000L
        if (newEnd <= System.currentTimeMillis()) {
            repo.setRest(sessionId, null, 0)
        } else {
            repo.setRest(sessionId, newEnd, (s.restTotalSec + deltaSec).coerceAtLeast(1))
        }
    }

    fun togglePause() = viewModelScope.launch { repo.togglePause(sessionId) }

    fun skipRest() = viewModelScope.launch { repo.setRest(sessionId, null, 0) }

    fun finish() {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            result = repo.finishSession(sessionId)
            finishing = false
        }
    }

    fun discard(onDone: () -> Unit) = viewModelScope.launch {
        repo.deleteSession(sessionId)
        onDone()
    }
}
