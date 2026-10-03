package com.gymora.ui.home

import androidx.compose.foundation.layout.heightIn
import com.gymora.ui.theme.GymoraShapes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Week calendar strip: days with a completed workout get a filled accent
 * circle (plus a check-style description for screen readers); today is outlined.
 */
@Composable
fun WeekStrip(
    weekStart: LocalDate,
    workoutDays: Set<LocalDate>,
    onShiftWeek: (Long) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val days = (0L..6L).map { weekStart.plusDays(it) }
    val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Text(
            text = days[3].format(monthFormat).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onShiftWeek(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                days.forEach { day ->
                    DayCell(day, trained = day in workoutDays, isToday = day == today, onClick = { onDayClick(day) })
                }
            }
            IconButton(onClick = { onShiftWeek(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, trained: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val scheme = MaterialTheme.colorScheme
    val description = buildString {
        append(day.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())))
        if (trained) append(", workout completed")
        if (isToday) append(", today")
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(
                if (isToday) {
                    Modifier.border(BorderStroke(1.5.dp, scheme.primary), GymoraShapes.card)
                } else {
                    Modifier
                },
            )
            .clip(GymoraShapes.card)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isToday) scheme.onSurface else scheme.onSurfaceVariant,
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(32.dp)
                .background(if (trained) scheme.primary else androidx.compose.ui.graphics.Color.Transparent, CircleShape),
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (trained) scheme.onPrimary else scheme.onSurface,
            )
        }
    }
}
