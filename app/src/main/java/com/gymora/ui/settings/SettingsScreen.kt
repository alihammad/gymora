package com.gymora.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit

/**
 * Settings screen (FR-048, FR-050, FR-051, PRD-§39): weight unit selector,
 * default rest duration, and theme selector — all persisted in Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Weight unit (FR-048)
            SettingsSection(title = "Weight Unit") {
                WeightUnit.entries.forEach { unit ->
                    SettingsRadioRow(
                        label = unit.name,
                        selected = settings.weightUnit == unit,
                        onClick = { viewModel.onWeightUnitSelected(unit) },
                    )
                }
            }

            // Default rest duration (FR-050)
            SettingsSection(title = "Default Rest Duration") {
                val presets = listOf(30, 60, 90, 120, 180)
                presets.forEach { seconds ->
                    val label = when {
                        seconds < 60 -> "${seconds}s"
                        else -> "${seconds / 60} min"
                    }
                    SettingsRadioRow(
                        label = label,
                        selected = settings.defaultRestSeconds == seconds,
                        onClick = { viewModel.onRestDurationSelected(seconds) },
                    )
                }
                // Custom option
                val isCustom = settings.defaultRestSeconds !in presets
                SettingsRadioRow(
                    label = if (isCustom) "Custom (${settings.defaultRestSeconds}s)" else "Custom…",
                    selected = isCustom,
                    onClick = { viewModel.onCustomRestClicked() },
                )
            }

            // Theme (FR-051)
            SettingsSection(title = "Theme") {
                Theme.entries.forEach { theme ->
                    SettingsRadioRow(
                        label = when (theme) {
                            Theme.SYSTEM -> "System default"
                            Theme.LIGHT -> "Light"
                            Theme.DARK -> "Dark"
                        },
                        selected = settings.theme == theme,
                        onClick = { viewModel.onThemeSelected(theme) },
                    )
                }
            }
        }
    }

    // Custom rest duration dialog
    if (uiState.showCustomRestDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onCustomRestDismissed,
            title = { Text("Custom Rest Duration") },
            text = {
                OutlinedTextField(
                    value = uiState.customRestSeconds,
                    onValueChange = viewModel::onCustomRestSecondsChanged,
                    label = { Text("Seconds") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onCustomRestConfirmed,
                    enabled = uiState.customRestSeconds.toIntOrNull()?.let { it > 0 } == true,
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCustomRestDismissed) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            content()
        }
    }
}

@Composable
private fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
