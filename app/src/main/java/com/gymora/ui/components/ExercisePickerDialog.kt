package com.gymora.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymora.domain.model.Exercise

/** "Add exercise" dialog: the library, searchable by exercise name or muscle group. */
@Composable
fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Add exercise",
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(exercises, query) {
        val q = query.trim()
        if (q.isEmpty()) {
            exercises
        } else {
            exercises.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.muscleGroup?.displayName?.contains(q, ignoreCase = true) == true
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search exercises") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text(
                        "No exercises match \"${query.trim()}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn {
                        items(filtered, key = { it.id }) { exercise ->
                            ListItem(
                                headlineContent = { Text(exercise.name) },
                                supportingContent = {
                                    Text(exercise.muscleGroup?.displayName ?: "")
                                },
                                trailingContent = {
                                    IconButton(onClick = { onPick(exercise.id) }) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add ${exercise.name}")
                                    }
                                },
                                modifier = Modifier.clickable { onPick(exercise.id) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
