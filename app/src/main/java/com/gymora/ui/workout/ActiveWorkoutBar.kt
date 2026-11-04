package com.gymora.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.ActiveWorkout
import com.gymora.domain.repository.WorkoutSessionRepository
import com.gymora.ui.theme.DisplayMetric
import com.gymora.ui.theme.LabelCaps
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** App-wide view of the unfinished workout, if any, for the persistent bar. */
@HiltViewModel
class ActiveWorkoutBarViewModel @Inject constructor(
    workoutSessionRepository: WorkoutSessionRepository,
) : ViewModel() {
    val activeWorkout: StateFlow<ActiveWorkout?> = workoutSessionRepository.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * Bar pinned to the bottom of every screen outside the workout while one is
 * in progress: routine name and live elapsed timer; tap to resume.
 */
@Composable
fun ActiveWorkoutBar(
    workout: ActiveWorkout,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(workout.startedAt) {
        while (true) {
            elapsed = Duration.between(workout.startedAt, Instant.now()).seconds.coerceAtLeast(0)
            delay(1000)
        }
    }
    val clock = formatElapsed(elapsed)
    val name = workout.session.routineNameSnapshot.ifBlank { "Workout" }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onResume)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { contentDescription = "Workout in progress, $clock elapsed. Tap to resume." },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("WORKOUT IN PROGRESS", style = LabelCaps, color = MaterialTheme.colorScheme.primary)
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(text = clock, style = DisplayMetric.copy(fontSize = 28.sp, lineHeight = 30.sp), color = MaterialTheme.colorScheme.primary)
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
