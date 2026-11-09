package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcStateColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/** Hairline-bordered surface. Clickable when [onClick] is provided. */
@Composable
fun OcCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = OcTheme.colors.bgBase,
    borderColor: Color = OcTheme.colors.borderBase,
    shape: Shape = RoundedCornerShape(OcRadius.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(1.dp, borderColor)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border) {
            Column(content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = border) {
            Column(content = content)
        }
    }
}

@Composable
fun OcDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = OcTheme.colors.borderMuted)
}

/** Small uppercase-free section caption, like the "Today" headers of the web UI. */
@Composable
fun OcSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = OcTheme.colors.textMuted,
        modifier = modifier.padding(horizontal = OcSpacing.gutter, vertical = OcSpacing.sm),
    )
}

/** Pill badge with semantic state colors. */
@Composable
fun OcBadge(text: String, colors: OcStateColors, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.fg,
        modifier = modifier
            .clip(RoundedCornerShape(OcRadius.xs))
            .background(colors.bg)
            .border(1.dp, colors.border, RoundedCornerShape(OcRadius.xs))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Rounded-square avatar with the project initial, as in the web session list. */
@Composable
fun OcProjectAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    val c = OcTheme.colors
    val initial = name.trim().trimStart('/', '.').firstOrNull()?.uppercaseChar()?.toString() ?: "/"
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(OcRadius.xs))
            .background(if (c.isDark) c.layer03 else c.iconBase)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (c.isDark) c.textBase else c.bgBase,
        )
    }
}

/** Dot that pulses while something is running; steady when the system asks for less motion. */
@Composable
fun OcPulsingDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp, pulsing: Boolean = true) {
    val alpha = if (pulsing && !OcTheme.reducedMotion) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val value by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulseAlpha",
        )
        value
    } else {
        1f
    }
    Box(
        modifier = modifier
            .size(size)
            .drawBehind { drawCircle(color = color, alpha = alpha) },
    )
}

@Composable
fun OcSpinner(modifier: Modifier = Modifier, size: Dp = 14.dp, color: Color = OcTheme.colors.textMuted) {
    CircularProgressIndicator(modifier = modifier.size(size), strokeWidth = 1.5.dp, color = color)
}

/** Shimmering placeholder block used by skeleton loaders; still when the system asks for less motion. */
@Composable
fun OcSkeleton(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(OcRadius.xs)) {
    val alpha = if (OcTheme.reducedMotion) {
        0.75f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val value by transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "skeletonAlpha",
        )
        value
    }
    Box(
        modifier = modifier
            .clip(shape)
            .alpha(alpha)
            .background(OcTheme.colors.layer03),
    )
}

/** Centered, width-limited container keeping content readable on large screens. */
@Composable
fun OcContentWidth(
    modifier: Modifier = Modifier,
    maxWidth: Dp = OcSpacing.contentMaxWidth,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(), content = content)
    }
}

/** Status dot in a small circle, e.g. connection state. */
@Composable
fun OcStatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
fun OcCaption(text: String, modifier: Modifier = Modifier, color: Color = OcTheme.colors.textFaint) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.Start,
        modifier = modifier,
    )
}

/** Fixed-height spacer for vertical rhythm. */
@Composable
fun OcVSpace(height: Dp) {
    Box(Modifier.height(height))
}
