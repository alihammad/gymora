package com.gymora.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Spacing scale: 4, 8, 12, 16, 24, 32dp. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Screen side padding. */
    val screen = lg

    /** Gap between cards. */
    val cardGap = md

    /** Gap between sections. */
    val section = xl
}

/** Shape tokens beyond the Material slots. */
object GymoraShapes {
    val card = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(14.dp)
    val input = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(percent = 50)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val dialog = RoundedCornerShape(24.dp)
}

val MaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp), // inputs
    medium = RoundedCornerShape(16.dp), // cards
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp), // dialogs, sheets
)

/** Minimum touch target and button height. */
object Sizes {
    val touchTarget = 48.dp
    val button = 52.dp
}
