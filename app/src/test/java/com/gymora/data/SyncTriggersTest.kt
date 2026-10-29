package com.gymora.data

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.ExerciseEntity
import com.gymora.data.local.entity.RoutineEntity
import com.gymora.data.local.entity.WorkoutExerciseEntity
import com.gymora.data.local.entity.WorkoutSessionEntity
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

/** Sync bookkeeping (v7) is kept by triggers whatever path writes the data. */
@RunWith(RobolectricTestRunner::class)
class SyncTriggersTest {

    private lateinit var database: GymoraDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = SyncTriggers.install(db)
            })
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAssignsSyncIdAndStaleUpdateCannotClearIt() = runTest {
        val dao = database.routineDao()
        val id = dao.insert(RoutineEntity(name = "Push", description = null, position = 0, createdAt = 0, updatedAt = 0))
        val inserted = dao.getById(id)!!
        assertTrue(inserted.syncId!!.matches(UUID_REGEX))
        assertTrue(inserted.updatedAt > 0)

        // Entities rebuilt from domain models carry no sync id or timestamp.
        dao.update(inserted.copy(name = "Push A", syncId = null, updatedAt = 0))
        val updated = dao.getById(id)!!
        assertEquals(inserted.syncId, updated.syncId)
        assertTrue(updated.updatedAt >= inserted.updatedAt)
    }

    @Test
    fun customExercisesGetIdsBuiltInsDoNot() = runTest {
        val dao = database.exerciseDao()
        fun exercise(name: String, custom: Boolean) = ExerciseEntity(
            name = name, muscleGroup = null, description = null, notes = null,
            isCustom = custom, deletedAt = null, createdAt = 0, updatedAt = 0,
        )
        assertNotNull(dao.getById(dao.insert(exercise("Mine", custom = true)))!!.syncId)
        assertNull(dao.getById(dao.insert(exercise("Bench Press", custom = false)))!!.syncId)
    }

    @Test
    fun childChangeBumpsParentAndDeleteLeavesTombstone() = runTest {
        val sessions = database.workoutSessionDao()
        val sessionId = sessions.insert(
            WorkoutSessionEntity(
                routineId = null, routineNameSnapshot = "Push", startedAt = 0, endedAt = 0,
                status = "COMPLETED", notes = null, createdAt = 0, syncId = "s1", updatedAt = 5,
            ),
        )
        assertEquals("Explicit values are kept", 5L, sessions.getById(sessionId)!!.updatedAt)

        database.workoutExerciseDao().insert(
            WorkoutExerciseEntity(
                sessionId = sessionId, exerciseId = null, exerciseNameSnapshot = "Row", position = 0, notes = null,
            ),
        )
        assertTrue(sessions.getById(sessionId)!!.updatedAt > 5)

        sessions.deleteById(sessionId)
        database.openHelper.readableDatabase
            .query("SELECT entity, sync_id FROM sync_tombstones").use { c ->
                assertEquals(1, c.count)
                c.moveToFirst()
                assertEquals("workout_sessions", c.getString(0))
                assertEquals("s1", c.getString(1))
            }
    }

    private companion object {
        val UUID_REGEX = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
    }
}
