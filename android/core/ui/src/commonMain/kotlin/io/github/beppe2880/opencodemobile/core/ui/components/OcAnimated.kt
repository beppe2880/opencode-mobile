package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.beppe2880.opencodemobile.core.ui.theme.OcMotion
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/*
 * Animated building blocks on the OcMotion tokens. None of them animates the state it is first
 * composed with: a chat opened (or scrolled back) with an open group shows it open at once.
 */

/**
 * Content under a header: it grows from the header downward when [visible] turns on and shrinks
 * back toward it when it turns off.
 */
@Composable
fun OcExpandable(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String = "OcExpandable",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val reduced = OcTheme.reducedMotion
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = OcMotion.expandEnter(reduced),
        exit = OcMotion.collapseExit(reduced),
        label = label,
        content = content,
    )
}

/**
 * The rotation of a disclosure chevron: [collapsedDegrees] folded, [expandedDegrees] open, turning
 * between them with the expansion.
 */
@Composable
fun animateChevronRotation(expanded: Boolean, collapsedDegrees: Float = 0f, expandedDegrees: Float = 180f): Float {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) expandedDegrees else collapsedDegrees,
        animationSpec = OcMotion.spec(OcMotion.MEDIUM_MS, OcTheme.reducedMotion),
        label = "chevron",
    )
    return rotation
}

/** [content] for [state], crossfading when it changes (spinner and check, icon swaps). */
@Composable
fun <T> OcCrossfade(state: T, modifier: Modifier = Modifier, label: String = "OcCrossfade", content: @Composable (T) -> Unit) {
    Crossfade(
        targetState = state,
        modifier = modifier,
        animationSpec = OcMotion.spec(OcMotion.SHORT_MS, OcTheme.reducedMotion),
        label = label,
        content = content,
    )
}

/**
 * [value], or the last non-null one once it turns null: what content animating out keeps showing
 * (the list of a closing popover, the card being answered).
 */
@Composable
fun <T : Any> rememberLastNonNull(value: T?): T? {
    val last = remember { LastValue<T>() }
    if (value != null) last.value = value
    return value ?: last.value
}

private class LastValue<T : Any> {
    var value: T? = null
}
