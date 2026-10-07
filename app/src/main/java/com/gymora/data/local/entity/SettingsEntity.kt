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
    /** Days between automatic backups; 0 = off. Device-local: kept when restoring a backup. */
    @ColumnInfo(name = "auto_backup_interval_days", defaultValue = "0") val autoBackupIntervalDays: Int = 0,
    @ColumnInfo(name = "auto_backup_folder_uri") val autoBackupFolderUri: String? = null,
    @ColumnInfo(name = "last_auto_backup_at") val lastAutoBackupAt: Long? = null,
    @ColumnInfo(name = "health_connect_enabled", defaultValue = "0") val healthConnectEnabled: Boolean = false,
    @ColumnInfo(name = "step_goal", defaultValue = "10000") val stepGoal: Int = 10_000,
    /** Include steps from Health Connect (watch, other apps) in the Home step count. */
    @ColumnInfo(name = "health_steps_enabled", defaultValue = "0") val healthStepsEnabled: Boolean = false,
) {
    companion object {
        const val SINGLE_ROW_ID = 1

        /** Columns that describe this device, not the user's data; a restore keeps them. */
        val DEVICE_LOCAL_COLUMNS = listOf(
            "auto_backup_interval_days",
            "auto_backup_folder_uri",
            "last_auto_backup_at",
            "health_connect_enabled",
            "health_steps_enabled",
        )

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
