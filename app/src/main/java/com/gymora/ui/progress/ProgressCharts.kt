package com.gymora.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymora.ui.theme.DisplayMetric

private val DayLabels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

/**
 * Seven bars, Monday to Sunday. Lime bars are past training days, the violet bar is today and a
 * dashed line marks the training-day average. [description] is read out by screen readers.
 */
@Composable
fun WeekVolumeChart(
    daily: List<Double>,
    todayIndex: Int,
    average: Double,
    description: String,
    modifier: Modifier = Modifier,
) {
    val lime = MaterialTheme.colorScheme.primary
    val violet = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val max = (daily.maxOrNull() ?: 0.0).coerceAtLeast(average).coerceAtLeast(1.0)
    Column(modifier = modifier.semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val slot = size.width / 7f
            val barWidth = slot * 0.55f
            drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            daily.forEachIndexed { i, value ->
                val h = (size.height * (value / max)).toFloat()
                if (h > 0f) {
                    drawRect(
                        color = if (i == todayIndex) violet else lime,
                        topLeft = Offset(i * slot + (slot - barWidth) / 2f, size.height - h),
                        size = Size(barWidth, h),
                    )
                }
            }
            if (average > 0) {
                val y = size.height - (size.height * (average / max)).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            DayLabels.forEachIndexed { i, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == todayIndex) violet else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Violet progression line with a soft fill and a marker on the latest value. */
@Composable
fun StrengthLineChart(
    values: List<Double>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val violet = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        if (values.isEmpty()) return@Canvas
        val max = values.max()
        val min = values.min()
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val pad = 10.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        fun x(i: Int) = if (values.size == 1) size.width / 2 else pad + w * i / (values.size - 1)
        fun y(v: Double) = (pad + h * (1 - (v - min) / span)).toFloat()

        for (g in 0..3) {
            val gy = pad + h * g / 3
            drawLine(grid, Offset(pad, gy), Offset(pad + w, gy), strokeWidth = 1.dp.toPx())
        }
        if (values.size > 1) {
            val line = Path().apply {
                values.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) }
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(x(values.lastIndex), size.height)
                lineTo(x(0), size.height)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(violet.copy(alpha = 0.35f), Color.Transparent)))
            drawPath(line, violet, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(x(values.lastIndex), y(values.last())))
        drawCircle(violet, radius = 4.dp.toPx(), center = Offset(x(values.lastIndex), y(values.last())))
    }
}

/** Ring showing [fraction] (0..1) of the weekly goal, with the percentage in the middle. */
@Composable
fun ConsistencyRing(fraction: Float, modifier: Modifier = Modifier) {
    val violet = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val clamped = fraction.coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(violet, -90f, 360f * clamped, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        }
        Text(
            text = "${(clamped * 100).toInt()}%",
            style = DisplayMetric,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
