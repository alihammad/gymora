package com.gymora.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.gymora.ui.components.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import com.gymora.ui.components.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.gymora.ui.components.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.gymora.ui.components.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.MuscleGroup

/**
 * Create/edit exercise screen (FR-006, FR-007): name required; muscle group,
 * description, and notes optional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditorScreen(
    onBack: () -> Unit,
    viewModel: ExerciseEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.exerciseId == null) "New Exercise" else "Edit Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            var menuExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
            ) {
                OutlinedTextField(
                    value = uiState.muscleGroup?.displayName ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Muscle group") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            viewModel.onMuscleGroupChanged(null)
                            menuExpanded = false
                        },
                    )
                    MuscleGroup.entries.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.displayName) },
                            onClick = {
                                viewModel.onMuscleGroupChanged(group)
                                menuExpanded = false
                            },
                        )
                    }
                }
            }

            TrackingFields(
                measurementType = uiState.measurementType,
                isUnilateral = uiState.isUnilateral,
                onMeasurementTypeChanged = viewModel::onMeasurementTypeChanged,
                onUnilateralChanged = viewModel::onUnilateralChanged,
            )

            FormGuideFields(
                formCues = uiState.formCues,
                mediaFile = uiState.mediaFile,
                onFormCuesChanged = viewModel::onFormCuesChanged,
                onMediaPicked = viewModel::onMediaPicked,
                onMediaRemoved = viewModel::onMediaRemoved,
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::onSave,
                enabled = !uiState.isLoading && uiState.name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}
