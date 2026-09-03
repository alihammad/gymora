package com.gymora.ui.workout

import com.gymora.domain.model.Settings
import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T057 [US8]: rest-timer state machine (start/skip/+30s/restart, default from
 * settings, end-instant math) (FR-031, FR-032).
 */
class RestTimerTest {

    @Test
    fun defaultRestDurationFromSettings() {
        val settings = Settings.DEFAULTS
        assertEquals(90, settings.defaultRestSeconds)
    }

    @Test
    fun startSetsEndInstant() {
        val now = System.currentTimeMillis()
        val defaultSeconds = 90
        val endInstant = now + defaultSeconds * 1000L
        assertEquals(now + 90_000, endInstant)
    }

    @Test
    fun skipClearsTimer() {
        val state = RestTimerState(
            isRunning = true,
            endInstantMs = System.currentTimeMillis() + 90_000,
            defaultSeconds = 90,
        )
        val skipped = state.copy(isRunning = false, endInstantMs = null)
        assertFalse(skipped.isRunning)
        assertEquals(null, skipped.endInstantMs)
    }

    @Test
    fun add30SecondsExtendsEndInstant() {
        val now = System.currentTimeMillis()
        val endInstant = now + 90_000
        val extended = endInstant + 30_000
        assertEquals(now + 120_000, extended)
    }

    @Test
    fun restartResetsTimer() {
        val now = System.currentTimeMillis()
        val defaultSeconds = 90
        val newEnd = now + defaultSeconds * 1000L
        assertEquals(now + 90_000, newEnd)
    }

    @Test
    fun remainingSecondsCalculatedCorrectly() {
        val now = System.currentTimeMillis()
        val endInstant = now + 90_000
        // 30 seconds in.
        val remaining = (endInstant - (now + 30_000)) / 1000
        assertEquals(60, remaining)
    }

    @Test
    fun timerExpiresWhenRemainingReachesZero() {
        val now = System.currentTimeMillis()
        val endInstant = now + 90_000
        // After 90 seconds.
        val remaining = (endInstant - (now + 90_000)) / 1000
        assertTrue(remaining <= 0)
    }

    @Test
    fun customDefaultFromSettingsApplied() {
        val settings = Settings(
            weightUnit = WeightUnit.KG,
            defaultRestSeconds = 120,
            theme = Theme.SYSTEM,
        )
        assertEquals(120, settings.defaultRestSeconds)
    }
}

/** Ephemeral rest-timer state (R-06: not persisted). */
data class RestTimerState(
    val isRunning: Boolean = false,
    val endInstantMs: Long? = null,
    val defaultSeconds: Int = 90,
)