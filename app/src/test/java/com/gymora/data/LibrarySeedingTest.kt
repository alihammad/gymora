package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.seed.LibrarySeederImpl
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * T012 [US1]: library seeding (FR-005, FR-010, SC-008, R-08).
 * Seeding inserts 30+ built-in exercises across 5 muscle groups, is idempotent,
 * and seeds zero routines.
 */
@RunWith(RobolectricTestRunner::class)
class LibrarySeedingTest {

    private lateinit var database: GymoraDatabase
    private lateinit var seeder: LibrarySeederImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        ).allowMainThreadQueries().build()
        seeder = LibrarySeederImpl(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedingInserts30PlusBuiltInExercisesAcross5MuscleGroups() = runTest {
        seeder.seed(database)

        val exercises = database.exerciseDao().getAllActiveOnce()
        assertTrue("expected 30+ seeded exercises, got ${exercises.size}", exercises.size >= 30)
        assertTrue("all seeded exercises are built-in", exercises.none { it.isCustom })

        // docs/exercises.json spans far more than 5 muscle groups.
        val groups = exercises.mapNotNull { it.muscleGroup }.distinct()
        assertTrue("expected many muscle groups, got ${groups.size}", groups.size >= 5)

        // The seed carries full descriptive metadata (instructions + equipment + muscle refs).
        val hasInstructions = exercises.any { it.instructionsJson != null }
        val hasEquipment = exercises.any { it.equipmentJson != null }
        val hasMuscleGroups = exercises.any { it.muscleGroupsJson != null }
        assertTrue("expected instructions metadata", hasInstructions)
        assertTrue("expected equipment metadata", hasEquipment)
        assertTrue("expected muscle-group metadata", hasMuscleGroups)
    }

    @Test
    fun seedingIsIdempotent() = runTest {
        seeder.seed(database)
        val firstCount = database.exerciseDao().count()

        seeder.seed(database)
        val secondCount = database.exerciseDao().count()

        assertEquals(firstCount, secondCount)
    }

    @Test
    fun seedingSeedsZeroRoutines() = runTest {
        seeder.seed(database)

        // The routines table lands in US2 (T025). Whether or not it exists yet,
        // seeding must never create routine rows (FR-010, BR-20).
        val db = database.openHelper.readableDatabase
        val tableCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='routines'",
        )
        val routinesTableExists = tableCursor.moveToFirst()
        tableCursor.close()

        if (routinesTableExists) {
            val countCursor = db.query("SELECT COUNT(*) FROM routines")
            countCursor.moveToFirst()
            assertEquals(0, countCursor.getInt(0))
            countCursor.close()
        }
    }
}
