package com.gymora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gymora.R

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(weight, FontStyle.Normal),
)

private val weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

val SpaceGrotesk = FontFamily(weights.map { variable(R.font.space_grotesk_variable, it) })
/** Heavy condensed italic face for workout names, headlines and big metrics. */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_bold_italic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.barlow_condensed_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
)
val Inter = FontFamily(weights.map { variable(R.font.inter_variable, it) })
/** Clean, friendly neo-grotesk used throughout the Slate design language. */
val Figtree = FontFamily(
    (weights + FontWeight.ExtraBold).map { variable(R.font.figtree_variable, it) },
)

// Tabular figures keep changing numbers (timers, reps, counts) from jittering.
private const val TABULAR = "tnum"

/** The text roles a design language defines beyond the Material [Typography] slots. */
data class GymoraTextStyles(
    val typography: Typography,
    /** Hero stats. */
    val displayStat: TextStyle,
    /** Hero workout / exercise names. */
    val displayHero: TextStyle,
    /** Large numeric metric (weight, reps, volume). */
    val displayMetric: TextStyle,
    /** Small caps caption above a metric, section or table column. */
    val labelCaps: TextStyle,
    /** Family for section headings. */
    val headingFamily: FontFamily,
)

// Theme-aware accessors: read the active design language, so call sites don't change per theme.

/** Display role for hero stats. */
val DisplayStat: TextStyle
    @Composable @ReadOnlyComposable
    get() = LocalGymoraStyle.current.text.displayStat

/** Hero workout / exercise name. Uppercase it only when [GymoraStyle.uppercaseHeadings] is set. */
val DisplayHero: TextStyle
    @Composable @ReadOnlyComposable
    get() = LocalGymoraStyle.current.text.displayHero

/** Large numeric metric (weight, reps, volume). Tabular so values do not jitter. */
val DisplayMetric: TextStyle
    @Composable @ReadOnlyComposable
    get() = LocalGymoraStyle.current.text.displayMetric

/** Small uppercase-style caption above a metric or section. */
val LabelCaps: TextStyle
    @Composable @ReadOnlyComposable
    get() = LocalGymoraStyle.current.text.labelCaps

/** Family for section headings in the active design language. */
val HeadingFamily: FontFamily
    @Composable @ReadOnlyComposable
    get() = LocalGymoraStyle.current.text.headingFamily

// ---- Performance: condensed italic display over Inter (the original look). ----

private fun condensed(size: Int, line: Int) = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.ExtraBold,
    fontStyle = FontStyle.Italic,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontFeatureSettings = TABULAR,
)

private fun body(family: FontFamily, weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = TABULAR,
)

private val PerformanceHero = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.ExtraBold,
    fontStyle = FontStyle.Italic,
    fontSize = 52.sp,
    lineHeight = 50.sp,
)

private val PerformanceMetric = condensed(40, 42)

val PerformanceTextStyles = GymoraTextStyles(
    typography = Typography(
        displayLarge = PerformanceHero,
        displayMedium = PerformanceMetric,
        displaySmall = body(SpaceGrotesk, FontWeight.SemiBold, 44, 48),
        headlineLarge = condensed(34, 36),
        headlineMedium = condensed(28, 30),
        headlineSmall = condensed(24, 26),
        titleLarge = condensed(24, 26),
        titleMedium = body(Inter, FontWeight.Medium, 16, 22),
        titleSmall = body(Inter, FontWeight.Medium, 14, 20),
        bodyLarge = body(Inter, FontWeight.Normal, 16, 24),
        bodyMedium = body(Inter, FontWeight.Normal, 14, 20),
        bodySmall = body(Inter, FontWeight.Normal, 12, 16),
        labelLarge = body(Inter, FontWeight.Medium, 14, 20),
        labelMedium = body(Inter, FontWeight.Medium, 12, 16, tracking = 0.5),
        labelSmall = body(Inter, FontWeight.Medium, 11, 16, tracking = 0.5),
    ),
    displayStat = body(SpaceGrotesk, FontWeight.SemiBold, 44, 48),
    displayHero = PerformanceHero,
    displayMetric = PerformanceMetric,
    labelCaps = body(Inter, FontWeight.SemiBold, 11, 14, tracking = 1.2),
    headingFamily = BarlowCondensed,
)

// ---- Slate: one upright family, bold titles, quiet tracked caps for table headers. ----

val SlateTextStyles = GymoraTextStyles(
    typography = Typography(
        displayLarge = body(Figtree, FontWeight.Bold, 34, 40),
        displayMedium = body(Figtree, FontWeight.Bold, 30, 36),
        displaySmall = body(Figtree, FontWeight.SemiBold, 40, 46),
        headlineLarge = body(Figtree, FontWeight.Bold, 30, 36),
        headlineMedium = body(Figtree, FontWeight.Bold, 26, 32),
        headlineSmall = body(Figtree, FontWeight.Bold, 22, 28),
        titleLarge = body(Figtree, FontWeight.SemiBold, 20, 26),
        titleMedium = body(Figtree, FontWeight.SemiBold, 17, 22),
        titleSmall = body(Figtree, FontWeight.SemiBold, 14, 20),
        bodyLarge = body(Figtree, FontWeight.Normal, 17, 24),
        bodyMedium = body(Figtree, FontWeight.Normal, 15, 20),
        bodySmall = body(Figtree, FontWeight.Normal, 13, 18),
        labelLarge = body(Figtree, FontWeight.SemiBold, 15, 20, tracking = 0.2),
        labelMedium = body(Figtree, FontWeight.Medium, 13, 16, tracking = 0.3),
        labelSmall = body(Figtree, FontWeight.Medium, 12, 16, tracking = 0.3),
    ),
    displayStat = body(Figtree, FontWeight.SemiBold, 40, 46),
    displayHero = body(Figtree, FontWeight.Bold, 30, 36),
    displayMetric = body(Figtree, FontWeight.Bold, 30, 36),
    labelCaps = body(Figtree, FontWeight.SemiBold, 12, 16, tracking = 1.6),
    headingFamily = Figtree,
)
