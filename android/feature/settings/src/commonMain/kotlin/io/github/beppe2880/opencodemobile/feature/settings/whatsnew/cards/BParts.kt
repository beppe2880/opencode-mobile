package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/** The 6 dots of the running mark, in the order the light travels. */
private val DOT_CELLS = listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2, 0 to 2, 0 to 1)

/** The animated dots of a running shell or subagent, as in the chat. */
@Composable
internal fun BRunningDots(color: Color, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val frame by rememberInfiniteTransition(label = "bDots").animateFloat(
        initialValue = 0f,
        targetValue = DOT_CELLS.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "bDotsFrame",
    )
    Canvas(modifier.size(width = height * 2 / 3, height = height)) {
        val cellW = size.width / 2
        val cellH = size.height / 3
        val radius = minOf(cellW, cellH) * 0.34f
        val head = frame.toInt() % DOT_CELLS.size
        DOT_CELLS.forEachIndexed { i, (col, row) ->
            val alpha = when ((head - i + DOT_CELLS.size) % DOT_CELLS.size) {
                0 -> 1f
                1 -> 0.7f
                2 -> 0.45f
                else -> 0.2f
            }
            drawCircle(color, radius, Offset(cellW * (col + 0.5f), cellH * (row + 0.5f)), alpha = alpha)
        }
    }
}

/** A running chip above the message box: dots and a count. */
@Composable
internal fun BChip(label: String, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier
            .heightIn(min = 28.dp)
            .clip(shape)
            .background(c.layer01, shape)
            .border(1.dp, c.borderBase, shape)
            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        BRunningDots(c.textMuted)
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textBase, maxLines = 1)
    }
}

/** A tool row of the transcript: icon, title, argument, optional [trailing] action and the status. */
@Composable
internal fun BToolRow(
    icon: ImageVector,
    title: String,
    argument: String,
    modifier: Modifier = Modifier,
    running: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    val c = OcTheme.colors
    Row(
        modifier.heightIn(min = 32.dp).padding(horizontal = OcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
        Text(title, style = OcTheme.mono.label, color = c.textBase)
        Text(
            argument,
            style = OcTheme.mono.codeSmall,
            color = c.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
        if (running) {
            OcSpinner(size = 14.dp)
        } else {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = c.success.fg, modifier = Modifier.size(14.dp))
        }
    }
}

/** A destructive Stop, as the Running list draws it; [spinner] while it stops. */
@Composable
internal fun BStopButton(text: String, modifier: Modifier = Modifier, spinner: Boolean = false) {
    val c = OcTheme.colors
    Box(
        modifier
            .heightIn(min = 28.dp)
            .clip(RoundedCornerShape(OcRadius.md))
            .background(if (spinner) c.layer02 else c.danger.bg)
            .padding(horizontal = OcSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        if (spinner) {
            OcSpinner(size = 14.dp, color = c.danger.fg)
        } else {
            Text(text, style = MaterialTheme.typography.labelMedium, color = c.danger.fg, maxLines = 1)
        }
    }
}

/** A short message over the scene, like the app's snackbar. */
@Composable
internal fun BToast(text: String, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = c.textInverse,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(OcRadius.md))
            .background(c.bgInverse)
            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
    )
}
