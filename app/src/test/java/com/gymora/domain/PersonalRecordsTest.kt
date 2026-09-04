package com.gymora.domain

import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.CompletedSet
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.PersonalRecord
import com.gymora.domain.model.PersonalRecords
import com.gymora.domain.model.WeightUnit
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T064 [US10]: Epley 1RM (weight × (1 + reps/30); reps ≥ 1; excludes
 * REPS_ONLY/zero-weight sets) and records selection logic (FR-047, R-09).
 */
class PersonalRecordsTest {

    // --- Epley 1RM (R-09) ---

    @Test
    fun epleyOneRepMaxForSingleRep() {
        assertEquals(100.0, WorkoutCalculators.estimatedOneRepMax(100.0, 1), 0.001)
    }

    @Test
    fun epleyOneRepMaxForMultipleReps() {
        // 100 × (1 + 10/30) = 100 × 1.333... = 133.333...
        assertEquals(133.333, WorkoutCalculators.estimatedOneRepMax(100.0, 10), 0.01)
    }

    @Test
    fun epleyOneRepMaxForHighReps() {
        // 60 × (1 + 20/30) = 60 × 1.666... = 100.0
        assertEquals(100.0, WorkoutCalculators.estimatedOneRepMax(60.0, 20), 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun epleyRejectsZeroReps() {
        WorkoutCalculators.estimatedOneRepMax(100.0, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun epleyRejectsNegativeReps() {
        WorkoutCalculators.estimatedOneRepMax(100.0, -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun epleyRejectsZeroWeight() {
        WorkoutCalculators.estimatedOneRepMax(0.0, 10)
    }

    @Test(expected = IllegalArgumentException::class)
    fun epleyRejectsNegativeWeight() {
        WorkoutCalculators.estimatedOneRepMax(-10.0, 5)
    }

    // --- Records selection logic ---

    @Test
    fun heaviestWeightRecordSelected() {
        val sets = listOf(
            CompletedSet(weight = 80.0, reps = 5, weightUnit = WeightUnit.KG, isCompleted = true),
            CompletedSet(weight = 100.0, reps = 3, weightUnit = WeightUnit.KG, isCompleted = true),
            CompletedSet(weight = 90.0, reps = 8, weightUnit = WeightUnit.KG, isCompleted = true),
        )
        val record = findHeaviestWeight(sets, "Bench Press", Instant.now())
        assertNotNull(record)
        assertEquals(100.0, record!!.value, 0.001)
    }

    @Test
    fun highestRepsRecordSelected() {
        val sets = listOf(
            CompletedSet(weight = 60.0, reps = 10, weightUnit = WeightUnit.KG, isCompleted = true),
            CompletedSet(weight = 60.0, reps = 15, weightUnit = WeightUnit.KG, isCompleted = true),
            CompletedSet(weight = 60.0, reps = 8, weightUnit = WeightUnit.KG, isCompleted = true),
        )
        val record = findHighestReps(sets, "Squat", Instant.now())
        assertNotNull(record)
        assertEquals(15.0, record!!.value, 0.001)
    }

    @Test
    fun bestEpleyRecordSelected() {
        val sets = listOf(
            CompletedSet(weight = 100.0, reps = 1, weightUnit = WeightUnit.KG, isCompleted = true),
            CompletedSet(weight = 80.0, reps = 8, weightUnit = WeightUnit.KG, isCompleted = true),
            // 80 × (1 + 8/30) = 101.33 > 100
        )
        val record = findBestEpley(sets, "Deadlift", Instant.now())
        assertNotNull(record)
        // 80 × (1 + 8/30) = 101.333...
        assertEquals(101.333, record!!.value, 0.01)
    }

    @Test
    fun emptySetsReturnNullRecords() {
        val empty = emptyList<CompletedSet>()
        assertNull(findHeaviestWeight(empty, "Test", Instant.now()))
        assertNull(findHighestReps(empty, "Test", Instant.now()))
        assertNull(findBestEpley(empty, "Test", Instant.now()))
    }

    @Test
    fun incompleteSetsAreExcluded() {
        val sets = listOf(
            CompletedSet(weight = 200.0, reps = 1, weightUnit = WeightUnit.KG, isCompleted = false),
            CompletedSet(weight = 50.0, reps = 5, weightUnit = WeightUnit.KG, isCompleted = true),
        )
        val record = findHeaviestWeight(sets, "Test", Instant.now())
        assertNotNull(record)
        assertEquals(50.0, record!!.value, 0.001)
    }

    @Test
    fun zeroWeightSetsExcludedFromWeightRecords() {
        val sets = listOf(
            CompletedSet(weight = 0.0, reps = 100, weightUnit = WeightUnit.KG, isCompleted = true),
            CompletedSet(weight = 50.0, reps = 5, weightUnit = WeightUnit.KG, isCompleted = true),
        )
        val record = findHeaviestWeight(sets, "Test", Instant.now())
        assertNotNull(record)
        assertEquals(50.0, record!!.value, 0.001)
    }

    @Test
    fun repsOnlySetsExcludedFromEpley() {
        val sets = listOf(
            CompletedSet(weight = null, reps = 20, weightUnit = null, isCompleted = true),
            CompletedSet(weight = 60.0, reps = 5, weightUnit = WeightUnit.KG, isCompleted = true),
        )
        val record = findBestEpley(sets, "Test", Instant.now())
        assertNotNull(record)
        // 60 × (1 + 5/30) = 70.0
        assertEquals(70.0, record!!.value, 0.01)
    }

    // --- Helper functions (mirror the logic that RecordsRepositoryImpl will use) ---

    companion object {
        fun findHeaviestWeight(
            sets: List<CompletedSet>,
            exerciseName: String,
            date: Instant,
        ): PersonalRecord? {
            val best = sets
                .filter { it.isCompleted && it.weight != null && it.weight > 0 }
                .maxByOrNull { it.weight!! }
            return best?.let { PersonalRecord(it.weight!!, exerciseName, date) }
        }

        fun findHighestReps(
            sets: List<CompletedSet>,
            exerciseName: String,
            date: Instant,
        ): PersonalRecord? {
            val best = sets
                .filter { it.isCompleted && it.reps != null && it.reps > 0 }
                .maxByOrNull { it.reps!! }
            return best?.let { PersonalRecord(it.reps!!.toDouble(), exerciseName, date) }
        }

        fun findBestEpley(
            sets: List<CompletedSet>,
            exerciseName: String,
            date: Instant,
        ): PersonalRecord? {
            val best = sets
                .filter { it.isCompleted && it.weight != null && it.weight > 0 && it.reps != null && it.reps >= 1 }
                .maxByOrNull { WorkoutCalculators.estimatedOneRepMax(it.weight!!, it.reps!!) }
            return best?.let {
                PersonalRecord(
                    WorkoutCalculators.estimatedOneRepMax(it.weight!!, it.reps!!),
                    exerciseName,
                    date,
                )
            }
        }
    }
}
