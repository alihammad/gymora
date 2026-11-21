package com.gymora.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
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

/** The shape set a design language uses. */
data class GymoraShapeSet(
    val card: CornerBasedShape,
    val button: CornerBasedShape,
    val input: CornerBasedShape,
    val chip: CornerBasedShape,
    val sheet: CornerBasedShape,
    val dialog: CornerBasedShape,
    /** Small progress segments and bars. */
    val segment: (Dp) -> Shape,
) {
    fun toMaterial() = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = input,
        medium = card,
        large = card,
        extraLarge = dialog, // dialogs, sheets
    )
}

/** Angular, cut-corner shapes of the Performance design language. */
val PerformanceShapes = GymoraShapeSet(
    card = CutCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 16.dp),
    button = CutCornerShape(topStart = 14.dp, topEnd = 0.dp, bottomEnd = 14.dp, bottomStart = 0.dp),
    input = RoundedCornerShape(6.dp),
    chip = CutCornerShape(topStart = 8.dp, topEnd = 0.dp, bottomEnd = 8.dp, bottomStart = 0.dp),
    sheet = CutCornerShape(topStart = 24.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 0.dp),
    dialog = CutCornerShape(topStart = 0.dp, topEnd = 20.dp, bottomEnd = 0.dp, bottomStart = 20.dp),
    segment = { CutCornerShape(topEnd = it) },
)

/** Soft, evenly rounded shapes of the Slate design language. */
val SlateShapes = GymoraShapeSet(
    card = RoundedCornerShape(12.dp),
    button = RoundedCornerShape(10.dp),
    input = RoundedCornerShape(8.dp),
    chip = RoundedCornerShape(50),
    sheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    dialog = RoundedCornerShape(16.dp),
    segment = { RoundedCornerShape(50) },
)

/** Shape tokens beyond the Material slots, resolved from the active design language. */
object GymoraShapes {
    val card: Shape @Composable @ReadOnlyComposable get() = LocalGymoraStyle.current.shapes.card
    val button: Shape @Composable @ReadOnlyComposable get() = LocalGymoraStyle.current.shapes.button
    val input: Shape @Composable @ReadOnlyComposable get() = LocalGymoraStyle.current.shapes.input
    val chip: Shape @Composable @ReadOnlyComposable get() = LocalGymoraStyle.current.shapes.chip
    val sheet: Shape @Composable @ReadOnlyComposable get() = LocalGymoraStyle.current.shapes.sheet
    val dialog: Shape @Composable @ReadOnlyComposable get() = LocalGymoraStyle.current.shapes.dialog
}

/** Minimum touch target and button height. */
object Sizes {
    val touchTarget = 48.dp
    val button = 52.dp
}
