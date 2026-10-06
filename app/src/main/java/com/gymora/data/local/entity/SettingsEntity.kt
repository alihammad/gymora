package com.gymora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit

/**
 * Single-row preferences table (FR-048..FR-051, data-model.md).
 * Stored in Room, not SharedPreferences (FR-054).
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "weight_unit") val weightUnit: String,
    @ColumnInfo(name = "default_rest_seconds") val defaultRestSeconds: Int,
    @ColumnInfo(name = "theme") val theme: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "weekly_goal", defaultValue = "3") val weeklyGoal: Int = 3,
    /** Reminder weekdays as a bitmask, Monday = bit 0; 0 = reminders off. */
    @ColumnInfo(name = "reminder_days", defaultValue = "0") val reminderDays: Int = 0,
    @ColumnInfo(name = "reminder_minute_of_day", defaultValue = "1080") val reminderMinuteOfDay: Int = 1080,
) {
    companion object {
        const val SINGLE_ROW_ID = 1

        /** Defaults per data-model.md: KG / 90s / SYSTEM. */
        val DEFAULTS = SettingsEntity(
            id = SINGLE_ROW_ID,
            weightUnit = WeightUnit.KG.name,
            defaultRestSeconds = 90,
            theme = Theme.SYSTEM.name,
            updatedAt = 0L,
        )
    }
}
