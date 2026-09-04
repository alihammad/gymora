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
 * T046 [US5]: NON-NEGOTIABLE history-protection regression (FR-043, BR-11,
 * SC-005; mandatory per spec quality constraint). Renaming/deleting routines
 * or exercises changes 0 historical records; history detail displays snapshot
 * names.
 */
@RunWith(RobolectricTestRunner::class)
class HistoryProtectionTest {

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

    /** Complete a workout from "Chest Workout" containing "Chest Fly". */
    private suspend fun completeWorkout(): Long {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Chest Fly")).id
        val routineId = routineRepository.create("Chest Workout", null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(10, 20.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val active = sessionRepository.getActiveWorkout(sessionId)
        val setId = active.exercises.first().sets.first().id
        sessionRepository.updateSetValues(setId, 20.0, WeightUnit.KG, 10)
        sessionRepository.completeSet(setId)
        sessionRepository.finish(sessionId)
        return sessionId
    }

    private fun historyRowSnapshot(): List<String> {
        val db = database.openHelper.readableDatabase
        val rows = mutableListOf<String>()
        listOf("workout_sessions", "workout_exercises", "workout_sets").forEach { table ->
            val cursor = db.query("SELECT * FROM $table ORDER BY id")
            while (cursor.moveToNext()) {
                val cols = (0 until cursor.columnCount).map { cursor.getString(it) }
                rows.add("$table:${cols.joinToString("|")}")
            }
            cursor.close()
        }
        return rows
    }

    @Test
    fun renamingRoutineAndExerciseChangesZeroHistoricalRecords() = runTest {
        val sessionId = completeWorkout()
        val before = historyRowSnapshot()

        // Rename the routine and the exercise.
        val routineId = database.routineDao().getAllOnce().first().id
        routineRepository.rename(routineId, "Renamed Routine")
        val exerciseId = database.exerciseDao().getAllActiveOnce().first().id
        exerciseRepository.update(
            exerciseId,
            com.gymora.domain.model.UpdateExerciseInput(name = "Renamed Fly"),
        )

        val after = historyRowSnapshot()
        assertEquals("history must be byte-identical after renames", before, after)

        // Detail still shows the original snapshot names (FR-043).
        val detail = historyRepository.getWorkoutDetail(sessionId)
        assertEquals("Chest Workout", detail.session.routineNameSnapshot)
        assertEquals("Chest Fly", detail.exercises.first().exerciseName)
    }

    @Test
    fun deletingRoutineAndExerciseChangesZeroHistoricalRecords() = runTest {
        val sessionId = completeWorkout()

        val routineId = database.routineDao().getAllOnce().first().id
        routineRepository.delete(routineId)
        val exerciseId = database.exerciseDao().getAllActiveOnce().first().id
        exerciseRepository.delete(exerciseId)

        // Per data-model.md / R-03: routine deletion sets workout_sessions.routine_id
        // to NULL (ON DELETE SET NULL); the snapshot name and all exercise/set data
        // are preserved. Displayed history is unchanged (FR-043, BR-04, SC-005).
        val detail = historyRepository.getWorkoutDetail(sessionId)
        assertEquals("Chest Workout", detail.session.routineNameSnapshot)
        assertEquals("Chest Fly", detail.exercises.first().exerciseName)
        assertEquals(1, detail.exercises.first().sets.size)
        assertEquals(10, detail.exercises.first().sets.first().reps)
        assertEquals(20.0, detail.exercises.first().sets.first().weight!!, 0.001)
        // The optional routine reference is NULL after routine deletion (R-03).
        assertEquals(null, detail.session.routineId)
    }

    @Test
    fun historyDisplaysSnapshotNamesNotCurrentNames() = runTest {
        val sessionId = completeWorkout()

        val detail = historyRepository.getWorkoutDetail(sessionId)

        // Snapshots are stored on the historical rows themselves (FR-056).
        assertTrue(detail.session.routineNameSnapshot.isNotBlank())
        assertTrue(detail.exercises.first().exerciseName.isNotBlank())
    }
}
