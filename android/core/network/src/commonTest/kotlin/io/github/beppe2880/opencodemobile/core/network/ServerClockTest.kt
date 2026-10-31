package io.github.beppe2880.opencodemobile.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerClockTest {

    @Test
    fun `unknown until a response arrives`() {
        assertEquals(0, ServerClock().offsetMs)
    }

    @Test
    fun `a device 13 s behind is corrected without showing less than the real time`() {
        val clock = ServerClock()
        val realOffset = 13_250L
        // Responses whose server time falls at different points of the Date second.
        for (serverNow in listOf(1_000_000L, 1_007_400L, 1_012_999L, 1_020_000L)) {
            val date = serverNow / 1_000 * 1_000
            clock.record(serverDateMs = date, receivedAtMs = serverNow - realOffset)
        }
        assertTrue(clock.offsetMs >= realOffset)
        assertEquals(realOffset, clock.offsetMs)
    }

    @Test
    fun `follows a device clock that is changed`() {
        val clock = ServerClock()
        repeat(ServerClock.WINDOW) { clock.record(serverDateMs = 10_000, receivedAtMs = 10_000 - 60_000) }
        repeat(ServerClock.WINDOW) { clock.record(serverDateMs = 20_000, receivedAtMs = 20_000) }
        assertEquals(999, clock.offsetMs)
    }
}
