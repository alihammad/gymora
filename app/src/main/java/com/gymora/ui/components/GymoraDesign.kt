package com.gymora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymora.ui.theme.BarlowCondensed
import com.gymora.ui.theme.DisplayMetric
import com.gymora.ui.theme.GymoraShapes
import com.gymora.ui.theme.GymoraThemeTokens
import com.gymora.ui.theme.LabelCaps

// Gymora performance-training components: hero cards, the high-energy CTA, big metrics and
// angular chips. Screens compose these instead of raw Material widgets so the app reads as one product.

/** Electric lime for actions, from the active palette. */
@Composable
private fun action() = MaterialTheme.colorScheme.primary

/**
 * Hero panel: dark diagonal gradient with lime slashes. [watermark] is drawn large and faint on the
 * right (e.g. a muscle-group icon) as the stand-in for athlete photography.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    watermark: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val lime = scheme.primary
    val clickable = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GymoraShapes.card)
            .background(
                Brush.linearGradient(
                    listOf(scheme.surfaceContainerHigh, scheme.surfaceContainerLowest, scheme.primaryContainer.copy(alpha = 0.55f)),
                ),
            )
            .then(clickable),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // Industrial slashes, decorative only.
            val w = size.width
            val h = size.height
            fun slash(x: Float, thickness: Float, alpha: Float) {
                val path = Path().apply {
                    moveTo(x, h)
                    lineTo(x + thickness, h)
                    lineTo(x + thickness + h * 0.5f, 0f)
                    lineTo(x + h * 0.5f, 0f)
                    close()
                }
                drawPath(path, lime.copy(alpha = alpha))
            }
            slash(w * 0.62f, w * 0.05f, 0.16f)
            slash(w * 0.72f, w * 0.012f, 0.30f)
            slash(w * 0.78f, w * 0.03f, 0.10f)
        }
        if (watermark != null) watermark()
        // Scrim keeps text contrast high over any artwork.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))),
        )
        content()
    }
}

/**
 * The dominant call to action: full-width lime slab with a cut corner and heavy italic caps.
 * Dark text on lime keeps contrast above 7:1.
 */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 60.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (enabled) scheme.primary else scheme.surfaceContainerHighest
    val content = if (enabled) scheme.onPrimary else GymoraThemeTokens.extraColors.textDisabled
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(GymoraShapes.button)
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            color = content,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        if (icon != null) {
            Spacer(Modifier.width(10.dp))
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        }
    }
}

/** Big number over a small caption. [accent] colors the number (lime by default, violet for analytics). */
@Composable
fun MetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    accent: Color = MaterialTheme.colorScheme.onSurface,
    valueStyle: androidx.compose.ui.text.TextStyle = DisplayMetric,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $value ${unit.orEmpty()}".trim()
        },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = valueStyle, color = accent)
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
        Text(text = label.uppercase(), style = LabelCaps, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Angular tile that holds a [MetricTile] or any short content. */
@Composable
fun AngularPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentBar: Color? = null,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val clickable = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(GymoraShapes.card)
            .background(scheme.surfaceContainer)
            .border(BorderStroke(1.dp, scheme.outlineVariant), GymoraShapes.card)
            .then(clickable),
    ) {
        if (accentBar != null) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accentBar))
        }
        Box(Modifier.padding(16.dp)) { content() }
    }
}

/** Section title: condensed italic caps with a short lime rule, optional trailing action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(18.dp).background(action()))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = BarlowCondensed),
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel.uppercase(),
                style = LabelCaps,
                color = action(),
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onAction)
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

/** Small angular tag, e.g. a muscle group. [selected] fills it lime. */
@Composable
fun MuscleChip(text: String, modifier: Modifier = Modifier, selected: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) scheme.onPrimary else scheme.onSurface,
        modifier = modifier
            .clip(GymoraShapes.chip)
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Segmented completion bar: one slab per step, filled lime up to [done]. */
@Composable
fun SegmentedProgress(done: Int, total: Int, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.semantics { contentDescription = "$done of $total complete" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total.coerceAtLeast(1)) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .clip(CutCornerShape(topEnd = height))
                    .background(if (index < done) scheme.primary else scheme.surfaceContainerHighest),
            )
        }
    }
}
