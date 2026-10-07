package com.gymora.domain.model

import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * User preferences (FR-048..FR-051). Pure-Kotlin mirror of the settings table
 * without persistence annotations (plan.md Structure Decision).
 */
data class Settings(
    val weightUnit: WeightUnit,
    val defaultRestSeconds: Int,
    val theme: Theme,
    /** Workouts per week (Monday–Sunday) the user aims for; drives the weekly streak. */
    val weeklyGoal: Int = DEFAULT_WEEKLY_GOAL,
    /** Weekdays a workout reminder is sent on; empty means reminders are off. */
    val reminderDays: Set<DayOfWeek> = emptySet(),
    /** Local time of day reminders are sent at. */
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
    /** Days between automatic backups; 0 turns auto-backup off. */
    val autoBackupIntervalDays: Int = 0,
    /** Folder (a document-tree URI) automatic backups are written to. */
    val autoBackupFolderUri: String? = null,
    /** When the last automatic backup succeeded, epoch millis. */
    val lastAutoBackupAt: Long? = null,
    /** Sync workouts and body weight with Health Connect. */
    val healthConnectEnabled: Boolean = false,
) {
    companion object {
        const val DEFAULT_WEEKLY_GOAL = 3
        const val MIN_WEEKLY_GOAL = 1
        const val MAX_WEEKLY_GOAL = 7
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(18, 0)

        val DEFAULTS = Settings(
            weightUnit = WeightUnit.KG,
            defaultRestSeconds = 90,
            theme = Theme.SYSTEM,
        )
    }
}
