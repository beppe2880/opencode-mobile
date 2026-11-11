package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.SwipeSensitivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawerSettleTest {

    private val width = 300f
    private val fling = 400f

    @Test
    fun slowReleaseSettlesOnTheSidePastHalfTheWidth() {
        assertTrue(drawerSettlesOpen(position = 150f, width = width, velocity = 0f, flingVelocity = fling))
        assertTrue(drawerSettlesOpen(position = 280f, width = width, velocity = -100f, flingVelocity = fling))
        assertFalse(drawerSettlesOpen(position = 149f, width = width, velocity = 0f, flingVelocity = fling))
        assertFalse(drawerSettlesOpen(position = 20f, width = width, velocity = 100f, flingVelocity = fling))
    }

    @Test
    fun flingTowardTheEndShowsWhereverItIs() {
        assertTrue(drawerSettlesOpen(position = 10f, width = width, velocity = 401f, flingVelocity = fling))
    }

    @Test
    fun flingTowardTheStartHidesWhereverItIs() {
        assertFalse(drawerSettlesOpen(position = 290f, width = width, velocity = -401f, flingVelocity = fling))
    }

    @Test
    fun releaseAtTheFlingVelocityIsDecidedByPosition() {
        assertFalse(drawerSettlesOpen(position = 100f, width = width, velocity = 400f, flingVelocity = fling))
        assertTrue(drawerSettlesOpen(position = 200f, width = width, velocity = -400f, flingVelocity = fling))
    }

    private fun settles(sensitivity: SwipeSensitivity, position: Float, velocity: Float = 0f, fromOpen: Boolean = false): Boolean {
        val tuning = sensitivity.drawerTuning
        // Velocities in dp per second, as the hosts compare them.
        return drawerSettlesOpen(position, width, velocity, tuning.flingVelocity.value, tuning.settleShare, fromOpen)
    }

    @Test
    fun mediumIsTheOriginalRule() {
        val medium = SwipeSensitivity.Medium.drawerTuning
        assertEquals(0.5f, medium.settleShare)
        assertEquals(400f, medium.flingVelocity.value)
        for (fromOpen in listOf(false, true)) {
            assertTrue(settles(SwipeSensitivity.Medium, 150f, fromOpen = fromOpen))
            assertFalse(settles(SwipeSensitivity.Medium, 149f, fromOpen = fromOpen))
        }
        assertFalse(settles(SwipeSensitivity.Medium, 10f, velocity = 400f))
        assertTrue(settles(SwipeSensitivity.Medium, 10f, velocity = 401f))
    }

    @Test
    fun highOpensAndClosesAfterAShortDragOrASlowFling() {
        // A quarter of the width opens it, a quarter back closes it.
        assertTrue(settles(SwipeSensitivity.High, 75f))
        assertFalse(settles(SwipeSensitivity.High, 74f))
        assertFalse(settles(SwipeSensitivity.High, 224f, fromOpen = true))
        assertTrue(settles(SwipeSensitivity.High, 225f, fromOpen = true))
        // Flings Medium ignores already count.
        assertTrue(settles(SwipeSensitivity.High, 10f, velocity = 300f))
        assertFalse(settles(SwipeSensitivity.High, 290f, velocity = -300f, fromOpen = true))
        assertFalse(settles(SwipeSensitivity.Medium, 10f, velocity = 300f))
    }

    @Test
    fun lowNeedsALongDragOrAFastFling() {
        // Two thirds of the width to open, two thirds back to close.
        assertFalse(settles(SwipeSensitivity.Low, 150f))
        assertFalse(settles(SwipeSensitivity.Low, 197f))
        assertTrue(settles(SwipeSensitivity.Low, 199f))
        assertTrue(settles(SwipeSensitivity.Low, 150f, fromOpen = true))
        assertTrue(settles(SwipeSensitivity.Low, 102f, fromOpen = true))
        assertFalse(settles(SwipeSensitivity.Low, 101f, fromOpen = true))
        // Flings that open with Medium are not enough.
        assertFalse(settles(SwipeSensitivity.Low, 10f, velocity = 600f))
        assertTrue(settles(SwipeSensitivity.Low, 10f, velocity = 701f))
        assertTrue(settles(SwipeSensitivity.Low, 290f, velocity = -600f, fromOpen = true))
        assertFalse(settles(SwipeSensitivity.Low, 290f, velocity = -701f, fromOpen = true))
    }

    @Test
    fun higherSensitivityStartsSoonerAndAcceptsSteeperDrags() {
        val (low, medium, high) = listOf(SwipeSensitivity.Low, SwipeSensitivity.Medium, SwipeSensitivity.High).map { it.drawerTuning }
        assertTrue(high.slopFactor < medium.slopFactor && medium.slopFactor < low.slopFactor)
        assertEquals(1f, medium.slopFactor)
        assertTrue(low.horizontalRatio >= 2f)
    }
}
