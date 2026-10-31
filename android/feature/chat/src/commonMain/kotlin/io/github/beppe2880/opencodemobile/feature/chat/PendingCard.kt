package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/*
 * Shared by the question and permission cards pinned above the composer: both fold into a
 * one-line bar so the transcript stays readable. Folding is purely local; it never replies.
 */

/** Drag handle and header row of an open card; a swipe down or the minimise button folds it. */
@Composable
internal fun PendingCardHeader(
    tagPrefix: String,
    collapseLabel: String,
    onCollapse: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val c = OcTheme.colors
    Column(Modifier.fillMaxWidth().verticalSwipe(onSwipeDown = onCollapse).testTag("${tagPrefix}_header")) {
        Box(
            Modifier
                .padding(top = OcSpacing.xs)
                .size(width = 32.dp, height = 4.dp)
                .background(c.borderStrong, CircleShape)
                .align(Alignment.CenterHorizontally)
                .testTag("${tagPrefix}_drag_handle"),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            content()
            OcIconButton(
                icon = Icons.Outlined.ExpandMore,
                contentDescription = collapseLabel,
                onClick = onCollapse,
                tint = c.iconMuted,
                modifier = Modifier.testTag("${tagPrefix}_collapse_button"),
            )
        }
    }
}

/** The folded card: one line that a tap or a swipe up opens again. */
@Composable
internal fun PendingCardBar(
    tagPrefix: String,
    icon: ImageVector,
    iconTint: Color,
    /** Null when [content] already says what is waiting. */
    iconDescription: String?,
    expandLabel: String,
    onExpand: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = expandLabel, role = Role.Button, onClick = onExpand)
            .verticalSwipe(onSwipeUp = onExpand)
            .padding(start = OcSpacing.md, end = OcSpacing.sm)
            .testTag("${tagPrefix}_collapsed_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Icon(icon, iconDescription, tint = iconTint, modifier = Modifier.size(18.dp))
        content()
        Icon(Icons.Outlined.ExpandLess, null, tint = c.iconMuted, modifier = Modifier.size(20.dp))
    }
}

/**
 * Calls [onSwipeDown] or [onSwipeUp] once a vertical drag ends past a short distance. The
 * gesture is not restarted on recomposition, so the callbacks must read current state.
 */
private fun Modifier.verticalSwipe(onSwipeDown: (() -> Unit)? = null, onSwipeUp: (() -> Unit)? = null): Modifier =
    pointerInput(Unit) {
        val threshold = SwipeThreshold.toPx()
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                when {
                    total > threshold -> onSwipeDown?.invoke()
                    total < -threshold -> onSwipeUp?.invoke()
                }
            },
        ) { change, amount ->
            change.consume()
            total += amount
        }
    }

private val SwipeThreshold = 24.dp
