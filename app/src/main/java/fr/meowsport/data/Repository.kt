package fr.meowsport.data

import androidx.room.withTransaction
import kotlin.math.abs

/** One exercise line while a template is being edited. */
data class EditLine(
    val lineId: Long?,
    val exercise: Exercise,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    val perHand: Boolean,
    val restSetSec: Int,
    val restAfterSec: Int,
    val note: String,
)

data class FinishResult(
    val sessionId: Long,
    val durationMs: Long,
    val doneSets: Int,
    val volume: Double,
    /** Human readable list of progress; empty when nothing was beaten. */
    val improvements: List<String>,
)

class Repository(internal val db: AppDatabase) {
    private val exercises = db.exercises()
    private val templates = db.templates()
    private val sessions = db.sessions()

    // ---------- Exercises ----------

    fun observeExercises() = exercises.observeAll()

    /** Adds any built-in exercise that is not in the database yet. */
    suspend fun seedCatalog() {
        val known = exercises.builtInKeys().toSet()
        val missing = ExerciseCatalog.all.filter { it.key !in known }
        if (missing.isNotEmpty()) exercises.insertAll(missing)
    }

    suspend fun createExercise(name: String, muscle: Muscle, equipment: Equipment): Long =
        exercises.insert(
            Exercise(name = name.trim(), nameFr = "", muscle = muscle, equipment = equipment, isCustom = true),
        )

    suspend fun exercise(id: Long) = exercises.get(id)

    // ---------- Templates ----------

    fun observeTemplates() = templates.observeAll()

    suspend fun template(id: Long) = templates.getFull(id)

    suspend fun deleteTemplate(id: Long) = templates.delete(id)

    /** Creates or updates a template and its lines. Returns the template id. */
    suspend fun saveTemplate(id: Long?, name: String, lines: List<EditLine>): Long = db.withTransaction {
        val templateId = if (id == null) {
            templates.insert(WorkoutTemplate(name = name.trim()))
        } else {
            val current = templates.get(id) ?: error("Template $id not found")
            templates.update(current.copy(name = name.trim()))
            id
        }
        val existing = templates.lines(templateId)
        val keptIds = lines.mapNotNull { it.lineId }.toSet()
        templates.deleteLines(existing.map { it.id }.filter { it !in keptIds })
        lines.forEachIndexed { index, l ->
            val row = TemplateExercise(
                id = l.lineId ?: 0,
                templateId = templateId,
                exerciseId = l.exercise.id,
                position = index,
                sets = l.sets,
                reps = l.reps,
                weight = l.weight,
                perHand = l.perHand,
                restSetSec = l.restSetSec,
                restAfterSec = l.restAfterSec,
                note = l.note,
            )
            if (l.lineId == null) templates.insertLine(row) else templates.updateLine(row)
        }
        templateId
    }

    /** Copies an example template into the user's templates. Returns the new template id. */
    suspend fun addExampleTemplate(example: ExampleTemplate): Long {
        seedCatalog()
        val byKey = exercises.byKeys(example.exercises.map { it.key }).associateBy { it.key }
        val lines = example.exercises.mapNotNull { e ->
            val ex = byKey[e.key] ?: return@mapNotNull null
            EditLine(
                lineId = null,
                exercise = ex,
                sets = e.sets,
                reps = e.reps,
                weight = 0.0,
                perHand = ex.equipment == Equipment.DUMBBELL || ex.equipment == Equipment.KETTLEBELL,
                restSetSec = e.restSetSec,
                restAfterSec = e.restAfterSec,
                note = "",
            )
        }
        return saveTemplate(null, example.name, lines)
    }

    // ---------- Sessions ----------

    fun observeActiveSession() = sessions.observeActive()
    suspend fun activeSession() = sessions.active()
    fun observeSession(id: Long) = sessions.observeFull(id)
    fun observeHistory() = sessions.observeFinished()
    suspend fun sessionFull(id: Long) = sessions.getFull(id)
    suspend fun finishedSessionsFull() = sessions.allFinishedFull()
    suspend fun deleteSession(id: Long) = sessions.delete(id)

