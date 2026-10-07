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
    version = 5,
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

        /** v2 → v3: superset grouping on template and performed exercises. */
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // See MIGRATION_1_2: the partial index must be dropped before validation.
                db.execSQL("DROP INDEX IF EXISTS index_workout_sessions_single_active")
                db.execSQL("ALTER TABLE `routine_exercises` ADD COLUMN `superset_group` INTEGER")
                db.execSQL("ALTER TABLE `workout_exercises` ADD COLUMN `superset_group` INTEGER")
            }
        }

        /** v3 → v4: weekly goal and workout reminder schedule. */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // See MIGRATION_1_2: the partial index must be dropped before validation.
                db.execSQL("DROP INDEX IF EXISTS index_workout_sessions_single_active")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `weekly_goal` INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `reminder_days` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "ALTER TABLE `settings` ADD COLUMN `reminder_minute_of_day` INTEGER NOT NULL DEFAULT 1080",
                )
            }
        }

        /**
         * v4 → v5: more set measurement types (time, distance, assisted/weighted
         * bodyweight), unilateral sides, exercise form cues and media, auto-backup
         * and Health Connect settings. Built-in exercises get tracking defaults.
         */
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // See MIGRATION_1_2: the partial index must be dropped before validation.
                db.execSQL("DROP INDEX IF EXISTS index_workout_sessions_single_active")
                db.execSQL(
                    "ALTER TABLE `exercises` ADD COLUMN `measurement_type` TEXT NOT NULL " +
                        "DEFAULT 'WEIGHT_AND_REPS'",
                )
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `is_unilateral` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `form_cues_json` TEXT")
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `media_file` TEXT")
                db.execSQL("ALTER TABLE `workout_sets` ADD COLUMN `duration_seconds` INTEGER")
                db.execSQL("ALTER TABLE `workout_sets` ADD COLUMN `distance_m` REAL")
                db.execSQL("ALTER TABLE `workout_sets` ADD COLUMN `side` TEXT")
                db.execSQL("ALTER TABLE `set_templates` ADD COLUMN `target_duration_seconds` INTEGER")
                db.execSQL("ALTER TABLE `set_templates` ADD COLUMN `target_distance_m` REAL")
                db.execSQL(
                    "ALTER TABLE `settings` ADD COLUMN `auto_backup_interval_days` INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `auto_backup_folder_uri` TEXT")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `last_auto_backup_at` INTEGER")
                db.execSQL(
                    "ALTER TABLE `settings` ADD COLUMN `health_connect_enabled` INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("ALTER TABLE `body_measurements` ADD COLUMN `external_id` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_body_measurements_external_id` " +
                        "ON `body_measurements` (`external_id`)",
                )
                applyTrackingDefaults(db)
            }
        }

        /**
         * Gives built-in exercises their best-guess tracking type and side mode, and
         * points their routine targets at the same type. History keeps its recorded types.
         */
        private fun applyTrackingDefaults(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            val updates = mutableListOf<Triple<Long, String, Boolean>>()
            db.query("SELECT id, name, category, equipment_json FROM exercises WHERE is_custom = 0").use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1)
                    val category = c.getString(2)?.let { raw ->
                        com.gymora.domain.model.ExerciseCategory.entries.firstOrNull { it.name == raw }
                    }
                    val equipment = com.gymora.data.local.ExerciseMetadataCodec
                        .decodeEquipment(c.getString(3)).map { it.name }
                    val type = com.gymora.domain.model.ExerciseTrackingDefaults
                        .measurementType(name, category, equipment)
                    val unilateral = com.gymora.domain.model.ExerciseTrackingDefaults.isUnilateral(name)
                    if (type != com.gymora.domain.model.MeasurementType.WEIGHT_AND_REPS || unilateral) {
                        updates += Triple(c.getLong(0), type.name, unilateral)
                    }
                }
            }
            updates.forEach { (id, type, unilateral) ->
                db.execSQL(
                    "UPDATE exercises SET measurement_type = ?, is_unilateral = ? WHERE id = ?",
                    arrayOf<Any>(type, if (unilateral) 1 else 0, id),
                )
                db.execSQL(
                    "UPDATE set_templates SET measurement_type = ? WHERE routine_exercise_id IN " +
                        "(SELECT id FROM routine_exercises WHERE exercise_id = ?)",
                    arrayOf<Any>(type, id),
                )
            }
        }
    }
}
