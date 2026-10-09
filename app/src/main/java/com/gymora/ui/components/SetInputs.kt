package com.gymora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextAlign
import com.gymora.ui.theme.DisplayMetric
import com.gymora.ui.theme.LabelCaps
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
    large: Boolean = false,
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
                    large = large,
                    accept = ::isDecimalInput,
                    onValueChange = { weightText = it.replace(',', '.'); report() },
                )
                SetField.REPS -> ValueField(
                    value = repsText,
                    label = "Reps",
                    placeholder = hint?.reps?.toString().orEmpty(),
                    keyboardType = KeyboardType.Number,
                    large = large,
                    accept = { it.length <= 4 && it.all(Char::isDigit) },
                    onValueChange = { repsText = it; report() },
                )
                SetField.DURATION -> ValueField(
                    value = durationText,
                    label = "Time",
                    placeholder = SetFormat.duration(hint?.durationSeconds).ifEmpty { "m:ss" },
                    // The phone keypad offers ':' only on some keyboards; plain seconds also work.
                    keyboardType = KeyboardType.Phone,
                    large = large,
                    accept = SetFormat::isDurationInput,
                    onValueChange = { durationText = it; report() },
                )
                SetField.DISTANCE -> ValueField(
                    value = distanceText,
                    label = SetFormat.distanceUnitLabel(unit),
                    placeholder = SetFormat.distance(hint?.distanceMeters, unit),
                    keyboardType = KeyboardType.Decimal,
                    large = large,
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
    large: Boolean,
    accept: (String) -> Boolean,
    onValueChange: (String) -> Unit,
) {
    val change: (String) -> Unit = { input -> if (accept(input.replace(',', '.'))) onValueChange(input) }
    val options = KeyboardOptions(keyboardType = keyboardType)
    if (large) {
        // Prominent entry: caption above, big centered number.
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label.uppercase(),
                style = LabelCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            OutlinedTextField(
                value = value,
                onValueChange = change,
                textStyle = DisplayMetric.copy(textAlign = TextAlign.Center),
                placeholder = placeholder.takeIf { it.isNotEmpty() }?.let {
                    { Text(it, style = DisplayMetric.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth()) }
                },
                singleLine = true,
                keyboardOptions = options,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = change,
            label = { Text(label) },
            placeholder = placeholder.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = options,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Digits with at most one decimal point and at most 6 characters. */
private fun isDecimalInput(text: String): Boolean =
    text.length <= 6 && text.all { it.isDigit() || it == '.' } && text.count { it == '.' } <= 1
