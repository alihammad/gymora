package com.gymora.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val NightVoltColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SurfaceRaised,
    onSecondaryContainer = TextPrimary,
    tertiary = Secondary,
    onTertiary = OnSecondary,
    tertiaryContainer = SurfaceRaised,
    onTertiaryContainer = TextPrimary,
    error = Error,
    onError = OnError,
    errorContainer = Error,
    onErrorContainer = OnError,
    background = Background,
    onBackground = TextPrimary,
    surface = Background,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHighest,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Primary,
    outline = Outline,
    outlineVariant = Divider,
    inverseSurface = TextPrimary,
    inverseOnSurface = Background,
    inversePrimary = Primary,
    scrim = Background,
    surfaceBright = SurfaceRaised,
    surfaceDim = Background,
    surfaceContainerLowest = Background,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceRaised,
    surfaceContainerHighest = SurfaceHighest,
)

private val LocalExtraColors = staticCompositionLocalOf { ExtraColors() }

/** Access to tokens that have no Material 3 ColorScheme slot. */
object GymoraThemeTokens {
    val extraColors: ExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtraColors.current
}

/**
 * Night volt Material 3 theme. Always dark, never uses dynamic color.
 *
 * @param themeMode kept for call-site compatibility; the persisted preference
 * is ignored because the design system is dark only.
 * @param content the themed content.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun GymoraTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalExtraColors provides ExtraColors()) {
        MaterialTheme(
            colorScheme = NightVoltColors,
            typography = Typography,
            shapes = MaterialShapes,
            content = content,
        )
    }
}

/** UI-level theme selection mirroring the persisted [Theme] setting. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
