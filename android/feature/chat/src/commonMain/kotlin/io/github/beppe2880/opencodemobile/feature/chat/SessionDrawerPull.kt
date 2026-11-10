package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Stable

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
 * Where a drawer released at [position] (0 closed, [width] open) settles: a release faster than
 * [flingVelocity] toward a side settles on that side wherever it is, a slower one on the side
 * past half its width. Positions and velocities are toward the end edge, in the same unit.
 */
fun drawerSettlesOpen(position: Float, width: Float, velocity: Float, flingVelocity: Float): Boolean = when {
    velocity > flingVelocity -> true
    velocity < -flingVelocity -> false
    else -> position >= width / 2
}
