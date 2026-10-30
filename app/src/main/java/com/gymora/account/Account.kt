package com.gymora.account

/**
 * Someone whose workouts, routines and settings live in their own database on this device.
 * [GUEST_ID] is the signed-out profile and keeps the pre-accounts data where it always was.
 */
data class Account(
    val id: String,
    val displayName: String,
    val email: String? = null,
) {
    val isGuest: Boolean get() = id == GUEST_ID

    companion object {
        const val GUEST_ID = "guest"

        val GUEST = Account(id = GUEST_ID, displayName = "Guest")
    }
}
