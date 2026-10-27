package fr.meowsport.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

data class ImportResult(val templates: Int, val sessions: Int, val skipped: Int)

/**
 * Export and import of templates and finished sessions as a JSON file.
 * Exercises are referenced by their catalog key, or by name, muscle and equipment for custom ones.
 */
class Backup(private val repo: Repository) {
    private val db get() = repo.db

    suspend fun export(): String {
        val root = JSONObject()
        root.put("app", "MeowSport")
        root.put("format", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val templates = JSONArray()
        for (t in db.templates().allFull()) {
            templates.put(
                JSONObject()
                    .put("name", t.template.name)
                    .put("createdAt", t.template.createdAt)
                    .put("nextNote", t.template.nextNote)
                    .putOpt("lastPerformedAt", t.template.lastPerformedAt)
                    .put(
                        "exercises",
                        JSONArray(
                            t.sorted.map { l ->
                                JSONObject()
                                    .put("exercise", exerciseRef(l.exercise))
                                    .put("sets", l.line.sets)
                                    .put("reps", l.line.reps)
                                    .put("weight", l.line.weight)
                                    .put("perHand", l.line.perHand)
                                    .put("restSetSec", l.line.restSetSec)
                                    .put("restAfterSec", l.line.restAfterSec)
                                    .put("note", l.line.note)
                            },
                        ),
                    ),
            )
        }
        root.put("templates", templates)

        val templateNames = db.templates().all().associate { it.id to it.name }
        val sessions = JSONArray()
        for (s in db.sessions().allFinishedFull()) {
            sessions.put(
                JSONObject()
                    .put("name", s.session.name)
                    .putOpt("template", s.session.templateId?.let { templateNames[it] })
                    .put("startedAt", s.session.startedAt)
                    .putOpt("endedAt", s.session.endedAt)
                    .put("note", s.session.note)
                    .put("previousNote", s.session.previousNote)
                    .put("improved", s.session.improved)
                    .put("totalVolume", s.session.totalVolume)
                    .put(
                        "exercises",
                        JSONArray(
                            s.sorted.map { e ->
                                JSONObject()
                                    .put("exercise", exerciseRef(e.exercise))
                                    .put("perHand", e.item.perHand)
                                    .put("restSetSec", e.item.restSetSec)
                                    .put("restAfterSec", e.item.restAfterSec)
                                    .put("targetSets", e.item.targetSets)
                                    .put("targetReps", e.item.targetReps)
                                    .put("targetWeight", e.item.targetWeight)
                                    .put("note", e.item.note)
                                    .put(
                                        "sets",
                                        JSONArray(
                                            e.sortedSets.map { set ->
                                                JSONObject()
                                                    .put("weight", set.weight)
                                                    .put("reps", set.reps)
                                                    .put("done", set.done)
                                                    .putOpt("doneAt", set.doneAt)
                                            },
                                        ),
                                    )
                            },
                        ),
                    ),
            )
        }
        root.put("sessions", sessions)
        return root.toString(2)
    }

    private fun exerciseRef(e: Exercise): JSONObject {
        val o = JSONObject()
        if (e.key != null) o.put("key", e.key)
        o.put("name", e.name)
        o.put("muscle", e.muscle.name)
        o.put("equipment", e.equipment.name)
        if (e.secondary.isNotBlank()) o.put("secondary", e.secondary)
        return o
    }

    /**
     * Adds the file's content to the app. Templates whose name already exists and sessions
     * with the same start time are skipped, so importing the same file twice changes nothing.
     */
    suspend fun import(json: String): ImportResult {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Ce fichier n'est pas un export MeowSport valide.")
        }
        if (root.optString("app") != "MeowSport") {
            throw IllegalArgumentException("Ce fichier n'est pas un export MeowSport.")
        }
        if (root.optInt("format", 0) > FORMAT_VERSION) {
            throw IllegalArgumentException("Ce fichier vient d'une version plus récente de l'app. Mets-la à jour d'abord.")
        }
        repo.seedCatalog()

        return db.withTransaction {
            val resolver = ExerciseResolver(db.exercises())
            var templatesAdded = 0
            var sessionsAdded = 0
            var skipped = 0

            val existingNames = db.templates().all().map { it.name.trim().lowercase() }.toMutableSet()
            val templates = root.optJSONArray("templates") ?: JSONArray()
            for (i in 0 until templates.length()) {
                val t = templates.getJSONObject(i)
                val name = t.optString("name").trim()
                if (name.isEmpty() || name.lowercase() in existingNames) {
                    skipped++
                    continue
                }
                val id = db.templates().insert(
                    WorkoutTemplate(
                        name = name,
                        createdAt = t.optLong("createdAt", System.currentTimeMillis()),
                        nextNote = t.optString("nextNote"),
                        lastPerformedAt = t.optLongOrNull("lastPerformedAt"),
                    ),
                )
                val lines = t.optJSONArray("exercises") ?: JSONArray()
                for (j in 0 until lines.length()) {
                    val l = lines.getJSONObject(j)
                    val ex = resolver.resolve(l.optJSONObject("exercise")) ?: continue
                    db.templates().insertLine(
                        TemplateExercise(
                            templateId = id,
                            exerciseId = ex.id,
                            position = j,
                            sets = l.optInt("sets", 3).coerceIn(1, 20),
                            reps = l.optInt("reps", 10).coerceAtLeast(0),
                            weight = l.optDouble("weight", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                            perHand = l.optBoolean("perHand", false),
                            restSetSec = l.optInt("restSetSec", 90),
                            restAfterSec = l.optInt("restAfterSec", 120),
                            note = l.optString("note"),
                        ),
                    )
                }
                existingNames += name.lowercase()
                templatesAdded++
            }

            val templateIds = db.templates().all().associate { it.name.trim().lowercase() to it.id }
            val existingStarts = db.sessions().startTimes().toMutableSet()
            val sessions = root.optJSONArray("sessions") ?: JSONArray()
            for (i in 0 until sessions.length()) {
                val s = sessions.getJSONObject(i)
                val startedAt = s.optLong("startedAt", 0L)
                if (startedAt <= 0L || startedAt in existingStarts) {
                    skipped++
                    continue
                }
                val sessionId = db.sessions().insert(
                    Session(
                        templateId = s.optString("template").takeIf { it.isNotBlank() }
                            ?.let { templateIds[it.trim().lowercase()] },
                        name = s.optString("name").ifBlank { "Séance" },
                        startedAt = startedAt,
                        endedAt = s.optLongOrNull("endedAt") ?: startedAt,
                        note = s.optString("note"),
                        previousNote = s.optString("previousNote"),
                        improved = s.optBoolean("improved", false),
                        totalVolume = s.optDouble("totalVolume", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                    ),
                )
                val exercises = s.optJSONArray("exercises") ?: JSONArray()
                for (j in 0 until exercises.length()) {
                    val e = exercises.getJSONObject(j)
                    val ex = resolver.resolve(e.optJSONObject("exercise")) ?: continue
                    val seId = db.sessions().insertExercise(
                        SessionExercise(
                            sessionId = sessionId,
                            exerciseId = ex.id,
                            position = j,
                            perHand = e.optBoolean("perHand", false),
                            restSetSec = e.optInt("restSetSec", 90),
                            restAfterSec = e.optInt("restAfterSec", 120),
                            targetSets = e.optInt("targetSets", 0),
                            targetReps = e.optInt("targetReps", 0),
                            targetWeight = e.optDouble("targetWeight", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                            note = e.optString("note"),
                        ),
                    )
                    val sets = e.optJSONArray("sets") ?: JSONArray()
                    db.sessions().insertSets(
                        (0 until sets.length()).map { k ->
                            val set = sets.getJSONObject(k)
                            SessionSet(
                                sessionExerciseId = seId,
                                setIndex = k,
                                weight = set.optDouble("weight", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                                reps = set.optInt("reps", 0),
                                done = set.optBoolean("done", false),
                                doneAt = set.optLongOrNull("doneAt"),
                            )
                        },
                    )
                }
                existingStarts += startedAt
                sessionsAdded++
            }
            ImportResult(templatesAdded, sessionsAdded, skipped)
        }
    }

    /** Finds the exercise a reference points to, creating a custom one when it is unknown. */
    private class ExerciseResolver(private val dao: ExerciseDao) {
        private var cache: List<Exercise>? = null

        suspend fun resolve(ref: JSONObject?): Exercise? {
            ref ?: return null
            val all = cache ?: dao.all().also { cache = it }
            val key = ref.optString("key").takeIf { it.isNotBlank() }
            if (key != null) all.firstOrNull { it.key == key }?.let { return it }
            val name = ref.optString("name").trim()
            if (name.isEmpty()) return null
            all.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
            val muscle = Muscle.entries.firstOrNull { it.name == ref.optString("muscle") } ?: Muscle.CHEST
            val equipment = Equipment.entries.firstOrNull { it.name == ref.optString("equipment") } ?: Equipment.OTHER
            val created = Exercise(
                name = name,
                nameFr = "",
                muscle = muscle,
                secondary = ref.optString("secondary"),
                equipment = equipment,
                isCustom = true,
            )
            val id = dao.insert(created)
            return created.copy(id = id).also { cache = all + it }
        }
    }

    companion object {
        const val FORMAT_VERSION = 1
    }
}

private fun JSONObject.optLongOrNull(name: String): Long? =
    if (has(name) && !isNull(name)) optLong(name) else null
