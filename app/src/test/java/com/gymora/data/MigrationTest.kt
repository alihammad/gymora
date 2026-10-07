package com.gymora.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.gymora.data.local.db.GymoraDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Upgrades keep existing data and match the current schema (BR-19, Constitution V). */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymoraDatabase::class.java,
    )

    @Test
    fun migrate3To4KeepsSettingsAndAddsGoalAndReminderDefaults() {
        helper.createDatabase(DB_NAME, 3).use { db ->
            db.execSQL(
                "INSERT INTO settings (id, weight_unit, default_rest_seconds, theme, updated_at) " +
                    "VALUES (1, 'LB', 120, 'DARK', 0)",
            )
        }

        // Validates the migrated schema against the v4 entities.
        val db = helper.runMigrationsAndValidate(DB_NAME, 4, true, GymoraDatabase.MIGRATION_3_4)

        db.query(
            "SELECT weight_unit, default_rest_seconds, weekly_goal, reminder_days, reminder_minute_of_day " +
                "FROM settings",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("LB", cursor.getString(0))
            assertEquals(120, cursor.getInt(1))
            assertEquals(3, cursor.getInt(2))
            assertEquals(0, cursor.getInt(3))
            assertEquals(18 * 60, cursor.getInt(4))
        }
        db.close()
    }

    @Test
    fun migrate4To5AddsTrackingAndGuessesBuiltInTypes() {
        helper.createDatabase(DB_NAME, 4).use { db ->
            val equipment = """[{"name":"Body Only","type":"WEIGHT","usageType":"SINGLE"}]"""
            db.execSQL(
                "INSERT INTO exercises (id, name, is_custom, created_at, updated_at, category, equipment_json) " +
                    "VALUES (1, 'Plank', 0, 0, 0, 'STRENGTH', '$equipment'), " +
                    "(2, 'One-Arm Dumbbell Row', 0, 0, 0, 'STRENGTH', NULL), " +
                    "(3, 'My Plank', 1, 0, 0, NULL, NULL)",
            )
            db.execSQL("INSERT INTO routines (id, name, position, created_at, updated_at) VALUES (1, 'Core', 0, 0, 0)")
            db.execSQL("INSERT INTO routine_exercises (id, routine_id, exercise_id, position) VALUES (1, 1, 1, 0)")
            db.execSQL(
                "INSERT INTO set_templates (id, routine_exercise_id, set_number, target_reps, measurement_type) " +
                    "VALUES (1, 1, 1, 10, 'WEIGHT_AND_REPS')",
            )
            db.execSQL(
                "INSERT INTO workout_sessions (id, routine_name_snapshot, started_at, status, created_at) " +
                    "VALUES (1, 'Core', 0, 'COMPLETED', 0)",
            )
            db.execSQL(
                "INSERT INTO workout_exercises (id, session_id, exercise_id, exercise_name_snapshot, position) " +
                    "VALUES (1, 1, 1, 'Plank', 0)",
            )
            db.execSQL(
                "INSERT INTO workout_sets " +
                    "(id, workout_exercise_id, set_number, reps, measurement_type, is_completed) " +
                    "VALUES (1, 1, 1, 10, 'WEIGHT_AND_REPS', 1)",
            )
            db.execSQL(
                "INSERT INTO settings (id, weight_unit, default_rest_seconds, theme, updated_at) " +
                    "VALUES (1, 'KG', 90, 'DARK', 0)",
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 5, true, GymoraDatabase.MIGRATION_4_5)

        db.query("SELECT id, measurement_type, is_unilateral FROM exercises ORDER BY id").use { c ->
            c.moveToNext()
            assertEquals("DURATION", c.getString(1))
            c.moveToNext()
            assertEquals("WEIGHT_AND_REPS", c.getString(1))
            assertEquals(1, c.getInt(2))
            c.moveToNext()
            assertEquals("Custom exercises keep the default", "WEIGHT_AND_REPS", c.getString(1))
        }
        db.query("SELECT measurement_type FROM set_templates").use { c ->
            c.moveToFirst()
            assertEquals("DURATION", c.getString(0))
        }
        db.query("SELECT measurement_type, reps, duration_seconds, side FROM workout_sets").use { c ->
            c.moveToFirst()
            assertEquals("History keeps its recorded type", "WEIGHT_AND_REPS", c.getString(0))
            assertEquals(10, c.getInt(1))
            assertTrue(c.isNull(2) && c.isNull(3))
        }
        db.query("SELECT auto_backup_interval_days, health_connect_enabled FROM settings").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
            assertEquals(0, c.getInt(1))
        }
        db.close()
    }

    @Test
    fun migrate5To6AddsStepGoalDefaultAndStepTable() {
        helper.createDatabase(DB_NAME, 5).use { db ->
            db.execSQL(
                "INSERT INTO settings (id, weight_unit, default_rest_seconds, theme, updated_at) " +
                    "VALUES (1, 'KG', 90, 'DARK', 0)",
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 6, true, GymoraDatabase.MIGRATION_5_6)

        db.query("SELECT step_goal, health_steps_enabled FROM settings").use { c ->
            c.moveToFirst()
            assertEquals(10_000, c.getInt(0))
            assertEquals(0, c.getInt(1))
        }
        db.execSQL("INSERT INTO step_days (date, steps, last_counter) VALUES ('2026-10-07', 1200, 5000)")
        db.close()
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
