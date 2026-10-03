package com.gymora.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gymora.data.local.dao.ExerciseDao
import com.gymora.data.local.dao.RoutineDao
import com.gymora.data.local.dao.RoutineExerciseDao
import com.gymora.data.local.dao.SetTemplateDao
import com.gymora.data.local.dao.SettingsDao
import com.gymora.data.local.dao.WorkoutExerciseDao
import com.gymora.data.local.dao.WorkoutSessionDao
import com.gymora.data.local.dao.WorkoutSetDao
import com.gymora.data.local.entity.ExerciseEntity
import com.gymora.data.local.entity.RoutineEntity
import com.gymora.data.local.entity.RoutineExerciseEntity
import com.gymora.data.local.entity.SetTemplateEntity
import com.gymora.data.local.entity.SettingsEntity
import com.gymora.data.local.entity.WorkoutExerciseEntity
import com.gymora.data.local.entity.WorkoutSessionEntity
import com.gymora.data.local.entity.WorkoutSetEntity

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
        WorkoutSessionEntity::class, // registered by T035 (US3)
        WorkoutExerciseEntity::class, // registered by T035 (US3)
        WorkoutSetEntity::class, // registered by T035 (US3)
        com.gymora.data.local.entity.BodyMeasurementEntity::class, // v2
    ],
    version = 2,
    exportSchema = true,
)
abstract class GymoraDatabase : RoomDatabase() {

    abstract fun bodyMeasurementDao(): com.gymora.data.local.dao.BodyMeasurementDao

    abstract fun settingsDao(): SettingsDao

    abstract fun exerciseDao(): ExerciseDao

    abstract fun routineDao(): RoutineDao

    abstract fun routineExerciseDao(): RoutineExerciseDao

    abstract fun setTemplateDao(): SetTemplateDao

    abstract fun workoutSessionDao(): WorkoutSessionDao

    abstract fun workoutExerciseDao(): WorkoutExerciseDao

    abstract fun workoutSetDao(): WorkoutSetDao

    companion object {
        const val NAME = "gymora.db"

        /** v1 → v2: adds body measurements; existing workout data is untouched. */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Room validates every table's indices right after a migration and
                // would reject the hand-made partial index (it is not in the entity).
                // Drop it here; DatabaseModule's onOpen callback recreates it.
                // Any future migration must do the same.
                db.execSQL("DROP INDEX IF EXISTS index_workout_sessions_single_active")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `body_measurements` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`measured_at` INTEGER NOT NULL, " +
                        "`weight_kg` REAL, `shoulders_cm` REAL, `chest_cm` REAL, " +
                        "`above_navel_cm` REAL, `navel_cm` REAL, `below_navel_cm` REAL, " +
                        "`thigh_cm` REAL)",
                )
            }
        }
    }
}
