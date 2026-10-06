package com.gymora

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Intents that open the app from outside it: the home-screen widget and reminder notifications. */
object LaunchIntents {

    /** Routine id to start a workout from as soon as the app opens. */
    const val EXTRA_START_ROUTINE_ID = "com.gymora.extra.START_ROUTINE_ID"

    private const val REQUEST_OPEN = 0
    private const val REQUEST_START = 1

    fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(context, REQUEST_OPEN, baseIntent(context), FLAGS)

    fun startRoutine(context: Context, routineId: Long): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_START,
            baseIntent(context).putExtra(EXTRA_START_ROUTINE_ID, routineId),
            FLAGS,
        )

    /** Reuses the running activity (singleTop) so the request arrives via onNewIntent. */
    private fun baseIntent(context: Context) = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}
