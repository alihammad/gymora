package com.gymora.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gymora.data.local.dao.SettingsDao
import com.gymora.data.local.entity.SettingsEntity

/**
 * Gymora Room database — the offline source of truth (FR-053, BR-18, BR-21).
 *
 * Entity registration is incremental per tasks.md T008: this shell starts with
 * [SettingsEntity]; story tasks T014/T025/T035 register the remaining entities.
 * The schema stays frozen at version 1 until release, so no migrations are
 * required during feature development. `fallbackToDestructiveMigration` is
 * deliberately NOT configured (BR-19, Constitution V).
 */
@Database(
    entities = [
        SettingsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class GymoraDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao

    companion object {
        const val NAME = "gymora.db"
    }
}
