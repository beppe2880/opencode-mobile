package io.github.beppe2880.opencodemobile.core.domain

import kotlinx.coroutines.flow.Flow

/** A pseudo-terminal running on the server in a project folder. */
data class TerminalInfo(
    val id: String,
    val title: String,
    val running: Boolean,
)

enum class TerminalLink { Connecting, Connected, Reconnecting, Exited }

/** What an attached terminal reports: its output, and how the connection is doing. */
sealed interface TerminalUpdate {
    /** Terminal output, escape sequences included. */
    class Output(val text: String) : TerminalUpdate

    data class Link(val state: TerminalLink) : TerminalUpdate
}

interface TerminalRepository {
    /** The running terminal of [directory], or a new one when there is none. */
    suspend fun open(directory: String): Result<TerminalInfo>

    suspend fun resize(directory: String, terminalId: String, columns: Int, rows: Int): Result<Unit>

    /** Ends the shell and removes the terminal from the server. */
    suspend fun close(directory: String, terminalId: String): Result<Unit>

    /**
     * Streams the terminal while collected, sending each [input] chunk as typed. Starts with the
     * output the server kept, reconnects after drops without replaying it again, and ends after
     * [TerminalLink.Exited] once the shell is gone. Fails with [OpenCodeError.Unauthorized].
     */
    fun attach(directory: String, terminalId: String, input: Flow<ByteArray>): Flow<TerminalUpdate>
}
