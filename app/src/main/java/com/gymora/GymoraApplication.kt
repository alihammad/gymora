package com.gymora

import android.app.Application
import com.gymora.data.backup.AutoBackupScheduler
import com.gymora.data.steps.StepSyncScheduler
import com.gymora.domain.repository.SettingsRepository
import com.gymora.reminders.ReminderReceiver
import com.gymora.reminders.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Application entry point; Hilt root for dependency injection. */
@HiltAndroidApp
class GymoraApplication : Application() {

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        ReminderReceiver.createChannel(this)
        // Nightly step snapshot, so steps taken while the app is closed land on the right day.
        StepSyncScheduler.ensureScheduled(this)
        // Keep the pending reminder alarm in step with the saved schedule.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settingsRepository.observeSettings()
                .map { it.reminderDays to it.reminderTime }
                .distinctUntilChanged()
                .collect { (days, time) -> reminderScheduler.schedule(days, time) }
        }
        // Likewise for the automatic backup schedule.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settingsRepository.observeSettings()
                .map { it.autoBackupIntervalDays to it.autoBackupFolderUri }
                .distinctUntilChanged()
                .collect { (days, folder) -> AutoBackupScheduler.schedule(this@GymoraApplication, days, folder) }
        }
    }
}
