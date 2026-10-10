package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.repository.ExerciseRepositoryImpl
import com.gymora.data.repository.RoutineRepositoryImpl
import com.gymora.data.repository.WorkoutSessionRepositoryImpl
import com.gymora.domain.model.ActiveWorkoutConflictException
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SetTemplateInput
import com.gymora.domain.model.ValidationException
import com.gymora.domain.model.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * T033 [US3]: workout execution (FR-019, FR-020, FR-023..FR-029).
 * Start copies routine into session rows; at-most-one-active enforced; set
 * logging persists immediately; add/delete sets; add exercise (both modes);
 * remove exercise leaves routine untouched.
 */
@RunWith(RobolectricTestRunner::class)
class WorkoutExecutionTest {

    private lateinit var database: GymoraDatabase
    private lateinit var sessionRepository: WorkoutSessionRepositoryImpl
    private lateinit var routineRepository: RoutineRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createRoutineWithOneExercise(): Pair<Long, Long> {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Bench Press")).id
        val routineId = routineRepository.create("Chest Workout", null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(10, 60.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )
        return routineId to exerciseId
    }

    @Test
    fun startWorkoutCopiesRoutineIntoActiveSessionWithSnapshot() = runTest {
        val (routineId, _) = createRoutineWithOneExercise()

        val sessionId = sessionRepository.startFromRoutine(routineId)

        val active = sessionRepository.observeActiveSession().first()
        assertNotNull(active)
        assertEquals(sessionId, active!!.session.id)
        assertEquals("Chest Workout", active.session.routineNameSnapshot)
        assertEquals(1, active.exercises.size)
        assertEquals("Bench Press", active.exercises.first().exerciseName)
        assertEquals(1, active.exercises.first().sets.size)
        // Planned values copied into the session set.
        assertEquals(10, active.exercises.first().sets.first().reps)
    }

    @Test
    fun adHocStartCreatesRoutinelessSessionWithOneExerciseAndSet() = runTest {
        val exerciseId = exerciseRepository.createCustom(CreateExerciseInput(name = "Squat")).id

        val sessionId = sessionRepository.startAdHoc(exerciseId)

        val active = sessionRepository.getActiveWorkout(sessionId)
        assertNull(active.session.routineId)
        assertEquals(1, active.exercises.size)
        assertEquals("Squat", active.exercises.first().exerciseName)
        assertEquals(1, active.exercises.first().sets.size)
    }

    @Test
    fun adHocStartThrowsConflictWhenWorkoutActive() = runTest {
        val exerciseId = exerciseRepository.createCustom(CreateExerciseInput(name = "Squat")).id
        sessionRepository.startAdHoc(exerciseId)

        val exception = try {
            sessionRepository.startAdHoc(exerciseId)
            null
        } catch (e: ActiveWorkoutConflictException) {
            e
        }
        assertNotNull(exception)
    }

    @Test
    fun startingSecondWorkoutThrowsConflict() = runTest {
        val (routineId, _) = createRoutineWithOneExercise()
        sessionRepository.startFromRoutine(routineId)

        val exception = try {
            sessionRepository.startFromRoutine(routineId)
            null
        } catch (e: ActiveWorkoutConflictException) {
            e
        }
        assertNotNull("expected ActiveWorkoutConflictException", exception)
    }

    @Test
    fun setLoggingPersistsImmediately() = runTest {
        val (routineId, _) = createRoutineWithOneExercise()
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val active = sessionRepository.getActiveWorkout(sessionId)
        val setId = active.exercises.first().sets.first().id

        sessionRepository.updateSetValues(setId, 22.5, WeightUnit.KG, 8)
        sessionRepository.completeSet(setId)

        // Re-read from the database: values are durable (FR-025, FR-027).
        val reloaded = sessionRepository.getActiveWorkout(sessionId)
        val set = reloaded.exercises.first().sets.first()
        assertEquals(22.5, set.weight!!, 0.001)
        assertEquals(8, set.reps)
        assertTrue(set.isCompleted)
        assertNotNull(set.completedAt)
    }

    @Test
    fun negativeSetValuesAreRejected() = runTest {
        val (routineId, _) = createRoutineWithOneExercise()
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val active = sessionRepository.getActiveWorkout(sessionId)
        val setId = active.exercises.first().sets.first().id

        val exception = try {
            sessionRepository.updateSetValues(setId, -5.0, WeightUnit.KG, 8)
            null
        } catch (e: ValidationException) {
            e
        }
        assertNotNull("expected ValidationException for negative weight", exception)
    }

    @Test
    fun addAndDeleteSetsDuringWorkout() = runTest {
        val (routineId, _) = createRoutineWithOneExercise()
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val active = sessionRepository.getActiveWorkout(sessionId)
        val workoutExerciseId = active.exercises.first().workoutExerciseId

        val newSetId = sessionRepository.addSet(workoutExerciseId)
        var reloaded = sessionRepository.getActiveWorkout(sessionId)
        assertEquals(2, reloaded.exercises.first().sets.size)

        sessionRepository.deleteSet(newSetId)
        reloaded = sessionRepository.getActiveWorkout(sessionId)
        assertEquals(1, reloaded.exercises.first().sets.size)
    }

    @Test
    fun addExerciseThisWorkoutOnlyDoesNotTouchRoutine() = runTest {
        val (routineId, _) = createRoutineWithOneExercise()
        val extraExerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Push-Up")).id
        val sessionId = sessionRepository.startFromRoutine(routineId)

        sessionRepository.addExerciseToSession(sessionId, extraExerciseId, addToRoutine = false)

        val active = sessionRepository.getActiveWorkout(sessionId)
        assertEquals(2, active.exercises.size)
        // Routine template unchanged (FR-028 "This workout only").
        val routine = routineRepository.getById(routineId)
        assertEquals(1, routine.exercises.size)
    }

    @Test
    fun addExerciseToRoutineAlsoAppendsToTemplate() = runTest {
        val (routineId, _) = createRoutineWithOneExercise()
        val extraExerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Dips")).id
        val sessionId = sessionRepository.startFromRoutine(routineId)

        sessionRepository.addExerciseToSession(sessionId, extraExerciseId, addToRoutine = true)

        val routine = routineRepository.getById(routineId)
        assertEquals(2, routine.exercises.size)
        assertTrue(routine.exercises.any { it.exerciseName == "Dips" })
    }

    @Test
    fun removeExerciseFromSessionLeavesRoutineUntouched() = runTest {
        val (routineId, _) = createRoutineWithOneExercise()
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val active = sessionRepository.getActiveWorkout(sessionId)
        val workoutExerciseId = active.exercises.first().workoutExerciseId

        sessionRepository.removeExerciseFromSession(workoutExerciseId)

        val reloaded = sessionRepository.getActiveWorkout(sessionId)
        assertTrue(reloaded.exercises.isEmpty())
        // Routine template unchanged (FR-029).
        val routine = routineRepository.getById(routineId)
        assertEquals(1, routine.exercises.size)
    }

    @Test
    fun reorderSessionExercisesPersistsOrderAndLeavesRoutineUntouched() = runTest {
        val (routineId, _) = createRoutineWithOneExercise()
        val extraExerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Dips")).id
        val sessionId = sessionRepository.startFromRoutine(routineId)
        sessionRepository.addExerciseToSession(sessionId, extraExerciseId, addToRoutine = false)
        val before = sessionRepository.getActiveWorkout(sessionId).exercises.map { it.workoutExerciseId }
        assertEquals(2, before.size)

        sessionRepository.reorderSessionExercises(sessionId, before.reversed())

        val after = sessionRepository.getActiveWorkout(sessionId).exercises.map { it.workoutExerciseId }
        assertEquals(before.reversed(), after)
        assertEquals(1, routineRepository.getById(routineId).exercises.size)
    }

    @Test
    fun observeActiveSessionReturnsNullWhenNoneActive() = runTest {
        val active = sessionRepository.observeActiveSession().first()
        assertNull(active)
    }
}
