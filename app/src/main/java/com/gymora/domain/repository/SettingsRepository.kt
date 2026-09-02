package com.gymora.domain.repository

import com.gymora.domain.model.Settings
import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit
import kotlinx.coroutines.flow.Flow

/**
 * Settings contract (FR-048..FR-051, contracts/repositories.md).
 * All writes are write-through (R-05): durable before returning.
 */
interface SettingsRepository {

    fun observeSettings(): Flow<Settings>

    /** Display-only; never rewrites stored weights (R-04). */
    suspend fun setWeightUnit(unit: WeightUnit)

    suspend fun setDefaultRestDuration(seconds: Int)

    suspend fun setTheme(theme: Theme)
}
