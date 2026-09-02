package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.repository.ExerciseRepositoryImpl
import com.gymora.data.repository.RoutineRepositoryImpl
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SetTemplateInput
import com.gymora.domain.model.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * T023 [US2]: RoutineRepository behavior (FR-011..FR-018).
 * Create/rename/delete/duplicate/reorder, exercise membership, set template
 * CRUD, and persisted positions.
 */
@RunWith(RobolectricTestRunner::class)
class RoutineRepositoryTest {

    private lateinit var database: GymoraDatabase
    private lateinit var repository: RoutineRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoutineRepositoryImpl(
            database.routineDao(),
            database.routineExerciseDao(),
            database.setTemplateDao(),
            database.exerciseDao(),
            database,
        )
        exerciseRepository = ExerciseRepositoryImpl(database.exerciseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createExercise(name: String): Long =
        exerciseRepository.createCustom(CreateExerciseInput(name = name)).id

    @Test
    fun createAndGetByIdReturnsDetail() = runTest {
        val exerciseId = createExercise("Bench Press")
        val routineId = repository.create("Chest Workout", "Push day")

        val detail = repository.getById(routineId)
        assertEquals("Chest Workout", detail.header.name)
        assertEquals("Push day", detail.header.description)

        val routineExerciseId = repository.addExercise(routineId, exerciseId, notes = null)
        repository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(
                targetReps = 10,
                targetWeight = 60.0,
                weightUnit = WeightUnit.KG,
                measurementType = MeasurementType.WEIGHT_AND_REPS,
            ),
        )

        val withSets = repository.getById(routineId)
        assertEquals(1, withSets.exercises.size)
        assertEquals(1, withSets.exercises.first().setTemplates.size)
        assertEquals(10, withSets.exercises.first().setTemplates.first().targetReps)
    }

    @Test
    fun renameUpdatesName() = runTest {
        val id = repository.create("Old Name", null)
        repository.rename(id, "New Name")
        assertEquals("New Name", repository.getById(id).header.name)
    }

    @Test
    fun deleteRemovesRoutineAndTemplateRows() = runTest {
        val exerciseId = createExercise("Squat")
        val id = repository.create("Leg Day", null)
        val routineExerciseId = repository.addExercise(id, exerciseId, notes = null)
        repository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(5, 100.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )

        repository.delete(id)

        val all = repository.observeAll().first()
        assertTrue(all.none { it.id == id })
        assertTrue(database.routineExerciseDao().getForRoutine(id).isEmpty())
        assertTrue(database.setTemplateDao().getForRoutineExercise(routineExerciseId).isEmpty())
    }

    @Test
    fun duplicateCreatesDeepCopyWithDerivedName() = runTest {
        val exerciseId = createExercise("Deadlift")
        val id = repository.create("Back Day", "Pull")
        val routineExerciseId = repository.addExercise(id, exerciseId, notes = "Careful")
        repository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(3, 140.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )

        val copyId = repository.duplicate(id)

        val copy = repository.getById(copyId)
        assertEquals("Back Day Copy", copy.header.name)
        assertEquals(1, copy.exercises.size)
        assertEquals("Careful", copy.exercises.first().notes)
        assertEquals(1, copy.exercises.first().setTemplates.size)
        // Copy is independently editable: editing it does not affect the original.
        repository.rename(copyId, "Back Day v2")
        assertEquals("Back Day", repository.getById(id).header.name)
    }

    @Test
    fun reorderPersistsPositions() = runTest {
        val a = repository.create("A", null)
        val b = repository.create("B", null)
        val c = repository.create("C", null)

        repository.reorder(listOf(c, a, b))

        val ordered = repository.observeAll().first()
        assertEquals(listOf("C", "A", "B"), ordered.map { it.name })
    }

    @Test
    fun addRemoveReorderExercisesPersistsOrder() = runTest {
        val e1 = createExercise("Exercise 1")
        val e2 = createExercise("Exercise 2")
        val e3 = createExercise("Exercise 3")
        val routineId = repository.create("Routine", null)

        val re1 = repository.addExercise(routineId, e1, notes = null)
        val re2 = repository.addExercise(routineId, e2, notes = null)
        val re3 = repository.addExercise(routineId, e3, notes = null)

        repository.reorderExercises(routineId, listOf(re3, re1, re2))
        var detail = repository.getById(routineId)
        assertEquals(
            listOf("Exercise 3", "Exercise 1", "Exercise 2"),
            detail.exercises.map { it.exerciseName },
        )

        repository.removeExercise(routineId, re1)
        detail = repository.getById(routineId)
        assertEquals(2, detail.exercises.size)
        assertTrue(detail.exercises.none { it.exerciseName == "Exercise 1" })
    }

    @Test
    fun setTemplateCrudPersists() = runTest {
        val exerciseId = createExercise("Row")
        val routineId = repository.create("Row Day", null)
        val routineExerciseId = repository.addExercise(routineId, exerciseId, notes = null)

        val templateId = repository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(8, 50.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )
        repository.updateSetTemplate(
            templateId,
            SetTemplateInput(12, 45.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )

        var detail = repository.getById(routineId)
        assertEquals(12, detail.exercises.first().setTemplates.first().targetReps)
        assertEquals(45.0, detail.exercises.first().setTemplates.first().targetWeight!!, 0.001)

        repository.deleteSetTemplate(templateId)
        detail = repository.getById(routineId)
        assertTrue(detail.exercises.first().setTemplates.isEmpty())
    }
}
