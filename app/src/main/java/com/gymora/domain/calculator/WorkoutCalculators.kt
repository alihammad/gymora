package com.gymora.domain.calculator

import com.gymora.domain.model.CompletedSet
import com.gymora.domain.model.ValidationException
import com.gymora.domain.model.WeightUnit
import java.time.Duration
import java.time.Instant

/**
 * Core workout calculations — the single implementation of each business rule
 * (Constitution IX). Pure functions, unit-tested on the JVM.
 */
object WorkoutCalculators {

    /** Exact conversion factor (R-04). */
    const val KG_PER_LB = 0.45359237

    /**
     * BR-12: total volume = Σ (weight × reps) over completed weighted sets
     * (weight > 0). Weights are normalized to [displayUnit] before summing (R-04).
     */
    fun totalVolume(sets: List<CompletedSet>, displayUnit: WeightUnit): Double {
        return sets
            .filter { it.isCompleted }
            .filter { it.weight != null && it.weight > 0 && it.reps != null }
            .sumOf { set ->
                val normalized = convertWeight(set.weight!!, set.weightUnit ?: displayUnit, displayUnit)
                normalized * set.reps!!
            }
    }

    /** BR-13: duration = endedAt − startedAt (timestamps, never an in-memory counter). */
    fun duration(startedAt: Instant, endedAt: Instant): Duration =
        Duration.between(startedAt, endedAt)

    /** Live timer display (FR-021): now − startedAt. */
    fun elapsed(startedAt: Instant, now: Instant): Duration =
        Duration.between(startedAt, now)

    /**
     * R-09: Epley estimated 1RM = weight × (1 + reps/30); reps ≥ 1, weight > 0.
     * REPS_ONLY sets and zero-weight sets are excluded by callers.
     */
    fun estimatedOneRepMax(weight: Double, reps: Int): Double {
        require(reps >= 1) { "reps must be >= 1" }
        require(weight > 0) { "weight must be > 0" }
        return if (reps == 1) weight else weight * (1 + reps / 30.0)
    }

    /** R-04: lossless unit conversion using 1 lb = 0.45359237 kg. */
    fun convertWeight(value: Double, from: WeightUnit, to: WeightUnit): Double {
        if (from == to) return value
        return when {
            from == WeightUnit.LB && to == WeightUnit.KG -> value * KG_PER_LB
            from == WeightUnit.KG && to == WeightUnit.LB -> value / KG_PER_LB
            else -> value
        }
    }

    /**
     * BR-16: weight ≥ 0 (decimals allowed), reps ≥ 0 whole numbers;
     * negative values rejected; zero weight valid (bodyweight).
     */
    fun validateSetInput(weight: Double?, reps: Int?) {
        if (weight != null && weight < 0) {
            throw ValidationException("weight", "Weight must not be negative")
        }
        if (reps != null && reps < 0) {
            throw ValidationException("reps", "Reps must not be negative")
        }
    }
}
