package com.gymora.ui.workout

/**
 * Ephemeral rest-timer state (R-06: not persisted — end-instant lives in
 * memory only). The timer is independent of the workout duration ticker
 * (FR-031, FR-032).
 */
data class RestTimerState(
    val isRunning: Boolean = false,
    val endInstantMs: Long? = null,
    val defaultSeconds: Int = 90,
    val remainingSeconds: Long = 0,
)