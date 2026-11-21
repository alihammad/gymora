package com.gymora.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A design language: everything about a theme's look that is not color. Palettes pick colors;
 * the style picks type, shapes and how much decoration components draw.
 */
data class GymoraStyle(
    val text: GymoraTextStyles,
    val shapes: GymoraShapeSet,
    /** Headings, hero names and primary buttons are set in caps. */
    val uppercaseHeadings: Boolean,
    /** Graded gym photos behind screens. */
    val photoBackdrops: Boolean,
    /** Slashes, gradients and accent rules on hero cards and section headers. */
    val decorations: Boolean,
    /** Active workout sets render as a compact SET / PREVIOUS / values / check table. */
    val tableSets: Boolean,
)

/** Condensed italic caps, cut corners, photo backdrops: the original high-energy look. */
val PerformanceStyle = GymoraStyle(
    text = PerformanceTextStyles,
    shapes = PerformanceShapes,
    uppercaseHeadings = true,
    photoBackdrops = true,
    decorations = true,
    tableSets = false,
)

/** Calm and utilitarian: one upright family, rounded corners, flat surfaces, tabular set logging. */
val SlateStyle = GymoraStyle(
    text = SlateTextStyles,
    shapes = SlateShapes,
    uppercaseHeadings = false,
    photoBackdrops = false,
    decorations = false,
    tableSets = true,
)

val LocalGymoraStyle = staticCompositionLocalOf { PerformanceStyle }

/** The active design language. */
val gymoraStyle: GymoraStyle
    @Composable @ReadOnlyComposable
    get() = LocalGymoraStyle.current

/** Uppercases [text] only when the active design language sets headings in caps. */
@Composable
@ReadOnlyComposable
fun headingCase(text: String): String = if (LocalGymoraStyle.current.uppercaseHeadings) text.uppercase() else text
