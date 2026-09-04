package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.repository.ExerciseRepositoryImpl
import com.gymora.data.repository.RecordsRepositoryImpl
import com.gymora.data.repository.RoutineRepositoryImpl
import com.gymora.data.repository.WorkoutSessionRepositoryImpl
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SetTemplateInput
import com.gymora.domain.model.WeightUnit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * T065 [US10]: records computed from completed sessions update when surpassed
 * and carry exercise/date context (FR-047).
 */
@RunWith(RobolectricTestRunner::class)
class RecordsRepositoryTest {

    private lateinit var database: GymoraDatabase
    private lateinit var recordsRepository: RecordsRepositoryImpl
    private lateinit var sessionRepository: WorkoutSessionRepositoryImpl
    private lateinit var routineRepository: RoutineRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        ).allowMainThreadQueries().build()
        recordsRepository = RecordsRepositoryImpl(database)
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

    @Test
    fun emptyDatabaseReturnsNullRecords() = runTest {
        val records = recordsRepository.getPersonalRecords()
        assertNull(records.heaviestWeight)
        assertNull(records.highestReps)
        assertNull(records.bestEstimatedOneRepMax)
        assertNull(records.largestWorkoutVolume)
    }

    @Test
    fun recordsComputedFromCompletedSessions() = runTest {
        // Create exercise and routine
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Bench Press")).id
        val routineId = routineRepository.create("Chest Day", null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(10, 80.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )

        // Start and complete a workout
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val workout = sessionRepository.getActiveWorkout(sessionId)
        val set = workout.exercises.first().sets.first()
        sessionRepository.updateSetValues(set.id, 80.0, WeightUnit.KG, 10)
        sessionRepository.completeSet(set.id)
        sessionRepository.finish(sessionId)

        val records = recordsRepository.getPersonalRecords()
        assertNotNull(records.heaviestWeight)
        assertEquals(80.0, records.heaviestWeight!!.value, 0.001)
        assertEquals("Bench Press", records.heaviestWeight!!.exerciseName)
    }

    @Test
    fun recordsUpdateWhenSurpassed() = runTest {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Squat")).id
        val routineId = routineRepository.create("Leg Day", null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(5, 100.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )

        // First workout: 100 kg
        val session1 = sessionRepository.startFromRoutine(routineId)
        val set1 = sessionRepository.getActiveWorkout(session1).exercises.first().sets.first()
        sessionRepository.updateSetValues(set1.id, 100.0, WeightUnit.KG, 5)
        sessionRepository.completeSet(set1.id)
        sessionRepository.finish(session1)

        var records = recordsRepository.getPersonalRecords()
        assertEquals(100.0, records.heaviestWeight!!.value, 0.001)

        // Second workout: 120 kg (surpasses)
        val session2 = sessionRepository.startFromRoutine(routineId)
        val set2 = sessionRepository.getActiveWorkout(session2).exercises.first().sets.first()
        sessionRepository.updateSetValues(set2.id, 120.0, WeightUnit.KG, 5)
        sessionRepository.completeSet(set2.id)
        sessionRepository.finish(session2)

        records = recordsRepository.getPersonalRecords()
        assertEquals(120.0, records.heaviestWeight!!.value, 0.001)
    }

    @Test
    fun recordsCarryDateContext() = runTest {
        val exerciseId = exerciseRepository
            .createCustom(CreateExerciseInput(name = "Deadlift")).id
        val routineId = routineRepository.create("Pull Day", null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(3, 150.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )

        val sessionId = sessionRepository.startFromRoutine(routineId)
        val set = sessionRepository.getActiveWorkout(sessionId).exercises.first().sets.first()
        sessionRepository.updateSetValues(set.id, 150.0, WeightUnit.KG, 3)
        sessionRepository.completeSet(set.id)
        sessionRepository.finish(sessionId)

        val records = recordsRepository.getPersonalRecords()
        assertNotNull(records.heaviestWeight)
        assertNotNull(records.heaviestWeight!!.date)
    }
}