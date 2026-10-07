package com.gymora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymora.domain.calculator.SetFormat
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SetField
import com.gymora.domain.model.WeightUnit

/** The user's preferred weight unit; also picks km or miles for distances. */
val LocalWeightUnit = staticCompositionLocalOf { WeightUnit.KG }

/** Parsed values of one set as entered; distance is in metres. */
data class SetEntry(
    val weight: Double? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
)

/**
 * The value fields a set of [type] records: weight/reps, time, distance, or a mix.
 * The fields own their text while typing (binding to a database round-trip drops
 * keystrokes), keyed by [key]; every valid edit reports the parsed [SetEntry].
 * [hint] shows as placeholders, e.g. last session's values.
 */
@Composable
fun SetInputRow(
    key: Any,
    type: MeasurementType,
    initial: SetEntry,
    onChange: (SetEntry) -> Unit,
    modifier: Modifier = Modifier,
    hint: SetEntry? = null,
) {
    val unit = LocalWeightUnit.current
    var weightText by remember(key) { mutableStateOf(SetFormat.number(initial.weight)) }
    var repsText by remember(key) { mutableStateOf(initial.reps?.toString().orEmpty()) }
    var durationText by remember(key) { mutableStateOf(SetFormat.duration(initial.durationSeconds)) }
    var distanceText by remember(key) {
        mutableStateOf(initial.distanceMeters?.let { SetFormat.distance(it, unit) }.orEmpty())
    }

    fun report() = onChange(
        SetEntry(
            weight = weightText.toDoubleOrNull(),
            reps = repsText.toIntOrNull(),
            durationSeconds = SetFormat.parseDuration(durationText),
            distanceMeters = distanceText.toDoubleOrNull()?.let { WorkoutCalculators.displayToMeters(it, unit) },
        ),
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        type.fields.forEach { field ->
            when (field) {
                SetField.WEIGHT -> ValueField(
                    value = weightText,
                    label = type.weightLabel,
                    placeholder = SetFormat.number(hint?.weight),
                    keyboardType = KeyboardType.Decimal,
                    accept = ::isDecimalInput,
                    onValueChange = { weightText = it.replace(',', '.'); report() },
                )
                SetField.REPS -> ValueField(
                    value = repsText,
                    label = "Reps",
                    placeholder = hint?.reps?.toString().orEmpty(),
                    keyboardType = KeyboardType.Number,
                    accept = { it.length <= 4 && it.all(Char::isDigit) },
                    onValueChange = { repsText = it; report() },
                )
                SetField.DURATION -> ValueField(
                    value = durationText,
                    label = "Time",
                    placeholder = SetFormat.duration(hint?.durationSeconds).ifEmpty { "m:ss" },
                    // The phone keypad offers ':' only on some keyboards; plain seconds also work.
                    keyboardType = KeyboardType.Phone,
                    accept = SetFormat::isDurationInput,
                    onValueChange = { durationText = it; report() },
                )
                SetField.DISTANCE -> ValueField(
                    value = distanceText,
                    label = SetFormat.distanceUnitLabel(unit),
                    placeholder = SetFormat.distance(hint?.distanceMeters, unit),
                    keyboardType = KeyboardType.Decimal,
                    accept = ::isDecimalInput,
                    onValueChange = { distanceText = it.replace(',', '.'); report() },
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ValueField(
    value: String,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    accept: (String) -> Boolean,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (accept(input.replace(',', '.'))) onValueChange(input) },
        label = { Text(label) },
        placeholder = placeholder.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.weight(1f),
    )
}

/** Digits with at most one decimal point and at most 6 characters. */
private fun isDecimalInput(text: String): Boolean =
    text.length <= 6 && text.all { it.isDigit() || it == '.' } && text.count { it == '.' } <= 1
