package com.gymora.domain

import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.CompletedSet
import com.gymora.domain.model.ValidationException
import com.gymora.domain.model.WeightUnit
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * T032 [US3]: calculator unit tests.
 * Volume = Σ(weight×reps) over completed weighted sets (BR-12);
 * duration/elapsed from timestamps (BR-13); input validation (BR-16).
 */
class WorkoutCalculatorsTest {

    // --- Volume (BR-12) ---

    @Test
    fun volumeSumsCompletedWeightedSets() {
        val sets = listOf(
            CompletedSet(weight = 60.0, reps = 10, weightUnit = WeightUnit.KG, isCompleted = true),
            CompletedSet(weight = 70.0, reps = 8, weightUnit = WeightUnit.KG, isCompleted = true),
        )
        // 60×10 + 70×8 = 600 + 560 = 1160
        assertEquals(1160.0, WorkoutCalculators.totalVolume(sets, WeightUnit.KG), 0.001)
    }

    @Test
    fun volumeIgnoresIncompleteSets() {
        val sets = listOf(
            CompletedSet(weight = 60.0, reps = 10, weightUnit = WeightUnit.KG, isCompleted = true),
            CompletedSet(weight = 70.0, reps = 8, weightUnit = WeightUnit.KG, isCompleted = false),
        )
        assertEquals(600.0, WorkoutCalculators.totalVolume(sets, WeightUnit.KG), 0.001)
    }

    @Test
    fun volumeIgnoresZeroWeightSets() {
        val sets = listOf(
            CompletedSet(weight = 0.0, reps = 20, weightUnit = WeightUnit.KG, isCompleted = true),
            CompletedSet(weight = 50.0, reps = 5, weightUnit = WeightUnit.KG, isCompleted = true),
        )
        assertEquals(250.0, WorkoutCalculators.totalVolume(sets, WeightUnit.KG), 0.001)
    }

    @Test
    fun volumeNormalizesUnitsToDisplayUnit() {
        val sets = listOf(
            CompletedSet(weight = 45.0, reps = 10, weightUnit = WeightUnit.LB, isCompleted = true),
        )
        // 45 lb = 20.41165... kg; volume in kg = 204.1165...
        val expected = 45.0 * 0.45359237 * 10
        assertEquals(expected, WorkoutCalculators.totalVolume(sets, WeightUnit.KG), 0.001)
    }

    // --- Duration (BR-13) ---

    @Test
    fun durationIsEndMinusStart() {
        val start = Instant.parse("2026-09-02T10:00:00Z")
        val end = Instant.parse("2026-09-02T10:45:30Z")
        val duration = WorkoutCalculators.duration(start, end)
        assertEquals(45 * 60 + 30L, duration.seconds)
    }

    @Test
    fun elapsedIsNowMinusStart() {
        val start = Instant.parse("2026-09-02T10:00:00Z")
        val now = Instant.parse("2026-09-02T10:05:00Z")
        val elapsed = WorkoutCalculators.elapsed(start, now)
        assertEquals(300L, elapsed.seconds)
    }

    // --- Input validation (BR-16) ---

    @Test
    fun negativeWeightIsRejected() {
        assertThrows(ValidationException::class.java) {
            WorkoutCalculators.validateSetInput(weight = -1.0, reps = 10)
        }
    }

    @Test
    fun negativeRepsIsRejected() {
        assertThrows(ValidationException::class.java) {
            WorkoutCalculators.validateSetInput(weight = 60.0, reps = -1)
        }
    }

    @Test
    fun zeroWeightIsAllowed() {
        WorkoutCalculators.validateSetInput(weight = 0.0, reps = 15)
    }

    @Test
    fun decimalWeightsAreAccepted() {
        WorkoutCalculators.validateSetInput(weight = 22.5, reps = 10)
        WorkoutCalculators.validateSetInput(weight = 2.5, reps = 12)
        WorkoutCalculators.validateSetInput(weight = 102.5, reps = 5)
    }
}
