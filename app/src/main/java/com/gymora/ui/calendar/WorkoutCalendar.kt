package com.gymora.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymora.domain.model.WorkoutStat
import com.gymora.ui.components.Card
import com.gymora.ui.theme.GymoraShapes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Month calendar of completed workouts, swiped one month per page and opening
 * on the current month. Days trained are filled and labelled with the routine;
 * tapping one reports the day and its workouts. Used by Profile and History.
 */
@Composable
fun WorkoutCalendar(
    workouts: List<WorkoutStat>,
    onDayClick: (LocalDate, List<WorkoutStat>) -> Unit,
    modifier: Modifier = Modifier,
    selectedDay: LocalDate? = null,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now()
    val byDay = remember(workouts) { workoutsByDay(workouts, zone) }
    val months = remember(workouts, today) { calendarMonths(workouts, today, zone) }
    Box(modifier) { CalendarPager(months.asReversed(), byDay, today, selectedDay, onDayClick) }
}

/** One month per page, oldest on the left; opens on the newest (current) month. */
@Composable
private fun CalendarPager(
    months: List<YearMonth>,
    byDay: Map<LocalDate, List<WorkoutStat>>,
    today: LocalDate,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate, List<WorkoutStat>) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = months.lastIndex) { months.size }
    HorizontalPager(
        state = pagerState,
        pageSpacing = 12.dp,
        key = { months[it].toString() },
    ) { page ->
        MonthCalendar(months[page], byDay, today, selectedDay, onDayClick)
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    byDay: Map<LocalDate, List<WorkoutStat>>,
    today: LocalDate,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate, List<WorkoutStat>) -> Unit,
) {
    val firstDay = DayOfWeek.MONDAY
    val leading = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val cells: List<LocalDate?> = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            Row(Modifier.fillMaxWidth()) {
                (0L..6L).forEach { i ->
                    Text(
                        firstDay.plus(i).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // Always six week rows so every month page has the same height.
            (cells + List(42 - cells.size) { null }).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f)) {
                            if (day != null) {
                                DayCell(day, byDay[day].orEmpty(), day == today, day == selectedDay, onDayClick)
                            }
                            else Spacer(Modifier.height(60.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    workouts: List<WorkoutStat>,
    isToday: Boolean,
    isSelected: Boolean,
    onDayClick: (LocalDate, List<WorkoutStat>) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val trained = workouts.isNotEmpty()
    val description = buildString {
        append(day.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())))
        if (trained) append(", ${workouts.joinToString { it.routineName }}")
        if (isToday) append(", today")
        if (isSelected) append(", selected")
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(GymoraShapes.input)
            .then(if (isSelected) Modifier.background(scheme.surfaceContainerHighest) else Modifier)
            .then(if (trained) Modifier.clickable { onDayClick(day, workouts) } else Modifier)
            .padding(vertical = 2.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .then(if (isToday) Modifier.border(BorderStroke(1.5.dp, scheme.primary), CircleShape) else Modifier)
                .background(if (trained) scheme.primary else Color.Transparent, CircleShape),
        ) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (trained) scheme.onPrimary else scheme.onSurface,
            )
        }
        if (trained) {
            Text(
                dayLabel(workouts),
                fontSize = 9.sp,
                lineHeight = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = scheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Workouts grouped by local calendar day. */
fun workoutsByDay(stats: List<WorkoutStat>, zone: ZoneId): Map<LocalDate, List<WorkoutStat>> =
    stats.groupBy { it.startedAt.atZone(zone).toLocalDate() }

/** Months to show in the calendar, newest first: from the first workout's month to [today]'s. */
fun calendarMonths(stats: List<WorkoutStat>, today: LocalDate, zone: ZoneId): List<YearMonth> {
    val current = YearMonth.from(today)
    val first = stats.minOfOrNull { it.startedAt }
        ?.let { YearMonth.from(it.atZone(zone).toLocalDate()) }
        ?.takeIf { it <= current }
        ?: current
    return generateSequence(current) { it.minusMonths(1) }.takeWhile { it >= first }.toList()
}

/** Calendar label for a day: the first routine name, plus "+n" when more workouts were done. */
fun dayLabel(workouts: List<WorkoutStat>): String =
    workouts.first().routineName + if (workouts.size > 1) " +${workouts.size - 1}" else ""