    /** Starts a session from a template, or a free session when [templateId] is null. */
    suspend fun startSession(templateId: Long?): Long = db.withTransaction {
        sessions.active()?.let { return@withTransaction it.id }
        val full = templateId?.let { templates.getFull(it) }
        val sessionId = sessions.insert(
            Session(
                templateId = full?.template?.id,
                name = full?.template?.name ?: "Séance libre",
                startedAt = System.currentTimeMillis(),
                previousNote = full?.template?.nextNote.orEmpty(),
            ),
        )
        full?.sorted?.forEachIndexed { index, l ->
            val seId = sessions.insertExercise(
                SessionExercise(
                    sessionId = sessionId,
                    exerciseId = l.exercise.id,
                    position = index,
                    perHand = l.line.perHand,
                    restSetSec = l.line.restSetSec,
                    restAfterSec = l.line.restAfterSec,
                    templateExerciseId = l.line.id,
                    targetSets = l.line.sets,
                    targetReps = l.line.reps,
                    targetWeight = l.line.weight,
                    note = l.line.note,
                ),
            )
            sessions.insertSets(
                (0 until l.line.sets.coerceAtLeast(1)).map {
                    SessionSet(sessionExerciseId = seId, setIndex = it, weight = l.line.weight, reps = l.line.reps)
                },
            )
        }
        sessionId
    }

    /**
     * Ticks or unticks a set. Returns the rest to start, in seconds, or null when no rest should start.
     */
    suspend fun toggleSet(setId: Long): Int? = db.withTransaction {
        val set = sessions.getSet(setId) ?: return@withTransaction null
        val nowDone = !set.done
        sessions.updateSet(set.copy(done = nowDone, doneAt = if (nowDone) System.currentTimeMillis() else null))
        if (!nowDone) return@withTransaction null
        val se = sessions.getExercise(set.sessionExerciseId) ?: return@withTransaction null
        val remaining = sessions.setsOf(se.id).count { !it.done && it.id != set.id }
        if (remaining > 0) {
            se.restSetSec
        } else {
            val full = sessions.getFull(se.sessionId) ?: return@withTransaction null
            val anyLeft = full.exercises.any { ex -> ex.sets.any { !it.done && it.id != set.id } }
            if (anyLeft) se.restAfterSec else null
        }
    }

    suspend fun updateSetValues(setId: Long, weight: Double, reps: Int) {
        val set = sessions.getSet(setId) ?: return
        if (set.weight != weight || set.reps != reps) sessions.updateSet(set.copy(weight = weight, reps = reps))
    }

    suspend fun addSet(sessionExerciseId: Long) {
        val sets = sessions.setsOf(sessionExerciseId)
        val last = sets.lastOrNull()
        val se = sessions.getExercise(sessionExerciseId) ?: return
        sessions.insertSet(
            SessionSet(
                sessionExerciseId = sessionExerciseId,
                setIndex = (last?.setIndex ?: -1) + 1,
                weight = last?.weight ?: se.targetWeight,
                reps = last?.reps ?: se.targetReps.coerceAtLeast(1),
            ),
        )
    }

    suspend fun removeLastSet(sessionExerciseId: Long) {
        sessions.setsOf(sessionExerciseId).lastOrNull()?.let { sessions.deleteSet(it) }
    }

    suspend fun removeSessionExercise(sessionExerciseId: Long) {
        sessions.getExercise(sessionExerciseId)?.let { sessions.deleteExercise(it) }
    }

    suspend fun addExercisesToSession(
        sessionId: Long,
        exerciseIds: List<Long>,
        sets: Int = 3,
        reps: Int = 10,
        restSetSec: Int = 90,
        restAfterSec: Int = 120,
    ) = db.withTransaction {
        val full = sessions.getFull(sessionId) ?: return@withTransaction
        var position = (full.exercises.maxOfOrNull { it.item.position } ?: -1) + 1
        for (id in exerciseIds) {
            val ex = exercises.get(id) ?: continue
            val seId = sessions.insertExercise(
                SessionExercise(
                    sessionId = sessionId,
                    exerciseId = id,
                    position = position++,
                    perHand = ex.equipment == Equipment.DUMBBELL,
                    restSetSec = restSetSec,
                    restAfterSec = restAfterSec,
                ),
            )
            sessions.insertSets((0 until sets.coerceAtLeast(1)).map { SessionSet(sessionExerciseId = seId, setIndex = it, weight = 0.0, reps = reps) })
        }
    }

    suspend fun updateExerciseNote(sessionExerciseId: Long, note: String) {
        sessions.setExerciseNote(sessionExerciseId, note)
    }

    suspend fun updateSessionNote(sessionId: Long, note: String) {
        sessions.setNote(sessionId, note)
    }

    /** Pauses the session clock, or resumes it. Returns true when the clock is now paused. */
    suspend fun togglePause(sessionId: Long): Boolean = db.withTransaction {
        val s = sessions.get(sessionId) ?: return@withTransaction false
        val now = System.currentTimeMillis()
        if (s.pausedAt == null) {
            sessions.update(s.copy(pausedAt = now))
            true
        } else {
            sessions.update(s.copy(startedAt = s.startedAt + (now - s.pausedAt), pausedAt = null))
            false
        }
    }

