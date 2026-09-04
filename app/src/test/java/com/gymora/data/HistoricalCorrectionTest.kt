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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * T068 [US11]: historical corrections write only to the target session's rows;
 * routines/templates/other workouts unchanged; history stays read-only unless
 * edit explicitly chosen (FR-042, BR-11).
 */
@RunWith(RobolectricTestRunner::class)
class HistoricalCorrectionTest {

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

    /** Create an exercise + routine + complete a workout, returning the session id. */
    private suspend fun completeWorkout(exerciseName: String): Long {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = exerciseName)).id
        val routineId = routineRepository.create("Routine $exerciseName", null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(10, 60.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val active = sessionRepository.getActiveWorkout(sessionId)
        val setId = active.exercises.first().sets.first().id
        sessionRepository.updateSetValues(setId, 60.0, WeightUnit.KG, 10)
        sessionRepository.completeSet(setId)
        sessionRepository.finish(sessionId)
        return sessionId
    }

    @Test
    fun correctSetWritesOnlyTargetSession() = runTest {
        val sessionA = completeWorkout("Bench Press")
        val sessionB = completeWorkout("Squat")

        // Snapshot routine/template rows before correction.
        val routineRowsBefore = rawRows("routines")
        val templateRowsBefore = rawRows("set_templates")

        val detailA = historyRepository.getWorkoutDetail(sessionA)
        val setId = detailA.exercises.first().sets.first().id

        historyRepository.correctSet(setId, 80.0, WeightUnit.KG, 5, true, "corrected")

        // Corrected set reflects new values.
        val corrected = historyRepository.getWorkoutDetail(sessionA)
        assertEquals(80.0, corrected.exercises.first().sets.first().weight!!, 0.001)
        assertEquals(5, corrected.exercises.first().sets.first().reps)

        // Session B is untouched.
        val untouchedB = historyRepository.getWorkoutDetail(sessionB)
        assertEquals(60.0, untouchedB.exercises.first().sets.first().weight!!, 0.001)
        assertEquals(10, untouchedB.exercises.first().sets.first().reps)

        // Routines and templates are unchanged (BR-11).
        assertEquals(routineRowsBefore, rawRows("routines"))
        assertEquals(templateRowsBefore, rawRows("set_templates"))
    }

    @Test
    fun addAndRemoveExerciseWithinWorkoutOnlyAffectsThatWorkout() = runTest {
        val sessionA = completeWorkout("Bench Press")
        val sessionB = completeWorkout("Squat")

        val newExercise = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Cable Fly"))

        historyRepository.addExerciseToHistoricalWorkout(sessionA, newExercise.id)

        val afterAdd = historyRepository.getWorkoutDetail(sessionA)
        assertEquals(2, afterAdd.exercises.size)
        val added = afterAdd.exercises.first { it.exerciseId == newExercise.id }
        assertEquals("Cable Fly", added.exerciseName)

        // Session B unchanged.
        assertEquals(1, historyRepository.getWorkoutDetail(sessionB).exercises.size)

        // Remove the added exercise from session A only.
        historyRepository.removeExerciseFromHistoricalWorkout(added.workoutExerciseId)
        val afterRemove = historyRepository.getWorkoutDetail(sessionA)
        assertEquals(1, afterRemove.exercises.size)
        assertEquals("Bench Press", afterRemove.exercises.first().exerciseName)

        // Underlying routines still have only their original exercise (BR-11).
        val routineId = database.routineDao().getAllOnce().first().id
        val routineExercises = database.routineExerciseDao().getForRoutine(routineId)
        assertTrue(routineExercises.none { it.exerciseId == newExercise.id })
    }

    @Test
    fun updateHistoricalWorkoutNotesOnlyTargetSession() = runTest {
        val sessionA = completeWorkout("Bench Press")
        val sessionB = completeWorkout("Squat")

        historyRepository.updateHistoricalWorkoutNotes(sessionA, "Great session")

        assertEquals(
            "Great session",
            historyRepository.getWorkoutDetail(sessionA).session.notes,
        )
        assertNull(historyRepository.getWorkoutDetail(sessionB).session.notes)
    }

    @Test
    fun historyRemainsReadOnlyWithoutCorrection() = runTest {
        val sessionA = completeWorkout("Bench Press")

        // Read path returns snapshot data unchanged when no correction is made.
        val before = historyRepository.getWorkoutDetail(sessionA)
        val after = historyRepository.getWorkoutDetail(sessionA)
        assertEquals(before.session.routineNameSnapshot, after.session.routineNameSnapshot)
        assertEquals(
            before.exercises.first().sets.first().weight,
            after.exercises.first().sets.first().weight,
        )
    }

    private fun rawRows(table: String): List<String> {
        val db = database.openHelper.readableDatabase
        val rows = mutableListOf<String>()
        val cursor = db.query("SELECT * FROM $table ORDER BY id")
        while (cursor.moveToNext()) {
            val cols = (0 until cursor.columnCount).map { cursor.getString(it) }
            rows.add("$table:${cols.joinToString("|")}")
        }
        cursor.close()
        return rows
    }
}
