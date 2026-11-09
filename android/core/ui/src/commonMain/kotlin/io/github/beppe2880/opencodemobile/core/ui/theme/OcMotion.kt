package io.github.beppe2880.opencodemobile.core.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * True when the system asks for less motion (Android "Remove animations" or an animator duration
 * scale of 0, iOS Reduce Motion). Provided by [OcTheme]; every [OcMotion] spec built with it is
 * instant, and looping decorations stand still.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Motion tokens: short, standard-eased transitions without springs, the same on Android and iOS.
 * Each builder takes `reduced` ([LocalReducedMotion], also [OcTheme.reducedMotion]) and then
 * returns an instant variant, so callers never branch on it themselves.
 *
 * - Expand / collapse: content grows from its header downward and shrinks back toward it.
 * - Popovers: scale and fade from the side of their anchor ([anchoredOrigin]).
 * - Appearing chips and cards: fade with a small grow; state swaps crossfade ([swap]).
 */
object OcMotion {
    /** Small elements: icons, chips, popovers, fades out. */
    const val SHORT_MS = 150

    /** Expanding and collapsing content, cards appearing, size changes. */
    const val MEDIUM_MS = 220

    /** Larger moves: page changes and panels. */
    const val LONG_MS = 300

    /** Standard easing for anything that moves on screen. */
    val Standard: Easing = FastOutSlowInEasing

    /** Elements entering: quick start, gentle stop. */
    val Enter: Easing = LinearOutSlowInEasing

    /** Elements leaving: gentle start, quick end. */
    val Exit: Easing = FastOutLinearInEasing

    /** Scale an anchored popover opens from and closes to. */
    const val POPOVER_SCALE = 0.9f

    /** Scale an inline swapped element (Send, Stop) appears from. */
    const val SWAP_SCALE = 0.6f

