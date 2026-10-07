package com.gymora.ui.components

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gymora.data.media.ExerciseMediaStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_DECODE_PX = 1080

/**
 * An exercise's demo image; GIFs and animated WebP play on Android 9+. Decoded off
 * the main thread and downscaled so a large photo cannot exhaust memory.
 */
@Composable
fun ExerciseMediaImage(
    fileName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val drawable by produceState<Drawable?>(initialValue = null, fileName) {
        value = withContext(Dispatchers.IO) {
            runCatching { decode(ExerciseMediaStore.file(context, fileName), context) }.getOrNull()
        }
    }
    val image = drawable ?: return
    AndroidView(
        factory = { ImageView(it).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER } },
        update = { view ->
            view.contentDescription = contentDescription
            view.setImageDrawable(image)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && image is AnimatedImageDrawable) image.start()
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .clip(RoundedCornerShape(12.dp)),
    )
}

private fun decode(file: File, context: android.content.Context): Drawable? {
    if (!file.exists()) return null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        return ImageDecoder.decodeDrawable(ImageDecoder.createSource(file)) { decoder, info, _ ->
            val largest = maxOf(info.size.width, info.size.height)
            if (largest > MAX_DECODE_PX) {
                val scale = MAX_DECODE_PX.toFloat() / largest
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
        }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_DECODE_PX) sample *= 2
    val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return null
    return BitmapDrawable(context.resources, bitmap)
}

/** Bulleted coaching cues. */
@Composable
fun FormCuesList(cues: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cues.forEach { cue ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                Text(cue, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
