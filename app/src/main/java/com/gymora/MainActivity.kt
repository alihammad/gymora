package com.gymora

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gymora.domain.model.Settings
import com.gymora.domain.repository.SettingsRepository
import com.gymora.ui.navigation.GymoraNavHost
import com.gymora.ui.theme.GymoraTheme
import com.gymora.ui.theme.palette
import com.gymora.widget.GymoraWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch // DEMO_SEED (temporary)

/**
 * Single-activity host for the Compose navigation graph (FR-004).
 * Applies the persisted theme preference (FR-051) from settings (T010).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    // DEMO_SEED (temporary): remove with DemoDataSeeder.kt
    @Inject
    lateinit var demoDataSeeder: com.gymora.data.local.seed.DemoDataSeeder

    /** Routine to start a workout from, requested by the widget or a reminder; null once handled. */
    private var startRoutineRequest by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only a fresh launch: after a configuration change the request was already handled.
        if (savedInstanceState == null) readStartRequest(intent)
        // DEMO_SEED (temporary)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            // Wait for first-launch library/routine seeding before inserting demo history.
            kotlinx.coroutines.delay(3000)
            demoDataSeeder.seed()
        }
        setContent {
            val settings by settingsRepository.observeSettings()
                .collectAsState(initial = Settings.DEFAULTS)
            val palette = settings.theme.palette(systemDark = isSystemInDarkTheme())

            // Edge-to-edge with transparent bars; icon tint follows the palette's brightness.
            DisposableEffect(palette.isDark) {
                val barStyle = if (palette.isDark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }

            GymoraTheme(palette = palette) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GymoraNavHost(
                        startRoutineRequest = startRoutineRequest,
                        onStartRoutineHandled = { startRoutineRequest = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readStartRequest(intent)
    }

    override fun onStop() {
        super.onStop()
        // Workouts, routines or the goal may have changed while the app was open.
        GymoraWidgetProvider.refresh(this)
    }

    private fun readStartRequest(intent: Intent) {
        val routineId = intent.getLongExtra(LaunchIntents.EXTRA_START_ROUTINE_ID, -1L)
        if (routineId > 0) startRoutineRequest = routineId
    }
}
