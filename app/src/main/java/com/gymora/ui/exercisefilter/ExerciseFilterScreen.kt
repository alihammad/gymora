package com.gymora.ui.exercisefilter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.ui.components.EmptyState
import com.gymora.ui.components.GymoraLoading
import com.gymora.ui.components.TopAppBar

/** Lists every exercise sharing the attribute (category, level, force, ...) of a tapped chip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFilterScreen(
    onBack: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    viewModel: ExerciseFilterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding)) {
                GymoraLoading(modifier = Modifier.padding(16.dp))
            }

            uiState.exercises.isEmpty() -> EmptyState(message = "No exercises found.")

            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(uiState.exercises, key = { it.id }) { exercise ->
                    ListItem(
                        modifier = Modifier.clickable { onExerciseClick(exercise.id) },
                        headlineContent = { Text(exercise.name) },
                        supportingContent = {
                            val tags = buildList {
                                exercise.muscleGroup?.displayName?.let { add(it) }
                                exercise.difficultyLevel?.displayName?.let { add(it) }
                                exercise.equipment.firstOrNull()?.name?.let { add(it) }
                            }
                            if (tags.isNotEmpty()) Text(tags.joinToString(" · "))
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
