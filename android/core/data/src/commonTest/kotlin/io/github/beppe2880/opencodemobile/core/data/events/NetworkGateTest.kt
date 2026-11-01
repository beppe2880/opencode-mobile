package io.github.beppe2880.opencodemobile.core.data.events

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkGateTest {

    private class Monitor(override val connected: Flow<Boolean>) : NetworkMonitor

    private suspend fun TestScope.started(connected: Flow<Boolean>): NetworkGate =
        NetworkGate(Monitor(connected)).also { it.start(backgroundScope) }

    @Test
    fun `the current state is known as soon as the gate starts`() = runTest {
        val network = MutableStateFlow(false)
        val gate = started(network)

        assertFalse(gate.online)
        assertEquals(0, currentTime)
    }

    @Test
    fun `a monitor that never reports does not block connections`() = runTest {
        val gate = started(flow { awaitCancellation() })

        assertTrue(gate.online)
        assertEquals(1_000, currentTime)
        gate.waitToAttempt()
    }

    @Test
    fun `a monitor that ends or fails without reporting counts as online`() = runTest {
        assertTrue(started(emptyFlow()).online)
        assertTrue(started(flow { throw IllegalStateException("no permission") }).online)
        assertEquals(0, currentTime)
    }

    @Test
    fun `waiting to attempt reads a change that happened while nobody was waiting`() = runTest {
        val network = MutableStateFlow(true)
        val gate = started(network)
        gate.waitToAttempt()

        // Lost while the caller was busy with a connection or waiting for something else.
        network.value = false
        runCurrent()
        val attempt = async { gate.waitToAttempt() }
        advanceTimeBy(60_000)
        assertFalse(attempt.isCompleted)

        network.value = true
        runCurrent()
        assertTrue(attempt.isCompleted)
    }

    @Test
    fun `a quick loss and recovery between attempts lets the next one start`() = runTest {
        val network = MutableStateFlow(true)
        val gate = started(network)
        network.value = false
        runCurrent()
        network.value = true
        runCurrent()

        gate.waitToAttempt()
        assertEquals(0, currentTime)
    }

    @Test
    fun `the backoff ends early on a network report and is skipped while offline`() = runTest {
        val network = MutableStateFlow(true)
        val gate = started(network)

        val waiting = async { gate.waitAfterFailure(60_000) }
        advanceTimeBy(1_000)
        network.value = false
        runCurrent()
        assertTrue(waiting.isCompleted)
        assertEquals(1_000, currentTime)

        gate.waitAfterFailure(60_000)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `without reports the backoff runs to its end`() = runTest {
        val gate = started(MutableStateFlow(true))

        gate.waitAfterFailure(5_000)

        assertEquals(5_000, currentTime)
    }

    @Test
    fun `flapping ends in the latest state`() = runTest {
        val network = MutableStateFlow(true)
        val gate = started(network)
        repeat(50) {
            network.value = it % 2 == 0
            runCurrent()
        }
        assertFalse(gate.online)

        val attempt = async { gate.waitToAttempt() }
        advanceTimeBy(10_000)
        assertFalse(attempt.isCompleted)
        network.value = true
        runCurrent()
        assertTrue(attempt.isCompleted)
    }
}
