package io.github.beppe2880.opencodemobile.core.network

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.fromHttpToGmtDate
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.concurrent.Volatile

/**
 * How far the server clock is ahead of this device, learned from the `Date` header of responses,
 * so durations of server timestamps (a command's start) do not depend on the phone's clock.
 *
 * `Date` has whole seconds and is written before the response arrives, so each response gives
 * `Date + 999 ms - arrival`: never behind the real offset by more than the transfer time, so an
 * elapsed time is not shown shorter than it is (on a LAN, by at most a few milliseconds). The
 * smallest of the last [WINDOW] samples is used, which removes most of the one-second rounding
 * and follows a device clock that is changed. 0 until a response arrives.
 */
class ServerClock {
    private val lock = SynchronizedObject()
    private val samples = ArrayDeque<Long>()

    @Volatile
    var offsetMs: Long = 0L
        private set

    fun record(serverDateMs: Long, receivedAtMs: Long) = synchronized(lock) {
        samples.addLast(serverDateMs + DATE_RESOLUTION_MS - 1 - receivedAtMs)
        while (samples.size > WINDOW) samples.removeFirst()
        offsetMs = samples.min()
    }

    internal companion object {
        const val WINDOW = 16
        const val DATE_RESOLUTION_MS = 1_000L
    }
}

/** Feeds [clock] from every response that has a `Date` header. */
internal fun serverClockPlugin(clock: ServerClock) = createClientPlugin("ServerClock") {
    onResponse { response ->
        val date = response.headers[HttpHeaders.Date] ?: return@onResponse
        val server = runCatching { date.fromHttpToGmtDate().timestamp }.getOrNull() ?: return@onResponse
        clock.record(server, response.responseTime.timestamp)
    }
}
