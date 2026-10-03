package com.gymora.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.model.WorkoutStat
import com.gymora.ui.components.Card
import com.gymora.ui.components.FilterChip
import com.gymora.ui.components.GymoraLoading
import com.gymora.ui.components.TopAppBar
import com.gymora.ui.theme.GymoraShapes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val MILLIS_PER_HOUR = 3_600_000.0

/**
 * Profile: bar charts of workout duration, volume and reps (one bar per workout,
 * filterable by period) followed by a month calendar, swiped left/right one month
 * per view, that labels every day trained with the routine done that day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onWorkoutClick: (Long) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now()
    val byDay = remember(state.allWorkouts) { workoutsByDay(state.allWorkouts, zone) }
    val months = remember(state.allWorkouts, today) { calendarMonths(state.allWorkouts, today, zone) }

    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfilePeriod.entries.forEach { p ->
                        FilterChip(
                            selected = state.period == p,
                            onClick = { viewModel.onPeriodSelected(p) },
                            label = { Text(p.label) },
                        )
                    }
                }
            }
            if (state.isLoading) {
                item { GymoraLoading(modifier = Modifier.padding(16.dp)) }
            } else {
                item { ChartsSection(state.chartWorkouts, state.weightUnit, zone) }
                item {
                    Text(
                        "Calendar",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                item { CalendarPager(months.asReversed(), byDay, today, onWorkoutClick) }
            }
        }
    }
}

@Composable
private fun ChartsSection(workouts: List<WorkoutStat>, unit: WeightUnit, zone: ZoneId) {
    if (workouts.isEmpty()) {
        Text(
            "No completed workouts in this period yet. Finish a workout to see your charts here.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        return
    }
    val dateFormat = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }
    val labels = remember(workouts) {
        workouts.map { it.startedAt.atZone(zone).toLocalDate().format(dateFormat) }
    }
    val durationBars = remember(workouts) {
        workouts.mapIndexed { i, w -> Bar(labels[i], w.duration.toMillis() / MILLIS_PER_HOUR) }
    }
    val volumeBars = remember(workouts, unit) {
        workouts.mapIndexed { i, w ->
            Bar(labels[i], WorkoutCalculators.convertWeight(w.volumeKg, WeightUnit.KG, unit))
        }
    }
    val repBars = remember(workouts) {
        workouts.mapIndexed { i, w -> Bar(labels[i], w.totalReps.toDouble()) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChartCard("Duration (hours)", durationBars) { formatNumber(it, decimals = 1) }
        ChartCard("Volume (${unit.name.lowercase(Locale.US)})", volumeBars, ::formatCompact)
        ChartCard("Reps", repBars, ::formatCompact)
    }
}

@Composable
private fun ChartCard(title: String, bars: List<Bar>, formatValue: (Double) -> String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ScrollableBarChart(
                bars = bars,
                formatValue = formatValue,
                description = "$title bar chart, ${bars.size} workouts from ${bars.first().label} to ${bars.last().label}",
            )
        }
    }
}

/** One month per page, oldest on the left; opens on the newest (current) month. */
@Composable
private fun CalendarPager(
    months: List<YearMonth>,
    byDay: Map<LocalDate, List<WorkoutStat>>,
    today: LocalDate,
    onWorkoutClick: (Long) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = months.lastIndex) { months.size }
    HorizontalPager(
        state = pagerState,
        pageSpacing = 12.dp,
        key = { months[it].toString() },
    ) { page ->
        MonthCalendar(months[page], byDay, today, onWorkoutClick)
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    byDay: Map<LocalDate, List<WorkoutStat>>,
    today: LocalDate,
    onWorkoutClick: (Long) -> Unit,
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
                            if (day != null) DayCell(day, byDay[day].orEmpty(), day == today, onWorkoutClick)
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
    onWorkoutClick: (Long) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val trained = workouts.isNotEmpty()
    val description = buildString {
        append(day.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())))
        if (trained) append(", ${workouts.joinToString { it.routineName }}")
        if (isToday) append(", today")
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(GymoraShapes.input)
            .then(if (trained) Modifier.clickable { onWorkoutClick(workouts.first().sessionId) } else Modifier)
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

private fun formatNumber(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value)

/** 1234 -> "1.2k", 950 -> "950". */
private fun formatCompact(value: Double): String =
    if (value >= 1000) formatNumber(value / 1000, 1).removeSuffix(".0") + "k" else formatNumber(value, 0)
