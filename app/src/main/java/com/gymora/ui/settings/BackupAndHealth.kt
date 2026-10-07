package com.gymora.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gymora.domain.model.Settings
import com.gymora.health.HealthConnectAvailability
import com.gymora.ui.components.OutlinedButton
import com.gymora.ui.components.TextButton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val BACKUP_INTERVALS = listOf(1 to "Daily", 7 to "Weekly", 30 to "Monthly")

/** Manual full backup / restore, plus scheduled backups to a chosen folder. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupSetting(
    settings: Settings,
    busy: Boolean,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onPickFolder: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
    onBackupNow: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "A backup holds everything: workouts, routines, exercises (with cues and images), " +
                "body measurements and settings. Restoring replaces all current data.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExport, enabled = !busy) { Text("Back up") }
            OutlinedButton(onClick = onRestore, enabled = !busy) { Text("Restore") }
        }

        Text("Automatic backups", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        val folder = settings.autoBackupFolderUri
        Text(
            text = folder?.let { "Folder: ${folderName(it)}" }
                ?: "Pick a folder (on this phone, an SD card, or a cloud app such as Google Drive " +
                "if it offers folders). The newest 7 backups are kept.",
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.autoBackupIntervalDays == 0,
                onClick = { onIntervalSelected(0) },
                label = { Text("Off") },
            )
            BACKUP_INTERVALS.forEach { (days, label) ->
                FilterChip(
                    selected = settings.autoBackupIntervalDays == days,
                    onClick = { if (folder == null) onPickFolder() else onIntervalSelected(days) },
                    label = { Text(label) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPickFolder) { Text(if (folder == null) "Choose folder" else "Change folder") }
            if (folder != null) TextButton(onClick = onBackupNow, enabled = !busy) { Text("Back up now") }
        }
        settings.lastAutoBackupAt?.let {
            Text(
                text = "Last automatic backup: ${formatTimestamp(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Opt-in Health Connect sync. */
@Composable
fun HealthConnectSetting(
    enabled: Boolean,
    availability: HealthConnectAvailability,
    permissionsGranted: Boolean,
    busy: Boolean,
    onToggle: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (availability) {
            HealthConnectAvailability.NOT_SUPPORTED -> Text(
                "Health Connect isn't available on this device.",
                style = MaterialTheme.typography.bodySmall,
            )
            HealthConnectAvailability.UPDATE_REQUIRED -> {
                Text("Health Connect needs an update first.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_HEALTH_CONNECT))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }) { Text("Update Health Connect") }
            }
            HealthConnectAvailability.AVAILABLE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sync with Health Connect", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Writes finished workouts and your weigh-ins; reads weight from smart " +
                                "scales and other apps into Body measurements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = enabled && permissionsGranted, onCheckedChange = onToggle)
                }
                if (enabled && !permissionsGranted) {
                    Text(
                        "Permissions were removed. Turn sync on again to grant them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (enabled && permissionsGranted) {
                    TextButton(onClick = onSyncNow, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Sync now")
                    }
                }
            }
        }
    }
}

private const val PLAY_STORE_HEALTH_CONNECT =
    "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"

/** Human-readable last segment of a document-tree URI, e.g. "Download/Gymora". */
private fun folderName(uri: String): String =
    runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(uri)).substringAfter(':').ifEmpty { "Storage" } }
        .getOrDefault(uri)

private fun formatTimestamp(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
