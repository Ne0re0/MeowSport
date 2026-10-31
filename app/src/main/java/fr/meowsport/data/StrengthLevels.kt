package fr.meowsport.data

import kotlin.math.min

enum class Level(val label: String) {
    BEGINNER("Débutant"),
    NOVICE("Novice"),
    INTERMEDIATE("Intermédiaire"),
    ADVANCED("Avancé"),
    EXPERT("Expert"),
}

/** Level of one muscle: [score] goes from 0 (beginner) to 4 (expert). */
data class MuscleLevel(
    val muscle: Muscle,
    val score: Float,
    val exerciseName: String,
    val bestSet: String,
) {
    val level: Level get() = Level.entries[score.toInt().coerceIn(0, 4)]
}

/**
 * Reference for one exercise.
 * [ratio]: estimated 1RM / body weight of an intermediate man (per hand for dumbbells).
 * [bodyFactor]: share of body weight lifted on top of the added load (pull-ups, dips, push-ups).
 * [reps]: for exercises judged on reps only (plank in seconds, crunches…), reps of an intermediate.
 */
private class Standard(val ratio: Double = 0.0, val bodyFactor: Double = 0.0, val reps: Int = 0)

/**
 * Strength levels, estimated from the best set ever done on each exercise.
 *
 * The 1RM is estimated with the Epley formula and compared with the body weight, as public strength
 * standards do. The thresholds are expressed relative to the intermediate level:
 * beginner 0.45, novice 0.7, intermediate 1, advanced 1.35, expert 1.75.
 * Women's references are lower (55 % of the men's for the upper body, 70 % for the lower body).
 * Each muscle takes the level of its best exercise. Exercises without a reference are not counted.
 */
object StrengthLevels {
    private val thresholds = doubleArrayOf(0.45, 0.7, 1.0, 1.35, 1.75)

    private val lowerBody = setOf(
        Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.CALVES,
        Muscle.ADDUCTORS, Muscle.ABDUCTORS, Muscle.LOWER_BACK,
    )

    private fun w(ratio: Double) = Standard(ratio = ratio)
    private fun bw(ratio: Double, factor: Double) = Standard(ratio = ratio, bodyFactor = factor)
    private fun reps(n: Int) = Standard(reps = n)

