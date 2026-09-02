package com.gymora.domain.model

import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit

/**
 * User preferences (FR-048..FR-051). Pure-Kotlin mirror of the settings table
 * without persistence annotations (plan.md Structure Decision).
 */
data class Settings(
    val weightUnit: WeightUnit,
    val defaultRestSeconds: Int,
    val theme: Theme,
) {
    companion object {
        val DEFAULTS = Settings(
            weightUnit = WeightUnit.KG,
            defaultRestSeconds = 90,
            theme = Theme.SYSTEM,
        )
    }
}
