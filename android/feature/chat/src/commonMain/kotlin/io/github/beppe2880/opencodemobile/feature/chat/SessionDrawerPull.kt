package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Stable

/**
 * The session list drawer of the chat's host, which a start-to-end swipe in the chat pulls open
 * ([io.github.beppe2880.opencodemobile.core.domain.ChatSwipe.SessionDrawer]). Distances and
 * velocities are in pixels toward the end edge (to the right, to the left in RTL); negative
 * values move back toward the start.
 */
@Stable
interface SessionDrawerPull {
    /** The finger moved by [delta]: the drawer follows it. */
    fun drag(delta: Float)

    /** The finger left at [velocity] per second: the drawer settles open or closed. */
    fun release(velocity: Float)
}
