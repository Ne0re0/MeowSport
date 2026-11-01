package fr.meowsport.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Exercise::class,
        WorkoutTemplate::class,
        TemplateExercise::class,
        Session::class,
        SessionExercise::class,
        SessionSet::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exercises(): ExerciseDao
    abstract fun templates(): TemplateDao
    abstract fun sessions(): SessionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN pausedAt INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                // Keep the notes already written in templates: the latest one becomes the exercise note.
                db.execSQL(
                    """
                    UPDATE exercises SET note = (
                        SELECT te.note FROM template_exercises te
                        WHERE te.exerciseId = exercises.id AND te.note != ''
                        ORDER BY te.id DESC LIMIT 1
                    )
                    WHERE EXISTS (
                        SELECT 1 FROM template_exercises te WHERE te.exerciseId = exercises.id AND te.note != ''
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE template_exercises ADD COLUMN supersetNext INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE session_exercises ADD COLUMN supersetNext INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "meowsport.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
