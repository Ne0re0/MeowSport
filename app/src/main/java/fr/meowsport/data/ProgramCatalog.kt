package fr.meowsport.data

/** One exercise of an example template, referenced by its catalog key. */
data class ExampleExercise(
    val key: String,
    val sets: Int,
    val reps: Int,
    val restSetSec: Int,
    val restAfterSec: Int = restSetSec,
)

data class ExampleTemplate(val name: String, val exercises: List<ExampleExercise>)

data class ExampleProgram(
    val name: String,
    val frequency: String,
    val description: String,
    val days: List<ExampleTemplate>,
)

/**
 * Example programs built on common hypertrophy guidelines: each muscle trained about twice a week,
 * roughly 10 to 20 hard sets per muscle per week, heavy compound lifts first (5 to 8 reps, 2 to 3 min rest),
 * then isolation work (10 to 15 reps, 60 to 90 s rest).
 */
object ProgramCatalog {
    private fun x(key: String, sets: Int, reps: Int, rest: Int, after: Int = rest) =
        ExampleExercise(key, sets, reps, rest, after)

    val all: List<ExampleProgram> = listOf(
        ExampleProgram(
            name = "Upper / Lower",
            frequency = "4 séances par semaine",
            description = "Haut du corps et bas du corps en alternance, par exemple lundi, mardi, jeudi, vendredi. " +
                "Chaque muscle est travaillé deux fois par semaine. Idéal pour progresser en force et en volume.",
            days = listOf(
                ExampleTemplate(
                    "Upper A",
                    listOf(
                        x("bench_press", 4, 6, 180),
                        x("barbell_row", 4, 8, 150),
                        x("incline_db_press", 3, 10, 120),
                        x("lat_pulldown", 3, 10, 120),
                        x("lateral_raise", 3, 15, 60),
                        x("ez_curl", 3, 10, 60),
                        x("rope_pushdown", 3, 12, 60),
                    ),
                ),
                ExampleTemplate(
                    "Lower A",
                    listOf(
                        x("back_squat", 4, 6, 180),
                        x("romanian_deadlift", 3, 8, 150),
                        x("leg_press", 3, 12, 120),
                        x("lying_leg_curl", 3, 12, 90),
                        x("standing_calf_raise", 4, 12, 60),
                        x("hanging_leg_raise", 3, 12, 60),
                    ),
                ),
                ExampleTemplate(
                    "Upper B",
                    listOf(
                        x("overhead_press", 4, 6, 180),
                        x("pull_up", 4, 8, 150),
                        x("db_bench_press", 3, 10, 120),
                        x("seated_row", 3, 10, 120),
                        x("face_pull", 3, 15, 60),
                        x("hammer_curl", 3, 12, 60),
                        x("overhead_cable_extension", 3, 12, 60),
                    ),
                ),
                ExampleTemplate(
                    "Lower B",
                    listOf(
                        x("deadlift", 3, 5, 180),
                        x("bulgarian_split_squat", 3, 10, 120),
                        x("hip_thrust", 3, 10, 120),
                        x("leg_extension", 3, 15, 90),
                        x("seated_leg_curl", 3, 12, 90),
                        x("seated_calf_raise", 4, 15, 60),
                        x("cable_crunch", 3, 15, 60),
                    ),
                ),
            ),
        ),
        ExampleProgram(
            name = "Push / Pull / Legs",
            frequency = "3 ou 6 séances par semaine",
            description = "Poussée (pecs, épaules, triceps), tirage (dos, biceps) et jambes. " +
                "En 3 séances pour débuter, ou 6 séances (le cycle deux fois) pour un volume maximal.",
            days = listOf(
                ExampleTemplate(
                    "Push",
                    listOf(
                        x("bench_press", 4, 6, 180),
                        x("incline_db_press", 3, 10, 120),
                        x("db_shoulder_press", 3, 10, 120),
                        x("cable_crossover", 3, 12, 60),
                        x("lateral_raise", 4, 15, 60),
                        x("rope_pushdown", 3, 12, 60),
                        x("overhead_cable_extension", 3, 12, 60),
                    ),
                ),
                ExampleTemplate(
                    "Pull",
                    listOf(
                        x("pull_up", 4, 8, 150),
                        x("barbell_row", 3, 8, 150),
                        x("seated_row", 3, 10, 120),
                        x("face_pull", 3, 15, 60),
                        x("barbell_shrug", 3, 12, 60),
                        x("barbell_curl", 3, 10, 60),
                        x("hammer_curl", 3, 12, 60),
                    ),
                ),
                ExampleTemplate(
                    "Legs",
                    listOf(
                        x("back_squat", 4, 6, 180),
                        x("romanian_deadlift", 3, 8, 150),
                        x("leg_press", 3, 12, 120),
                        x("leg_extension", 3, 15, 90),
                        x("lying_leg_curl", 3, 12, 90),
                        x("standing_calf_raise", 4, 12, 60),
                        x("hanging_leg_raise", 3, 12, 60),
                    ),
                ),
            ),
        ),
        ExampleProgram(
            name = "Full body",
            frequency = "3 séances par semaine",
            description = "Tout le corps à chaque séance, en alternant A et B (par exemple lundi, mercredi, vendredi). " +
                "Parfait pour débuter ou quand tu as peu de temps.",
            days = listOf(
                ExampleTemplate(
                    "Full body A",
                    listOf(
                        x("back_squat", 3, 8, 150),
                        x("bench_press", 3, 8, 150),
                        x("barbell_row", 3, 8, 120),
                        x("db_shoulder_press", 2, 10, 90),
                        x("lying_leg_curl", 2, 12, 90),
                        x("db_curl", 2, 12, 60),
                        x("crunch", 3, 15, 60),
                    ),
                ),
                ExampleTemplate(
                    "Full body B",
                    listOf(
                        x("deadlift", 3, 5, 180),
                        x("incline_db_press", 3, 10, 120),
                        x("lat_pulldown", 3, 10, 120),
                        x("walking_lunge", 3, 10, 90),
                        x("lateral_raise", 3, 15, 60),
                        x("triceps_pushdown", 3, 12, 60),
                        x("standing_calf_raise", 3, 12, 60),
                    ),
                ),
            ),
        ),
        ExampleProgram(
            name = "Bras et épaules",
            frequency = "Séance bonus",
            description = "Une séance d'appoint pour les bras et les épaules, à ajouter à ton programme si tu veux insister dessus.",
            days = listOf(
                ExampleTemplate(
                    "Bras et épaules",
                    listOf(
                        x("db_shoulder_press", 3, 10, 120),
                        x("cable_lateral_raise", 3, 15, 60),
                        x("reverse_pec_deck", 3, 15, 60),
                        x("ez_curl", 3, 10, 60),
                        x("skull_crusher", 3, 10, 60),
                        x("incline_db_curl", 3, 12, 60),
                        x("rope_pushdown", 3, 12, 60),
                    ),
                ),
            ),
        ),
    )
}
