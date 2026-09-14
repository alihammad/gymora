package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.seed.LibrarySeederImpl
import com.gymora.data.local.seed.RoutineSeederImpl
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Default-routine seeding: 10 ready-to-use routines on first launch, each with
 * 6+ exercises resolved from the exercise library, with planned set templates.
 */
@RunWith(RobolectricTestRunner::class)
class RoutineSeedingTest {

    private lateinit var database: GymoraDatabase
    private lateinit var librarySeeder: LibrarySeederImpl
    private lateinit var routineSeeder: RoutineSeederImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        ).allowMainThreadQueries().build()
        librarySeeder = LibrarySeederImpl(ApplicationProvider.getApplicationContext())
        routineSeeder = RoutineSeederImpl()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedingCreatesTenDefaultRoutines() = runTest {
        librarySeeder.seed(database)
        routineSeeder.seed(database)

        val routines = database.routineDao().getAllOnce()
        assertEquals("expected 10 default routines", 10, routines.size)
        // Home-screen order is contiguous starting at 0.
        assertEquals(
            (0 until routines.size).toList(),
            routines.map { it.position }.sorted(),
        )
    }

    @Test
    fun eachRoutineHasAtLeastSixResolvableExercises() = runTest {
        librarySeeder.seed(database)
        routineSeeder.seed(database)

        val routines = database.routineDao().getAllOnce()
        routines.forEach { routine ->
            val exercises = database.routineExerciseDao().getForRoutine(routine.id)
            assertTrue(
                "routine '${routine.name}' has ${exercises.size} exercises, expected >= 6",
                exercises.size >= 6,
            )
            // Every exercise must resolve to a real library exercise id.
            exercises.forEach { link ->
                val exercise = database.exerciseDao().getById(link.exerciseId)
                assertTrue(
                    "routine '${routine.name}' references missing exercise id ${link.exerciseId}",
                    exercise != null,
                )
            }
        }
    }

    @Test
    fun eachExerciseHasPlannedSetTemplates() = runTest {
        librarySeeder.seed(database)
        routineSeeder.seed(database)

        val routines = database.routineDao().getAllOnce()
        val routineExercises = routines.flatMap {
            database.routineExerciseDao().getForRoutine(it.id)
        }
        assertTrue("expected seeded routine exercises", routineExercises.isNotEmpty())

        routineExercises.forEach { link ->
            val sets = database.setTemplateDao().getForRoutineExercise(link.id)
            assertTrue(
                "routine exercise ${link.id} has no planned sets",
                sets.isNotEmpty(),
            )
            // Set numbers are contiguous and 1-based.
            assertEquals((1..sets.size).toList(), sets.map { it.setNumber }.sorted())
        }
    }

    @Test
    fun seedingIsIdempotent() = runTest {
        librarySeeder.seed(database)
        routineSeeder.seed(database)
        val firstCount = database.routineDao().getAllOnce().size

        routineSeeder.seed(database)
        val secondCount = database.routineDao().getAllOnce().size

        assertEquals(firstCount, secondCount)
    }
}