    suspend fun setRest(sessionId: Long, endsAt: Long?, totalSec: Int) {
        sessions.setRest(sessionId, endsAt, totalSec)
    }

    /** Ends the session, updates its template with what was done and lists the progress made. */
    suspend fun finishSession(sessionId: Long, updateTemplate: Boolean = true): FinishResult = db.withTransaction {
        val loaded = sessions.getFull(sessionId) ?: error("Session $sessionId not found")
        val now = System.currentTimeMillis()
        // A paused session ends at its pause time: move the start forward by the pause.
        val paused = loaded.session.pausedAt
        val full = if (paused != null) {
            loaded.copy(session = loaded.session.copy(startedAt = loaded.session.startedAt + (now - paused), pausedAt = null))
        } else {
            loaded
        }
        val improvements = mutableListOf<String>()
        var volume = 0.0
        var doneSets = 0

        for (ex in full.sorted) {
            val done = ex.sets.filter { it.done }
            doneSets += done.size
            val factor = if (ex.item.perHand) 2 else 1
            volume += done.sumOf { it.weight * it.reps * factor }
            if (done.isEmpty() || ex.item.templateExerciseId == null) continue
            progressOf(ex, done)?.let { improvements += it }
        }

        sessions.update(
            full.session.copy(
                endedAt = now,
                improved = improvements.isNotEmpty(),
                totalVolume = volume,
                restEndsAt = null,
            ),
        )

        val templateId = full.session.templateId
        val template = templateId?.let { templates.get(it) }
        if (template != null) {
            val lines = templates.lines(template.id).associateBy { it.id }
            var nextPosition = (lines.values.maxOfOrNull { it.position } ?: -1) + 1
            // The note and date are always kept; the weights and reps only when the setting allows it.
            if (updateTemplate) for (ex in full.sorted) {
                val done = ex.sets.filter { it.done }
                if (done.isEmpty()) continue
                val best = done.maxWith(compareBy<SessionSet>({ it.weight }, { it.reps }))
                val line = ex.item.templateExerciseId?.let { lines[it] }
                if (line != null) {
                    templates.updateLine(
                        line.copy(
                            sets = ex.sets.size,
                            reps = best.reps,
                            weight = best.weight,
                            note = ex.item.note,
                        ),
                    )
                } else {
                    templates.insertLine(
                        TemplateExercise(
                            templateId = template.id,
                            exerciseId = ex.item.exerciseId,
                            position = nextPosition++,
                            sets = ex.sets.size,
                            reps = best.reps,
                            weight = best.weight,
                            perHand = ex.item.perHand,
                            restSetSec = ex.item.restSetSec,
                            restAfterSec = ex.item.restAfterSec,
                            note = ex.item.note,
                        ),
                    )
                }
            }
            templates.update(template.copy(nextNote = full.session.note, lastPerformedAt = now))
        }

        FinishResult(sessionId, now - full.session.startedAt, doneSets, volume, improvements)
    }

    /** Compares what was done with the template targets. Returns a message when it is better. */
    private fun progressOf(ex: SessionExerciseFull, done: List<SessionSet>): String? {
        val name = ex.exercise.name
        val tw = ex.item.targetWeight
        val tr = ex.item.targetReps
        val ts = ex.item.targetSets
        val best = done.maxWith(compareBy<SessionSet>({ it.weight }, { it.reps }))
        return when {
            tw > 0 && best.weight > tw + 1e-6 ->
                "$name : ${formatWeight(tw)} → ${formatWeight(best.weight)} kg"
            abs(best.weight - tw) < 1e-6 && best.reps > tr ->
                "$name : $tr → ${best.reps} reps" + if (tw > 0) " à ${formatWeight(tw)} kg" else ""
            tw > 0 && done.sumOf { it.weight * it.reps } > ts * tr * tw + 1e-6 ->
                "$name : plus de volume (${formatWeight(ts * tr * tw)} → ${formatWeight(done.sumOf { it.weight * it.reps })} kg)"
            tw == 0.0 && best.weight == 0.0 && done.sumOf { it.reps } > ts * tr ->
                "$name : ${ts * tr} → ${done.sumOf { it.reps }} reps au total"
            else -> null
        }
    }
}

fun formatWeight(w: Double): String =
    if (abs(w - Math.round(w)) < 1e-6) Math.round(w).toString()
    else String.format(java.util.Locale.FRANCE, "%.2f", w).trimEnd('0').trimEnd(',')
