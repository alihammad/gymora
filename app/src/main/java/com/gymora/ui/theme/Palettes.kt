package com.gymora.ui.theme

import androidx.compose.ui.graphics.Color
import com.gymora.domain.model.Theme

/** Every color token a theme defines. Material 3 slots and [ExtraColors] are both derived from it. */
data class GymoraPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceLow: Color,
    val surfaceRaised: Color,
    val surfaceHighest: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryPressed: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryText: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val outline: Color,
    val divider: Color,
    val error: Color,
    val onError: Color,
    val warning: Color,
    val success: Color,
)

private val ErrorRed = Color(0xFFFF5C5C)
private val WarningAmber = Color(0xFFFFB020)
private val SuccessGreen = Color(0xFF34D399)

/** The original theme: near-black with lime and violet. */
val NightVoltPalette = GymoraPalette(
    isDark = true,
    background = Color(0xFF0E0F12),
    surface = Color(0xFF1A1C22),
    surfaceLow = Color(0xFF15171C),
    surfaceRaised = Color(0xFF23262E),
    surfaceHighest = Color(0xFF2A2D35),
    primary = Color(0xFFC6F432),
    onPrimary = Color(0xFF0E0F12),
    primaryContainer = Color(0xFF2B3A0A),
    onPrimaryContainer = Color(0xFFE3FA99),
    primaryPressed = Color(0xFFB0DB2B),
    secondary = Color(0xFF7B61FF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryText = Color(0xFF9D8BFF),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFF9A9CA5),
    textDisabled = Color(0xFF5C5F68),
    outline = Color(0xFF3A3D46),
    divider = Color(0xFF2A2D35),
    error = ErrorRed,
    onError = Color(0xFF0E0F12),
    warning = WarningAmber,
    success = Color(0xFFC6F432),
)

/** Premium analytics: charcoal with an electric cyan accent. */
val ObsidianPalette = GymoraPalette(
    isDark = true,
    background = Color(0xFF0B0B0D),
    surface = Color(0xFF18181B),
    surfaceLow = Color(0xFF111114),
    surfaceRaised = Color(0xFF202024),
    surfaceHighest = Color(0xFF27272B),
    primary = Color(0xFF00D1FF),
    onPrimary = Color(0xFF0B0B0D),
    primaryContainer = Color(0xFF06323D),
    onPrimaryContainer = Color(0xFFB3F0FF),
    primaryPressed = Color(0xFF00B8E0),
    secondary = Color(0xFF6366F1),
    onSecondary = Color(0xFFFFFFFF),
    secondaryText = Color(0xFFA5B4FC),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFA1A1AA),
    textDisabled = Color(0xFF5F5F66),
    outline = Color(0xFF34343A),
    divider = Color(0xFF232327),
    error = ErrorRed,
    onError = Color(0xFF0B0B0D),
    warning = WarningAmber,
    success = SuccessGreen,
)

/** Performance: graphite with lime reserved for wins and the primary action. */
val GraphiteLimePalette = GymoraPalette(
    isDark = true,
    background = Color(0xFF101312),
    surface = Color(0xFF1A211E),
    surfaceLow = Color(0xFF151A18),
    surfaceRaised = Color(0xFF232B27),
    surfaceHighest = Color(0xFF2B3530),
    primary = Color(0xFFB6F500),
    onPrimary = Color(0xFF101312),
    primaryContainer = Color(0xFF2A3A05),
    onPrimaryContainer = Color(0xFFDDFB8A),
    primaryPressed = Color(0xFFA3DB00),
    secondary = Color(0xFF7DD3C0),
    onSecondary = Color(0xFF101312),
    secondaryText = Color(0xFF7DD3C0),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF9BA8A2),
    textDisabled = Color(0xFF5B6862),
    outline = Color(0xFF38443E),
    divider = Color(0xFF26302B),
    error = ErrorRed,
    onError = Color(0xFF101312),
    warning = WarningAmber,
    success = Color(0xFFB6F500),
)

/** Fitness intelligence: deep navy with electric blue. */
val MidnightBluePalette = GymoraPalette(
    isDark = true,
    background = Color(0xFF080D18),
    surface = Color(0xFF111827),
    surfaceLow = Color(0xFF0D1320),
    surfaceRaised = Color(0xFF1A2336),
    surfaceHighest = Color(0xFF222D44),
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFBFDBFE),
    primaryPressed = Color(0xFF2F6FDB),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF080D18),
    secondaryText = Color(0xFF60A5FA),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textDisabled = Color(0xFF556075),
    outline = Color(0xFF2B3750),
    divider = Color(0xFF1E293B),
    error = ErrorRed,
    onError = Color(0xFF080D18),
    warning = WarningAmber,
    success = SuccessGreen,
)

