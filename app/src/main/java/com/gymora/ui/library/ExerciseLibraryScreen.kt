package com.gymora.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.Exercise
import com.gymora.domain.model.MuscleGroup
import com.gymora.ui.components.ConfirmDialog
import com.gymora.ui.components.EmptyState
import com.gymora.ui.components.EmptyStateCopy
import java.util.Locale

/**
 * Exercise library: browse grouped by muscle group, search (incl. custom),
 * delete with confirmation (FR-008, FR-009).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    onExerciseClick: (Long) -> Unit,
    onCreateExercise: () -> Unit,
    onExerciseHistory: (Long) -> Unit = {},
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Exercises") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateExercise) {
                Icon(Icons.Filled.Add, contentDescription = "Create exercise")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search exercises") },
                singleLine = true,
            )

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }

                uiState.searchResults != null -> {
                    if (uiState.isSearchEmpty) {
                        EmptyState(message = EmptyStateCopy.NO_SEARCH_RESULTS)
                    } else {
                        ExerciseList(
                            exercises = uiState.searchResults.orEmpty(),
                            onExerciseClick = onExerciseClick,
                            onExerciseHistory = onExerciseHistory,
                            onDelete = viewModel::onDeleteRequested,
                        )
                    }
                }

                else -> {
                    ExerciseList(
                        exercises = uiState.groupedExercises.values.flatten(),
                        onExerciseClick = onExerciseClick,
                        onExerciseHistory = onExerciseHistory,
                        onDelete = viewModel::onDeleteRequested,
                    )
                }
            }
        }
    }

    uiState.pendingDelete?.let { exercise ->
        ConfirmDialog(
            title = "Delete exercise",
            message = "Delete \"${exercise.name}\"? Your workout history is never affected.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = viewModel::onDeleteConfirmed,
            onDismiss = viewModel::onDeleteDismissed,
        )
    }
}

@Composable
private fun ExerciseList(
    exercises: List<Exercise>,
    onExerciseClick: (Long) -> Unit,
    onExerciseHistory: (Long) -> Unit,
    onDelete: (Exercise) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        exercises
            .groupBy { it.muscleGroup }
            .entries
            .sortedBy { entry -> entry.key?.ordinal ?: MuscleGroup.entries.size }
            .forEach { (group, groupExercises) ->
                item(key = "header-${group?.name ?: "none"}") {
                    Text(
                        text = group?.name?.lowercase()
                            ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                            ?: "Other",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(groupExercises, key = { it.id }) { exercise ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            modifier = Modifier.weight(1f),
                            headlineContent = { Text(exercise.name) },
                            supportingContent = {
                                if (exercise.isCustom) Text("Custom")
                            },
                        )
                        IconButton(onClick = { onExerciseHistory(exercise.id) }) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = "View history for ${exercise.name}",
                            )
                        }
                        IconButton(onClick = { onDelete(exercise) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete ${exercise.name}",
                            )
                        }
                    }
                    androidx.compose.material3.HorizontalDivider()
                }
            }
    }
}
