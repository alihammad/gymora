package com.gymora.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Fixed-row-height list whose rows are reordered by dragging their handle.
 * Rows are laid out manually (not lazily) so it can sit inside a scrolling
 * column. The order is committed once, on drop, through [onMove].
 *
 * [row] receives the item, the modifier that must be put on the drag handle,
 * and whether this row is the one being dragged.
 */
@Composable
fun <T> ReorderableList(
    items: List<T>,
    key: (T) -> Long,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    rowHeight: Dp = 60.dp,
    spacing: Dp = 8.dp,
    row: @Composable (item: T, handle: Modifier, isDragging: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val stepPx = with(density) { (rowHeight + spacing).toPx() }
    // Holds the dropped order until the caller's list catches up, so rows don't snap back.
    var optimistic by remember { mutableStateOf<List<T>?>(null) }
    LaunchedEffect(items) { optimistic = null }
    val shown = optimistic ?: items
    val currentItems by rememberUpdatedState(shown)
    val currentOnMove by rememberUpdatedState(onMove)

    var draggedKey by remember { mutableStateOf<Long?>(null) }
    var startIndex by remember { mutableStateOf(0) }
    val dragOffset = remember { mutableFloatStateOf(0f) }

    // Row the dragged item currently hovers over.
    val targetIndex = if (draggedKey == null) {
        startIndex
    } else {
        ((startIndex * stepPx + dragOffset.floatValue) / stepPx).roundToInt()
            .coerceIn(0, shown.lastIndex)
    }

    fun reset() {
        draggedKey = null
        dragOffset.floatValue = 0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight * shown.size + spacing * (shown.size - 1).coerceAtLeast(0)),
    ) {
        shown.forEachIndexed { index, item ->
            androidx.compose.runtime.key(key(item)) {
                val id = key(item)
                val isDragging = draggedKey == id
                // Neighbours shift one slot to make room for the dragged row.
                val slot = when {
                    draggedKey == null || isDragging -> index
                    startIndex < targetIndex && index in (startIndex + 1)..targetIndex -> index - 1
                    targetIndex < startIndex && index in targetIndex until startIndex -> index + 1
                    else -> index
                }
                val restingY by animateIntAsState((slot * stepPx).roundToInt(), label = "reorderSlot")
                val y = if (isDragging) (startIndex * stepPx + dragOffset.floatValue).roundToInt() else restingY

                val handle = Modifier.pointerInput(id) {
                    detectDragGestures(
                        onDragStart = {
                            startIndex = currentItems.indexOfFirst { key(it) == id }
                            dragOffset.floatValue = 0f
                            draggedKey = id
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset.floatValue += amount.y
                        },
                        onDragEnd = {
                            val from = startIndex
                            val to = ((from * stepPx + dragOffset.floatValue) / stepPx).roundToInt()
                                .coerceIn(0, currentItems.lastIndex)
                            reset()
                            if (from != to) {
                                optimistic = currentItems.toMutableList().also { it.add(to, it.removeAt(from)) }
                                currentOnMove(from, to)
                            }
                        },
                        onDragCancel = { reset() },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .offset { IntOffset(0, y) }
                        .zIndex(if (isDragging) 1f else 0f)
                        .then(if (isDragging) Modifier.shadow(8.dp) else Modifier),
                ) {
                    row(item, handle, isDragging)
                }
            }
        }
    }
}

/** Compact name-only row used while reordering: label, optional superset tag and the drag handle. */
@Composable
fun ReorderExerciseRow(
    name: String,
    inSuperset: Boolean,
    handle: Modifier,
    isDragging: Boolean,
) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.material3.MaterialTheme.shapes.medium,
        color = if (isDragging) {
            androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text = name,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (inSuperset) {
                    androidx.compose.material3.Text(
                        text = "Superset",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Box(
                modifier = handle.size(56.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder $name",
                )
            }
        }
    }
}
