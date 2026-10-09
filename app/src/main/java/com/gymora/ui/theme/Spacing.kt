package com.gymora.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
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
    /** Angular card: two opposite corners cut, the others square. */
    val card = CutCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 16.dp)
    val button = CutCornerShape(topStart = 14.dp, topEnd = 0.dp, bottomEnd = 14.dp, bottomStart = 0.dp)
    val input = RoundedCornerShape(6.dp)
    val chip = CutCornerShape(topStart = 8.dp, topEnd = 0.dp, bottomEnd = 8.dp, bottomStart = 0.dp)
    val sheet = CutCornerShape(topStart = 24.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
    val dialog = CutCornerShape(topStart = 0.dp, topEnd = 20.dp, bottomEnd = 0.dp, bottomStart = 20.dp)
}

val MaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp), // inputs
    medium = GymoraShapes.card,
    large = GymoraShapes.card,
    extraLarge = GymoraShapes.dialog, // dialogs, sheets
)

/** Minimum touch target and button height. */
object Sizes {
    val touchTarget = 48.dp
    val button = 52.dp
}