/** AI fitness: black with violet. */
val BlackPurplePalette = GymoraPalette(
    isDark = true,
    background = Color(0xFF09090B),
    surface = Color(0xFF18181B),
    surfaceLow = Color(0xFF111113),
    surfaceRaised = Color(0xFF222226),
    surfaceHighest = Color(0xFF2A2A2F),
    primary = Color(0xFF8B5CF6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2E1F5E),
    onPrimaryContainer = Color(0xFFDDD2FE),
    primaryPressed = Color(0xFF7C4DE8),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF09090B),
    secondaryText = Color(0xFFA78BFA),
    textPrimary = Color(0xFFFAFAFA),
    textSecondary = Color(0xFFA1A1AA),
    textDisabled = Color(0xFF5E5E66),
    outline = Color(0xFF36363C),
    divider = Color(0xFF26262B),
    error = ErrorRed,
    onError = Color(0xFF09090B),
    warning = WarningAmber,
    success = SuccessGreen,
)

/** High energy: charcoal with orange. */
val CharcoalOrangePalette = GymoraPalette(
    isDark = true,
    background = Color(0xFF101010),
    surface = Color(0xFF1C1C1C),
    surfaceLow = Color(0xFF151515),
    surfaceRaised = Color(0xFF262626),
    surfaceHighest = Color(0xFF2E2E2E),
    primary = Color(0xFFFF6B35),
    onPrimary = Color(0xFF101010),
    primaryContainer = Color(0xFF4A1F0E),
    onPrimaryContainer = Color(0xFFFFD2BF),
    primaryPressed = Color(0xFFE85F2D),
    secondary = Color(0xFFFFA94D),
    onSecondary = Color(0xFF101010),
    secondaryText = Color(0xFFFFA94D),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA3A3A3),
    textDisabled = Color(0xFF60605F),
    outline = Color(0xFF3C3C3C),
    divider = Color(0xFF2A2A2A),
    error = Color(0xFFFF4D4D),
    onError = Color(0xFF101010),
    warning = WarningAmber,
    success = Color(0xFF4ADE80),
)

/** Wellness and strength: deep green with mint. */
val DeepForestPalette = GymoraPalette(
    isDark = true,
    background = Color(0xFF07110D),
    surface = Color(0xFF102019),
    surfaceLow = Color(0xFF0B1812),
    surfaceRaised = Color(0xFF17291F),
    surfaceHighest = Color(0xFF1F3328),
    primary = Color(0xFF22C55E),
    onPrimary = Color(0xFF07110D),
    primaryContainer = Color(0xFF0F3D22),
    onPrimaryContainer = Color(0xFFBBF7D0),
    primaryPressed = Color(0xFF1DAE53),
    secondary = Color(0xFF86EFAC),
    onSecondary = Color(0xFF07110D),
    secondaryText = Color(0xFF86EFAC),
    textPrimary = Color(0xFFF4F7F2),
    textSecondary = Color(0xFF9DB3A5),
    textDisabled = Color(0xFF5B7466),
    outline = Color(0xFF2C4638),
    divider = Color(0xFF1B2E24),
    error = ErrorRed,
    onError = Color(0xFF07110D),
    warning = WarningAmber,
    success = Color(0xFF22C55E),
)

/** Clean light theme: warm off-white, white cards, green and blue accents. */
val LightMinimalPalette = GymoraPalette(
    isDark = false,
    background = Color(0xFFF7F7F5),
    surface = Color(0xFFFFFFFF),
    surfaceLow = Color(0xFFFBFBF9),
    surfaceRaised = Color(0xFFF0F0ED),
    surfaceHighest = Color(0xFFE8E8E4),
    primary = Color(0xFF16A34A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    primaryPressed = Color(0xFF15803D),
    secondary = Color(0xFF2563EB),
    onSecondary = Color(0xFFFFFFFF),
    secondaryText = Color(0xFF2563EB),
    textPrimary = Color(0xFF111113),
    textSecondary = Color(0xFF62646C),
    textDisabled = Color(0xFFA0A2A9),
    outline = Color(0xFFD4D4CF),
    divider = Color(0xFFE7E7E2),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    warning = Color(0xFFD97706),
    success = Color(0xFF16A34A),
)

/** Resolves the palette for a persisted [Theme]; [systemDark] only matters for [Theme.SYSTEM]. */
fun Theme.palette(systemDark: Boolean): GymoraPalette = when (this) {
    Theme.SYSTEM -> if (systemDark) NightVoltPalette else LightMinimalPalette
    Theme.LIGHT -> LightMinimalPalette
    Theme.DARK -> NightVoltPalette
    Theme.OBSIDIAN -> ObsidianPalette
    Theme.GRAPHITE_LIME -> GraphiteLimePalette
    Theme.MIDNIGHT_BLUE -> MidnightBluePalette
    Theme.BLACK_PURPLE -> BlackPurplePalette
    Theme.CHARCOAL_ORANGE -> CharcoalOrangePalette
    Theme.DEEP_FOREST -> DeepForestPalette
}
