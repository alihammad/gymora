package com.gymora.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first

/** One bar: [label] sits under it on the x axis, [value] is its height. */
data class Bar(val label: String, val value: Double)

private val ChartHeight = 180.dp
private val BarSlot = 48.dp
private val BarWidth = 28.dp
private val YAxisWidth = 44.dp
private val PlotTop = 20.dp

/**
 * Bar chart with a fixed y axis and a horizontally scrollable plot (scrolls both
 * ways, starts at the newest bar on the right). Bars are drawn oldest first.
 */
@Composable
fun ScrollableBarChart(
    bars: List<Bar>,
    formatValue: (Double) -> String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val axis = remember(bars) { niceAxis(bars.maxOfOrNull { it.value } ?: 0.0) }
    val scroll = rememberScrollState()
    val measurer = rememberTextMeasurer()
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val valueStyle = labelStyle.copy(color = MaterialTheme.colorScheme.onSurface)

    // Show the newest workouts first: jump to the right edge once the content is measured.
    LaunchedEffect(bars) {
        snapshotFlow { scroll.maxValue }.first { it != Int.MAX_VALUE }
        scroll.scrollTo(scroll.maxValue)
    }

    Row(modifier = modifier.fillMaxWidth().semantics { contentDescription = description }) {
        Canvas(Modifier.width(YAxisWidth).height(ChartHeight)) {
            val top = PlotTop.toPx()
            val plotHeight = size.height - top
            axis.ticks.forEach { tick ->
                val layout = measurer.measure(formatValue(tick), labelStyle)
                val y = top + plotHeight * (1 - (tick / axis.max).toFloat())
                drawText(
                    layout,
                    topLeft = Offset(size.width - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2f),
                )
            }
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            val contentWidth = maxOf(BarSlot * bars.size, maxWidth)
            Column(Modifier.horizontalScroll(scroll)) {
                Canvas(Modifier.width(contentWidth).height(ChartHeight)) {
                    val top = PlotTop.toPx()
                    val plotHeight = size.height - top
                    axis.ticks.forEach { tick ->
                        val y = top + plotHeight * (1 - (tick / axis.max).toFloat())
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                    val slot = BarSlot.toPx()
                    val barWidth = BarWidth.toPx()
                    bars.forEachIndexed { i, bar ->
                        val barHeight = plotHeight * (bar.value / axis.max).toFloat()
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(i * slot + (slot - barWidth) / 2, top + plotHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                        )
                        val layout = measurer.measure(formatValue(bar.value), valueStyle)
                        drawText(
                            layout,
                            topLeft = Offset(
                                i * slot + (slot - layout.size.width) / 2f,
                                top + plotHeight - barHeight - layout.size.height - 2.dp.toPx(),
                            ),
                        )
                    }
                }
                Row(Modifier.padding(top = 4.dp)) {
                    bars.forEach { bar ->
                        Text(
                            text = bar.label,
                            style = labelStyle,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.width(BarSlot),
                        )
                    }
                }
            }
        }
    }
}
