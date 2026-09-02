package com.gymora.domain.model

/** Base type for domain failures surfaced to the ViewModel edge (R-15, FR-060). */
sealed class DomainException(message: String) : Exception(message)

/** Input violated a domain rule (e.g., negative weight — BR-16). */
class ValidationException(
    val field: String,
    message: String,
) : DomainException(message)

/** Start requested while a session is ACTIVE (FR-020, BR-14). */
class ActiveWorkoutConflictException(
    val activeSessionId: Long,
) : DomainException("A workout is already in progress")

/** Referenced routine/exercise/session does not exist (or is soft-deleted where that matters). */
class EntityNotFoundException(
    val id: Long,
) : DomainException("Entity $id was not found")
