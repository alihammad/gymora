package com.gymora.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gymora.LaunchIntents
import com.gymora.R
import com.gymora.domain.repository.SettingsRepository
import com.gymora.domain.usecase.Engagement
import com.gymora.domain.usecase.GetEngagementUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires workout reminders and keeps the schedule alive: on [ACTION_REMIND] it
 * posts the notification (skipped if the user already trained today), and on
 * every broadcast — including boot and clock changes — it schedules the next one.
 */
class ReminderReceiver : BroadcastReceiver() {

    /** Entry point rather than @AndroidEntryPoint: onReceive is abstract, so Hilt's super call can't compile. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun settingsRepository(): SettingsRepository
        fun getEngagement(): GetEngagementUseCase
        fun reminderScheduler(): ReminderScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        val deps = EntryPointAccessors.fromApplication(context, Dependencies::class.java)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = deps.settingsRepository().observeSettings().first()
                if (intent.action == ACTION_REMIND && settings.reminderDays.isNotEmpty()) {
                    val engagement = deps.getEngagement()()
                    if (!engagement.trainedToday) notify(context, engagement)
                }
                deps.reminderScheduler().schedule(settings.reminderDays, settings.reminderTime)
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, engagement: Engagement) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel(context)
        val progress = engagement.progress
        val routine = engagement.nextRoutine
        val text = buildString {
            append("${progress.done} of ${progress.goal} workouts this week")
            if (routine != null) append(" · Next up: ${routine.name}")
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_gymora)
            .setContentTitle("Time to train")
            .setContentText(text)
            .setContentIntent(LaunchIntents.openApp(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        if (routine != null) {
            builder.addAction(0, "Start ${routine.name}", LaunchIntents.startRoutine(context, routine.id))
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    companion object {
        const val ACTION_REMIND = "com.gymora.action.WORKOUT_REMINDER"
        private const val CHANNEL_ID = "workout_reminders"
        private const val NOTIFICATION_ID = 1001

        /** Idempotent; also called at app start so the channel shows in system settings early. */
        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Workout reminders", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Reminders on the weekdays you plan to train" }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
