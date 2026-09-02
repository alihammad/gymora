package com.gymora.domain.repository

import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.Exercise
import com.gymora.domain.model.UpdateExerciseInput
import kotlinx.coroutines.flow.Flow

/**
 * Exercise library contract (FR-005..FR-010, contracts/repositories.md).
 * All writes are write-through (R-05). History is never touched here
 * (FR-009, BR-04); history displays snapshots (FR-043).
 */
interface ExerciseRepository {

    /** All non-deleted exercises for library browsing (FR-008). */
    fun observeLibrary(): Flow<List<Exercise>>

    /** Search by name substring (case-insensitive); includes custom, excludes soft-deleted. */
    suspend fun search(query: String): List<Exercise>

    /** Throws EntityNotFoundException when missing. */
    suspend fun getById(id: Long): Exercise

    /** Creates a custom exercise. Throws ValidationException on blank name. */
    suspend fun createCustom(input: CreateExerciseInput): Exercise

    /** Throws ValidationException / EntityNotFoundException. */
    suspend fun update(id: Long, input: UpdateExerciseInput): Exercise

    /**
     * Soft delete (R-03): marks deleted_at. Template-reference removal is handled
     * by DeleteExerciseUseCase (T029, US2). History is never touched.
     */
    suspend fun delete(id: Long)
}
