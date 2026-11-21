package com.gymora.data.sync

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.ExerciseEntity
import com.gymora.data.local.entity.RoutineEntity
import com.gymora.data.local.entity.RoutineExerciseEntity
import com.gymora.data.local.entity.SetTemplateEntity
import com.gymora.data.local.entity.SettingsEntity
import com.gymora.data.local.entity.WorkoutExerciseEntity
import com.gymora.data.local.entity.WorkoutSessionEntity
import com.gymora.data.local.entity.WorkoutSetEntity
import com.gymora.data.local.sync.SyncTriggers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Two phones signed in to the same account, sharing one cloud. */
@RunWith(RobolectricTestRunner::class)
class SyncEngineTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cloud = FakeSyncRemote()
    private lateinit var phoneA: Phone
    private lateinit var phoneB: Phone

    private inner class Phone {
        val database: GymoraDatabase = Room.inMemoryDatabaseBuilder(context, GymoraDatabase::class.java)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = SyncTriggers.install(db)
            })
            .allowMainThreadQueries()
            .build()
        val engine = SyncEngine(database, cloud)
        val db: SupportSQLiteDatabase get() = database.openHelper.writableDatabase

        suspend fun sync(onlyIfPending: Boolean = false) = engine.sync(UID, onlyIfPending)

        fun rows(sql: String, vararg args: Any?): List<List<Any?>> = db.query(sql, args).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        (0 until c.columnCount).map { i ->
                            when (c.getType(i)) {
                                android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                                android.database.Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                                android.database.Cursor.FIELD_TYPE_NULL -> null
                                else -> c.getString(i)
                            }
                        },
                    )
                }
            }
        }

        fun single(sql: String, vararg args: Any?): Any? = rows(sql, *args).firstOrNull()?.firstOrNull()
    }

    @Before
    fun setUp() = runTest {
        phoneA = Phone()
        phoneB = Phone()
        // The built-in library is seeded on every phone with the same sync ids.
        listOf(phoneA, phoneB).forEach { phone ->
            phone.database.exerciseDao().insert(exercise("Bench Press", custom = false, syncId = BENCH))
            phone.database.exerciseDao().insert(exercise("Squat", custom = false, syncId = SQUAT))
        }
    }

    @After
    fun tearDown() {
        phoneA.database.close()
        phoneB.database.close()
    }

    @Test
    fun aWorkoutLoggedOnOnePhoneAppearsOnTheOther() = runTest {
        val session = phoneA.logWorkout(status = "COMPLETED", sets = listOf(60.0 to 10, 70.0 to 8))
        phoneA.sync()

        assertTrue(phoneB.sync().downloaded > 0)
        val syncId = phoneA.single("SELECT sync_id FROM workout_sessions WHERE id = ?", session)
        val bSession = phoneB.single("SELECT id FROM workout_sessions WHERE sync_id = ?", syncId) as Long
        val bExercise = phoneB.single("SELECT exercise_id FROM workout_exercises WHERE session_id = ?", bSession)
        // Linked to phone B's own copy of the built-in exercise.
        assertEquals(phoneB.single("SELECT id FROM exercises WHERE sync_id = ?", BENCH), bExercise)
        assertEquals(
            listOf(listOf(60.0, 10L), listOf(70.0, 8L)),
            phoneB.rows(
                "SELECT s.weight, s.reps FROM workout_sets s " +
                    "JOIN workout_exercises e ON e.id = s.workout_exercise_id " +
                    "WHERE e.session_id = ? ORDER BY s.set_number",
                bSession,
            ),
        )
    }

    @Test
    fun aWorkoutInProgressUploadsOnlyOnceFinished() = runTest {
        val session = phoneA.logWorkout(status = "ACTIVE")
        phoneA.sync()
        assertTrue(cloud.docs(UID, "workouts").isEmpty())

        phoneA.db.execSQL(
            "UPDATE workout_sessions SET status = 'COMPLETED', ended_at = 2 WHERE id = ?",
            arrayOf(session),
        )
        phoneA.sync()
        assertEquals(1, cloud.docs(UID, "workouts").size)
    }

    @Test
    fun theLatestEditWins() = runTest {
        val routine = phoneA.createRoutine("Push")
        phoneA.sync()
        phoneB.sync()
        val bRoutine = phoneB.single("SELECT id FROM routines WHERE name = 'Push'") as Long

        phoneA.db.execSQL("UPDATE routines SET name = 'Push A' WHERE id = ?", arrayOf(routine))
        Thread.sleep(5)
        phoneB.db.execSQL("UPDATE routines SET name = 'Push B' WHERE id = ?", arrayOf(bRoutine))
        phoneA.sync()
        phoneB.sync()
        phoneA.sync()

        assertEquals("Push B", phoneA.single("SELECT name FROM routines WHERE id = ?", routine))
        assertEquals("Push B", phoneB.single("SELECT name FROM routines WHERE id = ?", bRoutine))
    }

    @Test
    fun aRoutineEditCarriesItsExercisesAndSets() = runTest {
        val routine = phoneA.createRoutine("Legs", exerciseSyncId = SQUAT, sets = 3)
        phoneA.sync()
        phoneB.sync()
        val bRoutine = phoneB.single("SELECT id FROM routines WHERE name = 'Legs'") as Long
        assertEquals(
            3L,
            phoneB.single(
                "SELECT COUNT(*) FROM set_templates t JOIN routine_exercises e ON e.id = t.routine_exercise_id " +
                    "WHERE e.routine_id = ?",
                bRoutine,
            ),
        )

        // Removing a set on A replaces B's sets on the next sync.
        phoneA.db.execSQL(
            "DELETE FROM set_templates WHERE set_number = 3 AND routine_exercise_id IN " +
                "(SELECT id FROM routine_exercises WHERE routine_id = ?)",
            arrayOf(routine),
        )
        phoneA.sync()
        phoneB.sync()
        assertEquals(
            2L,
            phoneB.single(
                "SELECT COUNT(*) FROM set_templates t JOIN routine_exercises e ON e.id = t.routine_exercise_id " +
                    "WHERE e.routine_id = ?",
                bRoutine,
            ),
        )
    }

    @Test
    fun deletionsReachTheOtherPhone() = runTest {
        val routine = phoneA.createRoutine("Pull")
        phoneA.sync()
        phoneB.sync()

        phoneA.database.routineDao().deleteById(routine)
        phoneA.sync()
        phoneB.sync()

        assertNull(phoneB.single("SELECT id FROM routines WHERE name = 'Pull'"))
        assertEquals(true, cloud.docs(UID, "routines").values.single().deleted)
    }

    @Test
    fun aDeletionHereBeatsAnOlderCloudVersion() = runTest {
        val routine = phoneA.createRoutine("Arms")
        phoneA.sync()
        phoneB.sync()
        val bRoutine = phoneB.single("SELECT id FROM routines WHERE name = 'Arms'") as Long
        phoneB.db.execSQL("UPDATE routines SET name = 'Arms old' WHERE id = ?", arrayOf(bRoutine))
        phoneB.sync()

        Thread.sleep(5)
        phoneA.database.routineDao().deleteById(routine)
        // A downloads B's older edit first; its own later deletion must still win.
        phoneA.sync()
        assertNull(phoneA.single("SELECT id FROM routines WHERE id = ?", routine))
        phoneB.sync()
        assertNull(phoneB.single("SELECT id FROM routines WHERE id = ?", bRoutine))
    }

    @Test
    fun downloadedChangesAreNotUploadedBack() = runTest {
        phoneA.logWorkout(status = "COMPLETED")
        phoneA.createRoutine("Push")
        phoneA.sync()

        phoneB.sync()
        val pushesAfterFirstRound = cloud.pushes
        assertEquals(0, phoneB.sync().uploaded)
        assertEquals(0, phoneA.sync().uploaded)
        assertEquals(pushesAfterFirstRound, cloud.pushes)
        assertTrue(!phoneB.engine.hasPendingChanges())
    }

    @Test
    fun aCustomExerciseArrivesWithTheRoutineThatUsesIt() = runTest {
        val custom = phoneA.database.exerciseDao().insert(exercise("Landmine Press", custom = true))
        val customSyncId = phoneA.single("SELECT sync_id FROM exercises WHERE id = ?", custom) as String
        phoneA.createRoutine("Shoulders", exerciseSyncId = customSyncId)
        phoneA.sync()
        phoneB.sync()

        val bExercise = phoneB.single("SELECT id FROM exercises WHERE sync_id = ?", customSyncId)
        assertNotNull(bExercise)
        assertEquals(
            bExercise,
            phoneB.single(
                "SELECT e.exercise_id FROM routine_exercises e JOIN routines r ON r.id = e.routine_id " +
                    "WHERE r.name = 'Shoulders'",
            ),
        )
        // Built-ins ship with the app and are never uploaded.
        assertEquals(setOf(customSyncId), cloud.docs(UID, "exercises").keys)
    }

    @Test
    fun personalSettingsSyncButPhoneSpecificOnesStay() = runTest {
        phoneB.database.settingsDao().upsert(
            SettingsEntity.DEFAULTS.copy(autoBackupFolderUri = "content://b", updatedAt = 1),
        )
        phoneA.database.settingsDao().upsert(
            SettingsEntity.DEFAULTS.copy(
                weightUnit = "LB",
                weeklyGoal = 5,
                reminderDays = 0b11,
                autoBackupFolderUri = "content://a",
                updatedAt = System.currentTimeMillis(),
            ),
        )
        phoneA.sync()
        phoneB.sync()

        assertEquals(
            listOf("LB", 5L, 0L, "content://b"),
            phoneB.rows(
                "SELECT weight_unit, weekly_goal, reminder_days, auto_backup_folder_uri FROM settings",
            ).single(),
        )
    }

    @Test
    fun anUntouchedDefaultRoutineNeverOverwritesTheEditedOne() = runTest {
        // Both phones seeded the same default routine; only A's user edited it.
        listOf(phoneA, phoneB).forEach { it.createRoutine("Full Body", syncId = DEFAULT_ROUTINE) }
        SyncTriggers.quietly(phoneB.db) { phoneB.db.execSQL("UPDATE routines SET updated_at = 1") }
        phoneA.db.execSQL("UPDATE routines SET name = 'Full Body (mine)' WHERE sync_id = ?", arrayOf(DEFAULT_ROUTINE))
        phoneA.sync()

        phoneB.sync()
        assertEquals("Full Body (mine)", phoneB.single("SELECT name FROM routines WHERE sync_id = ?", DEFAULT_ROUTINE))
        phoneA.sync()
        assertEquals("Full Body (mine)", phoneA.single("SELECT name FROM routines WHERE sync_id = ?", DEFAULT_ROUTINE))
    }

    @Test
    fun supersetsSurviveTheTrip() = runTest {
        val session = phoneA.logWorkout(status = "COMPLETED")
        val first = phoneA.single("SELECT id FROM workout_exercises WHERE session_id = ?", session) as Long
        val second = phoneA.database.workoutExerciseDao().insert(
            WorkoutExerciseEntity(
                sessionId = session, exerciseId = null, exerciseNameSnapshot = "Squat", position = 1, notes = null,
            ),
        )
        phoneA.db.execSQL(
            "UPDATE workout_exercises SET superset_group = ? WHERE id IN (?, ?)",
            arrayOf(first, first, second),
        )
        phoneA.sync()
        phoneB.sync()

        val groups = phoneB.rows("SELECT id, superset_group FROM workout_exercises ORDER BY position")
        // Labelled with phone B's own id of the first member, as the app expects.
        assertEquals(listOf(groups[0][0], groups[0][0]), groups.map { it[1] })
    }

    @Test
    fun nothingToUploadMeansNoNetwork() = runTest {
        phoneA.sync()
        cloud.failNext = IllegalStateException("offline")
        assertEquals(SyncResult(0, 0), phoneA.sync(onlyIfPending = true))
        assertNotNull(cloud.failNext)
    }

    @Test
    fun aFailedUploadIsRetriedNextTime() = runTest {
        phoneA.createRoutine("Core")
        cloud.failNext = IllegalStateException("offline")
        runCatching { phoneA.sync() }
        assertTrue(phoneA.engine.hasPendingChanges())

        phoneA.sync()
        assertEquals(1, cloud.docs(UID, "routines").size)
        assertTrue(!phoneA.engine.hasPendingChanges())
    }

    @Test
    fun deletingCloudDataForgetsWhatTheCloudHeld() = runTest {
        phoneA.createRoutine("Push")
        phoneA.sync()
        phoneA.engine.deleteCloudData(UID)

        assertEquals(UID, cloud.deletedAllFor)
        // If deleting the login then fails, everything uploads again.
        assertTrue(phoneA.engine.hasPendingChanges())
    }

    // ---- Helpers ----

    private fun exercise(name: String, custom: Boolean, syncId: String? = null) = ExerciseEntity(
        name = name, muscleGroup = null, description = null, notes = null, isCustom = custom,
        deletedAt = null, createdAt = 0, updatedAt = 0, syncId = syncId,
    )

    private suspend fun Phone.createRoutine(
        name: String,
        exerciseSyncId: String = BENCH,
        sets: Int = 1,
        syncId: String? = null,
    ): Long {
        val routine = database.routineDao().insert(
            RoutineEntity(name = name, description = null, position = 0, createdAt = 0, updatedAt = 0, syncId = syncId),
        )
        val exerciseId = single("SELECT id FROM exercises WHERE sync_id = ?", exerciseSyncId) as Long
        val routineExercise = database.routineExerciseDao().insert(
            RoutineExerciseEntity(routineId = routine, exerciseId = exerciseId, position = 0, notes = null),
        )
        repeat(sets) {
            database.setTemplateDao().insert(
                SetTemplateEntity(
                    routineExerciseId = routineExercise, setNumber = it + 1, targetReps = 8,
                    targetWeight = null, targetWeightUnit = null, measurementType = "WEIGHT_AND_REPS",
                ),
            )
        }
        return routine
    }

    private suspend fun Phone.logWorkout(status: String, sets: List<Pair<Double, Int>> = listOf(50.0 to 5)): Long {
        val session = database.workoutSessionDao().insert(
            WorkoutSessionEntity(
                routineId = null, routineNameSnapshot = "Push", startedAt = 1,
                endedAt = if (status == "ACTIVE") null else 2L, status = status, notes = null, createdAt = 1,
            ),
        )
        val exerciseId = single("SELECT id FROM exercises WHERE sync_id = ?", BENCH) as Long
        val workoutExercise = database.workoutExerciseDao().insert(
            WorkoutExerciseEntity(
                sessionId = session, exerciseId = exerciseId, exerciseNameSnapshot = "Bench Press", position = 0,
                notes = null,
            ),
        )
        sets.forEachIndexed { i, (weight, reps) ->
            database.workoutSetDao().insert(
                WorkoutSetEntity(
                    workoutExerciseId = workoutExercise, setNumber = i + 1, reps = reps, weight = weight,
                    weightUnit = "KG", measurementType = "WEIGHT_AND_REPS", isCompleted = true, completedAt = 2,
                    notes = null,
                ),
            )
        }
        return session
    }

    private companion object {
        const val UID = "uid-alice"
        const val BENCH = "11111111-1111-1111-1111-111111111111"
        const val SQUAT = "22222222-2222-2222-2222-222222222222"
        const val DEFAULT_ROUTINE = "33333333-3333-3333-3333-333333333333"
    }
}
