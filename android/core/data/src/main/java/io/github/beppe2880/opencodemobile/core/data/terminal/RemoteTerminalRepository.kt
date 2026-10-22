package io.github.beppe2880.opencodemobile.core.data.terminal

import io.github.beppe2880.opencodemobile.api.models.Pty
import io.github.beppe2880.opencodemobile.api.models.PtyCreateRequest
import io.github.beppe2880.opencodemobile.api.models.PtyUpdateRequest
import io.github.beppe2880.opencodemobile.api.models.PtyUpdateRequestSize
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class RemoteTerminalRepository(
    private val clients: OpenCodeClients,
    private val retryDelaysMillis: List<Long> = RETRY_DELAYS_MILLIS,
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

    override fun attach(directory: String, terminalId: String, input: Flow<ByteArray>): Flow<TerminalUpdate> = flow {
        // Absolute output position already shown; null until the first replay ends.
        var cursor: Long? = null
        var failures = 0
        emit(TerminalUpdate.Link(TerminalLink.Connecting))
        while (true) {
            try {
                clients.active().ptyConnect(terminalId, directory, cursor, input).collect { frame ->
                    when (frame) {
                        is PtyFrame.Cursor -> {
                            cursor = frame.value
                            failures = 0
                            emit(TerminalUpdate.Link(TerminalLink.Connected))
                        }

                        is PtyFrame.Output -> {
                            cursor = cursor?.plus(frame.text.length)
                            emit(TerminalUpdate.Output(frame.text))
                        }
                    }
                }
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                when (val error = e.toOpenCodeError()) {
                    is OpenCodeError.NotFound -> break
                    is OpenCodeError.Unauthorized -> throw error
                    else -> {
                        emit(TerminalUpdate.Link(TerminalLink.Reconnecting))
                        delay(retryDelaysMillis[minOf(failures, retryDelaysMillis.lastIndex)])
                        failures++
                    }
                }
            }
        }
        emit(TerminalUpdate.Link(TerminalLink.Exited))
    }

    private companion object {
        val RETRY_DELAYS_MILLIS = listOf(500L, 1_000L, 2_000L, 5_000L, 10_000L)
    }
}

private fun Pty.toDomain() = TerminalInfo(id = id, title = title, running = status == Pty.Status.RUNNING)
