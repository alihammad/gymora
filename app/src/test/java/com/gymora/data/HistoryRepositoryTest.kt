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
 * T047 [US5]: history listing (FR-040, FR-058, R-12).
 * History lists only COMPLETED sessions newest-first with LIMIT/OFFSET paging;
 * empty history returns an empty list.
 */
@RunWith(RobolectricTestRunner::class)
class HistoryRepositoryTest {

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

    private suspend fun completeWorkout(name: String, startedAtOffsetMs: Long): Long {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Exercise $name")).id
        val routineId = routineRepository.create(name, null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(10, 50.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )
        val sessionId = sessionRepository.startFromRoutine(routineId)
        // Shift started_at so ordering is deterministic.
        val db = database.openHelper.writableDatabase
        db.execSQL(
            "UPDATE workout_sessions SET started_at = started_at + $startedAtOffsetMs " +
                "WHERE id = $sessionId",
        )
        sessionRepository.finish(sessionId)
        return sessionId
    }

    @Test
    fun emptyHistoryReturnsEmptyList() = runTest {
        assertTrue(historyRepository.listCompleted(limit = 30, offset = 0).isEmpty())
    }

    @Test
    fun listsOnlyCompletedSessionsNewestFirst() = runTest {
        completeWorkout("First", 0)
        completeWorkout("Second", 60_000)
        completeWorkout("Third", 120_000)
        // An ACTIVE session must never appear in history (R-12, OQ-3).
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Active Ex")).id
        val routineId = routineRepository.create("Active Routine", null)
        routineRepository.addExercise(routineId, exerciseId, null)
        sessionRepository.startFromRoutine(routineId)

        val entries = historyRepository.listCompleted(limit = 30, offset = 0)

        assertEquals(3, entries.size)
        assertEquals(
            listOf("Third", "Second", "First"),
            entries.map { it.routineNameSnapshot },
        )
    }

    @Test
    fun pagingWithLimitOffsetWorks() = runTest {
        repeat(5) { i -> completeWorkout("W$i", i * 60_000L) }

        val page1 = historyRepository.listCompleted(limit = 2, offset = 0)
        val page2 = historyRepository.listCompleted(limit = 2, offset = 2)
        val page3 = historyRepository.listCompleted(limit = 2, offset = 4)

        assertEquals(2, page1.size)
        assertEquals(2, page2.size)
        assertEquals(1, page3.size)
        // Newest first across pages: W4, W3 | W2, W1 | W0
        assertEquals(listOf("W4", "W3"), page1.map { it.routineNameSnapshot })
        assertEquals(listOf("W2", "W1"), page2.map { it.routineNameSnapshot })
        assertEquals(listOf("W0"), page3.map { it.routineNameSnapshot })
    }
}
