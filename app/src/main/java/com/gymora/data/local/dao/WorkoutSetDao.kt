package com.gymora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gymora.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {

    @Query("SELECT * FROM workout_sets WHERE workout_exercise_id = :workoutExerciseId ORDER BY set_number")
    suspend fun getForExercise(workoutExerciseId: Long): List<WorkoutSetEntity>

    @Query(
        """
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_exercises we ON we.id = ws.workout_exercise_id
        WHERE we.session_id = :sessionId
        ORDER BY we.position, ws.set_number
        """,
    )
    suspend fun getForSession(sessionId: Long): List<WorkoutSetEntity>

    /** Immediate single-row writes (FR-025, FR-027). */
    @Insert
    suspend fun insert(entity: WorkoutSetEntity): Long

    @Update
    suspend fun update(entity: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSetEntity?

    @Query("SELECT COALESCE(MAX(set_number), 0) + 1 FROM workout_sets WHERE workout_exercise_id = :workoutExerciseId")
    suspend fun nextSetNumber(workoutExerciseId: Long): Int
}
