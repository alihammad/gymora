package com.gymora.domain.usecase

import com.gymora.domain.repository.RoutineRepository
import javax.inject.Inject

/**
 * Delete a routine without touching history (FR-012, FR-013, BR-03).
 * History protection is structural: this use case only calls the template-side
 * repository (Constitution IX — one implementation per business rule).
 */
class DeleteRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
) {

    suspend operator fun invoke(routineId: Long) {
        routineRepository.delete(routineId)
    }
}
