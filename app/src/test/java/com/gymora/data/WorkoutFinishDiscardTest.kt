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
 * T034 [US3]: finish & discard (FR-033..FR-037, BR-15).
 * Finish records end timestamp, sets COMPLETED, returns correct summary;
 * discard deletes session permanently; zero-completed-set finish allowed.
 */
@RunWith(RobolectricTestRunner::class)
class WorkoutFinishDiscardTest {

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

    private suspend fun createRoutineWithOneExercise(): Long {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Bench Press")).id
        val routineId = routineRepository.create("Chest Workout", null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(10, 60.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )
        return routineId
    }

    @Test
    fun finishRecordsEndTimestampAndReturnsSummary() = runTest {
        val routineId = createRoutineWithOneExercise()
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val active = sessionRepository.getActiveWorkout(sessionId)
        val setId = active.exercises.first().sets.first().id
        sessionRepository.updateSetValues(setId, 60.0, WeightUnit.KG, 10)
        sessionRepository.completeSet(setId)

        val summary = sessionRepository.finish(sessionId)

        assertEquals("Chest Workout", summary.routineNameSnapshot)
        assertEquals(1, summary.exerciseCount)
        assertEquals(1, summary.completedSetCount)
        assertEquals(10, summary.totalReps)
        assertEquals(600.0, summary.totalVolume, 0.001)
        assertNotNull(summary.duration)

        // Session is COMPLETED and no longer active.
        assertNull(sessionRepository.observeActiveSession().first())
    }

    @Test
    fun finishWithZeroCompletedSetsIsAllowed() = runTest {
        val routineId = createRoutineWithOneExercise()
        val sessionId = sessionRepository.startFromRoutine(routineId)

        val summary = sessionRepository.finish(sessionId)

        assertEquals(0, summary.completedSetCount)
        assertEquals(0.0, summary.totalVolume, 0.001)
    }

    @Test
    fun discardDeletesSessionPermanently() = runTest {
        val routineId = createRoutineWithOneExercise()
        val sessionId = sessionRepository.startFromRoutine(routineId)

        sessionRepository.discard(sessionId)

        assertNull(sessionRepository.observeActiveSession().first())
        // Session rows are gone (BR-15: removed permanently, never in history).
        val db = database.openHelper.readableDatabase
        val cursor = db.query("SELECT COUNT(*) FROM workout_sessions WHERE id = $sessionId")
        cursor.moveToFirst()
        assertEquals(0, cursor.getInt(0))
        cursor.close()
    }

    @Test
    fun finishedWorkoutAppearsInCompletedSessionsOnly() = runTest {
        val routineId = createRoutineWithOneExercise()
        val sessionId = sessionRepository.startFromRoutine(routineId)
        sessionRepository.finish(sessionId)

        val db = database.openHelper.readableDatabase
        val cursor = db.query(
            "SELECT status FROM workout_sessions WHERE id = $sessionId",
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("COMPLETED", cursor.getString(0))
        cursor.close()
    }
}
