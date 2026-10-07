package com.gymora.domain

import com.gymora.domain.calculator.SetFormat
import com.gymora.domain.calculator.SetSummary
import com.gymora.domain.calculator.SetLabels
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.CompletedSet
import com.gymora.domain.model.ExerciseCategory
import com.gymora.domain.model.ExerciseTrackingDefaults
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.Side
import com.gymora.domain.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetFormatTest {

    @Test
    fun parsesSecondsAndClockTimes() {
        assertEquals(90, SetFormat.parseDuration("90"))
        assertEquals(90, SetFormat.parseDuration("1:30"))
        assertEquals(3725, SetFormat.parseDuration("1:02:05"))
        assertNull(SetFormat.parseDuration(""))
        assertNull(SetFormat.parseDuration("1:"))
        assertNull(SetFormat.parseDuration("1:2:3:4"))
        assertNull(SetFormat.parseDuration("a"))
    }

    @Test
    fun formatsDurations() {
        assertEquals("0:45", SetFormat.duration(45))
        assertEquals("1:30", SetFormat.duration(90))
        assertEquals("1:02:05", SetFormat.duration(3725))
        assertEquals("", SetFormat.duration(null))
    }

    @Test
    fun durationInputAllowsPartialEntry() {
        assertTrue(SetFormat.isDurationInput("1:"))
        assertTrue(SetFormat.isDurationInput("1:02:0"))
        assertFalse(SetFormat.isDurationInput("1:02:03:"))
        assertFalse(SetFormat.isDurationInput("1.5"))
    }

    @Test
    fun distanceFollowsTheWeightUnit() {
        assertEquals("5", SetFormat.distance(5000.0, WeightUnit.KG))
        assertEquals("1", SetFormat.distance(1609.344, WeightUnit.LB))
        assertEquals(1609.344, WorkoutCalculators.displayToMeters(1.0, WeightUnit.LB), 1e-9)
    }

    @Test
    fun describesEachMeasurementType() {
        fun describe(
            type: MeasurementType,
            weight: Double? = null,
            reps: Int? = null,
            secs: Int? = null,
            m: Double? = null,
        ) = SetSummary.describe(type, weight, WeightUnit.KG, reps, secs, m, WeightUnit.KG)

        assertEquals("60 kg × 8", describe(MeasurementType.WEIGHT_AND_REPS, 60.0, 8))
        assertEquals("12 reps", describe(MeasurementType.REPS_ONLY, reps = 12))
        assertEquals("BW +10 kg × 6", describe(MeasurementType.WEIGHTED_BODYWEIGHT, 10.0, 6))
        assertEquals("BW × 6", describe(MeasurementType.WEIGHTED_BODYWEIGHT, null, 6))
        assertEquals("BW −20 kg × 8", describe(MeasurementType.ASSISTED_BODYWEIGHT, 20.0, 8))
        assertEquals("1:30", describe(MeasurementType.DURATION, secs = 90))
        assertEquals("5 km in 25:00", describe(MeasurementType.DISTANCE_AND_DURATION, secs = 1500, m = 5000.0))
        assertEquals("40 kg · 0.05 km", describe(MeasurementType.WEIGHT_AND_DISTANCE, 40.0, m = 50.0))
        assertEquals("—", describe(MeasurementType.DURATION))
    }

    @Test
    fun labelsUnilateralSetsPerSide() {
        assertEquals("3", SetLabels.label(3, null, 0))
        assertEquals("2L", SetLabels.label(3, Side.LEFT, 2))
    }

    @Test
    fun volumeSkipsAssistance() {
        val sets = listOf(
            CompletedSet(100.0, 5, WeightUnit.KG, true, MeasurementType.WEIGHT_AND_REPS),
            CompletedSet(20.0, 10, WeightUnit.KG, true, MeasurementType.ASSISTED_BODYWEIGHT),
            CompletedSet(10.0, 10, WeightUnit.KG, true, MeasurementType.WEIGHTED_BODYWEIGHT),
            CompletedSet(50.0, 5, WeightUnit.KG, true),
        )
        assertEquals(500.0 + 100.0 + 250.0, WorkoutCalculators.totalVolume(sets, WeightUnit.KG), 0.0)
    }

    @Test
    fun guessesTrackingForLibraryExercises() {
        fun type(
            name: String,
            category: ExerciseCategory? = ExerciseCategory.STRENGTH,
            equipment: List<String> = listOf("Barbell"),
        ) = ExerciseTrackingDefaults.measurementType(name, category, equipment)

        assertEquals(MeasurementType.DISTANCE_AND_DURATION, type("Running, Treadmill", ExerciseCategory.CARDIO))
        assertEquals(MeasurementType.DURATION, type("Child's Pose", ExerciseCategory.STRETCHING))
        assertEquals(MeasurementType.DURATION, type("Plank", equipment = listOf("Body Only")))
        assertEquals(MeasurementType.WEIGHT_AND_DISTANCE, type("Farmer's Walk", ExerciseCategory.STRONGMAN))
        assertEquals(MeasurementType.ASSISTED_BODYWEIGHT, type("Band Assisted Pull-Up", equipment = listOf("Other")))
        assertEquals(MeasurementType.WEIGHTED_BODYWEIGHT, type("Chin-Up", equipment = listOf("Body Only")))
        assertEquals(MeasurementType.WEIGHT_AND_REPS, type("Dip Machine", equipment = listOf("Machine")))
        assertEquals(MeasurementType.REPS_ONLY, type("Crunches", equipment = listOf("Body Only")))
        assertEquals(MeasurementType.WEIGHT_AND_REPS, type("Barbell Squat"))

        assertTrue(ExerciseTrackingDefaults.isUnilateral("One-Arm Kettlebell Row"))
        assertTrue(ExerciseTrackingDefaults.isUnilateral("Single Leg Glute Bridge"))
        assertFalse(ExerciseTrackingDefaults.isUnilateral("Alternating Deltoid Raise"))
    }
}
