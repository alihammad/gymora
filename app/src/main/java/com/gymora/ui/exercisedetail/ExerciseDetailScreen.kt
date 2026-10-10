package com.gymora.ui.exercisedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.gymora.ui.components.ActionButton
import com.gymora.ui.components.AngularPanel
import com.gymora.ui.components.HeroCard
import com.gymora.ui.components.MuscleChip
import com.gymora.ui.components.SectionHeader
import com.gymora.ui.routines.routineIcon
import com.gymora.ui.theme.DisplayHero
import com.gymora.ui.components.GymoraLoading
import com.gymora.ui.theme.GymoraShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import com.gymora.ui.components.ExerciseMediaImage
import com.gymora.ui.components.FormCuesList
import com.gymora.ui.components.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.gymora.ui.components.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.Exercise
import com.gymora.domain.model.MuscleGroupRef
import com.gymora.domain.model.MuscleGroupType
import com.gymora.ui.exercisefilter.ExerciseFilterKind

/**
 * Exercise detail screen: renders the full descriptive metadata
 * (classification, muscle groups, equipment, and step-by-step instructions)
 * in a clean, attribute-focused layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    onExerciseHistory: (Long) -> Unit = {},
    onFilterClick: (kind: String, value: String) -> Unit = { _, _ -> },
    onEdit: (Long) -> Unit = {},
    onStartExercise: (Long) -> Unit = {},
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.exercise?.let { exercise ->
                        IconButton(onClick = { onEdit(exercise.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit exercise")
                        }
                        IconButton(onClick = { onExerciseHistory(exercise.id) }) {
                            Icon(
                                Icons.Filled.FitnessCenter,
                                contentDescription = "View history",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            uiState.exercise?.let { exercise ->
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    ActionButton(
                        text = "Start exercise",
                        onClick = { onStartExercise(exercise.id) },
                        icon = Icons.Filled.PlayArrow,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    GymoraLoading()
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            else -> uiState.exercise?.let { exercise ->
                ExerciseDetailContent(exercise, onFilterClick, Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
private fun ExerciseDetailContent(
    exercise: Exercise,
    onFilterClick: (kind: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Media first: the demo animation (or artwork when there is none), edge to edge.
        if (exercise.mediaFile != null) {
            Box(
                modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                ExerciseMediaImage(exercise.mediaFile, "${exercise.name} demo")
            }
        } else {
            HeroCard(
                modifier = Modifier.padding(horizontal = 16.dp).height(160.dp),
                watermark = {
                    Icon(
                        painter = painterResource(routineIcon(exercise.name)),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        modifier = Modifier.align(Alignment.Center).size(120.dp),
                    )
                },
            ) {}
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        // Hero header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = exercise.name.uppercase(),
                style = DisplayHero.copy(fontSize = 44.sp, lineHeight = 43.sp),
            )
            val primaryName = exercise.muscleGroups
                .firstOrNull { it.type == MuscleGroupType.PRIMARY }
                ?.group
                ?.displayName
                ?: exercise.muscleGroup?.displayName
            if (primaryName != null) {
                Spacer(Modifier.height(8.dp))
                MuscleChip(text = primaryName, selected = true)
            }
        }

        // Attribute chips
        AttributeChips(exercise, onFilterClick)

        TrackingAndFormGuide(exercise)

        // Muscle groups (primary & secondary)
        if (exercise.muscleGroups.isNotEmpty()) {
            SectionCard(title = "Muscles Targeted") {
                val ordered = exercise.muscleGroups.sortedBy { it.type }
                ChipFlow {
                    ordered.forEach { ref ->
                        AttributeChip(
                            value = ref.group.displayName,
                            label = ref.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            onClick = { onFilterClick(ExerciseFilterKind.MUSCLE.name, ref.group.name) },
                        )
                    }
                }
            }
        } else if (exercise.muscleGroup != null) {
            SectionCard(title = "Muscle Group") {
                ChipFlow {
                    AttributeChip(
                        value = exercise.muscleGroup.displayName,
                        label = "Muscle",
                        onClick = {
                            onFilterClick(ExerciseFilterKind.MUSCLE.name, exercise.muscleGroup.name)
                        },
                    )
                }
            }
        }

        // Equipment
        if (exercise.equipment.isNotEmpty()) {
            SectionCard(title = "Equipment") {
                ChipFlow {
                    exercise.equipment.forEach { item ->
                        AttributeChip(
                            value = item.name,
                            label = item.usageType.name.lowercase().replaceFirstChar { it.uppercase() },
                            onClick = { onFilterClick(ExerciseFilterKind.EQUIPMENT.name, item.name) },
                        )
                    }
                }
            }
        }

        // Instructions
        if (exercise.instructions.isNotEmpty()) {
            SectionCard(
                title = "How to Perform",
                subtitle = "${exercise.instructions.size} steps",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    exercise.instructions.forEachIndexed { index, step ->
                        Row {
                            StepBadge(index + 1)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        // Notes / description (custom exercises)
        val description = exercise.description?.takeIf { it.isNotBlank() }
        if (description != null) {
            SectionCard(title = "Description") {
                Text(description)
            }
        }
        }
    }
}

/** How sets are logged, and the demo image and cues shown while training. */
@Composable
private fun TrackingAndFormGuide(exercise: Exercise) {
    SectionCard(title = "Tracking") {
        Text(
            text = exercise.measurementType.displayName +
                if (exercise.isUnilateral) " · each side logged separately" else "",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (exercise.formCues.isNotEmpty()) {
        SectionCard(title = "Technique cues") {
            FormCuesList(exercise.formCues)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AttributeChips(
    exercise: Exercise,
    onFilterClick: (kind: String, value: String) -> Unit,
) {
    // Triple(filter kind, display label, enum name used as the filter value)
    val chips = buildList {
        exercise.category?.let { add(Triple(ExerciseFilterKind.CATEGORY, it.displayName, it.name)) }
        exercise.difficultyLevel?.let { add(Triple(ExerciseFilterKind.LEVEL, it.displayName, it.name)) }
        exercise.forceType?.let { add(Triple(ExerciseFilterKind.FORCE, it.displayName, it.name)) }
        exercise.mechanics?.let { add(Triple(ExerciseFilterKind.MECHANICS, it.displayName, it.name)) }
        exercise.type?.let { add(Triple(ExerciseFilterKind.TYPE, it.name.lowercase(), it.name)) }
    }
    if (chips.isEmpty()) return

    ChipFlow {
        chips.forEach { (kind, value, filterValue) ->
            AttributeChip(
                value = value,
                label = kind.label,
                onClick = { onFilterClick(kind.name, filterValue) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

/** Tappable two-line pill: the value on top, what it is underneath. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeChip(value: String, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = GymoraShapes.chip,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    AngularPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(title = title)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StepBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
