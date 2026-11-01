package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.SessionMessagesResponse
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import io.github.beppe2880.opencodemobile.core.domain.TranscriptReducer
import io.github.beppe2880.opencodemobile.core.network.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A `bash` call recorded from OpenCode 2.2.1 while it ran (`for i in $(seq 1 30); ...`): where
 * the chat can find the shell it prints to.
 */
class LiveShellFixtureTest {

    private val callId = "toolu_011VK49E5rAcJEGeH7tHgxkQ"
    private val shellId = "sh_19846bb5a0016Hcm0TX7E0r087"
    private val command = "for i in \$(seq 1 30); do echo line \$i; sleep 1; done"

    @Test
    fun `the stored running tool has no shell id`() {
        val stored = Fixtures.decode<SessionMessagesResponse>("shell_running_messages.json").data.mapNotNull { it.toDomain() }
        val tool = stored.filterIsInstance<Message.Assistant>().flatMap { it.parts }.filterIsInstance<Part.Tool>().single()

        assertEquals(callId, tool.callId)
        assertEquals(ToolStatus.Running, tool.status)
        assertEquals("""{"command":"${command.replace("\"", "\\\"")}"}""", tool.input)
        assertNull(tool.metadata["shellID"])
    }

    @Test
    fun `only the transient progress event names the shell`() {
        val events = Fixtures.events("shell_running_events.sse").mapNotNull { it.toDomain() }
        val progress = events.filterIsInstance<ServerEvent.ToolProgress>().single()
        assertEquals(callId, progress.callId)
        assertEquals(mapOf("shellID" to shellId), progress.metadata)

        // Replayed, the stream keeps it on the tool until it ends.
        val scoped = events.filterIsInstance<ServerEvent.SessionScoped>()
        val running = scoped.takeWhile { it !is ServerEvent.ToolSucceeded }.fold(emptyList<Message>(), TranscriptReducer::reduce)
        val tool = running.filterIsInstance<Message.Assistant>().flatMap { it.parts }.filterIsInstance<Part.Tool>().single()
        assertEquals(ToolStatus.Running, tool.status)
        assertEquals(shellId, tool.metadata["shellID"])
    }
}
