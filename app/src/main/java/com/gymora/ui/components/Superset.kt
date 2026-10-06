package com.gymora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val RailX = 8.dp
private val RailDotY = 10.dp
private val RailContentInset = 24.dp

/**
 * Groups superset members under a "SUPERSET" label with a dotted rail down the
 * left edge, so the exercises read as one block performed back to back.
 */
@Composable
fun SupersetBlock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val railColor = MaterialTheme.colorScheme.outline
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val x = RailX.toPx()
                val dotY = RailDotY.toPx()
                val dash = 3.dp.toPx()
                drawLine(
                    color = railColor,
                    start = Offset(x, dotY),
                    end = Offset(x, size.height),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                )
                drawCircle(color = railColor, radius = 6.dp.toPx(), center = Offset(x, dotY))
            }
            .padding(start = RailContentInset),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "SUPERSET",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** Renders one block from SupersetRules.blocks: a superset when it has 2+ members, else a lone card. */
@Composable
fun <T> ExerciseBlock(block: List<T>, card: @Composable (T) -> Unit) {
    if (block.size > 1) {
        SupersetBlock { block.forEach { card(it) } }
    } else {
        block.forEach { card(it) }
    }
}

/**
 * Toggle shown between two adjacent exercises: links them into a superset, or
 * splits the superset at this point when they are already linked.
 */
@Composable
fun SupersetLinkButton(
    linked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = onClick) {
            Icon(
                imageVector = if (linked) Icons.Filled.LinkOff else Icons.Filled.Link,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(if (linked) "Unlink superset" else "Superset with next")
        }
    }
}
