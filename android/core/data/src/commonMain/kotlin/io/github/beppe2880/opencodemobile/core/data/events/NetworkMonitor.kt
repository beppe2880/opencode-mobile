package io.github.beppe2880.opencodemobile.core.data.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Signals that let a waiting reconnect retry immediately. */
internal interface NetworkMonitor {
    /**
     * Emits the current network state when collection starts and every change afterwards:
     * whether a usable default network is present.
     */
    val connected: Flow<Boolean>
}

/**
 * Keeps one connection to [NetworkMonitor] and turns it into the retry policy shared by the
 * server stream and the terminal: an attempt starts only with a network, a failure waits for
 * the backoff (cut short by a network change) and, while offline, waits for recovery before
 * touching the server again.
 */
internal class NetworkGate(private val monitor: NetworkMonitor) {
    private val changes = Channel<Boolean>(Channel.CONFLATED)
    private var collector: Job? = null
    private var online = false

    /** Starts monitoring in [scope] and learns the current state before callers retry. */
    suspend fun start(scope: CoroutineScope) {
        collector = scope.launch { monitor.connected.collect { changes.trySend(it) } }
        online = changes.receive()
    }

    /** Waits for a network before an attempt; with one present, returns at once. */
    suspend fun waitToAttempt() {
        if (!online) waitToRecover()
    }

    /**
     * Waits [delayMillis] after a failure while online. A transition to offline ends the wait
     * early and makes the next attempt wait for recovery instead.
     */
    suspend fun waitAfterFailure(delayMillis: Long) {
        if (!online) {
            waitToRecover()
            return
        }
        val change = withTimeoutOrNull(delayMillis) { changes.receive() }
        if (change == false) online = false
    }

    suspend fun stop() {
        changes.close()
        collector?.cancel()
    }

    private suspend fun waitToRecover() {
        while (true) {
            if (changes.receive()) {
                online = true
                return
            }
        }
    }
}
