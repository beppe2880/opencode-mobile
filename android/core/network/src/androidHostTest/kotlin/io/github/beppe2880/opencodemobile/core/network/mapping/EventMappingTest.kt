package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.SessionMessagesResponse
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import io.github.beppe2880.opencodemobile.core.domain.TranscriptReducer
import io.github.beppe2880.opencodemobile.core.network.Fixtures
import io.github.beppe2880.opencodemobile.core.network.decodeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventMappingTest {

    private val sessionId = "ses_ec9cb5c70ffe3M22jWcPWdSAIF"
    private val answer = "Read `a.txt` (it contains \"hello\") and ran `echo probe-ok`, which printed `probe-ok`."

    @Test
    fun `every recorded event decodes`() {
        assertEquals(94, Fixtures.ssePayloads().size)
        assertEquals(94, Fixtures.events().size)
    }

    @Test
    fun `unknown event types are skipped`() {
        assertNull(decodeEvent("""{"id":"evt_1","created":1,"type":"future.thing","data":{}}"""))
        assertNull(decodeEvent("[]"))
    }

    @Test
    fun `a branch switch carries its project directory`() {
        val payload = """{"id":"evt_3","created":1,"type":"vcs.branch.updated","location":{"directory":"/repo"},"data":{"branch":"feat"}}"""
        assertEquals(ServerEvent.BranchChanged("/repo", "feat"), decodeEvent(payload)?.toDomain())
    }

    @Test
    fun `a fork announces the new session`() {
        val payload = """{"id":"evt_2","created":1,"type":"session.forked","durable":{"aggregateID":"ses_new","seq":0,"version":2},""" +
            """"data":{"sessionID":"ses_new","parentID":"ses_old","boundary":{"type":"through","messageID":"msg_1"}}}"""
        assertEquals(ServerEvent.SessionChanged("ses_new"), decodeEvent(payload)?.toDomain())
    }

    @Test
    fun `maps the session subset and drops the rest`() {
        val events = Fixtures.events().mapNotNull { it.toDomain() }

        assertEquals(ServerEvent.Connected, events.first())
        assertEquals(ServerEvent.SessionChanged(sessionId), events[1])
        // The stored idle message reuses the event id (evt_ -> msg_).
        assertEquals(
            ServerEvent.ExecutionEnded(sessionId, SessionOutcome.Succeeded, "msg_13634c29a001sUSC1CxpcTg4ER", 1791910789786),
            events.last(),
        )
        assertTrue(events.contains(ServerEvent.McpsChanged))
        assertTrue(events.drop(1).filterNot { it == ServerEvent.McpsChanged }.all { it is ServerEvent.SessionScoped && it.sessionId == sessionId })

        val enqueued = events.filterIsInstance<ServerEvent.InboxEnqueued>().single()
        assertEquals("msg_13634b644001FZJ7cqjGjVVWUe", enqueued.inboxId)
        assertTrue(enqueued.text.startsWith("Read the file a.txt"))

        val called = events.filterIsInstance<ServerEvent.ToolCalled>().map { it.input }
        assertEquals(listOf("""{"path":"a.txt"}""", """{"command":"echo probe-ok"}"""), called)

        val ended = events.filterIsInstance<ServerEvent.StepEnded>().last()
        assertEquals("stop", ended.finish)
        assertEquals(1791910789784, ended.at)
        assertEquals(152L, ended.tokens.output)
        assertEquals(1791910789782, events.filterIsInstance<ServerEvent.StepStreamed>().last().at)
    }

    @Test
    fun `replaying the stream rebuilds the stored transcript`() {
        val live = Fixtures.events()
            .mapNotNull { it.toDomain() as? ServerEvent.SessionScoped }
            .fold(emptyList<Message>(), TranscriptReducer::reduce)
        val stored = Fixtures.decode<SessionMessagesResponse>("session_messages.json").data.mapNotNull { it.toDomain() }

        assertEquals(stored.map { it.id }, live.map { it.id })
        val user = live[0] as Message.User
        assertFalse(user.pending)
        assertEquals((stored[0] as Message.User).text, user.text)

        // The live idle marker matches the stored one, so turn boundaries agree.
        assertEquals(stored.last(), live.last())

        val steps = live.filterIsInstance<Message.Assistant>()
        assertTrue(steps.none { it.isStreaming })
        assertEquals(
            stored.filterIsInstance<Message.Assistant>().map { it.parts.map(Part::key) },
            steps.map { it.parts.map(Part::key) },
        )

        val parts = steps.flatMap { it.parts }
        assertEquals(
            listOf("reasoning", "tool:read", "tool:shell", "reasoning", "text"),
            parts.map { if (it is Part.Tool) "tool:${it.name}" else it.key.substringBefore('-') },
        )
        assertTrue(parts.none { (it is Part.Reasoning && it.streaming) || (it is Part.Text && it.streaming) })

        val tools = parts.filterIsInstance<Part.Tool>()
        assertTrue(tools.all { it.status == ToolStatus.Completed })
        assertEquals(listOf("Read file a.txt, lines 1-1\n1: hello", "probe-ok\n"), tools.map { it.output })
        assertEquals("sh_13634bce7001rRCsnxY0W7OlZ9", tools[1].metadata["shellID"])

        assertEquals(answer, (parts.last() as Part.Text).text)
        assertEquals("stop", steps.last().finish)
    }
}
