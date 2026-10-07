package com.gymora.ui.routines

import androidx.annotation.DrawableRes
import com.gymora.R

/**
 * Picks the icon for a routine from keywords in its name. Muscle-group icons come from
 * game-icons.net (CC BY 3.0, see NOTICE.md); activity icons from Tabler Icons (MIT).
 */
@DrawableRes
fun routineIcon(name: String): Int {
    val n = name.lowercase()
    return when {
        listOf("leg", "squat", "glute", "quad", "hamstring", "calf", "lower").any { it in n } ->
            R.drawable.ic_workout_legs
        listOf("chest", "pec", "push").any { it in n } -> R.drawable.ic_workout_chest
        listOf("back", "lat", "pull", "row").any { it in n } -> R.drawable.ic_workout_back
        listOf("shoulder", "delt").any { it in n } -> R.drawable.ic_workout_shoulders
        listOf("arm", "bicep", "tricep", "curl").any { it in n } -> R.drawable.ic_workout_arms
        listOf("core", "abs", "ab ", "stretch", "yoga", "mobility").any { it in n } ->
            R.drawable.ic_workout_core
        listOf("cardio", "hiit", "run", "burn", "conditioning").any { it in n } ->
            R.drawable.ic_workout_cardio
        listOf("fight", "box", "mma", "martial").any { it in n } -> R.drawable.ic_workout_combat
        else -> R.drawable.ic_workout_strength
    }
}
