package fr.meowsport.data

import fr.meowsport.data.Equipment.BARBELL
import fr.meowsport.data.Equipment.BAND
import fr.meowsport.data.Equipment.BODYWEIGHT
import fr.meowsport.data.Equipment.CABLE
import fr.meowsport.data.Equipment.DUMBBELL
import fr.meowsport.data.Equipment.EZ_BAR
import fr.meowsport.data.Equipment.KETTLEBELL
import fr.meowsport.data.Equipment.MACHINE
import fr.meowsport.data.Equipment.OTHER
import fr.meowsport.data.Equipment.SMITH
import fr.meowsport.data.Muscle.ABDUCTORS
import fr.meowsport.data.Muscle.ABS
import fr.meowsport.data.Muscle.ADDUCTORS
import fr.meowsport.data.Muscle.BACK
import fr.meowsport.data.Muscle.BICEPS
import fr.meowsport.data.Muscle.CALVES
import fr.meowsport.data.Muscle.CHEST
import fr.meowsport.data.Muscle.FOREARMS
import fr.meowsport.data.Muscle.GLUTES
import fr.meowsport.data.Muscle.HAMSTRINGS
import fr.meowsport.data.Muscle.LOWER_BACK
import fr.meowsport.data.Muscle.OBLIQUES
import fr.meowsport.data.Muscle.QUADS
import fr.meowsport.data.Muscle.REAR_DELTS
import fr.meowsport.data.Muscle.SHOULDERS
import fr.meowsport.data.Muscle.TRAPS
import fr.meowsport.data.Muscle.TRICEPS

/** Built-in list of gym exercises. Keys must never change: they identify rows in the database. */
object ExerciseCatalog {

    private fun e(
        key: String,
        name: String,
        nameFr: String,
        muscle: Muscle,
        equipment: Equipment,
        vararg secondary: Muscle,
    ) = Exercise(
        key = key,
        name = name,
        nameFr = nameFr,
        muscle = muscle,
        secondary = secondary.joinToString(",") { it.name },
        equipment = equipment,
    )

