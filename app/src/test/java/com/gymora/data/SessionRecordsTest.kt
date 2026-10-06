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
import com.gymora.domain.model.SessionRecord
import com.gymora.domain.model.SessionRecordKind
import com.gymora.domain.model.SetTemplateInput
import com.gymora.domain.model.WeightUnit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Personal records set within one workout, for the shareable summary card. */
@RunWith(RobolectricTestRunner::class)
class SessionRecordsTest {

    private lateinit var database: GymoraDatabase
    private lateinit var recordsRepository: RecordsRepositoryImpl
    private lateinit var sessionRepository: WorkoutSessionRepositoryImpl
    private lateinit var routineRepository: RoutineRepositoryImpl
    private var routineId = 0L
    private var nextStartedAt = 1_000_000L

    @Before
    fun setUp() = runTest {
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
        val exerciseId = ExerciseRepositoryImpl(database.exerciseDao())
            .createCustom(CreateExerciseInput(name = "Bench Press")).id
        routineId = routineRepository.create("Chest Day", null)
        val routineExerciseId = routineRepository.addExercise(routineId, exerciseId, null)
        routineRepository.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(10, 80.0, WeightUnit.KG, MeasurementType.WEIGHT_AND_REPS),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Completes a one-set workout; sessions get strictly increasing start times. */
    private suspend fun workout(weight: Double, reps: Int): Long {
        val sessionId = sessionRepository.startFromRoutine(routineId)
        val set = sessionRepository.getActiveWorkout(sessionId).exercises.first().sets.first()
        sessionRepository.updateSetValues(set.id, weight, WeightUnit.KG, reps)
        sessionRepository.completeSet(set.id)
        sessionRepository.finish(sessionId)
        val dao = database.workoutSessionDao()
        dao.update(dao.getById(sessionId)!!.copy(startedAt = nextStartedAt))
        nextStartedAt += 1_000
        return sessionId
    }

    @Test
    fun firstTimeDoingAnExerciseIsNotARecord() = runTest {
        val sessionId = workout(80.0, 10)
        assertEquals(emptyList<SessionRecord>(), recordsRepository.getSessionRecords(sessionId))
    }

    @Test
    fun heavierWeightThanEverBeforeIsAWeightRecord() = runTest {
        workout(80.0, 10)
        val sessionId = workout(85.0, 5)
        assertEquals(
            listOf(SessionRecord("Bench Press", SessionRecordKind.HEAVIEST_WEIGHT, 85.0)),
            recordsRepository.getSessionRecords(sessionId),
        )
    }

    @Test
    fun moreRepsAtTheSameWeightIsAnEstimatedOneRepMaxRecord() = runTest {
        workout(80.0, 8)
        val sessionId = workout(80.0, 10)
        val records = recordsRepository.getSessionRecords(sessionId)
        assertEquals(listOf(SessionRecordKind.BEST_ESTIMATED_ONE_REP_MAX), records.map { it.kind })
    }

    @Test
    fun matchingOrWorsePerformanceIsNotARecord() = runTest {
        workout(80.0, 10)
        val sessionId = workout(80.0, 10)
        assertEquals(emptyList<SessionRecord>(), recordsRepository.getSessionRecords(sessionId))
    }

    @Test
    fun laterWorkoutsDoNotCountAgainstAnEarlierOne() = runTest {
        workout(80.0, 10)
        val earlier = workout(90.0, 5)
        workout(100.0, 5)
        assertEquals(
            listOf(SessionRecordKind.HEAVIEST_WEIGHT),
            recordsRepository.getSessionRecords(earlier).map { it.kind },
        )
    }
}
