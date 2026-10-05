package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.WorkoutExerciseEntity
import com.gymora.data.local.entity.WorkoutSessionEntity
import com.gymora.data.local.entity.WorkoutSetEntity
import com.gymora.data.transfer.CsvCodec
import com.gymora.data.transfer.CsvImportException
import com.gymora.data.transfer.WorkoutCsvService
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SessionStatus
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkoutCsvServiceTest {

    private lateinit var database: GymoraDatabase
    private lateinit var service: WorkoutCsvService

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        ).allowMainThreadQueries().build()
        service = WorkoutCsvService(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun csvCodecRoundTripsQuotesCommasAndNewlines() {
        val row = listOf("a,b", "say \"hi\"", "line1\nline2", "")
        assertEquals(listOf(row), CsvCodec.parse(CsvCodec.formatRow(row) + "\r\n"))
    }

    @Test
    fun exportThenImportIntoEmptyDatabaseRestoresWorkout() = runTest {
        val startedAt = 1_760_000_000_000L
        val sessionId = database.workoutSessionDao().insert(
            WorkoutSessionEntity(
                routineId = null, routineNameSnapshot = "Push, heavy", startedAt = startedAt,
                endedAt = startedAt + 3_600_000, status = "COMPLETED",
                notes = "good \"one\"", createdAt = startedAt,
            ),
        )
        val weId = database.workoutExerciseDao().insert(
            WorkoutExerciseEntity(
                sessionId = sessionId, exerciseId = null,
                exerciseNameSnapshot = "Bench Press", position = 0, notes = null,
            ),
        )
        database.workoutSetDao().insert(set(weId, 1, 62.5, 8))
        database.workoutSetDao().insert(set(weId, 2, 62.5, 7))

        val out = ByteArrayOutputStream()
        assertEquals(1, service.export(out))
        val csv = out.toByteArray()

        // Re-importing into the same database is a no-op duplicate.
        val again = service.import(csv.inputStream())
        assertEquals(0, again.workoutsImported)
        assertEquals(1, again.duplicatesSkipped)

        database.workoutSessionDao().deleteById(sessionId)
        val result = service.import(csv.inputStream())
        assertEquals(1, result.workoutsImported)
        assertEquals(1, result.exercisesCreated)

        val session = database.workoutSessionDao().listCompleted(10, 0).single()
        assertEquals("Push, heavy", session.routineNameSnapshot)
        assertEquals("good \"one\"", session.notes)
        assertEquals(SessionStatus.COMPLETED.name, session.status)
        val sets = database.workoutSetDao().getForSession(session.id)
        assertEquals(listOf(62.5, 62.5), sets.map { it.weight })
        assertEquals(listOf(8, 7), sets.map { it.reps })
    }

    @Test
    fun importSkipsBadRowsAndRejectsMissingColumns() = runTest {
        val csv = "Start,Workout,Exercise,Weight,Reps\n" +
            "2026-01-05 18:00,Legs,Squat,100,5\n" +
            "not-a-date,Legs,Squat,100,5\n" +
            "2026-01-05 18:00,Legs,Squat,abc,5\n"
        val result = service.import(csv.byteInputStream())
        assertEquals(1, result.workoutsImported)
        assertEquals(2, result.rowsSkipped)
        assertTrue(result.errors.first().startsWith("Row 3"))

        try {
            service.import("Foo,Bar\n1,2\n".byteInputStream())
            error("expected CsvImportException")
        } catch (_: CsvImportException) {
            // expected
        }
    }

    private fun set(weId: Long, n: Int, weight: Double, reps: Int) = WorkoutSetEntity(
        workoutExerciseId = weId,
        setNumber = n,
        reps = reps,
        weight = weight,
        weightUnit = "KG",
        measurementType = MeasurementType.WEIGHT_AND_REPS.name,
        isCompleted = true,
        completedAt = null,
        notes = null,
    )
}