    /** The Android animator duration scale that means "no animations". */
    fun isReducedMotion(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f

    /** [durationMs], or 0 with [reduced] motion. */
    fun duration(durationMs: Int, reduced: Boolean): Int = if (reduced) 0 else durationMs

    /** A tween with the motion [easing]; a snap with [reduced] motion. */
    fun <T> spec(durationMs: Int, reduced: Boolean, easing: Easing = Standard, delayMs: Int = 0): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(durationMillis = durationMs, delayMillis = delayMs, easing = easing)

    /** For `animateContentSize`: a medium tween, instant with [reduced] motion. */
    fun sizeSpec(reduced: Boolean): FiniteAnimationSpec<IntSize> = spec(MEDIUM_MS, reduced)

    /** Content under a header growing downward from it ([from] the top edge by default) while it fades in. */
    fun expandEnter(reduced: Boolean, from: Alignment.Vertical = Alignment.Top): EnterTransition =
        if (reduced) {
            EnterTransition.None
        } else {
            expandVertically(spec(MEDIUM_MS, false), expandFrom = from) + fadeIn(spec(MEDIUM_MS, false, Enter))
        }

    /** The reverse of [expandEnter]: shrinks back toward the header while it fades out. */
    fun collapseExit(reduced: Boolean, towards: Alignment.Vertical = Alignment.Top): ExitTransition =
        if (reduced) {
            ExitTransition.None
        } else {
            shrinkVertically(spec(MEDIUM_MS, false), shrinkTowards = towards) + fadeOut(spec(SHORT_MS, false, Exit))
        }

    /** A popover opening from [origin] (its anchor's side, see [anchoredOrigin]). */
    fun popoverEnter(origin: TransformOrigin, reduced: Boolean): EnterTransition =
        if (reduced) {
            EnterTransition.None
        } else {
            scaleIn(spec(SHORT_MS, false, Enter), initialScale = POPOVER_SCALE, transformOrigin = origin) +
                fadeIn(spec(SHORT_MS, false, Enter))
        }

    /** A popover closing back into [origin]. */
    fun popoverExit(origin: TransformOrigin, reduced: Boolean): ExitTransition =
        if (reduced) {
            ExitTransition.None
        } else {
            scaleOut(spec(SHORT_MS, false, Exit), targetScale = POPOVER_SCALE, transformOrigin = origin) +
                fadeOut(spec(SHORT_MS, false, Exit))
        }

    /** A control appearing in a row (Send, Stop): scales and fades in while the row makes room from [from]. */
    fun inlineEnter(reduced: Boolean, from: Alignment.Horizontal = Alignment.End): EnterTransition =
        if (reduced) {
            EnterTransition.None
        } else {
            fadeIn(spec(SHORT_MS, false, Enter)) +
                scaleIn(spec(SHORT_MS, false, Enter), initialScale = SWAP_SCALE) +
                expandHorizontally(spec(SHORT_MS, false), expandFrom = from)
        }

    /** The reverse of [inlineEnter]. */
    fun inlineExit(reduced: Boolean, towards: Alignment.Horizontal = Alignment.End): ExitTransition =
        if (reduced) {
            ExitTransition.None
        } else {
            fadeOut(spec(SHORT_MS, false, Exit)) +
                scaleOut(spec(SHORT_MS, false, Exit), targetScale = SWAP_SCALE) +
                shrinkHorizontally(spec(SHORT_MS, false), shrinkTowards = towards)
        }

    /** A floating element (jump to latest) appearing in place: scale from [origin] and fade. */
    fun popEnter(reduced: Boolean, origin: TransformOrigin = TransformOrigin.Center): EnterTransition = popoverEnter(origin, reduced)

    /** The reverse of [popEnter]. */
    fun popExit(reduced: Boolean, origin: TransformOrigin = TransformOrigin.Center): ExitTransition = popoverExit(origin, reduced)

    /** One state replacing another in place (spinner and check, collapsed and open card): a crossfade, the size following. */
    fun swap(reduced: Boolean, clip: Boolean = true): ContentTransform =
        if (reduced) {
            ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = SizeTransform(clip) { _, _ -> snap() })
        } else {
            ContentTransform(
                fadeIn(spec(SHORT_MS, false, Enter, delayMs = SHORT_MS / 3)),
                fadeOut(spec(SHORT_MS, false, Exit)),
                sizeTransform = SizeTransform(clip) { _, _ -> spec(MEDIUM_MS, false) },
            )
        }

    /**
     * Where a popover grows from: the edge facing its anchor ([above] the anchor: its bottom edge)
     * on the side it is aligned to ([alignStart]: the start side, left in LTR, right in RTL).
     */
    fun anchoredOrigin(above: Boolean, layoutDirection: LayoutDirection, alignStart: Boolean = true): TransformOrigin {
        val left = alignStart == (layoutDirection == LayoutDirection.Ltr)
        return TransformOrigin(pivotFractionX = if (left) 0f else 1f, pivotFractionY = if (above) 1f else 0f)
    }
}

/** Opening a page (forward) slides it in from the end; going back, from the start. Fades with [OcMotion] tokens. */
fun <S> AnimatedContentTransitionScope<S>.ocPageTransition(forward: Boolean, reduced: Boolean): ContentTransform {
    if (reduced) return (EnterTransition.None togetherWith ExitTransition.None).using(SizeTransform(clip = false) { _, _ -> snap() })
    val direction = if (forward) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End
    val slide = OcMotion.spec<IntOffset>(OcMotion.LONG_MS, false)
    val enter = slideIntoContainer(direction, slide) { it / PAGE_SLIDE_FRACTION } + fadeIn(OcMotion.spec(OcMotion.LONG_MS, false, OcMotion.Enter))
    val exit = slideOutOfContainer(direction, slide) { it / PAGE_SLIDE_FRACTION } + fadeOut(OcMotion.spec(OcMotion.SHORT_MS, false, OcMotion.Exit))
    return (enter togetherWith exit).using(SizeTransform(clip = true) { _, _ -> OcMotion.spec(OcMotion.LONG_MS, false) })
}

/** Pages move this fraction (1/n) of their width: enough to show the direction, not a full slide. */
private const val PAGE_SLIDE_FRACTION = 8
