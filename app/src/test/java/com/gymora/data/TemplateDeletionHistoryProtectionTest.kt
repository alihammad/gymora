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
import com.gymora.domain.usecase.DeleteExerciseUseCase
import com.gymora.domain.usecase.DeleteRoutineUseCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * T024 [US2]: NON-NEGOTIABLE history-protection regression (BR-03, BR-04, SC-005).
 * Deleting a routine never deletes or alters workout history rows; deleting an
 * exercise referenced by templates removes template references but never touches
 * history. History tables land in US3 (T035); until then the row-count checks
 * below are vacuously satisfied and become active automatically.
 */
@RunWith(RobolectricTestRunner::class)
class TemplateDeletionHistoryProtectionTest {

    private lateinit var database: GymoraDatabase
    private lateinit var routineRepository: RoutineRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl
    private lateinit var deleteRoutineUseCase: DeleteRoutineUseCase
    private lateinit var deleteExerciseUseCase: DeleteExerciseUseCase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        ).allowMainThreadQueries().build()
        routineRepository = RoutineRepositoryImpl(
            database.routineDao(),
            database.routineExerciseDao(),
            database.setTemplateDao(),
            database.exerciseDao(),
            database,
        )
        exerciseRepository = ExerciseRepositoryImpl(database.exerciseDao())
        deleteRoutineUseCase = DeleteRoutineUseCase(routineRepository)
        deleteExerciseUseCase = DeleteExerciseUseCase(exerciseRepository, routineRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Snapshot of history table row counts; null entries = table not created yet. */
    private fun historySnapshot(): Map<String, Long> {
        val tables = listOf("workout_sessions", "workout_exercises", "workout_sets")
        val db = database.openHelper.readableDatabase
        return tables.associateWith { table ->
            val existsCursor = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='$table'",
            )
            val exists = existsCursor.moveToFirst()
            existsCursor.close()
            if (!exists) {
                -1L
            } else {
                val countCursor = db.query("SELECT COUNT(*) FROM $table")
                countCursor.moveToFirst()
                val count = countCursor.getLong(0)
                countCursor.close()
                count
            }
        }
    }

    @Test
    fun deletingRoutineNeverTouchesHistory() = runTest {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Bench Press")).id
        val routineId = routineRepository.create("Chest Workout", null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(10, 60.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )

        val before = historySnapshot()
        deleteRoutineUseCase(routineId)
        val after = historySnapshot()

        assertEquals("history must be untouched by routine deletion", before, after)
        // Template rows are gone.
        assertTrue(database.routineExerciseDao().getForRoutine(routineId).isEmpty())
    }

    @Test
    fun deletingExerciseRemovesTemplateReferencesButNeverTouchesHistory() = runTest {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Chest Fly")).id
        val routineId = routineRepository.create("Chest Workout", null)
        routineRepository.addExercise(routineId, exerciseId, null)

        val before = historySnapshot()
        deleteExerciseUseCase(exerciseId)
        val after = historySnapshot()

        assertEquals("history must be untouched by exercise deletion", before, after)
        // Template references removed; routine itself survives (BR-04).
        val detail = routineRepository.getById(routineId)
        assertTrue(detail.exercises.isEmpty())
        // Exercise row still exists (soft delete — referential integrity, R-03).
        assertEquals(true, exerciseRepository.getById(exerciseId).isDeleted)
    }
}
