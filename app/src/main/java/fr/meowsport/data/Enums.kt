package fr.meowsport.data

/** Muscle groups. [front] tells which side of the body shows the muscle. */
enum class Muscle(val label: String, val front: Boolean) {
    CHEST("Pectoraux", true),
    SHOULDERS("Épaules", true),
    BICEPS("Biceps", true),
    FOREARMS("Avant-bras", true),
    ABS("Abdominaux", true),
    OBLIQUES("Obliques", true),
    QUADS("Quadriceps", true),
    ADDUCTORS("Adducteurs", true),
    BACK("Dos", false),
    TRAPS("Trapèzes", false),
    REAR_DELTS("Deltoïdes arrière", false),
    TRICEPS("Triceps", false),
    LOWER_BACK("Lombaires", false),
    GLUTES("Fessiers", false),
    ABDUCTORS("Abducteurs", false),
    HAMSTRINGS("Ischio-jambiers", false),
    CALVES("Mollets", false),
}

enum class Equipment(val label: String) {
    BARBELL("Barre"),
    DUMBBELL("Haltères"),
    EZ_BAR("Barre EZ"),
    MACHINE("Machine"),
    SMITH("Smith machine"),
    CABLE("Poulie"),
    KETTLEBELL("Kettlebell"),
    BODYWEIGHT("Poids du corps"),
    BAND("Élastique"),
    OTHER("Autre"),
}
