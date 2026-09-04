package com.gymora.domain

import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * T060 [US9]: convertWeight is lossless/reversible (kg→lb→kg returns original
 * typed value) using exact factor 0.45359237 (FR-049, R-04).
 */
class UnitConversionTest {

    @Test
    fun kgToLbAndBackIsLossless() {
        val original = 100.0
        val inLb = WorkoutCalculators.convertWeight(original, WeightUnit.KG, WeightUnit.LB)
        val backToKg = WorkoutCalculators.convertWeight(inLb, WeightUnit.LB, WeightUnit.KG)
        assertEquals(original, backToKg, 0.0001)
    }

    @Test
    fun lbToKgAndBackIsLossless() {
        val original = 225.0
        val inKg = WorkoutCalculators.convertWeight(original, WeightUnit.LB, WeightUnit.KG)
        val backToLb = WorkoutCalculators.convertWeight(inKg, WeightUnit.KG, WeightUnit.LB)
        assertEquals(original, backToLb, 0.0001)
    }

    @Test
    fun sameUnitReturnsSameValue() {
        assertEquals(100.0, WorkoutCalculators.convertWeight(100.0, WeightUnit.KG, WeightUnit.KG), 0.0)
        assertEquals(200.0, WorkoutCalculators.convertWeight(200.0, WeightUnit.LB, WeightUnit.LB), 0.0)
    }

    @Test
    fun zeroConvertsCorrectly() {
        assertEquals(0.0, WorkoutCalculators.convertWeight(0.0, WeightUnit.KG, WeightUnit.LB), 0.0)
        assertEquals(0.0, WorkoutCalculators.convertWeight(0.0, WeightUnit.LB, WeightUnit.KG), 0.0)
    }

    @Test
    fun knownConversionValues() {
        // 1 kg = 2.2046226218487757... lb
        val oneKgInLb = WorkoutCalculators.convertWeight(1.0, WeightUnit.KG, WeightUnit.LB)
        assertEquals(2.2046, oneKgInLb, 0.001)

        // 1 lb = 0.45359237 kg
        val oneLbInKg = WorkoutCalculators.convertWeight(1.0, WeightUnit.LB, WeightUnit.KG)
        assertEquals(0.4536, oneLbInKg, 0.001)
    }
}
