package com.gymora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.gymora.domain.model.Settings
import com.gymora.domain.model.Theme
import com.gymora.domain.repository.SettingsRepository
import com.gymora.ui.navigation.GymoraNavHost
import com.gymora.ui.theme.GymoraTheme
import com.gymora.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity host for the Compose navigation graph (FR-004).
 * Applies the persisted theme preference (FR-051) from settings (T010).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsRepository.observeSettings()
                .collectAsState(initial = Settings.DEFAULTS)

            GymoraTheme(themeMode = settings.theme.toMode()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GymoraNavHost()
                }
            }
        }
    }
}

private fun Theme.toMode(): ThemeMode = when (this) {
    Theme.SYSTEM -> ThemeMode.SYSTEM
    Theme.LIGHT -> ThemeMode.LIGHT
    Theme.DARK -> ThemeMode.DARK
}
