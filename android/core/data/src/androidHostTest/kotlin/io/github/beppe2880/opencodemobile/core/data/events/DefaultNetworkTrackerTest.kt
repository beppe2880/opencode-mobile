package io.github.beppe2880.opencodemobile.core.data.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultNetworkTrackerTest {

    // Like android.net.Network: equal by id, a new object in every callback.
    private class Net(val id: Int) {
        override fun equals(other: Any?) = other is Net && other.id == id
        override fun hashCode() = id
    }

    @Test
    fun `a loss reported with another object for the same network goes offline`() {
        val tracker = DefaultNetworkTracker(Net(1))

        assertEquals(false, tracker.onLost(Net(1)))
        assertFalse(tracker.connected)
    }

    @Test
    fun `without a network at start it is offline until one arrives`() {
        val tracker = DefaultNetworkTracker<Net>(null)
        assertFalse(tracker.connected)
        assertNull(tracker.onLost(Net(1)))

        assertEquals(true, tracker.onAvailable(Net(1)))
        assertTrue(tracker.connected)
    }

    @Test
    fun `switching networks reports the new one and ignores the old one going`() {
        val tracker = DefaultNetworkTracker(Net(1))

        assertNull(tracker.onAvailable(Net(1)))
        assertEquals(true, tracker.onAvailable(Net(2)))
        assertNull(tracker.onLost(Net(1)))
        assertTrue(tracker.connected)
    }

    @Test
    fun `blocking only counts for the current network`() {
        val tracker = DefaultNetworkTracker(Net(1))

        assertNull(tracker.onBlockedStatusChanged(Net(2), true))
        assertEquals(false, tracker.onBlockedStatusChanged(Net(1), true))
        assertFalse(tracker.connected)
        assertEquals(true, tracker.onAvailable(Net(2)))
        assertTrue(tracker.connected)
    }
}
