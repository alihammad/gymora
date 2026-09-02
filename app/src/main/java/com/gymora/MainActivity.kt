package com.gymora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gymora.ui.theme.GymoraTheme
import com.gymora.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the Compose navigation graph (FR-004).
 * The navigation shell is wired in Phase 2 (T009).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymoraTheme(themeMode = ThemeMode.SYSTEM) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "Gymora")
                    }
                }
            }
        }
    }
}
