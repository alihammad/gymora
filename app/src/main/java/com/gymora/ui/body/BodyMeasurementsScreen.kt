package com.gymora.ui.body

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.BodyMetric
import com.gymora.domain.model.LatestMeasurements
import com.gymora.ui.components.Button
import com.gymora.ui.components.Card
import com.gymora.ui.components.ChartPoint
import com.gymora.ui.components.ConfirmDialog
import com.gymora.ui.components.OutlinedButton
import com.gymora.ui.components.OutlinedTextField
import com.gymora.ui.components.ProgressChartCard
import com.gymora.ui.components.SnackbarHost
import com.gymora.ui.components.TextButton
import com.gymora.ui.components.TopAppBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * Body measurements: the latest value of every metric, the weight trend, a form that
 * records one date at a time, and the full history by date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMeasurementsScreen(
    onBack: () -> Unit,
    viewModel: BodyMeasurementsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var formTop by remember { mutableFloatStateOf(0f) }

    state.message?.let { message ->
        LaunchedEffect(message) {
            snackbar.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body measurements") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BodyFigureCard(state.latest, state.metricUnits)
            LimbsCard(state.latest, state.metricUnits)

            if (state.weightHistory.size > 1) {
                val fmt = remember { DateTimeFormatter.ofPattern("d MMM") }
                ProgressChartCard(
                    title = "Weight",
                    unit = BodyUnits.unitLabel(BodyMetric.WEIGHT, state.metricUnits),
                    points = state.weightHistory.map { p ->
                        ChartPoint(fmt.format(p.date), BodyUnits.toDisplay(BodyMetric.WEIGHT, p.value, state.metricUnits))
                    },
                )
            }

            Box(Modifier.onGloballyPositioned { formTop = it.positionInParent().y }) {
                MeasurementFormCard(state, viewModel)
            }

            HistoryCard(
                state = state,
                onEdit = { date ->
                    viewModel.onDateSelected(date)
                    scope.launch { scroll.animateScrollTo(formTop.toInt()) }
                },
                onDelete = viewModel::onDeleteDay,
            )
        }
    }
}

// --- Latest: figure -----------------------------------------------------

/** A circumference ring on the figure: vertical position (0..1 of height) and width (fraction of figure width). */
private data class Ring(val metric: BodyMetric, val label: String, val y: Float, val width: Float)

private val RINGS = listOf(
    Ring(BodyMetric.NECK, "Neck", 0.13f, 0.18f),
    Ring(BodyMetric.SHOULDERS, "Shoulders", 0.195f, 1.00f),
    Ring(BodyMetric.CHEST, "Chest", 0.26f, 0.80f),
    Ring(BodyMetric.WAIST_ABOVE_NAVEL, "Above navel", 0.325f, 0.68f),
    Ring(BodyMetric.WAIST, "Waist", 0.39f, 0.64f),
    Ring(BodyMetric.WAIST_BELOW_NAVEL, "Below navel", 0.455f, 0.70f),
    Ring(BodyMetric.HIPS, "Hips", 0.52f, 0.74f),
)

private val FIGURE_HEIGHT = 400.dp
private val FIGURE_WIDTH: Dp = 110.dp

