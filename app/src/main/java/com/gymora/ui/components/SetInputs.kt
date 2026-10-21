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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
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
                SetField.REPS -> RepsField(
                    reps = repsText.toIntOrNull(),
                    hint = hint?.reps,
                    large = large,
                    onRepsChange = { repsText = it.toString(); report() },
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

/** Reps entry: a tappable field that opens a wheel picker, 1..[MAX_REPS]; empty until the user picks. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.RepsField(
    reps: Int?,
    hint: Int?,
    large: Boolean,
    onRepsChange: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = if (large) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = "REPS",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (large) 64.dp else 56.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable { open = true },
        ) {
            val style = if (large) DisplayMetric else MaterialTheme.typography.titleMedium
            if (reps != null) {
                Text(reps.toString(), style = style)
            } else {
                Text(
                    hint?.toString() ?: "—",
                    style = style,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
    if (open) {
        RepsPickerDialog(
            initial = reps ?: hint ?: DEFAULT_REPS,
            onDismiss = { open = false },
            onUpdate = { onRepsChange(it); open = false },
        )
    }
}

@Composable
private fun RepsPickerDialog(initial: Int, onDismiss: () -> Unit, onUpdate: (Int) -> Unit) {
    val itemHeight = 44.dp
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initial.coerceIn(1, MAX_REPS) - 1)
    // Rows are padded by two items above and below, so the snapped-to row sits in the middle.
    val selected by remember {
        derivedStateOf {
            val offsetRows = if (listState.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
            (listState.firstVisibleItemIndex + offsetRows + 1).coerceIn(1, MAX_REPS)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                Text("Reps", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.height(itemHeight * 5), contentAlignment = Alignment.Center) {
                    LazyColumn(
                        state = listState,
                        flingBehavior = rememberSnapFlingBehavior(listState),
                        contentPadding = PaddingValues(vertical = itemHeight * 2),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(MAX_REPS) { index ->
                            val value = index + 1
                            val distance = abs(value - selected)
                            Box(
                                modifier = Modifier.fillMaxWidth().height(itemHeight),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = value.toString(),
                                    style = if (distance == 0) {
                                        MaterialTheme.typography.headlineMedium
                                    } else {
                                        MaterialTheme.typography.titleLarge
                                    },
                                    fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = when (distance) { 0 -> 1f; 1 -> 0.35f; else -> 0.15f },
                                    ),
                                )
                            }
                        }
                    }
                    // Lines framing the selected row.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(modifier = Modifier.padding(top = itemHeight * 2), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(itemHeight))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onUpdate(selected) }) { Text("Update") }
            }
        }
    }
}

private const val MAX_REPS = 100
private const val DEFAULT_REPS = 8

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
