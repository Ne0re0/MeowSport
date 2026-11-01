package fr.meowsport.ui.exercises

import fr.meowsport.data.SessionFull
import fr.meowsport.data.SessionSet

/** One session of one exercise, with its checked sets. */
data class ExercisePoint(val sessionId: Long, val date: Long, val sets: List<SessionSet>, val perHand: Boolean) {
    val maxWeight: Double get() = sets.maxOf { it.weight }
    val maxReps: Int get() = sets.maxOf { it.reps }
    /** Epley estimate of the one-rep max, from the best set. */
    val e1rm: Double get() = sets.maxOf { if (it.reps <= 1) it.weight else it.weight * (1 + it.reps / 30.0) }
    val volume: Double get() = sets.sumOf { it.weight * it.reps } * (if (perHand) 2 else 1)
}

/** Every finished session where the exercise had at least one checked set, oldest first. */
fun historyOf(sessions: List<SessionFull>, exerciseId: Long): List<ExercisePoint> =
    sessions.flatMap { s ->
        s.exercises.filter { it.exercise.id == exerciseId }.mapNotNull { e ->
            val done = e.sortedSets.filter { it.done }
            if (done.isEmpty()) null else ExercisePoint(s.session.id, s.session.startedAt, done, e.item.perHand)
        }
    }.sortedBy { it.date }
