package com.gymora.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.backup.BackupException
import com.gymora.data.backup.BackupService
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.BodyMeasurementEntity
import com.gymora.data.media.ExerciseMediaStore
import com.gymora.data.repository.ExerciseRepositoryImpl
import com.gymora.data.repository.RoutineRepositoryImpl
import com.gymora.data.repository.SettingsRepositoryImpl
import com.gymora.data.repository.WorkoutSessionRepositoryImpl
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SetTemplateInput
import com.gymora.domain.model.WeightUnit
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Full backup and restore: all tables and media round-trip; device-local settings survive. */
@RunWith(RobolectricTestRunner::class)
class BackupServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: GymoraDatabase
    private lateinit var backup: BackupService
    private lateinit var media: ExerciseMediaStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, GymoraDatabase::class.java)
            .allowMainThreadQueries().build()
        media = ExerciseMediaStore(context)
        media.directory.deleteRecursively()
        backup = BackupService(database, media)
    }

    @After
    fun tearDown() {
        database.close()
        media.directory.deleteRecursively()
    }

    private suspend fun seed() {
        val exercises = ExerciseRepositoryImpl(database.exerciseDao())
        val routines = RoutineRepositoryImpl(
            database.routineDao(), database.routineExerciseDao(), database.setTemplateDao(),
            database.exerciseDao(), database,
        )
        val exerciseId = exercises.createCustom(
            CreateExerciseInput(
                name = "Plank",
                measurementType = MeasurementType.DURATION,
                formCues = listOf("Squeeze glutes"),
                mediaFile = "plank.gif",
            ),
        ).id
        val routineId = routines.create("Core", null)
        val routineExerciseId = routines.addExercise(routineId, exerciseId, null)
        routines.addSetTemplate(
            routineExerciseId,
            SetTemplateInput(0, null, null, MeasurementType.DURATION, targetDurationSeconds = 45),
        )
        val sessions = WorkoutSessionRepositoryImpl(database)
        val sessionId = sessions.startFromRoutine(routineId)
        sessions.completeSet(sessions.getActiveWorkout(sessionId).exercises.single().sets.single().id)
        sessions.finish(sessionId)
        database.bodyMeasurementDao().insert(
            BodyMeasurementEntity(
                measuredAt = 1_000L, weightKg = 80.5, shouldersCm = null, chestCm = 100.0,
                aboveNavelCm = null, navelCm = null, belowNavelCm = null, thighCm = null,
            ),
        )
        SettingsRepositoryImpl(database.settingsDao()).setWeightUnit(WeightUnit.LB)
        media.directory.mkdirs()
        File(media.directory, "plank.gif").writeText("GIF89a")
    }

    @Test
    fun restoreBringsBackEverythingAndKeepsDeviceSettings() = runTest {
        seed()
        val settings = SettingsRepositoryImpl(database.settingsDao())
        settings.setAutoBackup(7, "content://backup-folder")

        val bytes = ByteArrayOutputStream().also { backup.export(it) }.toByteArray()

        // Diverge: wipe user data, change preferences, and point auto-backup elsewhere.
        database.clearAllTables()
        settings.setWeightUnit(WeightUnit.KG)
        settings.setAutoBackup(1, "content://other-folder")
        media.directory.deleteRecursively()

        val summary = backup.restore(ByteArrayInputStream(bytes))

        assertEquals(1, summary.workouts)
        assertEquals(1, summary.routines)
        assertEquals(1, summary.measurements)
        assertEquals(1, summary.mediaFiles)

        val exercise = ExerciseRepositoryImpl(database.exerciseDao()).search("Plank").single()
        assertEquals(MeasurementType.DURATION, exercise.measurementType)
        assertEquals(listOf("Squeeze glutes"), exercise.formCues)
        assertEquals("GIF89a", File(media.directory, "plank.gif").readText())

        val session = database.workoutSessionDao().listCompleted(10, 0).single()
        val set = database.workoutExerciseDao().getForSession(session.id)
            .flatMap { database.workoutSetDao().getForExercise(it.id) }.single()
        assertEquals(45, set.durationSeconds)
        assertTrue(set.isCompleted)
        assertEquals(80.5, database.bodyMeasurementDao().getAllOnce().single().weightKg!!, 0.0)

        val restored = settings.observeSettings().first()
        assertEquals(WeightUnit.LB, restored.weightUnit) // user preference comes from the backup
        assertEquals("content://other-folder", restored.autoBackupFolderUri) // device setting is kept
        assertEquals(1, restored.autoBackupIntervalDays)
    }

    @Test
    fun rejectsFilesThatAreNotBackups() = runTest {
        seed()
        val notABackup = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { it.putNextEntry(ZipEntry("hello.txt")); it.write(1) }
        }.toByteArray()

        try {
            backup.restore(ByteArrayInputStream(notABackup))
            fail("expected BackupException")
        } catch (expected: BackupException) {
            // Data is untouched.
            assertEquals(1, database.workoutSessionDao().listCompleted(10, 0).size)
        }
    }

    @Test
    fun rejectsBackupsFromANewerSchema() = runTest {
        val json = """{"format":"gymora-backup","schemaVersion":999,"tables":{}}"""
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { it.putNextEntry(ZipEntry("backup.json")); it.write(json.toByteArray()) }
        }.toByteArray()

        val error = runCatching { backup.restore(ByteArrayInputStream(bytes)) }.exceptionOrNull()
        assertTrue(error is BackupException)
        assertFalse(error!!.message.isNullOrBlank())
    }
}
