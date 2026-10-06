package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.repository.ExerciseRepositoryImpl
import com.gymora.data.repository.RoutineRepositoryImpl
import com.gymora.data.repository.WorkoutSessionRepositoryImpl
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.SupersetRules
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Supersets persist on templates, survive edits and duplication, and carry into sessions. */
@RunWith(RobolectricTestRunner::class)
class SupersetRepositoryTest {

    private lateinit var database: GymoraDatabase
    private lateinit var repository: RoutineRepositoryImpl
    private lateinit var sessionRepository: WorkoutSessionRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoutineRepositoryImpl(
            database.routineDao(),
            database.routineExerciseDao(),
            database.setTemplateDao(),
            database.exerciseDao(),
            database,
        )
        sessionRepository = WorkoutSessionRepositoryImpl(database)
        exerciseRepository = ExerciseRepositoryImpl(database.exerciseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** A routine with exercises named A, B, C, ...; returns routine id and routine exercise ids. */
    private suspend fun routineWith(vararg names: String): Pair<Long, List<Long>> {
        val routineId = repository.create("Legs", null)
        val ids = names.map { name ->
            val exerciseId = exerciseRepository.createCustom(CreateExerciseInput(name = name)).id
            repository.addExercise(routineId, exerciseId, null)
        }
        return routineId to ids
    }

    private suspend fun blockNames(routineId: Long): List<List<String>> =
        SupersetRules.blocks(repository.getById(routineId).exercises) { it.supersetGroup }
            .map { block -> block.map { it.exerciseName } }

    @Test
    fun linkAndUnlinkPersist() = runTest {
        val (routineId, ids) = routineWith("A", "B", "C")

        repository.linkSupersetWithNext(routineId, ids[0])
        assertEquals(listOf(listOf("A", "B"), listOf("C")), blockNames(routineId))

        repository.linkSupersetWithNext(routineId, ids[1])
        assertEquals(listOf(listOf("A", "B", "C")), blockNames(routineId))

        repository.unlinkSupersetFromNext(routineId, ids[0])
        assertEquals(listOf(listOf("A"), listOf("B", "C")), blockNames(routineId))
    }

    @Test
    fun removingAMemberOfAPairDissolvesTheSuperset() = runTest {
        val (routineId, ids) = routineWith("A", "B", "C")
        repository.linkSupersetWithNext(routineId, ids[0])

        repository.removeExercise(routineId, ids[0])

        assertEquals(listOf(listOf("B"), listOf("C")), blockNames(routineId))
        assertEquals(null, repository.getById(routineId).exercises.first().supersetGroup)
    }

    @Test
    fun reorderingAMemberAwaySplitsTheSuperset() = runTest {
        val (routineId, ids) = routineWith("A", "B", "C")
        repository.linkSupersetWithNext(routineId, ids[0])

        repository.reorderExercises(routineId, listOf(ids[0], ids[2], ids[1]))

        assertEquals(listOf(listOf("A"), listOf("C"), listOf("B")), blockNames(routineId))
    }

    @Test
    fun duplicateKeepsSupersets() = runTest {
        val (routineId, ids) = routineWith("A", "B", "C")
        repository.linkSupersetWithNext(routineId, ids[1])

        val copyId = repository.duplicate(routineId)

        assertEquals(listOf(listOf("A"), listOf("B", "C")), blockNames(copyId))
    }

    @Test
    fun startedWorkoutKeepsSupersets() = runTest {
        val (routineId, ids) = routineWith("A", "B", "C")
        repository.linkSupersetWithNext(routineId, ids[0])

        val sessionId = sessionRepository.startFromRoutine(routineId)
        val exercises = sessionRepository.getActiveWorkout(sessionId).exercises

        val blocks = SupersetRules.blocks(exercises) { it.supersetGroup }
            .map { block -> block.map { it.exerciseName } }
        assertEquals(listOf(listOf("A", "B"), listOf("C")), blocks)
        assertEquals(exercises[0].workoutExerciseId, exercises[0].supersetGroup)
    }
}
