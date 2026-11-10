package io.github.beppe2880.opencodemobile.feature.chat

import kotlin.test.Test
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
}
