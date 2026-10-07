package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.repository.ExerciseRepositoryImpl
import com.gymora.data.repository.HistoryRepositoryImpl
import com.gymora.data.repository.RecordsRepositoryImpl
import com.gymora.data.repository.RoutineRepositoryImpl
import com.gymora.data.repository.WorkoutSessionRepositoryImpl
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SetTemplateInput
import com.gymora.domain.model.Side
import com.gymora.domain.model.UpdateExerciseInput
import com.gymora.domain.model.WeightUnit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Exercise tracking types, unilateral sides, and form guide fields end to end. */
@RunWith(RobolectricTestRunner::class)
class ExerciseTrackingTest {

    private lateinit var database: GymoraDatabase
    private lateinit var sessions: WorkoutSessionRepositoryImpl
    private lateinit var routines: RoutineRepositoryImpl
    private lateinit var exercises: ExerciseRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        ).allowMainThreadQueries().build()
        sessions = WorkoutSessionRepositoryImpl(database)
        routines = RoutineRepositoryImpl(
            database.routineDao(),
            database.routineExerciseDao(),
            database.setTemplateDao(),
            database.exerciseDao(),
            database,
        )
        exercises = ExerciseRepositoryImpl(database.exerciseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun startWith(input: CreateExerciseInput, template: SetTemplateInput): Long {
        val exerciseId = exercises.createCustom(input).id
        val routineId = routines.create("Day", null)
        val routineExerciseId = routines.addExercise(routineId, exerciseId, null)
        routines.addSetTemplate(routineExerciseId, template)
        return sessions.startFromRoutine(routineId)
    }

    @Test
    fun exerciseKeepsTrackingCuesAndMedia() = runTest {
        val id = exercises.createCustom(
            CreateExerciseInput(
                name = "Side Plank",
                measurementType = MeasurementType.DURATION,
                isUnilateral = true,
                formCues = listOf(" Hips high ", "", "Brace"),
                mediaFile = "plank.gif",
            ),
        ).id

        val saved = exercises.getById(id)
        assertEquals(MeasurementType.DURATION, saved.measurementType)
        assertEquals(true, saved.isUnilateral)
        assertEquals(listOf("Hips high", "Brace"), saved.formCues)
        assertEquals("plank.gif", saved.mediaFile)

        exercises.update(id, UpdateExerciseInput(name = "Side Plank", measurementType = MeasurementType.REPS_ONLY))
        val updated = exercises.getById(id)
        assertEquals(MeasurementType.REPS_ONLY, updated.measurementType)
        assertEquals(emptyList<String>(), updated.formCues)
        assertNull(updated.mediaFile)
    }

    @Test
    fun timedSetsStartWithTargetAndSaveDuration() = runTest {
        val sessionId = startWith(
            CreateExerciseInput(name = "Plank", measurementType = MeasurementType.DURATION),
            SetTemplateInput(0, null, null, MeasurementType.DURATION, targetDurationSeconds = 60),
        )
        val set = sessions.getActiveWorkout(sessionId).exercises.single().sets.single()
        assertEquals(MeasurementType.DURATION, set.measurementType)
        assertEquals(60, set.durationSeconds)

        sessions.updateSetValues(set.id, null, null, null, durationSeconds = 75, distanceMeters = null)
        assertEquals(75, sessions.getActiveWorkout(sessionId).exercises.single().sets.single().durationSeconds)
    }

    @Test
    fun unilateralExerciseGetsALeftAndRightSetPerTarget() = runTest {
        val sessionId = startWith(
            CreateExerciseInput(name = "One-Arm Row", isUnilateral = true),
            SetTemplateInput(10, 30.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )
        val exercise = sessions.getActiveWorkout(sessionId).exercises.single()
        assertEquals(listOf(Side.LEFT, Side.RIGHT), exercise.sets.map { it.side })

        sessions.addSet(exercise.workoutExerciseId)
        val sets = sessions.getActiveWorkout(sessionId).exercises.single().sets
        assertEquals(listOf(Side.LEFT, Side.RIGHT, Side.LEFT, Side.RIGHT), sets.map { it.side })
        assertEquals(listOf(1, 2, 3, 4), sets.map { it.setNumber })
    }

    @Test
    fun startUsesTheLibraryTypeEvenIfTheTemplateIsOlder() = runTest {
        val sessionId = startWith(
            CreateExerciseInput(name = "Row Erg", measurementType = MeasurementType.DISTANCE_AND_DURATION),
            SetTemplateInput(10, null, null, MeasurementType.WEIGHT_AND_REPS, targetDistanceMeters = 2000.0),
        )
        val set = sessions.getActiveWorkout(sessionId).exercises.single().sets.single()
        assertEquals(MeasurementType.DISTANCE_AND_DURATION, set.measurementType)
        assertEquals(2000.0, set.distanceMeters!!, 0.0)
    }

    @Test
    fun assistanceIsNotCountedAsLoad() = runTest {
        val sessionId = startWith(
            CreateExerciseInput(name = "Assisted Pull-Up", measurementType = MeasurementType.ASSISTED_BODYWEIGHT),
            SetTemplateInput(8, 40.0, WeightUnit.KG, MeasurementType.ASSISTED_BODYWEIGHT),
        )
        val set = sessions.getActiveWorkout(sessionId).exercises.single().sets.single()
        sessions.completeSet(set.id)

        val summary = sessions.finish(sessionId)
        assertEquals(0.0, summary.totalVolume, 0.0)
        assertEquals(8, summary.totalReps)

        val records = RecordsRepositoryImpl(database).getPersonalRecords()
        assertNull(records.heaviestWeight)
        assertNull(records.bestEstimatedOneRepMax)
        assertEquals(8.0, records.highestReps!!.value, 0.0)

        val detail = HistoryRepositoryImpl(database).getWorkoutDetail(sessionId)
        assertEquals(MeasurementType.ASSISTED_BODYWEIGHT, detail.exercises.single().sets.single().measurementType)
    }
}
