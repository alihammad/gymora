package com.gymora.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.gymora.domain.calculator.EngagementCalculators
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps one alarm pending for the next reminder. [ReminderReceiver] posts the
 * notification when it fires and schedules the one after it, so only the next
 * occurrence is ever registered with the system.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun schedule(days: Set<DayOfWeek>, time: LocalTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent()
        val next = EngagementCalculators.nextReminder(LocalDateTime.now(), days, time)
        if (next == null) {
            alarmManager.cancel(pending)
            return
        }
        // Inexact but Doze-safe; exact alarms need a special permission a reminder doesn't justify.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pending,
        )
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
