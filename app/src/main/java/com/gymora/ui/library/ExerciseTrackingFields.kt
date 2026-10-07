package com.gymora.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymora.domain.model.MeasurementType
import com.gymora.ui.components.ExerciseMediaImage
import com.gymora.ui.components.OutlinedButton
import com.gymora.ui.components.OutlinedTextField
import com.gymora.ui.components.TextButton

/** How sets of the exercise are tracked: value type and per-side logging. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingFields(
    measurementType: MeasurementType,
    isUnilateral: Boolean,
    onMeasurementTypeChanged: (MeasurementType) -> Unit,
    onUnilateralChanged: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = measurementType.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Track") },
            supportingText = { Text("e.g. ${measurementType.example}") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MeasurementType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(type.displayName)
                            Text(
                                type.example,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onMeasurementTypeChanged(type)
                        expanded = false
                    },
                )
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Log each side separately", style = MaterialTheme.typography.bodyLarge)
            Text(
                "For single-arm or single-leg work: every set has a left and a right entry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = isUnilateral, onCheckedChange = onUnilateralChanged)
    }
}

/** Demo image/GIF and form cues shown while training. */
@Composable
fun FormGuideFields(
    formCues: String,
    mediaFile: String?,
    onFormCuesChanged: (String) -> Unit,
    onMediaPicked: (Uri) -> Unit,
    onMediaRemoved: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onMediaPicked)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = formCues,
            onValueChange = onFormCuesChanged,
            label = { Text("Form cues (one per line)") },
            placeholder = { Text("Brace before each rep\nElbows at 45°") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        if (mediaFile != null) ExerciseMediaImage(mediaFile, contentDescription = "Demo image")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            ) { Text(if (mediaFile == null) "Add demo image or GIF" else "Replace image") }
            if (mediaFile != null) TextButton(onClick = onMediaRemoved) { Text("Remove") }
        }
    }
}
