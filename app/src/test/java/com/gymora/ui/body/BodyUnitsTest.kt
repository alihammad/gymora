package com.gymora.ui.body

import com.gymora.domain.model.BodyMetric
import org.junit.Assert.assertEquals
import org.junit.Test

class BodyUnitsTest {

    @Test
    fun lengthsConvertBetweenCmAndInches() {
        assertEquals(2.54, BodyUnits.toStored(BodyMetric.NECK, 1.0, metricUnits = false), 1e-9)
        assertEquals("15", BodyUnits.formText(BodyMetric.NECK, 38.1, metricUnits = false))
        assertEquals("38.1 cm", BodyUnits.formatted(BodyMetric.NECK, 38.1, metricUnits = true))
    }

    @Test
    fun caloriesAreNeverConverted() {
        assertEquals(2500.0, BodyUnits.toStored(BodyMetric.CALORIES, 2500.0, metricUnits = false), 0.0)
        assertEquals("2,500 kcal", BodyUnits.formatted(BodyMetric.CALORIES, 2500.0, metricUnits = false))
        assertEquals("2500", BodyUnits.formText(BodyMetric.CALORIES, 2500.0, metricUnits = true))
    }

    @Test
    fun parseAcceptsCommaDecimalsAndRejectsNonPositive() {
        assertEquals(ParsedField.Value(80.5), BodyUnits.parse(" 80,5 "))
        assertEquals(ParsedField.Blank, BodyUnits.parse("  "))
        assertEquals(ParsedField.Invalid, BodyUnits.parse("0"))
        assertEquals(ParsedField.Invalid, BodyUnits.parse("abc"))
    }

    @Test
    fun waistLabelsFollowTheUnit() {
        assertEquals("Waist – 2 in above navel (in)", BodyUnits.fieldLabel(BodyMetric.WAIST_ABOVE_NAVEL, false))
        assertEquals("Waist – 5 cm below navel (cm)", BodyUnits.fieldLabel(BodyMetric.WAIST_BELOW_NAVEL, true))
    }
}
