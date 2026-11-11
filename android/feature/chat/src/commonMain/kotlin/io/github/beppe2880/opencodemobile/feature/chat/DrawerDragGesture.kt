package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

/**
 * Horizontal drags that move the chat list drawer, started as [tuning] says: with
 * [DrawerSwipeTuning.Medium] exactly [detectHorizontalDragGestures]; otherwise the horizontal
 * movement must reach the system touch slop times [DrawerSwipeTuning.slopFactor], and be
 * [DrawerSwipeTuning.horizontalRatio] times the vertical one, before the drag is followed.
 * A drag a child (a list scrolling, a code block) consumed first is left to it.
 */
suspend fun PointerInputScope.detectDrawerDrag(
    tuning: DrawerSwipeTuning,
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onHorizontalDrag: (change: PointerInputChange, delta: Float) -> Unit,
) {
    if (tuning == DrawerSwipeTuning.Medium) {
        detectHorizontalDragGestures(
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
            onHorizontalDrag = onHorizontalDrag,
        )
        return
    }
    val slop = viewConfiguration.touchSlop * tuning.slopFactor
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val (started, moved) = awaitDrawerSlop(down, slop, tuning.horizontalRatio) ?: return@awaitEachGesture
        onDragStart(down.position)
        onHorizontalDrag(started, moved)
        started.consume()
        val ended = horizontalDrag(started.id) { change ->
            onHorizontalDrag(change, change.positionChange().x)
            change.consume()
        }
        if (ended) onDragEnd() else onDragCancel()
    }
}

/**
 * Waits until the pointer of [down] moved [slop] horizontally, and returns that move with the
 * horizontal distance covered; null when it lifted, a child consumed it, or the move was not
 * more than [ratio] times as horizontal as vertical.
 */
private suspend fun AwaitPointerEventScope.awaitDrawerSlop(down: PointerInputChange, slop: Float, ratio: Float): Pair<PointerInputChange, Float>? {
    var moved = Offset.Zero
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return null
        if (!change.pressed || change.isConsumed) return null
        moved += change.positionChange()
        if (abs(moved.x) >= slop) {
            // Too steep: a scroll, left to the content.
            return if (abs(moved.x) > ratio * abs(moved.y)) change to moved.x else null
        }
    }
}
