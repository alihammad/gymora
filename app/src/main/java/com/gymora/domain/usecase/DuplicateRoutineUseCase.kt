package com.gymora.domain.usecase

import com.gymora.domain.model.RoutineRules
import com.gymora.domain.repository.RoutineRepository
import javax.inject.Inject

/**
 * Deep-copy a routine incl. exercises and set templates (FR-014, R-10).
 * The copy gets the derived name "<name> Copy" and is independently editable.
 */
class DuplicateRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
) {

    suspend operator fun invoke(routineId: Long): Long {
        // duplicate() enforces RoutineRules.duplicateName internally (R-10).
        require(routineId > 0)
        return routineRepository.duplicate(routineId)
    }
}
