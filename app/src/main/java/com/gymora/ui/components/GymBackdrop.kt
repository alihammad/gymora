package com.gymora.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import com.gymora.R
import kotlin.math.max

/** Graded gym photos (Pexels licence, free for commercial use) used as screen backdrops. */
object GymBackdrops {
    @DrawableRes val Home = R.drawable.bg_gym_1
    @DrawableRes val Workout = R.drawable.bg_gym_2
    @DrawableRes val Alt = R.drawable.bg_gym_4
}

/**
 * Draws a dimmed gym photo (cropped to fill the top of the area) behind the content,
 * fading into the theme background so text and cards on top stay readable.
 */
@Composable
fun Modifier.gymBackdrop(@DrawableRes image: Int, imageAlpha: Float = 0.5f): Modifier {
    val painter = painterResource(image)
    val background = MaterialTheme.colorScheme.background
    return drawBehind {
        drawRect(background)
        val imgW = painter.intrinsicSize.width
        val imgH = painter.intrinsicSize.height
        val photoHeight = size.height * 0.65f
        // Crop-to-fill, anchored to the top centre.
        val scale = max(size.width / imgW, photoHeight / imgH)
        val drawW = imgW * scale
        val drawH = imgH * scale
        val left = (size.width - drawW) / 2f
        clipRect(0f, 0f, size.width, photoHeight) {
            translate(left, 0f) {
                with(painter) { draw(Size(drawW, drawH), alpha = imageAlpha) }
            }
        }
        drawRect(
            Brush.verticalGradient(
                0f to background.copy(alpha = 0.2f),
                0.4f to background.copy(alpha = 0.85f),
                0.65f to background,
            ),
        )
    }
}
