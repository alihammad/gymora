package com.gymora.ui.routines

import com.gymora.ui.components.GymoraLoading
import com.gymora.ui.theme.GymoraShapes
import com.gymora.ui.theme.GymoraThemeTokens
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import com.gymora.ui.components.ConfirmDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import com.gymora.ui.components.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.gymora.ui.components.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.ui.components.EmptyState
import com.gymora.ui.components.EmptyStateCopy

/**
 * My Routines destination (FR-002, T031a): routines as accent-coloured tiles
 * in a two-column grid; tap opens the editor, + creates a new routine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    onBack: () -> Unit,
    onRoutineClick: (Long) -> Unit,
    onCreateRoutine: () -> Unit,
    viewModel: RoutineListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Workouts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateRoutine,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create workout")
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                GymoraLoading(
                    modifier = Modifier.padding(innerPadding).padding(16.dp),
                )
            }

            uiState.routines.isEmpty() -> {
                EmptyState(
                    message = EmptyStateCopy.NO_ROUTINES,
                    actionLabel = EmptyStateCopy.CREATE_ROUTINE_ACTION,
                    onAction = onCreateRoutine,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 96.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = uiState.routines.withIndex().toList(),
                        key = { it.value.id },
                    ) { (index, routine) ->
                        RoutineTile(
                            name = routine.name,
                            exerciseCount = routine.exerciseCount,
                            accentIndex = index,
                            onClick = { onRoutineClick(routine.id) },
                            onDelete = { viewModel.onDeleteRequested(routine) },
                        )
                    }
                }
            }
        }
    }

    uiState.pendingDelete?.let { routine ->
        ConfirmDialog(
            title = "Delete workout",
            message = "Delete \"${routine.name}\"? Workouts already performed from it stay in history.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = viewModel::onDeleteConfirmed,
            onDismiss = viewModel::onDeleteDismissed,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoutineTile(
    name: String,
    exerciseCount: Int,
    accentIndex: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val extra = GymoraThemeTokens.extraColors
    val slot = accentIndex % extra.tileAccents.size
    val accent = extra.tileAccents[slot]
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        shape = GymoraShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .clip(GymoraShapes.card)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuOpen = true },
                onLongClickLabel = "Workout options",
            ),
    ) {
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Delete workout") },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .size(44.dp)
                    .background(accent, CircleShape),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    routineIcon(name),
                    contentDescription = null,
                    tint = extra.onTileAccents[slot],
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    color = extra.tileAccentTexts[slot],
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$exerciseCount exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
