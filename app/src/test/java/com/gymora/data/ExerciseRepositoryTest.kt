package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.repository.ExerciseRepositoryImpl
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.MuscleGroup
import com.gymora.domain.model.UpdateExerciseInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * T013 [US1]: ExerciseRepository behavior (FR-006..FR-009).
 * Create custom, edit, search includes custom exercises, soft delete hides
 * from library/search but keeps the row.
 */
@RunWith(RobolectricTestRunner::class)
class ExerciseRepositoryTest {

    private lateinit var database: GymoraDatabase
    private lateinit var repository: ExerciseRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ExerciseRepositoryImpl(database.exerciseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createCustomExerciseIsSavedAndSelectable() = runTest {
        val created = repository.createCustom(
            CreateExerciseInput(
                name = "My Custom Curl",
                muscleGroup = MuscleGroup.BICEPS,
                description = null,
                notes = null,
            ),
        )

        assertTrue(created.isCustom)
        assertEquals("My Custom Curl", created.name)

        val library = repository.observeLibrary().first()
        assertTrue(library.any { it.id == created.id })
    }

    @Test
    fun editExerciseUpdatesDetails() = runTest {
        val created = repository.createCustom(CreateExerciseInput(name = "Old Name"))

        val updated = repository.update(
            created.id,
            UpdateExerciseInput(
                name = "New Name",
                muscleGroup = MuscleGroup.QUADRICEPS,
                description = "Updated",
                notes = null,
            ),
        )

        assertEquals("New Name", updated.name)
        assertEquals(MuscleGroup.QUADRICEPS, updated.muscleGroup)
        assertEquals(updated, repository.getById(created.id))
    }

    @Test
    fun searchIncludesCustomExercises() = runTest {
        repository.createCustom(CreateExerciseInput(name = "Zebra Bench Special"))

        val results = repository.search("bench")

        assertTrue(results.any { it.name == "Zebra Bench Special" })
    }

    @Test
    fun softDeleteHidesFromLibraryAndSearchButKeepsRow() = runTest {
        val created = repository.createCustom(CreateExerciseInput(name = "Delete Me"))

        repository.delete(created.id)

        val library = repository.observeLibrary().first()
        assertTrue(library.none { it.id == created.id })
        assertTrue(repository.search("Delete Me").isEmpty())

        // Row must remain for historical referential integrity (FR-009, R-03).
        val stillThere = database.exerciseDao().getById(created.id)
        assertNotNull(stillThere)
        assertNotNull(stillThere?.deletedAt)
    }
}
