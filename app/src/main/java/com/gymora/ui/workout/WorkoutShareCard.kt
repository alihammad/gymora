package com.gymora.ui.workout

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.SessionRecord
import com.gymora.domain.model.SessionRecordKind
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.model.WorkoutSummary
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_RECORDS_SHOWN = 4

/**
 * The shareable workout card: duration, volume, sets, reps, personal records
 * and the weekly streak. Drawn on the summary screen and captured as a PNG.
 * Rectangular on purpose so the exported image has no transparent corners.
 */
@Composable
fun WorkoutShareCard(
    summary: WorkoutSummary,
    records: List<SessionRecord>,
    streakWeeks: Int,
    unit: WeightUnit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val unitLabel = unit.name.lowercase(Locale.US)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.surfaceContainer)))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "GYMORA",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = scheme.primary,
                modifier = Modifier.weight(1f),
            )
            Text(
                DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault())
                    .format(summary.date.atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        }
        Text(
            summary.routineNameSnapshot,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row {
            CardStat("Duration", formatElapsed(summary.duration.seconds), Modifier.weight(1f))
            CardStat("Volume", "${formatWhole(summary.totalVolume)} $unitLabel", Modifier.weight(1f))
        }
        Row {
            CardStat("Sets", summary.completedSetCount.toString(), Modifier.weight(1f))
            CardStat("Reps", summary.totalReps.toString(), Modifier.weight(1f))
        }
        if (records.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        if (records.size == 1) "1 personal record" else "${records.size} personal records",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                records.take(MAX_RECORDS_SHOWN).forEach { record ->
                    Row {
                        Text(
                            record.exerciseName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            formatRecord(record, unit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.primary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                if (records.size > MAX_RECORDS_SHOWN) {
                    Text(
                        "+${records.size - MAX_RECORDS_SHOWN} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (streakWeeks > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    "$streakWeeks-week streak",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CardStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** "Heaviest weight 100 kg", "Best est. 1RM 112.5 kg", "Most reps 25". */
private fun formatRecord(record: SessionRecord, unit: WeightUnit): String {
    val value = when (record.kind) {
        SessionRecordKind.MOST_REPS -> formatWhole(record.value)
        else -> {
            val converted = WorkoutCalculators.convertWeight(record.value, WeightUnit.KG, unit)
            "${String.format(Locale.US, "%.1f", converted).removeSuffix(".0")} ${unit.name.lowercase(Locale.US)}"
        }
    }
    return "${record.kind.label} $value"
}

private fun formatWhole(value: Double): String = String.format(Locale.getDefault(), "%,.0f", value)

/** Writes [bitmap] to the share cache and opens the system share sheet. */
suspend fun shareWorkoutImage(context: Context, bitmap: Bitmap) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "workout-summary.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri) // lets the share-sheet preview read the image
    context.startActivity(Intent.createChooser(send, "Share workout"))
}
