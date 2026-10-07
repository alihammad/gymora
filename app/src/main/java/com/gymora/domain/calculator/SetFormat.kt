package com.gymora.domain.calculator

import com.gymora.domain.model.ActiveSet
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.Side
import com.gymora.domain.model.WeightUnit

/**
 * Text form of set values: parsing what users type and describing logged sets.
 * The single implementation shared by entry fields, history and exports.
 */
object SetFormat {

    private const val SECONDS_PER_MINUTE = 60
    private const val SECONDS_PER_HOUR = 3600

    /** "40.0" -> "40", "42.5" -> "42.5", null -> "". */
    fun number(value: Double?): String = when {
        value == null -> ""
        value % 1.0 == 0.0 -> value.toLong().toString()
        else -> "%.2f".format(java.util.Locale.ROOT, value).trimEnd('0').trimEnd('.')
    }

    /** 75 -> "1:15", 3725 -> "1:02:05", null -> "". */
    fun duration(seconds: Int?): String {
        if (seconds == null) return ""
        val h = seconds / SECONDS_PER_HOUR
        val m = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val s = seconds % SECONDS_PER_MINUTE
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /**
     * Parses "90" (seconds), "1:30" (m:ss) or "1:02:05" (h:mm:ss). Returns null for
     * blank or malformed text.
     */
    fun parseDuration(text: String): Int? {
        val parts = text.trim().split(':')
        if (parts.size > 3) return null
        val numbers = parts.map { part -> part.takeIf { it.all(Char::isDigit) }?.toIntOrNull() ?: return null }
        return numbers.fold(0) { total, part -> total * SECONDS_PER_MINUTE + part }
    }

    /** True while [text] can still become a valid duration (digits and up to two colons). */
    fun isDurationInput(text: String): Boolean =
        text.length <= 8 && text.all { it.isDigit() || it == ':' } && text.count { it == ':' } <= 2

    fun distanceUnitLabel(unit: WeightUnit): String = if (unit == WeightUnit.LB) "mi" else "km"

    fun distance(meters: Double?, unit: WeightUnit): String =
        meters?.let { number(WorkoutCalculators.metersToDisplay(it, unit)) }.orEmpty()
}

/** Display labels for sets: plain numbers, or per-side numbers for unilateral sets. */
object SetLabels {

    /** Label of each set in order: "1", "2"… or "1L", "1R", "2L"… for unilateral sets. */
    fun of(sets: List<ActiveSet>): List<String> {
        val perSide = mutableMapOf<Side, Int>()
        return sets.map { set ->
            val sideIndex = set.side?.let { side -> (perSide[side] ?: 0).plus(1).also { perSide[side] = it } } ?: 0
            label(set.setNumber, set.side, sideIndex)
        }
    }

    /** "1L", "1R", or the set number for two-sided sets. [sideIndex] counts sets on the same side. */
    fun label(setNumber: Int, side: Side?, sideIndex: Int): String =
        if (side == null) setNumber.toString() else "$sideIndex${side.shortLabel}"
}

/** One-line, human-readable summaries of logged or planned sets. */
object SetSummary {

    /**
     * One-line summary of a set, e.g. "60 kg × 8", "BW +10 kg × 6", "BW −20 kg × 8",
     * "1:30", "5 km in 25:00", "40 kg · 0.05 km". Missing values show as "—".
     */
    fun describe(
        type: MeasurementType,
        weight: Double?,
        weightUnit: WeightUnit?,
        reps: Int?,
        durationSeconds: Int?,
        distanceMeters: Double?,
        displayUnit: WeightUnit,
    ): String {
        val load = weight?.let { "${SetFormat.number(it)} ${(weightUnit ?: displayUnit).name.lowercase()}" }
        val repsText = reps?.toString() ?: MISSING
        val distanceText = distanceMeters?.let {
            "${SetFormat.distance(it, displayUnit)} ${SetFormat.distanceUnitLabel(displayUnit)}"
        }
        val time = SetFormat.duration(durationSeconds).ifEmpty { null }
        return when (type) {
            MeasurementType.WEIGHT_AND_REPS -> "${load ?: MISSING} × $repsText"
            MeasurementType.REPS_ONLY -> "$repsText reps"
            MeasurementType.WEIGHTED_BODYWEIGHT -> bodyweight("+", weight, load, repsText)
            MeasurementType.ASSISTED_BODYWEIGHT -> bodyweight("−", weight, load, repsText)
            MeasurementType.DURATION -> time ?: MISSING
            MeasurementType.DISTANCE_AND_DURATION -> joined(" ", distanceText, time?.let { "in $it" })
            MeasurementType.WEIGHT_AND_DISTANCE -> joined(" · ", load, distanceText)
        }
    }

    private const val MISSING = "—"

    /** "BW × 8", or "BW +10 kg × 8" with added load / "BW −20 kg × 8" with assistance. */
    private fun bodyweight(sign: String, weight: Double?, load: String?, repsText: String): String =
        if (load == null || weight == 0.0) "BW × $repsText" else "BW $sign$load × $repsText"

    private fun joined(separator: String, vararg parts: String?): String =
        parts.filterNotNull().joinToString(separator).ifEmpty { MISSING }
}