    val all: List<Exercise> = listOf(
        // Chest
        e("bench_press", "Bench press", "Développé couché barre", CHEST, BARBELL, TRICEPS, SHOULDERS),
        e("db_bench_press", "Dumbbell bench press", "Développé couché haltères", CHEST, DUMBBELL, TRICEPS, SHOULDERS),
        e("incline_bench_press", "Incline bench press", "Développé incliné barre", CHEST, BARBELL, SHOULDERS, TRICEPS),
        e("incline_db_press", "Incline dumbbell press", "Développé incliné haltères", CHEST, DUMBBELL, SHOULDERS, TRICEPS),
        e("decline_bench_press", "Decline bench press", "Développé décliné barre", CHEST, BARBELL, TRICEPS),
        e("decline_db_press", "Decline dumbbell press", "Développé décliné haltères", CHEST, DUMBBELL, TRICEPS),
        e("smith_bench_press", "Smith machine bench press", "Développé couché Smith machine", CHEST, SMITH, TRICEPS, SHOULDERS),
        e("smith_incline_press", "Smith machine incline press", "Développé incliné Smith machine", CHEST, SMITH, SHOULDERS, TRICEPS),
        e("machine_chest_press", "Chest press machine", "Presse pectoraux", CHEST, MACHINE, TRICEPS, SHOULDERS),
        e("incline_chest_press_machine", "Incline chest press machine", "Presse pectoraux inclinée", CHEST, MACHINE, SHOULDERS, TRICEPS),
        e("db_fly", "Dumbbell fly", "Écarté couché haltères", CHEST, DUMBBELL, SHOULDERS),
        e("incline_db_fly", "Incline dumbbell fly", "Écarté incliné haltères", CHEST, DUMBBELL, SHOULDERS),
        e("pec_deck", "Pec deck", "Pec deck (butterfly)", CHEST, MACHINE),
        e("cable_crossover", "Cable crossover", "Écarté à la poulie vis-à-vis", CHEST, CABLE, SHOULDERS),
        e("low_cable_fly", "Low to high cable fly", "Écarté poulie basse vers haut", CHEST, CABLE, SHOULDERS),
        e("push_up", "Push-up", "Pompes", CHEST, BODYWEIGHT, TRICEPS, SHOULDERS),
        e("chest_dip", "Chest dip", "Dips pectoraux", CHEST, BODYWEIGHT, TRICEPS, SHOULDERS),
        e("db_pullover", "Dumbbell pullover", "Pull-over haltère", CHEST, DUMBBELL, BACK),
        e("svend_press", "Svend press", "Svend press (disque)", CHEST, OTHER, SHOULDERS),

        // Back
        e("pull_up", "Pull-up", "Tractions pronation", BACK, BODYWEIGHT, BICEPS, REAR_DELTS),
        e("chin_up", "Chin-up", "Tractions supination", BACK, BODYWEIGHT, BICEPS),
        e("assisted_pull_up", "Assisted pull-up machine", "Tractions assistées", BACK, MACHINE, BICEPS),
        e("lat_pulldown", "Lat pulldown", "Tirage vertical poitrine", BACK, CABLE, BICEPS, REAR_DELTS),
        e("close_grip_pulldown", "Close-grip lat pulldown", "Tirage vertical prise serrée", BACK, CABLE, BICEPS),
        e("single_arm_pulldown", "Single-arm cable pulldown", "Tirage vertical unilatéral", BACK, CABLE, BICEPS),
        e("straight_arm_pulldown", "Straight-arm pulldown", "Pull-over à la poulie haute", BACK, CABLE, TRICEPS),
        e("seated_row", "Seated cable row", "Tirage horizontal assis (poulie)", BACK, CABLE, BICEPS, REAR_DELTS),
        e("machine_row", "Seated row machine", "Rowing assis machine", BACK, MACHINE, BICEPS, REAR_DELTS),
        e("barbell_row", "Barbell row", "Rowing barre", BACK, BARBELL, BICEPS, LOWER_BACK),
        e("pendlay_row", "Pendlay row", "Rowing Pendlay", BACK, BARBELL, BICEPS, LOWER_BACK),
        e("db_row", "One-arm dumbbell row", "Rowing haltère un bras", BACK, DUMBBELL, BICEPS, REAR_DELTS),
        e("chest_supported_row", "Chest-supported dumbbell row", "Rowing haltères buste appuyé", BACK, DUMBBELL, BICEPS, REAR_DELTS),
        e("t_bar_row", "T-bar row", "Rowing T-bar", BACK, MACHINE, BICEPS, LOWER_BACK),
        e("smith_row", "Smith machine row", "Rowing Smith machine", BACK, SMITH, BICEPS),
        e("inverted_row", "Inverted row", "Rowing inversé", BACK, BODYWEIGHT, BICEPS),
        e("deadlift", "Deadlift", "Soulevé de terre", LOWER_BACK, BARBELL, GLUTES, HAMSTRINGS, BACK),
        e("rack_pull", "Rack pull", "Rack pull", LOWER_BACK, BARBELL, BACK, GLUTES, TRAPS),
        e("back_extension", "Back extension", "Extension lombaire (banc à 45°)", LOWER_BACK, BODYWEIGHT, GLUTES, HAMSTRINGS),
        e("back_extension_machine", "Back extension machine", "Extension lombaire machine", LOWER_BACK, MACHINE, GLUTES),
        e("good_morning", "Good morning", "Good morning", HAMSTRINGS, BARBELL, LOWER_BACK, GLUTES),
        e("superman", "Superman", "Superman", LOWER_BACK, BODYWEIGHT, GLUTES),

        // Shoulders
        e("overhead_press", "Overhead press", "Développé militaire barre", SHOULDERS, BARBELL, TRICEPS, TRAPS),
        e("db_shoulder_press", "Dumbbell shoulder press", "Développé épaules haltères", SHOULDERS, DUMBBELL, TRICEPS),
        e("arnold_press", "Arnold press", "Développé Arnold", SHOULDERS, DUMBBELL, TRICEPS),
        e("machine_shoulder_press", "Shoulder press machine", "Presse épaules machine", SHOULDERS, MACHINE, TRICEPS),
        e("smith_shoulder_press", "Smith machine shoulder press", "Développé épaules Smith machine", SHOULDERS, SMITH, TRICEPS),
        e("lateral_raise", "Dumbbell lateral raise", "Élévations latérales haltères", SHOULDERS, DUMBBELL, TRAPS),
        e("cable_lateral_raise", "Cable lateral raise", "Élévation latérale à la poulie", SHOULDERS, CABLE),
        e("machine_lateral_raise", "Lateral raise machine", "Élévations latérales machine", SHOULDERS, MACHINE),
        e("front_raise", "Dumbbell front raise", "Élévations frontales haltères", SHOULDERS, DUMBBELL, CHEST),
        e("cable_front_raise", "Cable front raise", "Élévation frontale à la poulie", SHOULDERS, CABLE),
        e("upright_row", "Upright row", "Tirage menton", SHOULDERS, BARBELL, TRAPS, BICEPS),
        e("rear_delt_fly", "Rear delt dumbbell fly", "Oiseau haltères", REAR_DELTS, DUMBBELL, TRAPS, BACK),
        e("reverse_pec_deck", "Reverse pec deck", "Pec deck inversé", REAR_DELTS, MACHINE, TRAPS),
        e("face_pull", "Face pull", "Face pull", REAR_DELTS, CABLE, TRAPS),
        e("cable_rear_delt_fly", "Cable rear delt fly", "Oiseau à la poulie", REAR_DELTS, CABLE, TRAPS),
        e("landmine_press", "Landmine press", "Développé landmine", SHOULDERS, BARBELL, CHEST, TRICEPS),
        e("pike_push_up", "Pike push-up", "Pompes piquées", SHOULDERS, BODYWEIGHT, TRICEPS),

        // Traps
        e("barbell_shrug", "Barbell shrug", "Shrugs barre", TRAPS, BARBELL),
        e("db_shrug", "Dumbbell shrug", "Shrugs haltères", TRAPS, DUMBBELL),
        e("machine_shrug", "Shrug machine", "Shrugs machine", TRAPS, MACHINE),
        e("farmer_walk", "Farmer's walk", "Marche du fermier", TRAPS, DUMBBELL, FOREARMS),

        // Biceps
        e("barbell_curl", "Barbell curl", "Curl barre", BICEPS, BARBELL, FOREARMS),
        e("ez_curl", "EZ-bar curl", "Curl barre EZ", BICEPS, EZ_BAR, FOREARMS),
        e("db_curl", "Dumbbell curl", "Curl haltères", BICEPS, DUMBBELL, FOREARMS),
        e("hammer_curl", "Hammer curl", "Curl marteau", BICEPS, DUMBBELL, FOREARMS),
        e("incline_db_curl", "Incline dumbbell curl", "Curl incliné haltères", BICEPS, DUMBBELL),
        e("concentration_curl", "Concentration curl", "Curl concentré", BICEPS, DUMBBELL),
        e("preacher_curl", "Preacher curl", "Curl pupitre", BICEPS, EZ_BAR),
        e("machine_preacher_curl", "Preacher curl machine", "Curl pupitre machine", BICEPS, MACHINE),
        e("cable_curl", "Cable curl", "Curl à la poulie basse", BICEPS, CABLE, FOREARMS),
        e("rope_hammer_curl", "Cable rope hammer curl", "Curl marteau à la corde", BICEPS, CABLE, FOREARMS),
        e("bayesian_curl", "Bayesian cable curl", "Curl bayésien à la poulie", BICEPS, CABLE),
        e("spider_curl", "Spider curl", "Spider curl", BICEPS, DUMBBELL),
        e("zottman_curl", "Zottman curl", "Curl Zottman", BICEPS, DUMBBELL, FOREARMS),

        // Triceps
        e("close_grip_bench", "Close-grip bench press", "Développé couché prise serrée", TRICEPS, BARBELL, CHEST),
        e("skull_crusher", "Skull crusher", "Barre au front", TRICEPS, EZ_BAR),
        e("db_skull_crusher", "Dumbbell skull crusher", "Extension triceps couché haltères", TRICEPS, DUMBBELL),
        e("triceps_pushdown", "Triceps pushdown", "Extension triceps poulie haute (barre)", TRICEPS, CABLE),
        e("rope_pushdown", "Rope pushdown", "Extension triceps à la corde", TRICEPS, CABLE),
        e("overhead_cable_extension", "Overhead cable triceps extension", "Extension triceps poulie au-dessus de la tête", TRICEPS, CABLE),
        e("overhead_db_extension", "Overhead dumbbell extension", "Extension triceps haltère au-dessus de la tête", TRICEPS, DUMBBELL),
        e("db_kickback", "Dumbbell kickback", "Kickback haltère", TRICEPS, DUMBBELL),
        e("triceps_dip", "Triceps dip", "Dips triceps", TRICEPS, BODYWEIGHT, CHEST, SHOULDERS),
        e("bench_dip", "Bench dip", "Dips sur banc", TRICEPS, BODYWEIGHT),
        e("machine_dip", "Dip machine", "Dips machine", TRICEPS, MACHINE, CHEST),
        e("triceps_extension_machine", "Triceps extension machine", "Extension triceps machine", TRICEPS, MACHINE),
        e("diamond_push_up", "Diamond push-up", "Pompes diamant", TRICEPS, BODYWEIGHT, CHEST),

        // Forearms
        e("wrist_curl", "Wrist curl", "Curl poignets", FOREARMS, BARBELL),
        e("reverse_wrist_curl", "Reverse wrist curl", "Curl poignets inversé", FOREARMS, DUMBBELL),
        e("reverse_curl", "Reverse curl", "Curl inversé", FOREARMS, EZ_BAR, BICEPS),
        e("dead_hang", "Dead hang", "Suspension à la barre", FOREARMS, BODYWEIGHT, BACK),

        // Quads
        e("back_squat", "Back squat", "Squat barre", QUADS, BARBELL, GLUTES, HAMSTRINGS),
        e("front_squat", "Front squat", "Squat avant", QUADS, BARBELL, GLUTES, ABS),
        e("smith_squat", "Smith machine squat", "Squat Smith machine", QUADS, SMITH, GLUTES),
        e("goblet_squat", "Goblet squat", "Goblet squat", QUADS, DUMBBELL, GLUTES),
        e("hack_squat", "Hack squat", "Hack squat", QUADS, MACHINE, GLUTES),
        e("pendulum_squat", "Pendulum squat", "Pendulum squat", QUADS, MACHINE, GLUTES),
        e("leg_press", "Leg press (45° diagonal)", "Presse à cuisses inclinée 45°", QUADS, MACHINE, GLUTES, HAMSTRINGS),
        e("seated_leg_press", "Seated leg press (horizontal)", "Presse à cuisses horizontale assise", QUADS, MACHINE, GLUTES, HAMSTRINGS),
        e("vertical_leg_press", "Vertical leg press", "Presse à cuisses verticale", QUADS, MACHINE, GLUTES),
        e("single_leg_press", "Single-leg press", "Presse à cuisses unilatérale", QUADS, MACHINE, GLUTES),
        e("leg_extension", "Leg extension", "Leg extension", QUADS, MACHINE),
        e("bulgarian_split_squat", "Bulgarian split squat", "Squat bulgare", QUADS, DUMBBELL, GLUTES),
        e("walking_lunge", "Walking lunge", "Fentes marchées", QUADS, DUMBBELL, GLUTES, HAMSTRINGS),
        e("reverse_lunge", "Reverse lunge", "Fentes arrière", QUADS, DUMBBELL, GLUTES),
        e("smith_lunge", "Smith machine lunge", "Fentes Smith machine", QUADS, SMITH, GLUTES),
        e("step_up", "Step-up", "Montées sur banc", QUADS, DUMBBELL, GLUTES),
        e("sissy_squat", "Sissy squat", "Sissy squat", QUADS, BODYWEIGHT),
        e("belt_squat", "Belt squat", "Belt squat", QUADS, MACHINE, GLUTES),
        e("air_squat", "Bodyweight squat", "Squat au poids du corps", QUADS, BODYWEIGHT, GLUTES),
        e("wall_sit", "Wall sit", "Chaise contre le mur", QUADS, BODYWEIGHT),

        // Hamstrings
        e("romanian_deadlift", "Romanian deadlift", "Soulevé de terre roumain", HAMSTRINGS, BARBELL, GLUTES, LOWER_BACK),
        e("db_romanian_deadlift", "Dumbbell Romanian deadlift", "Soulevé de terre roumain haltères", HAMSTRINGS, DUMBBELL, GLUTES, LOWER_BACK),
        e("stiff_leg_deadlift", "Stiff-leg deadlift", "Soulevé de terre jambes tendues", HAMSTRINGS, BARBELL, LOWER_BACK, GLUTES),
        e("lying_leg_curl", "Lying leg curl", "Leg curl allongé", HAMSTRINGS, MACHINE, CALVES),
        e("seated_leg_curl", "Seated leg curl", "Leg curl assis", HAMSTRINGS, MACHINE),
        e("standing_leg_curl", "Standing leg curl", "Leg curl debout unilatéral", HAMSTRINGS, MACHINE),
        e("nordic_curl", "Nordic hamstring curl", "Nordic curl", HAMSTRINGS, BODYWEIGHT),
        e("kb_swing", "Kettlebell swing", "Swing kettlebell", HAMSTRINGS, KETTLEBELL, GLUTES, LOWER_BACK),

        // Glutes
        e("hip_thrust", "Barbell hip thrust", "Hip thrust barre", GLUTES, BARBELL, HAMSTRINGS),
        e("hip_thrust_machine", "Hip thrust machine", "Hip thrust machine", GLUTES, MACHINE, HAMSTRINGS),
        e("smith_hip_thrust", "Smith machine hip thrust", "Hip thrust Smith machine", GLUTES, SMITH, HAMSTRINGS),
        e("glute_bridge", "Glute bridge", "Pont fessier", GLUTES, BODYWEIGHT, HAMSTRINGS),
        e("cable_kickback", "Cable glute kickback", "Kickback fessier à la poulie", GLUTES, CABLE, HAMSTRINGS),
        e("glute_kickback_machine", "Glute kickback machine", "Kickback fessier machine", GLUTES, MACHINE, HAMSTRINGS),
        e("sumo_deadlift", "Sumo deadlift", "Soulevé de terre sumo", GLUTES, BARBELL, ADDUCTORS, QUADS, LOWER_BACK),
        e("cable_pull_through", "Cable pull-through", "Pull-through à la poulie", GLUTES, CABLE, HAMSTRINGS),

        // Adductors / abductors
        e("hip_adduction", "Hip adduction machine", "Adducteurs machine", ADDUCTORS, MACHINE),
        e("cable_hip_adduction", "Cable hip adduction", "Adduction à la poulie", ADDUCTORS, CABLE),
        e("copenhagen_plank", "Copenhagen plank", "Planche Copenhague", ADDUCTORS, BODYWEIGHT, OBLIQUES),
        e("hip_abduction", "Hip abduction machine", "Abducteurs machine", ABDUCTORS, MACHINE, GLUTES),
        e("cable_hip_abduction", "Cable hip abduction", "Abduction à la poulie", ABDUCTORS, CABLE, GLUTES),
        e("banded_side_walk", "Banded lateral walk", "Marche latérale avec élastique", ABDUCTORS, BAND, GLUTES),

        // Calves
        e("standing_calf_raise", "Standing calf raise", "Mollets debout machine", CALVES, MACHINE),
        e("seated_calf_raise", "Seated calf raise", "Mollets assis machine", CALVES, MACHINE),
        e("leg_press_calf_raise", "Leg press calf raise", "Mollets à la presse", CALVES, MACHINE),
        e("smith_calf_raise", "Smith machine calf raise", "Mollets Smith machine", CALVES, SMITH),
        e("db_calf_raise", "Dumbbell calf raise", "Mollets debout haltères", CALVES, DUMBBELL),

        // Abs and core
        e("crunch", "Crunch", "Crunch", ABS, BODYWEIGHT),
        e("cable_crunch", "Cable crunch", "Crunch à la poulie haute", ABS, CABLE),
        e("machine_crunch", "Ab crunch machine", "Crunch machine", ABS, MACHINE),
        e("hanging_leg_raise", "Hanging leg raise", "Relevé de jambes suspendu", ABS, BODYWEIGHT, OBLIQUES),
        e("captains_chair", "Captain's chair leg raise", "Relevé de jambes à la chaise romaine", ABS, BODYWEIGHT),
        e("lying_leg_raise", "Lying leg raise", "Relevé de jambes allongé", ABS, BODYWEIGHT),
        e("plank", "Plank", "Gainage (planche)", ABS, BODYWEIGHT, OBLIQUES, SHOULDERS),
        e("side_plank", "Side plank", "Gainage latéral", OBLIQUES, BODYWEIGHT, ABS),
        e("ab_wheel", "Ab wheel rollout", "Roue abdominale", ABS, OTHER, BACK),
        e("decline_sit_up", "Decline sit-up", "Sit-up sur banc décliné", ABS, BODYWEIGHT),
        e("russian_twist", "Russian twist", "Russian twist", OBLIQUES, BODYWEIGHT, ABS),
        e("cable_woodchop", "Cable woodchop", "Bûcheron à la poulie", OBLIQUES, CABLE, ABS),
        e("pallof_press", "Pallof press", "Pallof press", OBLIQUES, CABLE, ABS),
        e("bicycle_crunch", "Bicycle crunch", "Crunch vélo", OBLIQUES, BODYWEIGHT, ABS),
        e("db_side_bend", "Dumbbell side bend", "Flexion latérale haltère", OBLIQUES, DUMBBELL),
        e("dead_bug", "Dead bug", "Dead bug", ABS, BODYWEIGHT),
        e("mountain_climber", "Mountain climber", "Mountain climbers", ABS, BODYWEIGHT, SHOULDERS, QUADS),

        // Full body / other
        e("clean_and_press", "Clean and press", "Épaulé-jeté", SHOULDERS, BARBELL, QUADS, GLUTES, TRAPS),
        e("power_clean", "Power clean", "Épaulé (power clean)", TRAPS, BARBELL, GLUTES, HAMSTRINGS, QUADS),
        e("thruster", "Thruster", "Thruster", QUADS, BARBELL, SHOULDERS, GLUTES),
        e("kb_goblet_squat", "Kettlebell goblet squat", "Goblet squat kettlebell", QUADS, KETTLEBELL, GLUTES),
        e("burpee", "Burpee", "Burpees", QUADS, BODYWEIGHT, CHEST, SHOULDERS),
        e("sled_push", "Sled push", "Poussée de traîneau", QUADS, OTHER, GLUTES, CALVES),
    )
}