    private val standards: Map<String, Standard> = mapOf(
        // Chest
        "bench_press" to w(1.2), "db_bench_press" to w(0.45), "incline_bench_press" to w(1.0),
        "incline_db_press" to w(0.4), "decline_bench_press" to w(1.25), "decline_db_press" to w(0.45),
        "smith_bench_press" to w(1.1), "smith_incline_press" to w(0.95), "machine_chest_press" to w(1.1),
        "incline_chest_press_machine" to w(1.0), "db_fly" to w(0.25), "incline_db_fly" to w(0.22),
        "pec_deck" to w(1.0), "cable_crossover" to w(0.3), "low_cable_fly" to w(0.25),
        "chest_dip" to bw(1.5, 1.0), "push_up" to bw(1.3, 0.65),
        // Back
        "pull_up" to bw(1.4, 1.0), "chin_up" to bw(1.45, 1.0), "lat_pulldown" to w(1.0),
        "close_grip_pulldown" to w(1.0), "seated_row" to w(1.0), "machine_row" to w(1.0),
        "barbell_row" to w(1.0), "pendlay_row" to w(0.95), "db_row" to w(0.5),
        "chest_supported_row" to w(0.4), "t_bar_row" to w(1.0), "inverted_row" to bw(1.0, 0.6),
        "deadlift" to w(1.9), "rack_pull" to w(2.1), "back_extension" to bw(1.6, 0.6),
        // Shoulders
        "overhead_press" to w(0.75), "db_shoulder_press" to w(0.32), "arnold_press" to w(0.28),
        "machine_shoulder_press" to w(0.8), "smith_shoulder_press" to w(0.75),
        "lateral_raise" to w(0.15), "cable_lateral_raise" to w(0.1), "machine_lateral_raise" to w(0.5),
        "front_raise" to w(0.17), "upright_row" to w(0.6),
        "rear_delt_fly" to w(0.15), "reverse_pec_deck" to w(0.6), "face_pull" to w(0.5),
        "cable_rear_delt_fly" to w(0.15),
        // Traps
        "barbell_shrug" to w(1.6), "db_shrug" to w(0.6), "machine_shrug" to w(1.6),
        // Biceps
        "barbell_curl" to w(0.6), "ez_curl" to w(0.55), "db_curl" to w(0.25), "hammer_curl" to w(0.27),
        "incline_db_curl" to w(0.2), "concentration_curl" to w(0.2), "preacher_curl" to w(0.5),
        "machine_preacher_curl" to w(0.55), "cable_curl" to w(0.55), "rope_hammer_curl" to w(0.5),
        "spider_curl" to w(0.2),
        // Triceps
        "close_grip_bench" to w(1.0), "skull_crusher" to w(0.55), "db_skull_crusher" to w(0.2),
        "triceps_pushdown" to w(0.6), "rope_pushdown" to w(0.5), "overhead_cable_extension" to w(0.5),
        "overhead_db_extension" to w(0.35), "db_kickback" to w(0.12), "triceps_dip" to bw(1.5, 1.0),
        "machine_dip" to w(1.3), "triceps_extension_machine" to w(0.7), "diamond_push_up" to bw(1.1, 0.65),
        // Forearms
        "wrist_curl" to w(0.6), "reverse_curl" to w(0.45),
        // Legs
        "back_squat" to w(1.5), "front_squat" to w(1.2), "smith_squat" to w(1.5), "goblet_squat" to w(0.6),
        "hack_squat" to w(1.6), "pendulum_squat" to w(1.5), "leg_press" to w(2.6), "seated_leg_press" to w(1.8), "vertical_leg_press" to w(2.0), "single_leg_press" to w(1.2), "leg_extension" to w(1.0),
        "bulgarian_split_squat" to w(0.35), "walking_lunge" to w(0.35), "reverse_lunge" to w(0.35),
        "belt_squat" to w(1.5),
        "romanian_deadlift" to w(1.3), "db_romanian_deadlift" to w(0.5), "stiff_leg_deadlift" to w(1.2),
        "good_morning" to w(0.8), "lying_leg_curl" to w(0.75), "seated_leg_curl" to w(0.8),
        "standing_leg_curl" to w(0.35),
        "hip_thrust" to w(1.9), "hip_thrust_machine" to w(1.8), "smith_hip_thrust" to w(1.8),
        "sumo_deadlift" to w(1.9), "glute_kickback_machine" to w(0.8),
        "hip_adduction" to w(1.2), "hip_abduction" to w(1.2),
        "standing_calf_raise" to w(1.5), "seated_calf_raise" to w(1.0), "leg_press_calf_raise" to w(2.0),
        "smith_calf_raise" to w(1.3),
        // Abs and core
        "cable_crunch" to w(0.8), "machine_crunch" to w(0.8),
        "crunch" to reps(40), "hanging_leg_raise" to reps(15), "captains_chair" to reps(20),
        "lying_leg_raise" to reps(25), "plank" to reps(90), "side_plank" to reps(60),
        "decline_sit_up" to reps(25), "ab_wheel" to reps(12), "russian_twist" to reps(40),
        "bicycle_crunch" to reps(40),
    )

    fun compute(sessions: List<SessionFull>, bodyWeight: Double, female: Boolean): Map<Muscle, MuscleLevel> {
        val best = HashMap<Muscle, MuscleLevel>()
        for (s in sessions) {
            for (e in s.exercises) {
                val key = e.exercise.key ?: continue
                val std = standards[key] ?: continue
                val muscle = e.exercise.muscle
                val sexFactor = when {
                    !female -> 1.0
                    muscle in lowerBody -> 0.7
                    else -> 0.55
                }
                for (set in e.sets) {
                    if (!set.done || set.reps <= 0) continue
                    val x = if (std.reps > 0) {
                        set.reps.toDouble() / (std.reps * (if (female) 0.75 else 1.0))
                    } else {
                        val load = set.weight + std.bodyFactor * bodyWeight
                        if (load <= 0) continue
                        val e1rm = load * (1 + min(set.reps, 20) / 30.0)
                        (e1rm / bodyWeight) / (std.ratio * sexFactor)
                    }
                    val score = scoreOf(x)
                    val current = best[muscle]
                    if (current == null || score > current.score) {
                        val unit = if (e.item.perHand) " kg/main" else " kg"
                        val text = when {
                            std.reps > 0 -> "${set.reps} reps"
                            set.weight > 0 -> "${formatWeight(set.weight)}$unit × ${set.reps}"
                            else -> "${set.reps} reps"
                        }
                        best[muscle] = MuscleLevel(muscle, score, e.exercise.name, text)
                    }
                }
            }
        }
        return best
    }

    /** 0 = beginner … 4 = expert, continuous between levels. */
    private fun scoreOf(x: Double): Float {
        if (x <= thresholds[0]) return 0f
        for (i in 0 until thresholds.size - 1) {
            if (x < thresholds[i + 1]) {
                return (i + (x - thresholds[i]) / (thresholds[i + 1] - thresholds[i])).toFloat()
            }
        }
        return 4f
    }
}
