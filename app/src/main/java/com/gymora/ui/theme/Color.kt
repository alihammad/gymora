package com.gymora.ui.theme

import androidx.compose.ui.graphics.Color

// Design tokens live in GymoraPalette (see Palettes.kt), one per selectable theme. Screens must read
// colors from MaterialTheme.colorScheme or GymoraTheme.extraColors; never hardcode hex values.

/** Colors that have no Material 3 ColorScheme slot. */
data class ExtraColors(
    val secondaryText: Color,
    val textDisabled: Color,
    val warning: Color,
    val success: Color,
    val divider: Color,
    val track: Color,
    val primaryPressed: Color,
    val glow: Color,
    /** Accents cycled per workout tile. */
    val tileAccents: List<Color>,
    /** Foreground for icons drawn on a [tileAccents] fill. */
    val onTileAccents: List<Color>,
    /** Accent colors safe to use as small text on the surface (secondary uses its readable variant). */
    val tileAccentTexts: List<Color>,
)

fun GymoraPalette.toExtraColors() = ExtraColors(
    secondaryText = secondaryText,
    textDisabled = textDisabled,
    warning = warning,
    success = success,
    divider = divider,
    track = surfaceHighest,
    primaryPressed = primaryPressed,
    glow = primary.copy(alpha = 0.06f),
    tileAccents = listOf(primary, secondary),
    onTileAccents = listOf(onPrimary, onSecondary),
    tileAccentTexts = listOf(primary, secondaryText),
)
