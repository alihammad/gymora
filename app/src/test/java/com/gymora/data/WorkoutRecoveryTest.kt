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
import com.gymora.domain.usecase.ResumeWorkoutUseCase
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
 * T043 [US4]: workout recovery after interruption (FR-038, FR-039, SC-003).
 * On launch with an ACTIVE session the recovery state exposes workout name +
 * start time; resume returns the full session graph with all logged sets;
 * discard requires explicit confirmation before deletion (UI-enforced; the
 * repository deletes only when called explicitly).
 */
@RunWith(RobolectricTestRunner::class)
class WorkoutRecoveryTest {

    private lateinit var database: GymoraDatabase
    private lateinit var sessionRepository: WorkoutSessionRepositoryImpl
    private lateinit var routineRepository: RoutineRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl
    private lateinit var resumeWorkoutUseCase: ResumeWorkoutUseCase

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
        resumeWorkoutUseCase = ResumeWorkoutUseCase(sessionRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun startWorkoutWithLoggedSets(): Long {
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
        sessionRepository.updateSetValues(setId, 62.5, WeightUnit.KG, 9)
        sessionRepository.completeSet(setId)
        return sessionId
    }

    @Test
    fun onLaunchWithActiveSessionRecoveryStateExposesNameAndStartTime() = runTest {
        startWorkoutWithLoggedSets()

        // Simulates app relaunch: the repository detects the ACTIVE session.
        val recovery = resumeWorkoutUseCase.detect()

        assertNotNull("expected an unfinished workout to be detected", recovery)
        assertEquals("Chest Workout", recovery!!.session.routineNameSnapshot)
        assertNotNull(recovery.session.startedAt)
    }

    @Test
    fun resumeReturnsFullSessionGraphWithAllLoggedSets() = runTest {
        val sessionId = startWorkoutWithLoggedSets()

        val recovery = resumeWorkoutUseCase.detect()!!
        val resumed = resumeWorkoutUseCase.resume(recovery.session.id)

        assertEquals(sessionId, resumed.session.id)
        assertEquals(1, resumed.exercises.size)
        val set = resumed.exercises.first().sets.first()
        // Every already-entered set is intact (FR-039, SC-003).
        assertEquals(62.5, set.weight!!, 0.001)
        assertEquals(9, set.reps)
        assertTrue(set.isCompleted)
    }

    @Test
    fun noActiveSessionMeansNoRecoveryPrompt() = runTest {
        assertNull(resumeWorkoutUseCase.detect())
    }

    @Test
    fun discardRemovesSessionOnlyWhenExplicitlyInvoked() = runTest {
        val sessionId = startWorkoutWithLoggedSets()

        // Detection alone never deletes (confirmation is a UI concern, FR-037).
        assertNotNull(resumeWorkoutUseCase.detect())

        sessionRepository.discard(sessionId)
        assertNull(sessionRepository.observeActiveSession().first())
    }
}
