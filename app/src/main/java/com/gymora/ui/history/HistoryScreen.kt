package com.gymora.ui.history

import com.gymora.ui.components.GymoraLoading
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.gymora.ui.components.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.ui.components.EmptyState
import com.gymora.ui.components.EmptyStateCopy
import com.gymora.ui.workout.formatElapsed
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import com.gymora.domain.model.WorkoutStat
import com.gymora.ui.calendar.WorkoutCalendar
import com.gymora.ui.components.FilterChip
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Workout history (FR-040, FR-058, FR-059): completed workouts newest first
 * with date, name, and duration, loaded incrementally; or, in calendar view,
 * a month calendar listing the workouts of the tapped day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onEntryClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Load more as the user nears the end (incremental loading, FR-058).
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.entries.size - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.onLoadMore()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("History") }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.view == HistoryView.LIST,
                    onClick = { viewModel.onViewSelected(HistoryView.LIST) },
                    label = { Text("List") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ViewList, null, Modifier.size(18.dp)) },
                )
                FilterChip(
                    selected = uiState.view == HistoryView.CALENDAR,
                    onClick = { viewModel.onViewSelected(HistoryView.CALENDAR) },
                    label = { Text("Calendar") },
                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp)) },
                )
            }
            when {
                uiState.isLoading -> GymoraLoading(modifier = Modifier.padding(16.dp))
                uiState.entries.isEmpty() -> EmptyState(message = EmptyStateCopy.NO_HISTORY)
                uiState.view == HistoryView.CALENDAR -> HistoryCalendar(
                    workouts = uiState.calendarWorkouts,
                    selectedDay = uiState.selectedDay,
                    onDaySelected = viewModel::onCalendarDaySelected,
                    onEntryClick = onEntryClick,
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                    items(uiState.entries, key = { it.id }) { entry ->
                        HistoryRow(
                            name = entry.routineNameSnapshot,
                            subtitle = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
                                .format(entry.startedAt.atZone(ZoneId.systemDefault())),
                            durationSeconds = entry.duration.seconds,
                            onClick = { onEntryClick(entry.id) },
                        )
                    }
                    if (uiState.isLoadingMore) {
                        item {
                            GymoraLoading(modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Month calendar with the selected day's workouts listed underneath. */
@Composable
private fun HistoryCalendar(
    workouts: List<WorkoutStat>,
    selectedDay: LocalDate?,
    onDaySelected: (LocalDate) -> Unit,
    onEntryClick: (Long) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val dayWorkouts = remember(workouts, selectedDay) {
        workouts.filter { it.startedAt.atZone(zone).toLocalDate() == selectedDay }
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            WorkoutCalendar(
                workouts = workouts,
                selectedDay = selectedDay,
                onDayClick = { day, _ -> onDaySelected(day) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (selectedDay != null) {
            item {
                Text(
                    selectedDay.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(dayWorkouts, key = { it.sessionId }) { workout ->
                HistoryRow(
                    name = workout.routineName,
                    subtitle = DateTimeFormatter.ofPattern("HH:mm").format(workout.startedAt.atZone(zone)),
                    durationSeconds = workout.duration.seconds,
                    onClick = { onEntryClick(workout.sessionId) },
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(name: String, subtitle: String, durationSeconds: Long, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Text(formatElapsed(durationSeconds), style = MaterialTheme.typography.bodyMedium)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
