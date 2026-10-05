package com.gymora.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private fun GymoraPalette.toColorScheme(): ColorScheme {
    val scrim = if (isDark) background else Color.Black
    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = surfaceRaised,
            onSecondaryContainer = textPrimary,
            tertiary = secondary,
            onTertiary = onSecondary,
            tertiaryContainer = surfaceRaised,
            onTertiaryContainer = textPrimary,
            error = error,
            onError = onError,
            errorContainer = error,
            onErrorContainer = onError,
            background = background,
            onBackground = textPrimary,
            surface = background,
            onSurface = textPrimary,
            surfaceVariant = surfaceHighest,
            onSurfaceVariant = textSecondary,
            surfaceTint = primary,
            outline = outline,
            outlineVariant = divider,
            inverseSurface = textPrimary,
            inverseOnSurface = background,
            inversePrimary = primary,
            scrim = scrim,
            surfaceBright = surfaceRaised,
            surfaceDim = background,
            surfaceContainerLowest = background,
            surfaceContainerLow = surfaceLow,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceRaised,
            surfaceContainerHighest = surfaceHighest,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = surfaceRaised,
            onSecondaryContainer = textPrimary,
            tertiary = secondary,
            onTertiary = onSecondary,
            tertiaryContainer = surfaceRaised,
            onTertiaryContainer = textPrimary,
            error = error,
            onError = onError,
            errorContainer = error,
            onErrorContainer = onError,
            background = background,
            onBackground = textPrimary,
            surface = background,
            onSurface = textPrimary,
            surfaceVariant = surfaceHighest,
            onSurfaceVariant = textSecondary,
            surfaceTint = primary,
            outline = outline,
            outlineVariant = divider,
            inverseSurface = textPrimary,
            inverseOnSurface = background,
            inversePrimary = primary,
            scrim = scrim,
            surfaceBright = surface,
            surfaceDim = surfaceHighest,
            surfaceContainerLowest = background,
            surfaceContainerLow = surfaceLow,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceRaised,
            surfaceContainerHighest = surfaceHighest,
        )
    }
}

private val LocalExtraColors = staticCompositionLocalOf<ExtraColors> {
    error("GymoraTheme not provided")
}

/** Access to tokens that have no Material 3 ColorScheme slot. */
object GymoraThemeTokens {
    val extraColors: ExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtraColors.current
}

/**
 * Gymora Material 3 theme. Never uses dynamic color.
 *
 * @param palette the selected theme's colors; see [com.gymora.domain.model.Theme] and [palette].
 * @param content the themed content.
 */
@Composable
fun GymoraTheme(
    palette: GymoraPalette = NightVoltPalette,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(palette) { palette.toColorScheme() }
    val extraColors = remember(palette) { palette.toExtraColors() }
    CompositionLocalProvider(LocalExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = MaterialShapes,
            content = content,
        )
    }
}
