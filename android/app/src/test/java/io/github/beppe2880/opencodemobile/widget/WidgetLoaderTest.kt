package io.github.beppe2880.opencodemobile.widget

import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.ServerInfo
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLoaderTest {

    private val servers = FakeServers()
    private val sessions = FakeSessions()
    private val loader = WidgetLoader(servers, sessions, timeoutMillis = 1_000)

    @Test
    fun `running sessions come first, then the most recently updated, without subagents or archived ones`() {
        val recent = listOf(
            session("ses_old", updatedAt = 1),
            session("ses_new", updatedAt = 5),
            session("ses_run", updatedAt = 2),
            session("ses_child", updatedAt = 9, parentId = "ses_new"),
            session("ses_archived", updatedAt = 8, archivedAt = 8),
        )

        val shown = widgetSessions(recent, running = setOf("ses_run"))

        assertEquals(listOf("ses_run", "ses_new", "ses_old"), shown.map { it.id })
        assertEquals(listOf(true, false, false), shown.map { it.running })
        assertEquals("project", shown.first().project)
    }

    @Test
    fun `the widget shows at most a dozen sessions`() {
        val recent = (1..20).map { session("ses_$it", updatedAt = it.toLong()) }

        assertEquals(MAX_WIDGET_SESSIONS, widgetSessions(recent, emptySet()).size)
    }

    @Test
    fun `without an active server the widget asks to connect`() = runTest {
        servers.activeServer.value = null

        assertEquals(WidgetModel.NoServer, loader.load())
    }

    @Test
    fun `sessions and running state come from the server`() = runTest {
        sessions.remote = listOf(session("ses_a", updatedAt = 1), session("ses_b", updatedAt = 2))
        sessions.running = setOf("ses_a")

        val model = loader.load() as WidgetModel.Sessions

        assertEquals("Desktop", model.serverName)
        assertEquals(listOf("ses_a", "ses_b"), model.sessions.map { it.id })
        assertEquals(1, model.runningCount)
        assertEquals(false, model.stale)
    }

    @Test
    fun `an unreachable server falls back to the saved sessions, none of them running`() = runTest {
        sessions.failing = true
        sessions.cached = listOf(session("ses_saved", updatedAt = 1))
        sessions.running = setOf("ses_saved")

        val model = loader.load() as WidgetModel.Sessions

        assertEquals(listOf("ses_saved"), model.sessions.map { it.id })
        assertEquals(0, model.runningCount)
        assertEquals(true, model.stale)
    }

    @Test
    fun `a server that does not answer in time also falls back to the saved sessions`() = runTest {
        sessions.hanging = true
        sessions.cached = listOf(session("ses_saved", updatedAt = 1))

        val model = loader.load() as WidgetModel.Sessions

        assertEquals(true, model.stale)
        assertEquals(listOf("ses_saved"), model.sessions.map { it.id })
    }

    private fun session(id: String, updatedAt: Long, parentId: String? = null, archivedAt: Long? = null) =
        Session(id, parentId, "prj", "Title $id", "/home/user/project", null, null, 0.0, TokenUsage(), null, 0, updatedAt, archivedAt)
}

private class FakeSessions : SessionRepository {
    var remote: List<Session> = emptyList()
    var cached: List<Session>? = null
    var running: Set<String> = emptySet()
    var failing = false
    var hanging = false

    override suspend fun list(search: String?, directory: String?, limit: Int): Result<List<Session>> {
        if (hanging) awaitCancellation()
        return if (failing) Result.failure(OpenCodeError.Unreachable("down")) else Result.success(remote)
    }

    override suspend fun active(): Result<Set<String>> =
        if (failing) Result.failure(OpenCodeError.Unreachable("down")) else Result.success(running)

    override suspend fun cachedList(serverId: String, directory: String?) = cached
    override suspend fun get(sessionId: String) = error("unused")
    override suspend fun create(directory: String, agent: String?, model: ModelRef?) = error("unused")
    override suspend fun rename(sessionId: String, title: String) = Result.success(Unit)
    override suspend fun delete(sessionId: String) = Result.success(Unit)
    override suspend fun fork(sessionId: String, beforeMessageId: String?) = error("unused")
    override suspend fun projects() = Result.success(emptyList<Project>())
}

private class FakeServers : ServerRepository {
    private val profile = ServerProfile("srv", "Desktop", "http://localhost:4096", secret = "x", credential = CredentialKind.Password, addedAt = 0)
    override val servers = MutableStateFlow(listOf(profile))
    override val activeServer = MutableStateFlow<ServerProfile?>(profile)
    override suspend fun probe(baseUrl: String, password: String?) = Result.success(ServerInfo("2", emptyList()))
    override suspend fun pair(baseUrl: String, code: String, name: String?) = Result.success(profile)
    override suspend fun addWithPassword(baseUrl: String, password: String, name: String?) = Result.success(profile)
    override suspend fun activate(serverId: String) = Unit
    override suspend fun rename(serverId: String, name: String) = Unit
    override suspend fun remove(serverId: String) = Unit
}
