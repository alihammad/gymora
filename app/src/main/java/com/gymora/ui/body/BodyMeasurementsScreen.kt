package com.gymora.ui.body

import com.gymora.ui.theme.GymoraShapes
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.gymora.ui.components.Button
import com.gymora.ui.components.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.gymora.ui.components.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.gymora.ui.components.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.gymora.ui.components.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.LatestMeasurements
import com.gymora.domain.model.WeightUnit
import com.gymora.ui.components.ChartPoint
import com.gymora.ui.components.ProgressChartCard
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Body measurements: log weight and circumferences, then see the latest
 * values on a body figure. Measurements left blank are not drawn at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMeasurementsScreen(
    onBack: () -> Unit,
    viewModel: BodyMeasurementsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BodyFigureCard(state.latest, state.metric)

            if (state.weightHistory.size > 1) {
                val fmt = remember { DateTimeFormatter.ofPattern("d MMM") }
                ProgressChartCard(
                    title = "Weight",
                    unit = if (state.metric) "kg" else "lb",
                    points = state.weightHistory.map { (date, kg) ->
                        ChartPoint(
                            fmt.format(date.atZone(ZoneId.systemDefault())),
                            if (state.metric) kg else WorkoutCalculators.convertWeight(kg, WeightUnit.KG, WeightUnit.LB),
                        )
                    },
                )
            }

            MeasurementFormCard(state, viewModel)
        }
    }
}

// --- Figure -------------------------------------------------------------

/** A circumference ring on the figure: vertical position (0..1 of height) and width (fraction of shoulder width). */
private data class Ring(val label: String, val valueCm: Double?, val y: Float, val width: Float)

@Composable
private fun BodyFigureCard(latest: LatestMeasurements, metric: Boolean) {
    val rings = listOf(
        Ring("Shoulders", latest.shouldersCm, 0.19f, 1.00f),
        Ring("Chest", latest.chestCm, 0.26f, 0.80f),
        Ring("Above navel", latest.aboveNavelCm, 0.33f, 0.68f),
        Ring("Navel", latest.navelCm, 0.40f, 0.64f),
        Ring("Below navel", latest.belowNavelCm, 0.47f, 0.70f),
        Ring("Thighs", latest.thighCm, 0.64f, 0.66f),
    ).filter { it.valueCm != null }

    val dateText = latest.date?.let {
        DateTimeFormatter.ofPattern("dd/MM/yyyy").format(it.atZone(ZoneId.systemDefault()))
    }
    val summary = buildString {
        append("Body figure. ")
        latest.weightKg?.let { append("Weight ${formatWeight(it, metric)}. ") }
        rings.forEach { append("${it.label} ${formatLength(it.valueCm!!, metric)}. ") }
    }

    Card(
        shape = GymoraShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = summary },
    ) {
        if (latest.date == null) {
            Text(
                "No measurements yet. Add some below and they will appear on the figure.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(20.dp),
            )
            return@Card
        }
        Row(modifier = Modifier.padding(20.dp)) {
            Column(
                modifier = Modifier.weight(1f).height(FIGURE_HEIGHT),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (latest.weightKg != null) {
                    Column {
                        Text("Weight", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatWeight(latest.weightKg, metric), style = MaterialTheme.typography.titleLarge)
                    }
                } else {
                    Box {}
                }
                Text(dateText.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BodyFigure(rings = rings, metric = metric, modifier = Modifier.weight(1.7f).height(FIGURE_HEIGHT))
        }
    }
}

private val FIGURE_HEIGHT = 360.dp
private val FIGURE_WIDTH: Dp = 110.dp

@Composable
private fun BodyFigure(rings: List<Ring>, metric: Boolean, modifier: Modifier = Modifier) {
    val body = MaterialTheme.colorScheme.surfaceVariant
    val ringColor = MaterialTheme.colorScheme.primary
    BoxWithConstraints(modifier = modifier) {
        val h = maxHeight
        Canvas(modifier = Modifier.width(FIGURE_WIDTH).height(h)) {
            val w = size.width
            val ht = size.height
            val cx = w / 2
            val r = CornerRadius(w * 0.1f)

            // head + neck
            drawCircle(body, radius = ht * 0.05f, center = Offset(cx, ht * 0.065f))
            drawRoundRect(body, Offset(cx - w * 0.06f, ht * 0.105f), Size(w * 0.12f, ht * 0.06f), r)
            // torso
            drawRoundRect(body, Offset(w * 0.14f, ht * 0.16f), Size(w * 0.72f, ht * 0.36f), CornerRadius(w * 0.16f))
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
            Column(
                modifier = Modifier
                    .offset(x = FIGURE_WIDTH + 8.dp, y = h * ring.y - 18.dp),
            ) {
                Text(ring.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatLength(ring.valueCm!!, metric), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// --- Form ---------------------------------------------------------------

@Composable
private fun MeasurementFormCard(state: BodyMeasurementsUiState, viewModel: BodyMeasurementsViewModel) {
    val f = state.form
    val len = if (state.metric) "cm" else "in"
    val two = if (state.metric) "5 cm" else "2 in"
    val latest = state.latest

    @Composable
    fun field(label: String, value: String, hint: Double?, onChange: (String) -> Unit) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            placeholder = hint?.let { { Text(it.toString()) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    fun cm(v: Double?): Double? = v?.let { if (state.metric) round1(it) else round1(it / BodyMeasurementsViewModel.CM_PER_INCH) }

    Card(
        shape = GymoraShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add measurements", style = MaterialTheme.typography.titleMedium)
            Text(
                "Fill in only what you measured. Anything left blank is not shown on the figure.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            field("Weight (${if (state.metric) "kg" else "lb"})", f.weight,
                latest.weightKg?.let { if (state.metric) round1(it) else round1(WorkoutCalculators.convertWeight(it, WeightUnit.KG, WeightUnit.LB)) }) {
                viewModel.onFormChanged(f.copy(weight = it))
            }
            field("Shoulders ($len)", f.shoulders, cm(latest.shouldersCm)) { viewModel.onFormChanged(f.copy(shoulders = it)) }
            field("Chest ($len)", f.chest, cm(latest.chestCm)) { viewModel.onFormChanged(f.copy(chest = it)) }
            field("$two above navel ($len)", f.aboveNavel, cm(latest.aboveNavelCm)) { viewModel.onFormChanged(f.copy(aboveNavel = it)) }
            field("Navel circumference ($len)", f.navel, cm(latest.navelCm)) { viewModel.onFormChanged(f.copy(navel = it)) }
            field("$two below navel ($len)", f.belowNavel, cm(latest.belowNavelCm)) { viewModel.onFormChanged(f.copy(belowNavel = it)) }
            field("Thighs ($len)", f.thigh, cm(latest.thighCm)) { viewModel.onFormChanged(f.copy(thigh = it)) }
            Button(
                onClick = viewModel::onSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Save measurements")
            }
        }
    }
}

// --- Formatting ---------------------------------------------------------

private fun round1(v: Double): Double = Math.round(v * 10) / 10.0

private fun formatLength(cm: Double, metric: Boolean): String =
    if (metric) String.format(Locale.US, "%.1f cm", cm)
    else String.format(Locale.US, "%.1f in", cm / BodyMeasurementsViewModel.CM_PER_INCH)

private fun formatWeight(kg: Double, metric: Boolean): String =
    if (metric) String.format(Locale.US, "%.1f kg", kg)
    else String.format(Locale.US, "%.1f lbs", WorkoutCalculators.convertWeight(kg, WeightUnit.KG, WeightUnit.LB))
