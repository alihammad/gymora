package com.gymora.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.gymora.data.local.db.GymoraDatabase
import org.junit.Assert.assertEquals
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

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
