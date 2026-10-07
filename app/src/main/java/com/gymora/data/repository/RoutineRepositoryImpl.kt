package com.gymora.data.repository

import androidx.room.withTransaction
import com.gymora.data.local.dao.ExerciseDao
import com.gymora.data.local.dao.RoutineDao
import com.gymora.data.local.dao.RoutineExerciseDao
import com.gymora.data.local.dao.SetTemplateDao
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.parseMeasurementType
import com.gymora.data.local.toDomain
import com.gymora.data.local.entity.RoutineEntity
import com.gymora.data.local.entity.RoutineExerciseEntity
import com.gymora.data.local.entity.SetTemplateEntity
import com.gymora.domain.model.EntityNotFoundException
import com.gymora.domain.model.RoutineDetail
import com.gymora.domain.model.RoutineExerciseDetail
import com.gymora.domain.model.RoutineHeader
import com.gymora.domain.model.RoutineRules
import com.gymora.domain.model.RoutineSummary
import com.gymora.domain.model.SetTemplateInput
import com.gymora.domain.model.SupersetEntry
import com.gymora.domain.model.SupersetRules
import com.gymora.domain.model.ValidationException
import com.gymora.domain.repository.RoutineRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao,
    private val routineExerciseDao: RoutineExerciseDao,
    private val setTemplateDao: SetTemplateDao,
    private val exerciseDao: ExerciseDao,
    private val database: GymoraDatabase,
) : RoutineRepository {

    override fun observeAll(): Flow<List<RoutineSummary>> =
        // Combined so adding/removing exercises (which never touches the
        // routines table) still refreshes the counts.
        combine(
            routineDao.observeAll(),
            routineExerciseDao.observeRoutineIds(),
        ) { entities, exerciseRoutineIds ->
            val counts = exerciseRoutineIds.groupingBy { it }.eachCount()
            entities.map { entity ->
                RoutineSummary(
                    id = entity.id,
                    name = entity.name,
                    exerciseCount = counts[entity.id] ?: 0,
                    // Last-performed date requires workout_sessions (US3, T039).
                    lastPerformedAt = null,
                )
            }
        }

    override suspend fun getById(id: Long): RoutineDetail {
        val header = routineDao.getById(id) ?: throw EntityNotFoundException(id)
        val exerciseEntities = routineExerciseDao.getForRoutine(id)

        val exercises = exerciseEntities.map { routineExercise ->
            val library = exerciseDao.getById(routineExercise.exerciseId)
            RoutineExerciseDetail(
                routineExerciseId = routineExercise.id,
                exerciseId = routineExercise.exerciseId,
                exerciseName = library?.name ?: "Unknown exercise",
                position = routineExercise.position,
                notes = routineExercise.notes,
                setTemplates = setTemplateDao.getForRoutineExercise(routineExercise.id)
                    .map { it.toDomain() },
                supersetGroup = routineExercise.supersetGroup,
                measurementType = parseMeasurementType(library?.measurementType),
                isUnilateral = library?.isUnilateral == true,
            )
        }

        return RoutineDetail(
            header = RoutineHeader(
                id = header.id,
                name = header.name,
                description = header.description,
                position = header.position,
            ),
            exercises = exercises,
        )
    }

    override suspend fun create(name: String, description: String?): Long {
        RoutineRules.validateName(name)
        val now = System.currentTimeMillis()
        return routineDao.insert(
            RoutineEntity(
                name = name.trim(),
                description = description,
                position = routineDao.nextPosition(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun rename(id: Long, name: String) {
        RoutineRules.validateName(name)
        val entity = requireRoutine(id)
        routineDao.update(entity.copy(name = name.trim(), updatedAt = System.currentTimeMillis()))
    }

    override suspend fun updateDescription(id: Long, description: String?) {
        val entity = requireRoutine(id)
        routineDao.update(
            entity.copy(description = description, updatedAt = System.currentTimeMillis()),
        )
    }

    override suspend fun delete(id: Long) {
        requireRoutine(id)
        // CASCADE removes routine_exercises and set_templates; history is never
        // written here (FR-013, BR-03 — enforced structurally, tested in T024).
        routineDao.deleteById(id)
    }

    override suspend fun duplicate(id: Long): Long {
        val source = getById(id)
        val now = System.currentTimeMillis()

        return database.withTransaction {
            val newRoutineId = routineDao.insert(
                RoutineEntity(
                    name = RoutineRules.duplicateName(source.header.name),
                    description = source.header.description,
                    position = routineDao.nextPosition(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            val newIds = mutableMapOf<Long, Long>()
            source.exercises.forEach { exercise ->
                val newRoutineExerciseId = routineExerciseDao.insert(
                    RoutineExerciseEntity(
                        routineId = newRoutineId,
                        exerciseId = exercise.exerciseId,
                        position = exercise.position,
                        notes = exercise.notes,
                    ),
                )
                newIds[exercise.routineExerciseId] = newRoutineExerciseId
                exercise.setTemplates.forEach { template ->
                    setTemplateDao.insert(
                        SetTemplateEntity(
                            routineExerciseId = newRoutineExerciseId,
                            setNumber = template.setNumber,
                            targetReps = template.targetReps,
                            targetWeight = template.targetWeight,
                            targetWeightUnit = template.weightUnit?.name,
                            measurementType = template.measurementType.name,
                            targetDurationSeconds = template.targetDurationSeconds,
                            targetDistanceMeters = template.targetDistanceMeters,
                        ),
                    )
                }
            }
            // Second pass: a group is its first member's id, known only once all rows exist.
            source.exercises.forEach { exercise ->
                exercise.supersetGroup?.let { group ->
                    routineExerciseDao.updateSupersetGroup(
                        newIds.getValue(exercise.routineExerciseId),
                        newIds[group],
                    )
                }
            }
            newRoutineId
        }
    }

    override suspend fun reorder(orderedRoutineIds: List<Long>) {
        database.withTransaction {
            orderedRoutineIds.forEachIndexed { index, routineId ->
                routineDao.updatePosition(routineId, index)
            }
        }
    }

    override suspend fun removeExerciseReferences(exerciseId: Long) {
        routineExerciseDao.deleteForExercise(exerciseId)
    }

    override suspend fun addExercise(routineId: Long, exerciseId: Long, notes: String?): Long {
        requireRoutine(routineId)
        return routineExerciseDao.insert(
            RoutineExerciseEntity(
                routineId = routineId,
                exerciseId = exerciseId,
                position = routineExerciseDao.nextPosition(routineId),
                notes = notes,
            ),
        )
    }

    override suspend fun removeExercise(routineId: Long, routineExerciseId: Long) {
        routineExerciseDao.deleteById(routineExerciseId)
        renumberPositions(routineId)
    }

    override suspend fun reorderExercises(routineId: Long, orderedRoutineExerciseIds: List<Long>) {
        database.withTransaction {
            // Two passes to respect UNIQUE(routine_id, position): first move all
            // rows to negative positions, then assign the final order.
            orderedRoutineExerciseIds.forEachIndexed { index, routineExerciseId ->
                routineExerciseDao.updatePosition(routineExerciseId, -(index + 1))
            }
            orderedRoutineExerciseIds.forEachIndexed { index, routineExerciseId ->
                routineExerciseDao.updatePosition(routineExerciseId, index)
            }
            // A move can split a superset or strand a single member.
            applySupersetGroups(routineId) { SupersetRules.normalize(it) }
        }
    }

    override suspend fun linkSupersetWithNext(routineId: Long, routineExerciseId: Long) {
        database.withTransaction {
            applySupersetGroups(routineId) { entries ->
                SupersetRules.linkWithNext(entries, indexOf(entries, routineExerciseId))
            }
        }
    }

    override suspend fun unlinkSupersetFromNext(routineId: Long, routineExerciseId: Long) {
        database.withTransaction {
            applySupersetGroups(routineId) { entries ->
                SupersetRules.unlinkFromNext(entries, indexOf(entries, routineExerciseId))
            }
        }
    }

    override suspend fun updateExerciseNotes(routineExerciseId: Long, notes: String?) {
        val entity = routineExerciseDao.getById(routineExerciseId)
            ?: throw EntityNotFoundException(routineExerciseId)
        routineExerciseDao.update(entity.copy(notes = notes))
    }

    override suspend fun addSetTemplate(routineExerciseId: Long, template: SetTemplateInput): Long {
        validateTemplate(template)
        return setTemplateDao.insert(
            SetTemplateEntity(
                routineExerciseId = routineExerciseId,
                setNumber = setTemplateDao.nextSetNumber(routineExerciseId),
                targetReps = template.targetReps,
                targetWeight = template.targetWeight,
                targetWeightUnit = template.weightUnit?.name,
                measurementType = template.measurementType.name,
                targetDurationSeconds = template.targetDurationSeconds,
                targetDistanceMeters = template.targetDistanceMeters,
            ),
        )
    }

    override suspend fun updateSetTemplate(templateId: Long, template: SetTemplateInput) {
        validateTemplate(template)
        val entity = setTemplateDao.getById(templateId) ?: throw EntityNotFoundException(templateId)
        setTemplateDao.update(
            entity.copy(
                targetReps = template.targetReps,
                targetWeight = template.targetWeight,
                targetWeightUnit = template.weightUnit?.name,
                measurementType = template.measurementType.name,
                targetDurationSeconds = template.targetDurationSeconds,
                targetDistanceMeters = template.targetDistanceMeters,
            ),
        )
    }

    override suspend fun deleteSetTemplate(templateId: Long) {
        setTemplateDao.deleteById(templateId)
    }

    private suspend fun requireRoutine(id: Long): RoutineEntity =
        routineDao.getById(id) ?: throw EntityNotFoundException(id)

    private suspend fun renumberPositions(routineId: Long) {
        database.withTransaction {
            val entities = routineExerciseDao.getForRoutine(routineId)
            // Two passes to respect UNIQUE(routine_id, position).
            entities.forEachIndexed { index, entity ->
                routineExerciseDao.updatePosition(entity.id, -(index + 1))
            }
            entities.forEachIndexed { index, entity ->
                routineExerciseDao.updatePosition(entity.id, index)
            }
            // Removing a superset member can leave a single exercise behind.
            applySupersetGroups(routineId) { SupersetRules.normalize(it) }
        }
    }

    /** Computes new superset groups from the routine's current order and writes changed rows. */
    private suspend fun applySupersetGroups(
        routineId: Long,
        compute: (List<SupersetEntry>) -> Map<Long, Long?>,
    ) {
        val entities = routineExerciseDao.getForRoutine(routineId)
        val groups = compute(entities.map { SupersetEntry(it.id, it.supersetGroup) })
        entities.forEach { entity ->
            val group = groups[entity.id]
            if (group != entity.supersetGroup) {
                routineExerciseDao.updateSupersetGroup(entity.id, group)
            }
        }
    }

    private fun indexOf(entries: List<SupersetEntry>, routineExerciseId: Long): Int =
        entries.indexOfFirst { it.id == routineExerciseId }
            .takeIf { it >= 0 } ?: throw EntityNotFoundException(routineExerciseId)

    private fun validateTemplate(template: SetTemplateInput) {
        val problem = when {
            template.targetReps < 0 -> "targetReps" to "Target reps must not be negative"
            (template.targetWeight ?: 0.0) < 0 -> "targetWeight" to "Target weight must not be negative"
            (template.targetDurationSeconds ?: 0) < 0 || (template.targetDistanceMeters ?: 0.0) < 0 ->
                "target" to "Targets must not be negative"
            else -> null
        }
        problem?.let { (field, message) -> throw ValidationException(field, message) }
    }
}
