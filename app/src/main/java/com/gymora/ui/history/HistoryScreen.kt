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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.ui.components.EmptyState
import com.gymora.ui.components.EmptyStateCopy
import com.gymora.ui.workout.formatElapsed
import java.time.format.DateTimeFormatter

/**
 * Workout history list (FR-040, FR-058, FR-059): completed workouts newest
 * first with date, name, and duration; incremental loading in a LazyColumn.
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
        if (uiState.isLoading) {
            GymoraLoading(modifier = Modifier.padding(innerPadding).padding(16.dp))
        } else if (uiState.entries.isEmpty()) {
            EmptyState(
                message = EmptyStateCopy.NO_HISTORY,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = listState,
            ) {
                items(uiState.entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.routineNameSnapshot) },
                        supportingContent = {
                            Text(
                                DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
                                    .format(entry.startedAt.atZone(java.time.ZoneId.systemDefault())),
                            )
                        },
                        trailingContent = {
                            Text(
                                formatElapsed(entry.duration.seconds),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        modifier = Modifier.clickable { onEntryClick(entry.id) },
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
