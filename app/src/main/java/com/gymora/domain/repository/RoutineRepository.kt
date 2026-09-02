package com.gymora.domain.repository

import com.gymora.domain.model.RoutineDetail
import com.gymora.domain.model.RoutineSummary
import com.gymora.domain.model.SetTemplateInput
import kotlinx.coroutines.flow.Flow

/**
 * Routine template contract (FR-011..FR-018, contracts/repositories.md).
 * All writes are write-through (R-05). Deletion never touches history
 * (FR-013, BR-03).
 */
interface RoutineRepository {

    /** Routines ordered by position (FR-015). */
    fun observeAll(): Flow<List<RoutineSummary>>

    /** Header + ordered exercises + set templates. Throws EntityNotFoundException. */
    suspend fun getById(id: Long): RoutineDetail

    /** ValidationException on blank name. Returns the new routine id. */
    suspend fun create(name: String, description: String?): Long

    /** History keeps its snapshot (FR-043). */
    suspend fun rename(id: Long, name: String)

    suspend fun updateDescription(id: Long, description: String?)

    /** Deletes routine + template rows only. History untouched (FR-013, BR-03). */
    suspend fun delete(id: Long)

    /** Deep copy incl. exercises and set templates; name "<name> Copy" (FR-014, R-10). */
    suspend fun duplicate(id: Long): Long

    /** Persist a new home-screen order transactionally (FR-015). */
    suspend fun reorder(orderedRoutineIds: List<Long>)

    /** Remove all template references to an exercise (soft-delete cleanup, R-03, BR-04). */
    suspend fun removeExerciseReferences(exerciseId: Long)

    // Exercise membership (FR-016, FR-017)
    suspend fun addExercise(routineId: Long, exerciseId: Long, notes: String?): Long
    suspend fun removeExercise(routineId: Long, routineExerciseId: Long)
    suspend fun reorderExercises(routineId: Long, orderedRoutineExerciseIds: List<Long>)
    suspend fun updateExerciseNotes(routineExerciseId: Long, notes: String?)

    // Planned sets (FR-018)
    suspend fun addSetTemplate(routineExerciseId: Long, template: SetTemplateInput): Long
    suspend fun updateSetTemplate(templateId: Long, template: SetTemplateInput)
    suspend fun deleteSetTemplate(templateId: Long)
}
