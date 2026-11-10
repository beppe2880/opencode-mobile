package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ChatSwipe
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_untitled
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/** Share of the width a drag must cover to switch session. */
private const val SWITCH_FRACTION = 0.25f

/** Drag toward a side without a session moves this much, so the edge is felt. */
private const val EDGE_RESISTANCE = 0.2f

/** Drags starting this close to the start edge are left to the system back gesture (iOS). */
private val EDGE_GUARD = 24.dp

/**
 * Horizontal swipe over [content]. With [ChatSwipe.SwitchChats] it opens the [previous] (swipe
 * right) or [next] (swipe left) session; with [ChatSwipe.SessionDrawer] it pulls the host's
 * [drawer] along with the finger, the chat staying in place. Nested horizontal scrollers (code,
 * diffs) consume their drags first.
 */
@Composable
internal fun SessionSwipe(
    previous: SessionPeek?,
    next: SessionPeek?,
    mode: ChatSwipe,
    drawer: SessionDrawerPull?,
    onSwitch: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val pullDrawer = drawer?.takeIf { mode == ChatSwipe.SessionDrawer }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val switching = mode == ChatSwipe.SwitchChats && (previous != null || next != null)
    val offset = remember { Animatable(0f) }
    var width by remember { mutableIntStateOf(1) }
    val threshold = width * SWITCH_FRACTION
    val target = when {
        offset.value <= -threshold -> next
        offset.value >= threshold -> previous
        else -> null
    }
    val gesture = when {
        pullDrawer != null -> Modifier.pointerInput(pullDrawer, rtl) { pullDrawerGesture(pullDrawer, toEnd = if (rtl) -1f else 1f) }
        switching -> Modifier.switchGesture(offset, previous, next, threshold, onSwitch)
        else -> Modifier
    }
    Box(modifier = modifier.onSizeChanged { width = it.width.coerceAtLeast(1) }.then(gesture)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offset.value * 0.5f
                    alpha = 1f - (abs(offset.value) / width).coerceIn(0f, 0.5f)
                },
            content = content,
        )
        val peek = if (offset.value < 0) next else if (offset.value > 0) previous else null
        if (peek != null) {
            val progress = (abs(offset.value) / threshold).coerceIn(0f, 1f)
            SwipePeek(
                peek = peek,
                forward = offset.value < 0,
                ready = target != null,
                modifier = Modifier
                    .align(if (offset.value < 0) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = OcSpacing.md)
                    .graphicsLayer { alpha = progress },
            )
        }
    }
}

/**
 * Feeds horizontal drags to [drawer], in pixels toward the end edge ([toEnd] is the sign of a
 * move toward it), and the release velocity. Drags starting within [EDGE_GUARD] of the start
 * edge are left alone.
 */
private suspend fun PointerInputScope.pullDrawerGesture(drawer: SessionDrawerPull, toEnd: Float) {
    val guard = EDGE_GUARD.toPx()
    var ignored = false
    val velocity = VelocityTracker()
    detectHorizontalDragGestures(
        onDragStart = { start ->
            velocity.resetTracking()
            ignored = (if (toEnd > 0) start.x else size.width - start.x) < guard
        },
        onDragEnd = { if (!ignored) drawer.release(velocity.calculateVelocity().x * toEnd) },
        onDragCancel = { if (!ignored) drawer.release(0f) },
        onHorizontalDrag = { change, delta ->
            if (ignored) return@detectHorizontalDragGestures
            change.consume()
            velocity.addPosition(change.uptimeMillis, change.position)
            drawer.drag(delta * toEnd)
        },
    )
}

/** Drags the chat aside to reveal [previous] or [next]; past [threshold] the release opens it. */
@Composable
private fun Modifier.switchGesture(
    offset: Animatable<Float, AnimationVector1D>,
    previous: SessionPeek?,
    next: SessionPeek?,
    threshold: Float,
    onSwitch: (String) -> Unit,
): Modifier {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnSwitch by rememberUpdatedState(onSwitch)
    return pointerInput(previous, next, threshold) {
        var armed = false
        detectHorizontalDragGestures(
            onDragStart = { armed = false },
            onDragEnd = {
                val goTo = when {
                    offset.value <= -threshold -> next
                    offset.value >= threshold -> previous
                    else -> null
                }
                scope.launch {
                    if (goTo != null) currentOnSwitch(goTo.id) else offset.animateTo(0f)
                }
            },
            onDragCancel = { scope.launch { offset.animateTo(0f) } },
            onHorizontalDrag = { change, delta ->
                change.consume()
                val proposed = offset.value + delta
                val blocked = (proposed < 0 && next == null) || (proposed > 0 && previous == null)
                val value = if (blocked) offset.value + delta * EDGE_RESISTANCE else proposed
                val crossed = abs(value) >= threshold && !blocked
                if (crossed != armed) {
                    armed = crossed
                    if (crossed) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                scope.launch { offset.snapTo(value) }
            },
        )
    }
}

@Composable
private fun SwipePeek(peek: SessionPeek, forward: Boolean, ready: Boolean, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .widthIn(max = 240.dp)
            .background(if (ready) c.bgInverse else c.layer02, RoundedCornerShape(OcRadius.lg))
            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm)
            .testTag("chat_swipe_peek"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        val tint = if (ready) c.textInverse else c.textBase
        if (!forward) Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            peek.title.ifBlank { stringResource(Res.string.chat_untitled) },
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (forward) Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = tint, modifier = Modifier.size(16.dp))
    }
}
