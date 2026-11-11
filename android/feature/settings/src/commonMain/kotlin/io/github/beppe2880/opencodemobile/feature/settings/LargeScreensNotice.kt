package io.github.beppe2880.opencodemobile.feature.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.platform.ScreenCapabilities
import io.github.beppe2880.opencodemobile.core.ui.platform.rememberReducedMotion
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfold_prompt_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfold_prompt_title
import org.jetbrains.compose.resources.stringResource

/** What the large screen category says above its options. */
internal enum class LargeScreensHint {
    /** A folded foldable: invite to unfold it. */
    Unfold,

    /** A narrow window that unfolding would not widen, like a tablet in split screen. */
    NarrowWindow,

    /** A wide window: the options speak for themselves. */
    None,
}

/** The hint for a window that is [wide] or not on a device with [screen]. */
internal fun largeScreensHint(screen: ScreenCapabilities, wide: Boolean): LargeScreensHint = when {
    wide -> LargeScreensHint.None
    screen.foldable -> LargeScreensHint.Unfold
    else -> LargeScreensHint.NarrowWindow
}

/** Above the large screen options: the unfold prompt or the narrow window note; nothing once the window is wide. */
@Composable
internal fun LargeScreensNotice(screen: ScreenCapabilities, wide: Boolean, modifier: Modifier = Modifier) {
    when (largeScreensHint(screen, wide)) {
        LargeScreensHint.Unfold -> UnfoldPrompt(modifier)
        LargeScreensHint.NarrowWindow -> NarrowWindowNote(modifier)
        LargeScreensHint.None -> Unit
    }
}

/** On a folded foldable: a phone opening, and why to open it. Read as one element; the drawing is decorative. */
@Composable
private fun UnfoldPrompt(modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.layer01, RoundedCornerShape(OcRadius.lg))
            .padding(OcSpacing.md)
            .semantics(mergeDescendants = true) {}
            .testTag("settings_unfold_prompt"),
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UnfoldingPhone()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
            Text(stringResource(Res.string.settings_unfold_prompt_title), style = MaterialTheme.typography.titleSmall, color = c.textBase)
            Text(stringResource(Res.string.settings_unfold_prompt_body), style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
    }
}

/**
 * Two halves of a phone: the right one turns on the hinge from folded over the left one to open,
 * holds, folds back and starts over. With reduced motion it stays open. The angle is read only while
 * drawing, so the loop never recomposes.
 */
@Composable
private fun UnfoldingPhone() {
    val c = OcTheme.colors
    val reduced = rememberReducedMotion()
    val angle: State<Float> = if (reduced) {
        remember { mutableFloatStateOf(OPEN) }
    } else {
        rememberInfiniteTransition(label = "unfold").animateFloat(
            initialValue = FOLDED,
            targetValue = FOLDED,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = CYCLE_MS
                    FOLDED at 0
                    FOLDED at 700 using FastOutSlowInEasing
                    OPEN at 1700
                    OPEN at 3600 using FastOutSlowInEasing
                    FOLDED at 4300
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "fold_angle",
        )
    }
    val left = RoundedCornerShape(topStart = PanelRadius, bottomStart = PanelRadius, topEnd = HingeRadius, bottomEnd = HingeRadius)
    val right = RoundedCornerShape(topStart = HingeRadius, bottomStart = HingeRadius, topEnd = PanelRadius, bottomEnd = PanelRadius)
    val face = c.layer02
    val back = c.layer04
    val edge = c.borderStrong
    val line = c.textFaint
    val accent = c.textAccent
    // The hinge turns the same way whatever the reading direction.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.size(ArtWidth, ArtHeight).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            Row(
                Modifier.graphicsLayer {
                    // Folded, the phone is one half wide: keep it centered.
                    translationX = PanelWidth.toPx() / 2 * (angle.value / FOLDED)
                },
            ) {
                Spacer(
                    Modifier.size(PanelWidth, PanelHeight).drawBehind {
                        drawPanel(left, face, edge)
                        // The session list.
                        repeat(3) { i -> drawBar(top = 0.22f + i * 0.2f, start = 0.2f, end = 0.8f, line) }
                    },
                )
                Spacer(
                    Modifier
                        .size(PanelWidth, PanelHeight)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            rotationY = -angle.value
                            cameraDistance = 6f * density
                        }
                        .drawBehind {
                            val a = angle.value
                            // Past a right angle the outside of the phone faces the viewer.
                            drawPanel(right, if (a > 90f) back else face, edge)
                            if (a < 90f) {
                                // The chat beside the list, fading in as it opens.
                                val shown = 1f - a / 90f
                                drawBar(top = 0.22f, start = 0.2f, end = 0.65f, accent.copy(alpha = accent.alpha * 0.6f * shown))
                                drawBar(top = 0.42f, start = 0.2f, end = 0.8f, line.copy(alpha = line.alpha * shown))
                                drawBar(top = 0.62f, start = 0.35f, end = 0.8f, line.copy(alpha = line.alpha * shown))
                            }
                        },
                )
            }
        }
    }
}

private fun DrawScope.drawPanel(shape: RoundedCornerShape, fill: Color, edge: Color) {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(outline, fill)
    drawOutline(outline, edge, style = Stroke(width = 1.dp.toPx()))
}

/** A rounded line of content at [top] of the panel, from [start] to [end] of its width. */
private fun DrawScope.drawBar(top: Float, start: Float, end: Float, color: Color) {
    val h = 2.5.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * start, size.height * top),
        size = Size(size.width * (end - start), h),
        cornerRadius = CornerRadius(h / 2),
    )
}

private const val FOLDED = 180f
private const val OPEN = 0f
private const val CYCLE_MS = 4800

private val PanelWidth = 22.dp
private val PanelHeight = 40.dp
private val PanelRadius = 5.dp
private val HingeRadius = 1.dp
private val ArtWidth = 52.dp
private val ArtHeight = 48.dp
