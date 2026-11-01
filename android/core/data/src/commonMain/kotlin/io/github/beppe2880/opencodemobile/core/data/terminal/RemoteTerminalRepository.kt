package io.github.beppe2880.opencodemobile.core.data.terminal

import io.github.beppe2880.opencodemobile.api.models.Pty
import io.github.beppe2880.opencodemobile.api.models.PtyCreateRequest
import io.github.beppe2880.opencodemobile.api.models.PtyUpdateRequest
import io.github.beppe2880.opencodemobile.api.models.PtyUpdateRequestSize
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.events.NetworkGate
import io.github.beppe2880.opencodemobile.core.data.events.NetworkMonitor
import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.TerminalInfo
import io.github.beppe2880.opencodemobile.core.domain.TerminalLink
import io.github.beppe2880.opencodemobile.core.domain.TerminalRepository
import io.github.beppe2880.opencodemobile.core.domain.TerminalUpdate
import io.github.beppe2880.opencodemobile.core.network.PtyFrame
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import io.github.beppe2880.opencodemobile.core.network.awaitSuccess
import io.github.beppe2880.opencodemobile.core.network.toOpenCodeError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

internal class RemoteTerminalRepository(
    private val clients: OpenCodeClients,
    private val retryDelaysMillis: List<Long> = RETRY_DELAYS_MILLIS,
    private val network: NetworkMonitor,
) : TerminalRepository {

    override suspend fun open(directory: String): Result<TerminalInfo> = resultOf {
        val api = clients.active().pty
        val running = awaitBody { api.ptyList(locationDirectory = directory) }.data
            .firstOrNull { it.status == Pty.Status.RUNNING }
        (running ?: awaitBody { api.ptyCreate(PtyCreateRequest(), locationDirectory = directory) }.data).toDomain()
    }

    override suspend fun resize(directory: String, terminalId: String, columns: Int, rows: Int): Result<Unit> = resultOf {
        val api = clients.active().pty
        val size = PtyUpdateRequestSize(cols = columns.toLong(), rows = rows.toLong())
        awaitSuccess { api.ptyUpdate(terminalId, PtyUpdateRequest(propertySize = size), locationDirectory = directory) }
    }

    override suspend fun close(directory: String, terminalId: String): Result<Unit> = resultOf {
        val api = clients.active().pty
        awaitSuccess { api.ptyRemove(terminalId, locationDirectory = directory) }
    }

    override fun attach(
        directory: String,
        terminalId: String,
        input: Flow<ByteArray>,
        visible: Flow<Boolean>,
    ): Flow<TerminalUpdate> = channelFlow {
        // Absolute output position already shown; null until the first replay ends.
        var cursor: Long? = null
        var failures = 0
        var link: TerminalLink? = null
        suspend fun link(state: TerminalLink) {
            if (state != link) send(TerminalUpdate.Link(state))
            link = state
        }
        val gate = NetworkGate(network)
        gate.start(this)
        try {
            link(TerminalLink.Connecting)
            while (true) {
                // Both at once: the network can go while the terminal is hidden, and the other way round.
                combine(visible, gate.connected, ::Pair).first { (shown, online) ->
                    // Shown without a network: the old connection is gone, so do not look connected.
                    if (shown && !online && cursor != null) link(TerminalLink.Reconnecting)
                    shown && online
                }
                var hidden = false
                var failure: Throwable? = null
                val connection = launch {
                    try {
                        clients.active().ptyConnect(terminalId, directory, cursor, input).collect { frame ->
                            when (frame) {
                                is PtyFrame.Cursor -> {
                                    cursor = frame.value
                                    failures = 0
                                    link(TerminalLink.Connected)
                                }

                                is PtyFrame.Output -> {
                                    cursor = cursor?.plus(frame.text.length)
                                    send(TerminalUpdate.Output(frame.text))
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        if (!hidden) throw e
                    } catch (e: Exception) {
                        failure = e
                    }
                }
                // Closing the socket, instead of just stopping collection, also stops its pings.
                val watcher = launch {
                    visible.collect { state ->
                        if (!state) {
                            hidden = true
                            connection.cancel()
                        }
                    }
                }
                connection.join()
                watcher.cancel()
                if (hidden) {
                    // Closed for hiding: the screen keeps its last state, the next one is sent again.
                    link = null
                    continue
                }
                if (failure == null) break
                when (val error = failure.toOpenCodeError()) {
                    is OpenCodeError.NotFound -> break
                    is OpenCodeError.Unauthorized -> throw error
                    else -> {
                        link(TerminalLink.Reconnecting)
                        visible.firstOrNull { it } ?: awaitCancellation()
                        gate.waitAfterFailure(retryDelaysMillis[minOf(failures, retryDelaysMillis.lastIndex)])
                        failures++
                    }
                }
            }
            link(TerminalLink.Exited)
        } finally {
            gate.stop()
        }
    }

    private companion object {
        val RETRY_DELAYS_MILLIS = listOf(500L, 1_000L, 2_000L, 5_000L, 10_000L)
    }
}

private fun Pty.toDomain() = TerminalInfo(id = id, title = title, running = status == Pty.Status.RUNNING)
