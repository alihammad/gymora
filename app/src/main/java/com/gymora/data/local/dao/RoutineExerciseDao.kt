package com.gymora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gymora.data.local.entity.RoutineExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineExerciseDao {

    @Query("SELECT * FROM routine_exercises WHERE routine_id = :routineId ORDER BY position")
    suspend fun getForRoutine(routineId: Long): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routine_exercises WHERE id = :id")
    suspend fun getById(id: Long): RoutineExerciseEntity?

    @Query("SELECT * FROM routine_exercises WHERE routine_id = :routineId ORDER BY position")
    fun observeForRoutine(routineId: Long): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM routine_exercises WHERE routine_id = :routineId")
    suspend fun nextPosition(routineId: Long): Int

    @Insert
    suspend fun insert(entity: RoutineExerciseEntity): Long

    @Update
    suspend fun update(entity: RoutineExerciseEntity)

    @Query("DELETE FROM routine_exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM routine_exercises WHERE routine_id = :routineId")
    suspend fun deleteForRoutine(routineId: Long)

    /** Used when an exercise is soft-deleted (R-03, BR-04). */
    @Query("DELETE FROM routine_exercises WHERE exercise_id = :exerciseId")
    suspend fun deleteForExercise(exerciseId: Long)

    @Query("UPDATE routine_exercises SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)
}
