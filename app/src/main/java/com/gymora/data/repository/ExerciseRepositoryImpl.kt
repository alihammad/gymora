package com.gymora.data.repository

import com.gymora.data.local.dao.ExerciseDao
import com.gymora.data.local.entity.ExerciseEntity
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.EntityNotFoundException
import com.gymora.domain.model.Exercise
import com.gymora.domain.model.ExerciseValidation
import com.gymora.domain.model.MuscleGroup
import com.gymora.domain.model.UpdateExerciseInput
import com.gymora.domain.repository.ExerciseRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
) : ExerciseRepository {

    override fun observeLibrary(): Flow<List<Exercise>> =
        exerciseDao.observeActiveLibrary().map { entities -> entities.map { it.toDomain() } }

    override suspend fun search(query: String): List<Exercise> =
        exerciseDao.search(query.trim()).map { it.toDomain() }

    override suspend fun getById(id: Long): Exercise {
        val entity = exerciseDao.getById(id) ?: throw EntityNotFoundException(id)
        return entity.toDomain()
    }

    override suspend fun createCustom(input: CreateExerciseInput): Exercise {
        ExerciseValidation.validate(input)
        val now = System.currentTimeMillis()
        val id = exerciseDao.insert(
            ExerciseEntity(
                name = input.name.trim(),
                muscleGroup = input.muscleGroup?.name,
                description = input.description,
                notes = input.notes,
                isCustom = true,
                deletedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return getById(id)
    }

    override suspend fun update(id: Long, input: UpdateExerciseInput): Exercise {
        ExerciseValidation.validate(input)
        val entity = exerciseDao.getById(id) ?: throw EntityNotFoundException(id)
        exerciseDao.update(
            entity.copy(
                name = input.name.trim(),
                muscleGroup = input.muscleGroup?.name,
                description = input.description,
                notes = input.notes,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return getById(id)
    }

    override suspend fun delete(id: Long) {
        exerciseDao.getById(id) ?: throw EntityNotFoundException(id)
        exerciseDao.softDelete(id, System.currentTimeMillis())
    }

    private fun ExerciseEntity.toDomain(): Exercise = Exercise(
        id = id,
        name = name,
        muscleGroup = muscleGroup?.let { MuscleGroup.valueOf(it) },
        description = description,
        notes = notes,
        isCustom = isCustom,
        isDeleted = deletedAt != null,
    )
}