@Composable
private fun BodyFigureCard(latest: LatestMeasurements, metricUnits: Boolean) {
    val rings = RINGS.filter { latest[it.metric] != null }
    val summary = buildString {
        append("Latest measurements. ")
        listOf(BodyMetric.WEIGHT, BodyMetric.CALORIES).forEach { m ->
            latest[m]?.let { append("${m.label} ${BodyUnits.formatted(m, it, metricUnits)}. ") }
        }
        rings.forEach { append("${it.metric.label} ${BodyUnits.formatted(it.metric, latest[it.metric]!!, metricUnits)}. ") }
    }

    Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = summary }) {
        if (latest.date == null) {
            Text(
                "No measurements yet. Add some below and they will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(20.dp),
            )
            return@Card
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat(BodyMetric.WEIGHT, latest, metricUnits)
                Stat(BodyMetric.CALORIES, latest, metricUnits)
            }
            if (rings.isNotEmpty()) BodyFigure(rings, latest, metricUnits)
            Text(
                "Last entry ${formatDate(latest.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Stat(metric: BodyMetric, latest: LatestMeasurements, metricUnits: Boolean) {
    val value = latest[metric] ?: return
    Column {
        Text(metric.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(BodyUnits.formatted(metric, value, metricUnits), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun BodyFigure(rings: List<Ring>, latest: LatestMeasurements, metricUnits: Boolean) {
    val body = MaterialTheme.colorScheme.surfaceVariant
    val ringColor = MaterialTheme.colorScheme.primary
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(FIGURE_HEIGHT)) {
        val h = maxHeight
        val labelWidth = maxWidth - FIGURE_WIDTH - 12.dp
        Canvas(modifier = Modifier.width(FIGURE_WIDTH).height(h)) {
            val w = size.width
            val ht = size.height
            val cx = w / 2
            val r = CornerRadius(w * 0.1f)

            // head + neck
            drawCircle(body, radius = ht * 0.05f, center = Offset(cx, ht * 0.065f))
            drawRoundRect(body, Offset(cx - w * 0.06f, ht * 0.105f), Size(w * 0.12f, ht * 0.06f), r)
            // torso
            drawRoundRect(body, Offset(w * 0.14f, ht * 0.16f), Size(w * 0.72f, ht * 0.38f), CornerRadius(w * 0.16f))
            // arms
            drawRoundRect(body, Offset(0f, ht * 0.18f), Size(w * 0.13f, ht * 0.34f), CornerRadius(w * 0.07f))
            drawRoundRect(body, Offset(w * 0.87f, ht * 0.18f), Size(w * 0.13f, ht * 0.34f), CornerRadius(w * 0.07f))
            // legs
            drawRoundRect(body, Offset(w * 0.17f, ht * 0.50f), Size(w * 0.31f, ht * 0.48f), CornerRadius(w * 0.1f))
            drawRoundRect(body, Offset(w * 0.52f, ht * 0.50f), Size(w * 0.31f, ht * 0.48f), CornerRadius(w * 0.1f))

            // only measurements that have a value get a ring
            rings.forEach { ring ->
                val rw = w * ring.width
                drawOval(
                    color = ringColor,
                    topLeft = Offset(cx - rw / 2, ht * ring.y - ht * 0.012f),
                    size = Size(rw, ht * 0.024f),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
        rings.forEach { ring ->
            Row(
                modifier = Modifier
                    .offset(x = FIGURE_WIDTH + 12.dp, y = h * ring.y - 10.dp)
                    .width(labelWidth),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    ring.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    BodyUnits.formatted(ring.metric, latest[ring.metric]!!, metricUnits),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// --- Latest: limbs ------------------------------------------------------

private val LIMBS = listOf(
    Triple("Bicep", BodyMetric.LEFT_BICEP, BodyMetric.RIGHT_BICEP),
    Triple("Arm", BodyMetric.LEFT_ARM, BodyMetric.RIGHT_ARM),
    Triple("Thigh", BodyMetric.LEFT_THIGH, BodyMetric.RIGHT_THIGH),
    Triple("Calf", BodyMetric.LEFT_CALF, BodyMetric.RIGHT_CALF),
)

/** Left/right limb measurements side by side; hidden until one is recorded. */
@Composable
private fun LimbsCard(latest: LatestMeasurements, metricUnits: Boolean) {
    val rows = LIMBS.filter { (_, l, r) -> latest[l] != null || latest[r] != null }
    if (rows.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Arms and legs", style = MaterialTheme.typography.titleMedium)
            TableRow("", "Left", "Right", header = true)
            rows.forEach { (name, l, r) ->
                TableRow(
                    name,
                    latest[l]?.let { BodyUnits.formatted(l, it, metricUnits) } ?: "–",
                    latest[r]?.let { BodyUnits.formatted(r, it, metricUnits) } ?: "–",
                )
            }
        }
    }
}

@Composable
private fun TableRow(name: String, left: String, right: String, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth()) {
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(left, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(right, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

// --- Form ---------------------------------------------------------------

private val FORM_SECTIONS = listOf(
    "General" to listOf(listOf(BodyMetric.WEIGHT), listOf(BodyMetric.CALORIES)),
    "Upper body" to listOf(
        listOf(BodyMetric.NECK),
        listOf(BodyMetric.SHOULDERS),
        listOf(BodyMetric.CHEST),
        listOf(BodyMetric.LEFT_BICEP, BodyMetric.RIGHT_BICEP),
        listOf(BodyMetric.LEFT_ARM, BodyMetric.RIGHT_ARM),
    ),
    "Waist" to listOf(
        listOf(BodyMetric.WAIST_ABOVE_NAVEL),
        listOf(BodyMetric.WAIST),
        listOf(BodyMetric.WAIST_BELOW_NAVEL),
    ),
    "Lower body" to listOf(
        listOf(BodyMetric.HIPS),
        listOf(BodyMetric.LEFT_THIGH, BodyMetric.RIGHT_THIGH),
        listOf(BodyMetric.LEFT_CALF, BodyMetric.RIGHT_CALF),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasurementFormCard(state: BodyMeasurementsUiState, viewModel: BodyMeasurementsViewModel) {
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val latest = state.latest

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (state.dateHasEntry) "Edit measurements" else "Add measurements",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDay(state.selectedDate), style = MaterialTheme.typography.bodyLarge)
                }
                OutlinedButton(onClick = { pickDate = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Text("Change", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(
                if (state.dateHasEntry) {
                    "Already recorded on this date. Saving replaces it; clear a field to remove that value."
                } else {
                    "Fill in only what you measured. Grey numbers are your latest values."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FORM_SECTIONS.forEach { (title, rows) ->
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { metric ->
                            MetricField(
                                metric = metric,
                                state = state,
                                hint = latest[metric]?.let { BodyUnits.formText(metric, it, state.metricUnits) },
                                onChange = { viewModel.onFieldChanged(metric, it) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Button(
                onClick = viewModel::onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp),
            ) {
                Text(if (state.dateHasEntry) "Update ${formatDay(state.selectedDate)}" else "Save measurements")
            }
        }
    }

    if (pickDate) {
        val today = LocalDate.now()
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)

                override fun isSelectableYear(year: Int) = year <= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        viewModel.onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = picker) }
    }
}

@Composable
private fun MetricField(
    metric: BodyMetric,
    state: BodyMeasurementsUiState,
    hint: String?,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val invalid = metric in state.invalid
    OutlinedTextField(
        value = state.form[metric].orEmpty(),
        onValueChange = onChange,
        label = { Text(BodyUnits.fieldLabel(metric, state.metricUnits)) },
        placeholder = hint?.let { { Text(it) } },
        isError = invalid,
        supportingText = if (invalid) ({ Text("Enter a positive number") }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
    )
}

// --- History ------------------------------------------------------------

@Composable
private fun HistoryCard(
    state: BodyMeasurementsUiState,
    onEdit: (LocalDate) -> Unit,
    onDelete: (BodyHistoryDay) -> Unit,
) {
    if (state.history.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<BodyHistoryDay?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(
                "History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            state.history.forEachIndexed { index, day ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                val key = day.date.toString()
                HistoryDayRow(
                    day = day,
                    metricUnits = state.metricUnits,
                    expanded = expanded == key,
                    onToggle = { expanded = if (expanded == key) null else key },
                    onEdit = { onEdit(day.date) },
                    onDelete = { confirmDelete = day },
                )
            }
        }
    }

    confirmDelete?.let { day ->
        ConfirmDialog(
            title = "Delete ${formatDay(day.date)}?",
            message = "All measurements recorded on this date will be removed. This cannot be undone.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = {
                onDelete(day)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun HistoryDayRow(
    day: BodyHistoryDay,
    metricUnits: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val count = day.values.size
    val headline = listOf(BodyMetric.WEIGHT, BodyMetric.WAIST, BodyMetric.CHEST)
        .firstNotNullOfOrNull { m -> day.values[m]?.let { "${m.label} ${BodyUnits.formatted(m, it, metricUnits)}" } }
        ?: day.values.entries.first().let { (m, v) -> "${m.label} ${BodyUnits.formatted(m, v, metricUnits)}" }

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .animateContentSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formatDay(day.date), style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (count > 1) "$headline · ${count - 1} more" else headline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
            )
        }
        if (expanded) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BodyMetric.entries.forEach { m ->
                    val v = day.values[m] ?: return@forEach
                    Row {
                        Text(m.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(BodyUnits.formatted(m, v, metricUnits), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (day.ownIds.isEmpty()) {
                    Text(
                        "Imported from Health Connect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    if (day.ownIds.isNotEmpty()) TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
        }
    }
}

// --- Formatting ---------------------------------------------------------

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

private fun formatDay(date: LocalDate): String =
    if (date == LocalDate.now()) "Today" else DAY_FORMAT.format(date)

private fun formatDate(instant: Instant): String = formatDay(instant.atZone(ZoneId.systemDefault()).toLocalDate())
