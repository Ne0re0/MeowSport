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
    val supersetNext: Boolean = false,
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
        // Built-in names can be improved between versions: refresh them.
        val stored = exercises.all().filter { it.key != null }.associateBy { it.key }
        for (ex in ExerciseCatalog.all) {
            val old = stored[ex.key] ?: continue
            if (old.name != ex.name || old.nameFr != ex.nameFr) exercises.updateNames(ex.key!!, ex.name, ex.nameFr)
        }
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
                supersetNext = l.supersetNext,
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
                    supersetNext = l.line.supersetNext,
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
        val full = sessions.getFull(se.sessionId) ?: return@withTransaction null
        val group = supersetGroup(full.sorted, se.id)
        if (group.size > 1) {
            fun left(e: SessionExerciseFull) = e.sets.count { !it.done && it.id != set.id }
            val index = group.indexOfFirst { it.item.id == se.id }
            return@withTransaction when {
                // Go straight to the next exercise of the superset, no rest.
                group.drop(index + 1).any { left(it) > 0 } -> 0
                // End of a round: rest, then start the next round.
                group.any { left(it) > 0 } -> se.restSetSec
                // Superset finished.
                full.exercises.any { ex -> ex.sets.any { !it.done && it.id != set.id } } -> group.last().item.restAfterSec
                else -> null
            }
        }
        val remaining = sessions.setsOf(se.id).count { !it.done && it.id != set.id }
        if (remaining > 0) {
            se.restSetSec
        } else {
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

    /** Moves a session exercise up (-1) or down (+1). */
    suspend fun moveSessionExercise(sessionId: Long, sessionExerciseId: Long, delta: Int) = db.withTransaction {
        val list = sessions.getFull(sessionId)?.sorted?.map { it.item }?.toMutableList() ?: return@withTransaction
        val i = list.indexOfFirst { it.id == sessionExerciseId }
        val j = i + delta
        if (i < 0 || j !in list.indices) return@withTransaction
        // Moving an exercise breaks its superset links.
        if (i > 0) list[i - 1] = list[i - 1].copy(supersetNext = false)
        list[i] = list[i].copy(supersetNext = false)
        val moved = list.removeAt(i)
        list.add(j, moved)
        if (j > 0) list[j - 1] = list[j - 1].copy(supersetNext = false)
        val stored = sessions.getFull(sessionId)?.exercises?.associate { it.item.id to it.item }.orEmpty()
        list.forEachIndexed { index, e ->
            val updated = e.copy(position = index)
            if (stored[e.id] != updated) sessions.updateExercise(updated)
        }
    }

    /** Links (or unlinks) a session exercise with the next one as a superset. */
    suspend fun setSessionSuperset(sessionExerciseId: Long, linked: Boolean) {
        val e = sessions.getExercise(sessionExerciseId) ?: return
        sessions.updateExercise(e.copy(supersetNext = linked))
    }

    suspend fun setSessionExerciseRest(sessionExerciseId: Long, restSetSec: Int, restAfterSec: Int) {
        val e = sessions.getExercise(sessionExerciseId) ?: return
        sessions.updateExercise(e.copy(restSetSec = restSetSec, restAfterSec = restAfterSec))
    }

    suspend fun removeSessionExercise(sessionExerciseId: Long) = db.withTransaction {
        val e = sessions.getExercise(sessionExerciseId) ?: return@withTransaction
        // The exercise before it no longer has a superset partner.
        val sorted = sessions.getFull(e.sessionId)?.sorted.orEmpty()
        val i = sorted.indexOfFirst { it.item.id == e.id }
        if (i > 0 && sorted[i - 1].item.supersetNext && !e.supersetNext) {
            sessions.updateExercise(sorted[i - 1].item.copy(supersetNext = false))
        }
        sessions.deleteExercise(e)
    }

    /**
     * Swaps the exercise of a session line, e.g. when a machine is taken.
     * Sets already done stay as they are; the weight of the other sets is cleared.
     * The template line follows the swap at the end of the session (if template updates are on).
     */
    suspend fun replaceSessionExercise(sessionExerciseId: Long, exerciseId: Long) = db.withTransaction {
        val item = sessions.getExercise(sessionExerciseId) ?: return@withTransaction
        if (item.exerciseId == exerciseId) return@withTransaction
        val ex = exercises.get(exerciseId) ?: return@withTransaction
        sessions.updateExercise(
            item.copy(
                exerciseId = exerciseId,
                perHand = ex.equipment == Equipment.DUMBBELL || ex.equipment == Equipment.KETTLEBELL,
                // No targets for a new exercise: no false "progress" at the end.
                targetSets = 0,
                targetReps = 0,
                targetWeight = 0.0,
            ),
        )
        sessions.clearPendingWeights(sessionExerciseId)
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

    /** Saves the persistent note of an exercise. An empty note removes it. */
    suspend fun setExerciseNote(exerciseId: Long, note: String) = exercises.setNote(exerciseId, note.trim())

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
    /**
     * True when the session no longer has the shape of its template: exercises added, removed,
     * replaced or moved, or rests and superset links changed.
     */
    suspend fun sessionDiffersFromTemplate(sessionId: Long): Boolean {
        val full = sessions.getFull(sessionId) ?: return false
        val templateId = full.session.templateId ?: return false
        val lines = templates.lines(templateId).sortedBy { it.position }
        val exs = full.sorted.map { it.item }
        if (lines.size != exs.size) return true
        return lines.zip(exs).any { (l, e) ->
            e.templateExerciseId != l.id || e.exerciseId != l.exerciseId || e.restSetSec != l.restSetSec ||
                e.restAfterSec != l.restAfterSec || e.supersetNext != l.supersetNext
        }
    }

    /**
     * Ends the session. [updateTemplate] keeps the progress (weights, reps) in the template;
     * [syncStructure] also gives the template the session's exercises, order, rests and supersets.
     */
    suspend fun finishSession(
        sessionId: Long,
        updateTemplate: Boolean = true,
        syncStructure: Boolean = false,
    ): FinishResult = db.withTransaction {
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
            if (done.isEmpty() || ex.item.templateExerciseId == null || ex.item.targetSets == 0) continue
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
            val bySession = mutableMapOf<Long, Long>() // session exercise id -> template line id
            for (ex in full.sorted) {
                val done = ex.sets.filter { it.done }
                val best = done.maxWithOrNull(compareBy<SessionSet>({ it.weight }, { it.reps }))
                val line = ex.item.templateExerciseId?.let { lines[it] }
                if (line != null) {
                    var updated = line
                    // Same exercise as planned: keep the progress (weights, reps, sets) if the setting allows it.
                    if (updateTemplate && best != null && (syncStructure || line.exerciseId == ex.item.exerciseId)) {
                        updated = updated.copy(sets = ex.sets.size, reps = best.reps, weight = best.weight, note = ex.item.note)
                    }
                    // The template takes the session's shape: replaced exercise, rests, superset links.
                    if (syncStructure) {
                        updated = updated.copy(
                            exerciseId = ex.item.exerciseId,
                            perHand = ex.item.perHand,
                            restSetSec = ex.item.restSetSec,
                            restAfterSec = ex.item.restAfterSec,
                            supersetNext = ex.item.supersetNext,
                            sets = ex.sets.size,
                        )
                    }
                    if (updated != line) templates.updateLine(updated)
                    bySession[ex.item.id] = line.id
                } else if (syncStructure) {
                    // Exercise added during the session.
                    val ref = best ?: ex.sortedSets.firstOrNull()
                    bySession[ex.item.id] = templates.insertLine(
                        TemplateExercise(
                            templateId = template.id,
                            exerciseId = ex.item.exerciseId,
                            position = lines.size + bySession.size,
                            sets = ex.sets.size.coerceAtLeast(1),
                            reps = ref?.reps ?: 10,
                            weight = ref?.weight ?: 0.0,
                            perHand = ex.item.perHand,
                            restSetSec = ex.item.restSetSec,
                            restAfterSec = ex.item.restAfterSec,
                            note = ex.item.note,
                            supersetNext = ex.item.supersetNext,
                        ),
                    )
                }
            }
            if (syncStructure) {
                // Exercises removed during the session leave the template; the rest follows the session order.
                val kept = full.sorted.mapNotNull { bySession[it.item.id] }.distinct()
                templates.deleteLines(lines.keys.filter { it !in kept })
                val current = templates.lines(template.id).associateBy { it.id }
                kept.forEachIndexed { index, lineId ->
                    val line = current[lineId] ?: return@forEachIndexed
                    if (line.position != index) templates.updateLine(line.copy(position = index))
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

    /** The exercises done as one superset with the given one (just itself when not linked). */
    private fun supersetGroup(sorted: List<SessionExerciseFull>, sessionExerciseId: Long): List<SessionExerciseFull> {
        val i = sorted.indexOfFirst { it.item.id == sessionExerciseId }
        if (i < 0) return emptyList()
        var start = i
        while (start > 0 && sorted[start - 1].item.supersetNext) start--
        var end = i
        while (end < sorted.lastIndex && sorted[end].item.supersetNext) end++
        return sorted.subList(start, end + 1)
    }
}

fun formatWeight(w: Double): String =
    if (abs(w - Math.round(w)) < 1e-6) Math.round(w).toString()
    else String.format(java.util.Locale.FRANCE, "%.2f", w).trimEnd('0').trimEnd(',')
