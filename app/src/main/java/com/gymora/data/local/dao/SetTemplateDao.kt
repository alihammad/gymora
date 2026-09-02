package com.gymora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gymora.data.local.entity.SetTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetTemplateDao {

    @Query("SELECT * FROM set_templates WHERE routine_exercise_id = :routineExerciseId ORDER BY set_number")
    suspend fun getForRoutineExercise(routineExerciseId: Long): List<SetTemplateEntity>

    @Query("SELECT * FROM set_templates WHERE routine_exercise_id = :routineExerciseId ORDER BY set_number")
    fun observeForRoutineExercise(routineExerciseId: Long): Flow<List<SetTemplateEntity>>

    @Query("SELECT COALESCE(MAX(set_number), 0) + 1 FROM set_templates WHERE routine_exercise_id = :routineExerciseId")
    suspend fun nextSetNumber(routineExerciseId: Long): Int

    @Insert
    suspend fun insert(entity: SetTemplateEntity): Long

    @Update
    suspend fun update(entity: SetTemplateEntity)

    @Query("DELETE FROM set_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM set_templates WHERE id = :id")
    suspend fun getById(id: Long): SetTemplateEntity?
}
