package com.gymora.ui.components

import com.gymora.ui.theme.GymoraShapes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

/** One point on a progress chart, oldest first. */
data class ChartPoint(val label: String, val value: Double)

/**
 * Card with the latest value, the change since the first point (green up /
 * red down, with an arrow so it is not colour-only) and a line chart.
 */
@Composable
fun ProgressChartCard(
    title: String,
    unit: String,
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = GymoraShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (points.isEmpty()) {
                Text(
                    "Not enough data yet. Complete a workout to see progress.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }
            val latest = points.last().value
            val first = points.first().value
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${format(latest)} $unit",
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (points.size > 1 && first > 0) {
                    val pct = (latest - first) / first * 100
                    val up = pct >= 0
                    Text(
                        text = (if (up) "▲ +" else "▼ ") + String.format(Locale.US, "%.0f%%", pct) + " since first",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            if (points.size > 1) {
                LineChart(points = points, modifier = Modifier.fillMaxWidth().height(140.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(points.first().label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(points.last().label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.height(4.dp))
                Text("Log one more workout to see a trend.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LineChart(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val summary = "Line chart, ${points.size} points, from ${format(points.first().value)} to ${format(points.last().value)}"
    Canvas(modifier = modifier.semantics { contentDescription = summary }) {
        val max = points.maxOf { it.value }
        val min = points.minOf { it.value }
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val pad = 8.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        fun x(i: Int) = pad + w * i / (points.size - 1)
        fun y(v: Double) = (pad + h * (1 - (v - min) / span)).toFloat()

        for (g in 0..2) {
            val gy = pad + h * g / 2
            drawLine(grid, Offset(pad, gy), Offset(pad + w, gy), strokeWidth = 1.dp.toPx())
        }
        val path = Path().apply {
            points.forEachIndexed { i, p ->
                if (i == 0) moveTo(x(i), y(p.value)) else lineTo(x(i), y(p.value))
            }
        }
        val fill = Path().apply {
            addPath(path)
            lineTo(x(points.lastIndex), size.height)
            lineTo(x(0), size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.30f), Color.Transparent)))
        drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.forEachIndexed { i, p ->
            drawCircle(line, radius = 4.dp.toPx(), center = Offset(x(i), y(p.value)))
        }
    }
}

/** Tiny trend line with an end dot, for list rows. */
@Composable
fun Sparkline(values: List<Double>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val pad = 6.dp.toPx()
        val min = values.min()
        val span = (values.max() - min).takeIf { it > 0 } ?: 1.0
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        fun x(i: Int) = if (values.size == 1) size.width / 2 else pad + w * i / (values.size - 1)
        fun y(v: Double) = (pad + h * (1 - (v - min) / span)).toFloat()
        if (values.size > 1) {
            val path = Path().apply {
                values.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) }
            }
            drawPath(
                path,
                color.copy(alpha = 0.6f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        drawCircle(color, radius = 4.dp.toPx(), center = Offset(x(values.lastIndex), y(values.last())))
    }
}

private fun format(value: Double): String =
    if (value >= 100) String.format(Locale.US, "%.0f", value) else String.format(Locale.US, "%.1f", value)
