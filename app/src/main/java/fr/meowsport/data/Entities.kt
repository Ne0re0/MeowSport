package fr.meowsport.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "exercises", indices = [Index("key", unique = true)])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable key for built-in exercises, null for the user's own ones. */
    val key: String? = null,
    val name: String,
    val nameFr: String,
    val muscle: Muscle,
    /** Secondary muscles, comma separated enum names. */
    val secondary: String = "",
    val equipment: Equipment,
    val isCustom: Boolean = false,
) {
    val secondaryMuscles: List<Muscle>
        get() = secondary.split(',').mapNotNull { s -> Muscle.entries.firstOrNull { it.name == s.trim() } }
}

@Entity(tableName = "templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Note written during the last session, shown next time. */
    val nextNote: String = "",
    val lastPerformedAt: Long? = null,
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(entity = WorkoutTemplate::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("templateId"), Index("exerciseId")],
)
data class TemplateExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val position: Int,
    val sets: Int = 3,
    val reps: Int = 10,
    val weight: Double = 0.0,
    val perHand: Boolean = false,
    /** Rest between two sets, in seconds. */
    val restSetSec: Int = 90,
    /** Rest after the last set, before the next exercise, in seconds. */
    val restAfterSec: Int = 120,
    val note: String = "",
)

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(entity = WorkoutTemplate::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("templateId")],
)
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long?,
    val name: String,
    val startedAt: Long,
    /** Null while the session is running. */
    val endedAt: Long? = null,
    val note: String = "",
    /** Note left by the previous session, shown during this one. */
    val previousNote: String = "",
    val improved: Boolean = false,
    val totalVolume: Double = 0.0,
    /** When the current rest timer ends, or null. */
    val restEndsAt: Long? = null,
    val restTotalSec: Int = 0,
    /** When the session clock was paused, or null while it runs. */
    val pausedAt: Long? = null,
) {
    /** Session clock, in ms. Pauses are removed by moving [startedAt] forward on resume. */
    fun elapsedMs(now: Long = System.currentTimeMillis()) = ((pausedAt ?: now) - startedAt).coerceAtLeast(0)
}

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(entity = Session::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int,
    val perHand: Boolean,
    val restSetSec: Int,
    val restAfterSec: Int,
    /** The template line this exercise came from, if any. */
    val templateExerciseId: Long? = null,
    /** Targets from the template when the session started, used to spot progress. */
    val targetSets: Int = 0,
    val targetReps: Int = 0,
    val targetWeight: Double = 0.0,
    val note: String = "",
)

@Entity(
    tableName = "session_sets",
    foreignKeys = [
        ForeignKey(entity = SessionExercise::class, parentColumns = ["id"], childColumns = ["sessionExerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionExerciseId")],
)
data class SessionSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val setIndex: Int,
    val weight: Double,
    val reps: Int,
    val done: Boolean = false,
    val doneAt: Long? = null,
)

data class TemplateExerciseWithExercise(
    @Embedded val line: TemplateExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
)

data class TemplateFull(
    @Embedded val template: WorkoutTemplate,
    @Relation(entity = TemplateExercise::class, parentColumn = "id", entityColumn = "templateId")
    val lines: List<TemplateExerciseWithExercise>,
) {
    val sorted get() = lines.sortedBy { it.line.position }
}

data class SessionExerciseFull(
    @Embedded val item: SessionExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<SessionSet>,
) {
    val sortedSets get() = sets.sortedBy { it.setIndex }
}

data class SessionFull(
    @Embedded val session: Session,
    @Relation(entity = SessionExercise::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseFull>,
) {
    val sorted get() = exercises.sortedBy { it.item.position }
}
