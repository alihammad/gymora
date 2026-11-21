package com.gymora.ui.body

import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.BodyMetric
import com.gymora.domain.model.BodyMetricKind
import com.gymora.domain.model.WeightUnit
import java.util.Locale

/**
 * Display units for body measurements: metric (kg / cm) or imperial (lb / in),
 * following the app's weight unit. Caloric intake is always kcal.
 */
internal object BodyUnits {

    const val CM_PER_INCH = 2.54

    fun unitLabel(metric: BodyMetric, metricUnits: Boolean): String = when (metric.kind) {
        BodyMetricKind.WEIGHT -> if (metricUnits) "kg" else "lb"
        BodyMetricKind.LENGTH -> if (metricUnits) "cm" else "in"
        BodyMetricKind.ENERGY -> "kcal"
    }

    /** Form label, e.g. "Waist – 2 in above navel (in)". */
    fun fieldLabel(metric: BodyMetric, metricUnits: Boolean): String {
        val offset = if (metricUnits) "5 cm" else "2 in"
        val name = when (metric) {
            BodyMetric.WAIST_ABOVE_NAVEL -> "Waist – $offset above navel"
            BodyMetric.WAIST_BELOW_NAVEL -> "Waist – $offset below navel"
            BodyMetric.WAIST -> "Waist (at navel)"
            else -> metric.label
        }
        return "$name (${unitLabel(metric, metricUnits)})"
    }

    /** Stored value (kg / cm / kcal) → display unit. */
    fun toDisplay(metric: BodyMetric, stored: Double, metricUnits: Boolean): Double = when {
        metricUnits -> stored
        metric.kind == BodyMetricKind.WEIGHT -> WorkoutCalculators.convertWeight(stored, WeightUnit.KG, WeightUnit.LB)
        metric.kind == BodyMetricKind.LENGTH -> stored / CM_PER_INCH
        else -> stored
    }

    /** Display unit → stored value (kg / cm / kcal). */
    fun toStored(metric: BodyMetric, display: Double, metricUnits: Boolean): Double = when {
        metricUnits -> display
        metric.kind == BodyMetricKind.WEIGHT -> WorkoutCalculators.convertWeight(display, WeightUnit.LB, WeightUnit.KG)
        metric.kind == BodyMetricKind.LENGTH -> display * CM_PER_INCH
        else -> display
    }

    /** Text for a form field: one decimal, trailing ".0" dropped; whole kcal. */
    fun formText(metric: BodyMetric, stored: Double, metricUnits: Boolean): String {
        val v = toDisplay(metric, stored, metricUnits)
        return if (metric.kind == BodyMetricKind.ENERGY) {
            String.format(Locale.US, "%.0f", v)
        } else {
            String.format(Locale.US, "%.1f", v).removeSuffix(".0")
        }
    }

    /** Read-only value with its unit, e.g. "98.5 cm", "2,150 kcal". */
    fun formatted(metric: BodyMetric, stored: Double, metricUnits: Boolean): String {
        val v = toDisplay(metric, stored, metricUnits)
        val unit = unitLabel(metric, metricUnits)
        return if (metric.kind == BodyMetricKind.ENERGY) {
            String.format(Locale.US, "%,.0f %s", v, unit)
        } else {
            String.format(Locale.US, "%.1f %s", v, unit)
        }
    }

    /**
     * Parses a form field. Blank → [ParsedField.Blank]; a positive number (comma or
     * dot decimal) → [ParsedField.Value]; anything else → [ParsedField.Invalid].
     */
    fun parse(text: String): ParsedField {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ParsedField.Blank
        val value = trimmed.replace(',', '.').toDoubleOrNull()
        return if (value != null && value > 0 && value.isFinite()) ParsedField.Value(value) else ParsedField.Invalid
    }
}

internal sealed interface ParsedField {
    data object Blank : ParsedField
    data object Invalid : ParsedField
    data class Value(val value: Double) : ParsedField
}
