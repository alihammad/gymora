package com.gymora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gymora.data.local.entity.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {

    @Query("SELECT * FROM workout_exercises WHERE session_id = :sessionId ORDER BY position")
    suspend fun getForSession(sessionId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE session_id = :sessionId ORDER BY position")
    fun observeForSession(sessionId: Long): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM workout_exercises WHERE session_id = :sessionId")
    suspend fun nextPosition(sessionId: Long): Int

    @Insert
    suspend fun insert(entity: WorkoutExerciseEntity): Long

    @Update
    suspend fun update(entity: WorkoutExerciseEntity)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getById(id: Long): WorkoutExerciseEntity?

    /** FR-044: find all workout exercises for a given exercise across sessions. */
    @Query(
        """
        SELECT we.* FROM workout_exercises we
        INNER JOIN workout_sessions ws ON ws.id = we.session_id
        WHERE we.exercise_id = :exerciseId AND ws.status = 'COMPLETED'
        ORDER BY ws.started_at DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getForExerciseAcrossSessions(
        exerciseId: Long,
        limit: Int,
        offset: Int,
    ): List<WorkoutExerciseEntity>
}
