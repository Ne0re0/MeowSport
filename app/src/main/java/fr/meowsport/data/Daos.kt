package fr.meowsport.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun get(id: Long): Exercise?

    @Query("SELECT `key` FROM exercises WHERE `key` IS NOT NULL")
    suspend fun builtInKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<Exercise>)

    @Insert
    suspend fun insert(item: Exercise): Long
}

@Dao
interface TemplateDao {
    @Transaction
    @Query("SELECT * FROM templates ORDER BY COALESCE(lastPerformedAt, createdAt) DESC")
    fun observeAll(): Flow<List<TemplateFull>>

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getFull(id: Long): TemplateFull?

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun get(id: Long): WorkoutTemplate?

    @Insert
    suspend fun insert(t: WorkoutTemplate): Long

    @Update
    suspend fun update(t: WorkoutTemplate)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId")
    suspend fun lines(templateId: Long): List<TemplateExercise>

    @Insert
    suspend fun insertLine(l: TemplateExercise): Long

    @Update
    suspend fun updateLine(l: TemplateExercise)

    @Query("DELETE FROM template_exercises WHERE id IN (:ids)")
    suspend fun deleteLines(ids: List<Long>)
}

@Dao
interface SessionDao {
    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeFull(id: Long): Flow<SessionFull?>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getFull(id: Long): SessionFull?

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: Long): Session?

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun active(): Session?

    @Query("SELECT * FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinished(): Flow<List<Session>>

    @Insert
    suspend fun insert(s: Session): Long

    @Update
    suspend fun update(s: Session)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sessions SET note = :note WHERE id = :id")
    suspend fun setNote(id: Long, note: String)

    @Query("UPDATE sessions SET restEndsAt = :endsAt, restTotalSec = :totalSec WHERE id = :id")
    suspend fun setRest(id: Long, endsAt: Long?, totalSec: Int)

    @Query("UPDATE session_exercises SET note = :note WHERE id = :id")
    suspend fun setExerciseNote(id: Long, note: String)

    @Insert
    suspend fun insertExercise(e: SessionExercise): Long

    @Update
    suspend fun updateExercise(e: SessionExercise)

    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun getExercise(id: Long): SessionExercise?

    @Delete
    suspend fun deleteExercise(e: SessionExercise)

    @Insert
    suspend fun insertSet(s: SessionSet): Long

    @Insert
    suspend fun insertSets(s: List<SessionSet>)

    @Update
    suspend fun updateSet(s: SessionSet)

    @Delete
    suspend fun deleteSet(s: SessionSet)

    @Query("SELECT * FROM session_sets WHERE id = :id")
    suspend fun getSet(id: Long): SessionSet?

    @Query("SELECT * FROM session_sets WHERE sessionExerciseId = :id ORDER BY setIndex")
    suspend fun setsOf(id: Long): List<SessionSet>
}
