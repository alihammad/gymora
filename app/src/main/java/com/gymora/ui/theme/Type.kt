package com.gymora.ui.theme

import androidx.compose.material3.Typography
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

// Tabular figures keep changing numbers (timers, reps, counts) from jittering.
private const val TABULAR = "tnum"

/** Display role for hero stats: Space Grotesk 44/48 SemiBold. */
val DisplayStat = TextStyle(
    fontFamily = SpaceGrotesk,
    fontWeight = FontWeight.SemiBold,
    fontSize = 44.sp,
    lineHeight = 48.sp,
    fontFeatureSettings = TABULAR,
)

/** Hero workout / exercise name: condensed, italic, very large. Pair with uppercase text. */
val DisplayHero = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.ExtraBold,
    fontStyle = FontStyle.Italic,
    fontSize = 52.sp,
    lineHeight = 50.sp,
)

/** Large numeric metric (weight, reps, volume). Tabular so values do not jitter. */
val DisplayMetric = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.ExtraBold,
    fontStyle = FontStyle.Italic,
    fontSize = 40.sp,
    lineHeight = 42.sp,
    fontFeatureSettings = TABULAR,
)

/** Small uppercase-style caption above a metric or section. */
val LabelCaps = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.2.sp,
)

private fun condensed(size: Int, line: Int) = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.ExtraBold,
    fontStyle = FontStyle.Italic,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontFeatureSettings = TABULAR,
)

val Typography = Typography(
    displayLarge = DisplayHero,
    displayMedium = DisplayMetric,
    displaySmall = DisplayStat,
    headlineLarge = condensed(34, 36),
    headlineMedium = condensed(28, 30),
    headlineSmall = condensed(24, 26),
    titleLarge = condensed(24, 26),
    titleMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 22.sp, fontFeatureSettings = TABULAR,
    ),
    titleSmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, fontFeatureSettings = TABULAR,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR,
    ),
    bodySmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TABULAR,
    ),
    labelLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontFeatureSettings = TABULAR,
    ),
    labelSmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontFeatureSettings = TABULAR,
    ),
)
