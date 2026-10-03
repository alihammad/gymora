package com.gymora.ui.theme

import androidx.compose.ui.graphics.Color

// "Night volt" design tokens. Screens must read colors from MaterialTheme.colorScheme
// or GymoraTheme.extraColors; never hardcode hex values.

val Background = Color(0xFF0E0F12)
val Surface = Color(0xFF1A1C22)
val SurfaceLow = Color(0xFF15171C)
val SurfaceRaised = Color(0xFF23262E)
val SurfaceHighest = Color(0xFF2A2D35)

val Primary = Color(0xFFC6F432)
val OnPrimary = Color(0xFF0E0F12)
val PrimaryContainer = Color(0xFF2B3A0A)
val OnPrimaryContainer = Color(0xFFE3FA99)

val Secondary = Color(0xFF7B61FF)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryText = Color(0xFF9D8BFF)

val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF9A9CA5)
val TextDisabled = Color(0xFF5C5F68)

val Outline = Color(0xFF3A3D46)
val Divider = Color(0xFF2A2D35)

val Error = Color(0xFFFF5C5C)
val OnError = Color(0xFF0E0F12)
val Warning = Color(0xFFFFB020)

/** Primary pressed fill (slightly darker lime). */
val PrimaryPressed = Color(0xFFB0DB2B)

/** Colors that have no Material 3 ColorScheme slot. */
data class ExtraColors(
    val secondaryText: Color = SecondaryText,
    val textDisabled: Color = TextDisabled,
    val warning: Color = Warning,
    val success: Color = Primary,
    val divider: Color = Divider,
    val track: Color = SurfaceHighest,
    val primaryPressed: Color = PrimaryPressed,
    val glow: Color = Primary.copy(alpha = 0.06f),
)

/**
 * Accents cycled per routine tile. Night volt limits accents to lime and violet.
 */
val TileAccents = listOf(Primary, Secondary)

/** Foreground for icons drawn on a [TileAccents] fill. */
val OnTileAccents = listOf(OnPrimary, OnSecondary)

/** Accent colors safe to use as small text on dark surfaces (violet uses its lighter variant). */
val TileAccentTexts = listOf(Primary, SecondaryText)
