package com.gymora.data.repository

import com.gymora.data.local.ExerciseMetadataCodec
import com.gymora.data.local.dao.ExerciseDao
import com.gymora.data.local.parseMeasurementType
import com.gymora.data.local.entity.ExerciseEntity
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.DifficultyLevel
import com.gymora.domain.model.EntityNotFoundException
import com.gymora.domain.model.EquipmentType
import com.gymora.domain.model.EquipmentUsageType
import com.gymora.domain.model.Exercise
import com.gymora.domain.model.ExerciseCategory
import com.gymora.domain.model.ExerciseType
import com.gymora.domain.model.ExerciseValidation
import com.gymora.domain.model.ForceType
import com.gymora.domain.model.Mechanics
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

    override fun observeById(id: Long): Flow<Exercise?> =
        exerciseDao.observeById(id).map { it?.toDomain() }

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
                measurementType = input.measurementType.name,
                isUnilateral = input.isUnilateral,
                formCuesJson = ExerciseMetadataCodec.encodeFormCues(input.formCues),
                mediaFile = input.mediaFile,
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
                measurementType = input.measurementType.name,
                isUnilateral = input.isUnilateral,
                formCuesJson = ExerciseMetadataCodec.encodeFormCues(input.formCues),
                mediaFile = input.mediaFile,
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
        type = type?.let { ExerciseType.valueOf(it) },
        difficultyLevel = difficultyLevel?.let { DifficultyLevel.valueOf(it) },
        forceType = forceType?.let { ForceType.valueOf(it) },
        mechanics = mechanics?.let { Mechanics.valueOf(it) },
        category = category?.let { ExerciseCategory.valueOf(it) },
        instructions = ExerciseMetadataCodec.decodeInstructions(instructionsJson),
        muscleGroups = ExerciseMetadataCodec.decodeMuscleGroups(muscleGroupsJson),
        equipment = ExerciseMetadataCodec.decodeEquipment(equipmentJson),
        measurementType = parseMeasurementType(measurementType),
        isUnilateral = isUnilateral,
        formCues = ExerciseMetadataCodec.decodeFormCues(formCuesJson),
        mediaFile = mediaFile,
    )
}
