package io.github.beppe2880.opencodemobile.core.data.events

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Signals that let a waiting reconnect retry immediately. */
internal interface NetworkMonitor {
    /**
     * Emits the current network state when collection starts and every change afterwards:
     * whether a usable default network is present. A new network replacing the previous one
     * is reported as `true` again, so a waiting retry can start at once.
     */
    val connected: Flow<Boolean>
}

/**
 * Keeps one connection to [NetworkMonitor] and turns it into the retry policy shared by the
 * server stream and the terminal: an attempt starts only with a network, a failure waits for
 * the backoff (cut short by a network report) and, while offline, waits for recovery before
 * touching the server again.
 *
 * The latest reported state is the only truth: it is read before every attempt, so a change
 * that happened while the caller was busy or suspended elsewhere is never missed. Until the
 * monitor reports, or when it fails or ends without reporting, the network counts as present:
 * a broken monitor must not stop the app from connecting.
 */
internal class NetworkGate(
    private val monitor: NetworkMonitor,
    private val firstReportTimeoutMillis: Long = FIRST_REPORT_TIMEOUT_MILLIS,
) {
    // Null until the monitor reports.
    private val state = MutableStateFlow<Boolean?>(null)

    // Every report, repeated "online" ones included, to cut a backoff short.
    private val reports = Channel<Boolean>(Channel.CONFLATED)
    private var collector: Job? = null

    val online: Boolean get() = state.value != false

    /** Whether the network counts as present, starting with the current state. */
    val connected: Flow<Boolean> = state.map { it != false }

    /** Starts monitoring in [scope] and gives the monitor a moment to report the current state. */
    suspend fun start(scope: CoroutineScope) {
        collector = scope.launch {
            try {
                monitor.connected.collect {
                    // Report first: whoever resumes on the new state finds its report queued.
                    reports.trySend(it)
                    state.value = it
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                state.value = true
            }
            if (state.value == null) state.value = true
        }
        withTimeoutOrNull(firstReportTimeoutMillis) { state.first { it != null } }
        // The current state is not a change: it must not cut the first backoff short.
        reports.tryReceive()
    }

    /** Waits for a network before an attempt; with one present, returns at once. */
    suspend fun waitToAttempt() {
        state.first { it != false }
    }

    /**
     * Waits [delayMillis] after a failure while online. Any network report ends the wait early:
     * a new network is worth trying at once, and going offline hands over to [waitToAttempt].
     */
    suspend fun waitAfterFailure(delayMillis: Long) {
        if (online) withTimeoutOrNull(delayMillis) { reports.receive() }
    }

    fun stop() {
        reports.close()
        collector?.cancel()
    }

    private companion object {
        const val FIRST_REPORT_TIMEOUT_MILLIS = 1_000L
    }
}
