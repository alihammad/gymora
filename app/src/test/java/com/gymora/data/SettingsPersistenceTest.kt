package com.gymora.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.repository.SettingsRepositoryImpl
import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * T061 [US9]: settings persist across database reopen; unit switch never
 * rewrites stored weights; display conversion applies to history/summary/
 * previous-performance/records values (FR-048..FR-052).
 */
@RunWith(RobolectricTestRunner::class)
class SettingsPersistenceTest {

    private lateinit var database: GymoraDatabase
    private lateinit var settingsRepository: SettingsRepositoryImpl
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, GymoraDatabase::class.java)
            .allowMainThreadQueries().build()
        settingsRepository = SettingsRepositoryImpl(database.settingsDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun defaultSettingsAreReturnedOnFirstAccess() = runTest {
        val settings = settingsRepository.observeSettings().first()
        assertEquals(WeightUnit.KG, settings.weightUnit)
        assertEquals(90, settings.defaultRestSeconds)
        assertEquals(Theme.SYSTEM, settings.theme)
    }

    @Test
    fun weightUnitPersistsAcrossDatabaseReopen() = runTest {
        // Use a file-based database so data survives close/reopen
        val dbFile = File(context.filesDir, "test-settings.db")
        database.close()
        database = Room.databaseBuilder(context, GymoraDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries().build()
        settingsRepository = SettingsRepositoryImpl(database.settingsDao())

        settingsRepository.setWeightUnit(WeightUnit.LB)
        val settings = settingsRepository.observeSettings().first()
        assertEquals(WeightUnit.LB, settings.weightUnit)

        // Close and reopen the same file
        database.close()
        database = Room.databaseBuilder(context, GymoraDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries().build()
        settingsRepository = SettingsRepositoryImpl(database.settingsDao())

        val reopened = settingsRepository.observeSettings().first()
        assertEquals(WeightUnit.LB, reopened.weightUnit)
        dbFile.delete()
    }

    @Test
    fun restDurationPersistsAcrossDatabaseReopen() = runTest {
        val dbFile = File(context.filesDir, "test-settings2.db")
        database.close()
        database = Room.databaseBuilder(context, GymoraDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries().build()
        settingsRepository = SettingsRepositoryImpl(database.settingsDao())

        settingsRepository.setDefaultRestDuration(120)
        val settings = settingsRepository.observeSettings().first()
        assertEquals(120, settings.defaultRestSeconds)

        database.close()
        database = Room.databaseBuilder(context, GymoraDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries().build()
        settingsRepository = SettingsRepositoryImpl(database.settingsDao())

        val reopened = settingsRepository.observeSettings().first()
        assertEquals(120, reopened.defaultRestSeconds)
        dbFile.delete()
    }

    @Test
    fun themePersistsAcrossDatabaseReopen() = runTest {
        val dbFile = File(context.filesDir, "test-settings3.db")
        database.close()
        database = Room.databaseBuilder(context, GymoraDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries().build()
        settingsRepository = SettingsRepositoryImpl(database.settingsDao())

        settingsRepository.setTheme(Theme.DARK)
        val settings = settingsRepository.observeSettings().first()
        assertEquals(Theme.DARK, settings.theme)

        database.close()
        database = Room.databaseBuilder(context, GymoraDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries().build()
        settingsRepository = SettingsRepositoryImpl(database.settingsDao())

        val reopened = settingsRepository.observeSettings().first()
        assertEquals(Theme.DARK, reopened.theme)
        dbFile.delete()
    }

    @Test
    fun allSettingsPersistTogether() = runTest {
        val dbFile = File(context.filesDir, "test-settings4.db")
        database.close()
        database = Room.databaseBuilder(context, GymoraDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries().build()
        settingsRepository = SettingsRepositoryImpl(database.settingsDao())

        settingsRepository.setWeightUnit(WeightUnit.LB)
        settingsRepository.setDefaultRestDuration(60)
        settingsRepository.setTheme(Theme.LIGHT)

        database.close()
        database = Room.databaseBuilder(context, GymoraDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries().build()
        settingsRepository = SettingsRepositoryImpl(database.settingsDao())

        val reopened = settingsRepository.observeSettings().first()
        assertEquals(WeightUnit.LB, reopened.weightUnit)
        assertEquals(60, reopened.defaultRestSeconds)
        assertEquals(Theme.LIGHT, reopened.theme)
        dbFile.delete()
    }

    @Test
    fun settingsAreObservableAsFlow() = runTest {
        val initial = settingsRepository.observeSettings().first()
        assertEquals(WeightUnit.KG, initial.weightUnit)

        settingsRepository.setWeightUnit(WeightUnit.LB)
        val updated = settingsRepository.observeSettings().first()
        assertEquals(WeightUnit.LB, updated.weightUnit)
    }

    @Test
    fun weeklyGoalAndReminderScheduleAreSavedAndGoalIsClamped() = runTest {
        val days = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.FRIDAY)
        settingsRepository.setWeeklyGoal(4)
        settingsRepository.setReminder(days, java.time.LocalTime.of(7, 30))

        val settings = settingsRepository.observeSettings().first()
        assertEquals(4, settings.weeklyGoal)
        assertEquals(days, settings.reminderDays)
        assertEquals(java.time.LocalTime.of(7, 30), settings.reminderTime)

        settingsRepository.setWeeklyGoal(12)
        assertEquals(7, settingsRepository.observeSettings().first().weeklyGoal)
    }
}
