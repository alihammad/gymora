package com.gymora.ui.routines

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Picks the closest Material icon for a routine from keywords in its name.
 * Material has no anatomical icons, so these are the nearest metaphors
 * (legs = running figure, chest = heart, back = standing figure, ...).
 */
fun routineIcon(name: String): ImageVector {
    val n = name.lowercase()
    return when {
        listOf("leg", "squat", "glute", "quad", "hamstring", "calf", "lower").any { it in n } ->
            Icons.Filled.DirectionsRun
        listOf("chest", "pec", "push").any { it in n } -> Icons.Filled.Favorite
        listOf("back", "lat", "pull", "row").any { it in n } -> Icons.Filled.AccessibilityNew
        listOf("shoulder", "delt").any { it in n } -> Icons.Filled.SportsGymnastics
        listOf("arm", "bicep", "tricep", "curl").any { it in n } -> Icons.Filled.FitnessCenter
        listOf("core", "abs", "ab ", "stretch", "yoga", "mobility").any { it in n } ->
            Icons.Filled.SelfImprovement
        listOf("cardio", "hiit", "run", "burn", "conditioning").any { it in n } -> Icons.Filled.Whatshot
        listOf("fight", "box", "mma", "martial").any { it in n } -> Icons.Filled.SportsMartialArts
        else -> Icons.Filled.FitnessCenter
    }
}
