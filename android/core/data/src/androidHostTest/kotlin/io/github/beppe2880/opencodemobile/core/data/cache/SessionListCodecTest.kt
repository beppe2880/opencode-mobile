package io.github.beppe2880.opencodemobile.core.data.cache

import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionListCodecTest {

    private val full = Session(
        id = "ses_1", parentId = "ses_0", projectId = "p", title = "Title", directory = "/home/user/project",
        agent = "build", model = ModelRef("m", "p", "high"), cost = 0.25,
        tokens = TokenUsage(input = 1, output = 2, reasoning = 3, cacheRead = 4, cacheWrite = 5),
        outcome = SessionOutcome.Interrupted, createdAt = 10, updatedAt = 20, archivedAt = 30,
        idleAt = 40, viewedAt = 35, revertMessageId = "msg_1",
    )
    private val bare = full.copy(
        id = "ses_2", parentId = null, agent = null, model = null, tokens = TokenUsage(), outcome = null,
        archivedAt = null, revertMessageId = null,
    )

    @Test
    fun `sessions and projects survive a round trip`() {
        val sessions = listOf(full, bare)
        assertEquals(sessions, SessionListCodec.decodeSessions(SessionListCodec.encodeSessions(sessions)))
        assertEquals(emptyList<Session>(), SessionListCodec.decodeSessions(SessionListCodec.encodeSessions(emptyList())))

        val projects = listOf(Project("p", "/home/user/project", "Project", "git", 5), Project("q", "/q", null, null, 0))
        assertEquals(projects, SessionListCodec.decodeProjects(SessionListCodec.encodeProjects(projects)))
    }

    @Test
    fun `missing or corrupt data reads as nothing stored`() {
        assertNull(SessionListCodec.decodeSessions(null))
        assertNull(SessionListCodec.decodeSessions("{not json"))
        assertNull(SessionListCodec.decodeProjects("""[{"id":"p"}]"""))
    }

    @Test
    fun `unknown outcomes and extra fields are tolerated`() {
        val raw = """[{"id":"s","projectId":"p","title":"t","directory":"/d","outcome":"Exploded","createdAt":1,"updatedAt":2,"extra":true}]"""
        val session = SessionListCodec.decodeSessions(raw)!!.single()
        assertNull(session.outcome)
        assertEquals(TokenUsage(), session.tokens)
    }
}
