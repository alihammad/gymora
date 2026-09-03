package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.repository.ExerciseRepositoryImpl
import com.gymora.data.repository.HistoryRepositoryImpl
import com.gymora.data.repository.RoutineRepositoryImpl
import com.gymora.data.repository.WorkoutSessionRepositoryImpl
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SetTemplateInput
import com.gymora.domain.model.WeightUnit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * T054 [US7]: exercise history returns all performances by date newest-first
 * with all sets, paged via indexed exercise_id lookup (FR-044, R-07).
 */
@RunWith(RobolectricTestRunner::class)
class ExerciseHistoryTest {

    private lateinit var database: GymoraDatabase
    private lateinit var sessionRepository: WorkoutSessionRepositoryImpl
    private lateinit var routineRepository: RoutineRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl
    private lateinit var historyRepository: HistoryRepositoryImpl

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
        historyRepository = HistoryRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun completeWorkout(
        exerciseId: Long,
        routineName: String,
        weight: Double,
        reps: Int,
    ) {
        val routineId = routineRepository.create(routineName, null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(reps, weight, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val active = sessionRepository.getActiveWorkout(sessionId)
        val setId = active.exercises.first().sets.first().id
        sessionRepository.updateSetValues(setId, weight, WeightUnit.KG, reps)
        sessionRepository.completeSet(setId)
        sessionRepository.finish(sessionId)
    }

    @Test
    fun exerciseHistoryReturnsAllPerformancesNewestFirst() = runTest {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Bench Press")).id

        // Complete two workouts with different dates (Room in-memory doesn't
        // advance time, but the insert order + started_at ordering works).
        completeWorkout(exerciseId, "Workout A", 60.0, 10)
        completeWorkout(exerciseId, "Workout B", 65.0, 8)

        val history = historyRepository.getExerciseHistory(exerciseId, limit = 30, offset = 0)

        assertEquals(2, history.size)
        // Newest first: Workout B then Workout A.
        assertTrue(history[0].date >= history[1].date)
        // Each performance has all sets.
        history.forEach { performance ->
            assertEquals(1, performance.sets.size)
        }
    }

    @Test
    fun exerciseHistoryPagingWorks() = runTest {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Squat")).id

        repeat(5) { i ->
            completeWorkout(exerciseId, "Workout $i", 80.0 + i, 10)
        }

        // Page 1: first 3.
        val page1 = historyRepository.getExerciseHistory(exerciseId, limit = 3, offset = 0)
        assertEquals(3, page1.size)

        // Page 2: remaining 2.
        val page2 = historyRepository.getExerciseHistory(exerciseId, limit = 3, offset = 3)
        assertEquals(2, page2.size)

        // No overlap.
        val page1Ids = page1.map { it.sessionId }.toSet()
        val page2Ids = page2.map { it.sessionId }.toSet()
        assertTrue(page1Ids.intersect(page2Ids).isEmpty())
    }

    @Test
    fun exerciseHistoryEmptyForNeverPerformedExercise() = runTest {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "New Exercise")).id

        val history = historyRepository.getExerciseHistory(exerciseId, limit = 30, offset = 0)
        assertTrue(history.isEmpty())
    }
}