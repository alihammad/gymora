package com.gymora.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gymora.data.local.dao.ExerciseDao
import com.gymora.data.local.dao.RoutineDao
import com.gymora.data.local.dao.RoutineExerciseDao
import com.gymora.data.local.dao.SetTemplateDao
import com.gymora.data.local.dao.SettingsDao
import com.gymora.data.local.entity.ExerciseEntity
import com.gymora.data.local.entity.RoutineEntity
import com.gymora.data.local.entity.RoutineExerciseEntity
import com.gymora.data.local.entity.SetTemplateEntity
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
        ExerciseEntity::class, // registered by T014 (US1)
        RoutineEntity::class, // registered by T025 (US2)
        RoutineExerciseEntity::class, // registered by T025 (US2)
        SetTemplateEntity::class, // registered by T025 (US2)
    ],
    version = 1,
    exportSchema = true,
)
abstract class GymoraDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao

    abstract fun exerciseDao(): ExerciseDao

    abstract fun routineDao(): RoutineDao

    abstract fun routineExerciseDao(): RoutineExerciseDao

    abstract fun setTemplateDao(): SetTemplateDao

    companion object {
        const val NAME = "gymora.db"
    }
}
