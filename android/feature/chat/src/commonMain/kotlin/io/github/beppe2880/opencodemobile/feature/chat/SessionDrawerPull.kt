package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.SwipeSensitivity

/**
 * The session list drawer of the chat's host, which a start-to-end swipe in the chat pulls open
 * ([io.github.beppe2880.opencodemobile.core.domain.ChatSwipe.SessionDrawer]). Distances and
 * velocities are in pixels toward the end edge (to the right, to the left in RTL); negative
 * values move back toward the start. On wide screens it is the session list beside the chat,
 * which the swipe hides and shows the same way.
 */
@Stable
interface SessionDrawerPull {
    /** The finger moved by [delta]: the drawer follows it. */
    fun drag(delta: Float)

    /** The finger left at [velocity] per second: the drawer settles open or closed. */
    fun release(velocity: Float)
}

/**
 * How a [SwipeSensitivity] tunes the drawer swipe, all in one place:
 * - [settleShare]: share of the width a slow drag must cover to change side (open from closed,
 *   or closed from open);
 * - [flingVelocity]: a release faster than this per second settles on its side wherever it is;
 * - [slopFactor]: multiplies the system touch slop before the swipe starts following the finger;
 * - [horizontalRatio]: when above 0, the movement up to the slop must be this many times more
 *   horizontal than vertical, or the swipe leaves the gesture to scrolling.
 *
 * [SwipeSensitivity.Medium] is the original behaviour: half the width, 400 dp/s, the system slop.
 */
enum class DrawerSwipeTuning(val settleShare: Float, val flingVelocity: Dp, val slopFactor: Float, val horizontalRatio: Float) {
    Low(settleShare = 0.66f, flingVelocity = 700.dp, slopFactor = 2f, horizontalRatio = 2f),
    Medium(settleShare = 0.5f, flingVelocity = 400.dp, slopFactor = 1f, horizontalRatio = 0f),
    High(settleShare = 0.25f, flingVelocity = 250.dp, slopFactor = 0.5f, horizontalRatio = 1f),
}

/** The drawer swipe tuning of this sensitivity. */
val SwipeSensitivity.drawerTuning: DrawerSwipeTuning
    get() = when (this) {
        SwipeSensitivity.Low -> DrawerSwipeTuning.Low
        SwipeSensitivity.Medium -> DrawerSwipeTuning.Medium
        SwipeSensitivity.High -> DrawerSwipeTuning.High
    }

/**
 * Where a drawer released at [position] (0 closed, [width] open) settles: a release faster than
 * [flingVelocity] toward a side settles on that side wherever it is. A slower one changes side
 * once the drag covered [settleShare] of the width from where it started: past `settleShare` of
 * the width when it started closed, below `1 - settleShare` when it started open ([fromOpen]).
 * At the default half both are the same point. Positions and velocities are toward the end
 * edge, in the same unit.
 */
fun drawerSettlesOpen(
    position: Float,
    width: Float,
    velocity: Float,
    flingVelocity: Float,
    settleShare: Float = 0.5f,
    fromOpen: Boolean = false,
): Boolean = when {
    velocity > flingVelocity -> true
    velocity < -flingVelocity -> false
    fromOpen -> position >= width * (1f - settleShare)
    else -> position >= width * settleShare
}
