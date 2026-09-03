package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.repository.ExerciseRepositoryImpl
import com.gymora.data.repository.RoutineRepositoryImpl
import com.gymora.data.repository.WorkoutSessionRepositoryImpl
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SetTemplateInput
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.usecase.PreviousPerformanceUseCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * T051 [US6]: previous-performance lookup (FR-045, FR-046, SC-002).
 * Returns the most recent completed sets per exercise; null for never-performed
 * exercises; pre-filled values are overridable on save.
 */
@RunWith(RobolectricTestRunner::class)
class PreviousPerformanceTest {

    private lateinit var database: GymoraDatabase
    private lateinit var sessionRepository: WorkoutSessionRepositoryImpl
    private lateinit var routineRepository: RoutineRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl
    private lateinit var previousPerformanceUseCase: PreviousPerformanceUseCase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        ).allowMainThreadQueries().build()
        sessionRepository = WorkoutSessionRepositoryImpl(database)
        routineRepository = RoutineRepositoryImpl(
            database.routineDao(),
            database.routineExerciseDao(),
            database.setTemplateDao(),
            database.exerciseDao(),
            database,
        )
        exerciseRepository = ExerciseRepositoryImpl(database.exerciseDao())
        previousPerformanceUseCase = PreviousPerformanceUseCase(sessionRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun completeWorkoutWithLoggedSets(weight: Double, reps: Int): Long {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Bench Press")).id
        val routineId = routineRepository.create("Chest Workout", null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(10, 60.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val active = sessionRepository.getActiveWorkout(sessionId)
        val setId = active.exercises.first().sets.first().id
        sessionRepository.updateSetValues(setId, weight, WeightUnit.KG, reps)
        sessionRepository.completeSet(setId)
        sessionRepository.finish(sessionId)
        return exerciseId
    }

    @Test
    fun previousPerformanceReturnsMostRecentCompletedSets() = runTest {
        val exerciseId = completeWorkoutWithLoggedSets(weight = 60.0, reps = 10)

        val performance = previousPerformanceUseCase(exerciseId)

        assertEquals(1, performance!!.sets.size)
        assertEquals(60.0, performance.sets.first().weight!!, 0.001)
        assertEquals(10, performance.sets.first().reps)
    }

    @Test
    fun neverPerformedExerciseReturnsNull() = runTest {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "New Exercise")).id

        assertNull(previousPerformanceUseCase(exerciseId))
    }

    @Test
    fun preFilledValuesAreOverridableOnSave() = runTest {
        val exerciseId = completeWorkoutWithLoggedSets(weight = 60.0, reps = 10)

        // Start a second workout of the same exercise and override the pre-fill.
        val routineId = routineRepository.create("Chest Workout 2", null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(10, 60.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val active = sessionRepository.getActiveWorkout(sessionId)
        val setId = active.exercises.first().sets.first().id

        // The user modifies the pre-filled values; the modified values are saved.
        sessionRepository.updateSetValues(setId, 65.0, WeightUnit.KG, 8)
        sessionRepository.completeSet(setId)

        val reloaded = sessionRepository.getActiveWorkout(sessionId)
        val set = reloaded.exercises.first().sets.first()
        assertEquals(65.0, set.weight!!, 0.001)
        assertEquals(8, set.reps)
    }
}
