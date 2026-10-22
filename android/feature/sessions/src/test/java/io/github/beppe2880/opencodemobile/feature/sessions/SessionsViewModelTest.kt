package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.AgentMode
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import io.github.beppe2880.opencodemobile.core.domain.PromptDelivery
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.ServerInfo
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class SessionsViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val zone = ZoneOffset.UTC
    private val now = LocalDateTime.of(2026, 10, 13, 15, 0).toInstant(zone).toEpochMilli()
    private val hour = 3_600_000L
    private val sessions = FakeSessionRepository(
        listOf(
            session("today", now - hour),
            session("yesterday", now - 20 * hour),
            session("week", now - 3 * 24 * hour),
            session("old", now - 30 * 24 * hour),
        ),
    )
    private val events = FakeEventRepository()
    private val chat = FakeChat()
    private val servers = FakeServers()

    private fun vm() = SessionsViewModel(sessions, FakeCatalog(), events, servers, chat, clock = { now }, zone = zone)

    private fun SessionsState.ids() = groups.flatMap { g -> g.sessions.map { it.id } }

    @Test
    fun `switching server shows its stored lists at once, then refreshes them`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.FilterChanged("/p/today"))
        advanceUntilIdle()
        sessions.storedLists["s2" to null] = listOf(session("stored", now - hour))
        sessions.storedProjects["s2"] = listOf(Project("q", "/q", null, null, 0))
        val gate = CompletableDeferred<Unit>()
        sessions.gate = gate

        servers.activate("s2")
        runCurrent()

        val shown = vm.state.value
        assertEquals("Laptop", shown.serverName)
        assertEquals(listOf("stored"), shown.ids())
        assertEquals(listOf("/q"), shown.projects.map { it.directory })
        assertEquals(null, shown.directoryFilter)
        assertEquals(emptySet<String>(), shown.running)
        assertFalse(shown.loading)

        sessions.listResult = Result.success(listOf(session("fresh", now)))
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("fresh"), vm.state.value.ids())
        assertEquals(listOf("/p/today"), vm.state.value.projects.map { it.directory })

        // Going back restores the filter chosen on the first server.
        servers.activate("s1")
        advanceUntilIdle()
        assertEquals("/p/today", vm.state.value.directoryFilter)
        assertEquals("/p/today", sessions.lastDirectory)
    }

    @Test
    fun `an unreachable server keeps showing its stored list`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        sessions.storedLists["s2" to null] = listOf(session("stored", now - hour))
        sessions.listResult = Result.failure(OpenCodeError.Unreachable("down"))

        servers.activate("s2")
        advanceUntilIdle()

        assertEquals(listOf("stored"), vm.state.value.ids())
        assertEquals(null, vm.state.value.error)
        assertFalse(vm.state.value.loading)
        assertEquals(null, withTimeoutOrNull(1_000) { vm.effect.first() })
    }

    @Test
    fun `removing the server never shows its sessions under the next one`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        sessions.gate = gate

        servers.remove("s1")
        runCurrent()

        assertEquals("Laptop", vm.state.value.serverName)
        assertTrue(vm.state.value.groups.isEmpty())
        assertTrue(vm.state.value.loading)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
        assertEquals(4, vm.state.value.ids().size)
    }

    @Test
    fun `the most recent sessions get their last messages prefetched`() = runTest(main.testDispatcher) {
        sessions.listResult = Result.success((1..7).map { session("s$it", now - it * hour) }.shuffled())
        vm()
        advanceUntilIdle()

        assertEquals(listOf(listOf("s1", "s2", "s3", "s4", "s5") to 2), chat.prefetched)
    }

    @Test
    fun `groups sessions by day bucket`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(listOf(DayBucket.Today, DayBucket.Yesterday, DayBucket.ThisWeek, DayBucket.Older), state.groups.map { it.bucket })
        assertEquals("Desk", state.serverName)
        assertEquals(setOf("today"), state.running)
    }

    @Test
    fun `execution events toggle the running indicator`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        events.flow.emit(ServerEvent.ExecutionStarted("old"))
        runCurrent()
        assertTrue("old" in vm.state.value.running)

        events.flow.emit(ServerEvent.ExecutionEnded("old", SessionOutcome.Succeeded))
        runCurrent()
        assertFalse("old" in vm.state.value.running)
    }

    @Test
    fun `search is debounced and sent to the repository`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.QueryChanged("a"))
        vm.onEvent(SessionsEvent.QueryChanged("ab"))
        advanceTimeBy(100)
        assertEquals(null, sessions.lastSearch)
        advanceUntilIdle()
        assertEquals("ab", sessions.lastSearch)
    }

    @Test
    fun `failed delete restores the session and reports an error`() = runTest(main.testDispatcher) {
        sessions.deleteResult = Result.failure(OpenCodeError.Unreachable("down"))
        val vm = vm()
        advanceUntilIdle()
        val target = vm.state.value.groups.first().sessions.first()

        vm.onEvent(SessionsEvent.RequestDelete(target))
        vm.onEvent(SessionsEvent.ConfirmDelete)
        assertTrue(vm.state.value.groups.none { g -> g.sessions.any { it.id == target.id } })
        advanceUntilIdle()

        assertTrue(vm.state.value.groups.any { g -> g.sessions.any { it.id == target.id } })
        assertTrue(vm.effect.first() is SessionsEffect.ShowMessage)
    }

    @Test
    fun `initial failure shows the error state`() = runTest(main.testDispatcher) {
        sessions.listResult = Result.failure(OpenCodeError.Unreachable("down"))
        val vm = vm()
        advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.groups.isEmpty())
    }

    @Test
    fun `creating a session opens its chat`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.StartNewSession)
        advanceUntilIdle()
        val sheet = vm.state.value.newSession!!
        assertEquals("/p/today", sheet.directory)
        assertEquals("build", sheet.agent)
        assertEquals(listOf("build"), sheet.agents.map { it.id })

        vm.onEvent(SessionsEvent.NewSessionDirectory("/tmp/x", custom = true))
        vm.onEvent(SessionsEvent.CreateSession)
        advanceUntilIdle()

        assertEquals("/tmp/x", sessions.created.single())
        assertEquals(null, vm.state.value.newSession)
        assertEquals(SessionsEffect.OpenChat("new"), vm.effect.first())
    }

    @Test
    fun `a new session sheet opened before the list loads gets the directory of the latest session`() = runTest(main.testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        sessions.gate = gate
        val vm = vm()
        runCurrent()
        vm.onEvent(SessionsEvent.StartNewSession)
        runCurrent()
        assertEquals("", vm.state.value.newSession!!.directory)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("/p/today", vm.state.value.newSession!!.directory)
    }

    @Test
    fun `a directory typed before the list loads is kept`() = runTest(main.testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        sessions.gate = gate
        val vm = vm()
        runCurrent()
        vm.onEvent(SessionsEvent.StartNewSession)
        vm.onEvent(SessionsEvent.NewSessionDirectory("/tmp/x", custom = true))

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("/tmp/x", vm.state.value.newSession!!.directory)
    }

    private fun session(id: String, updated: Long) = Session(
        id = id, parentId = null, projectId = "p", title = "Title $id", directory = "/p/$id",
        agent = "build", model = null, cost = 0.0, tokens = TokenUsage(), outcome = null,
        createdAt = updated, updatedAt = updated, archivedAt = null,
    )
}

private class FakeSessionRepository(initial: List<Session>) : SessionRepository {
    var listResult: Result<List<Session>> = Result.success(initial)
    var deleteResult: Result<Unit> = Result.success(Unit)
    var lastSearch: String? = null
    var lastDirectory: String? = null
    val created = mutableListOf<String>()
    val storedLists = mutableMapOf<Pair<String, String?>, List<Session>>()
    val storedProjects = mutableMapOf<String, List<Project>>()

    /** When set, network loads wait for it. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun list(search: String?, directory: String?, limit: Int): Result<List<Session>> {
        gate?.await()
        lastSearch = search
        lastDirectory = directory
        return listResult
    }
    override suspend fun cachedList(serverId: String, directory: String?): List<Session>? = storedLists[serverId to directory]
    override suspend fun cachedProjects(serverId: String): List<Project>? = storedProjects[serverId]
    override suspend fun active(): Result<Set<String>> = Result.success(setOf("today"))
    override suspend fun get(sessionId: String): Result<Session> = Result.failure(OpenCodeError.NotFound(sessionId))
    override suspend fun create(directory: String, agent: String?, model: ModelRef?): Result<Session> {
        created += directory
        return listResult.map { it.first().copy(id = "new", directory = directory) }
    }
    override suspend fun rename(sessionId: String, title: String): Result<Unit> = Result.success(Unit)
    override suspend fun delete(sessionId: String): Result<Unit> = deleteResult
    override suspend fun fork(sessionId: String, beforeMessageId: String?): Result<Session> = Result.failure(UnsupportedOperationException())
    override suspend fun projects(): Result<List<Project>> {
        gate?.await()
        return Result.success(listOf(Project("p", "/p/today", null, null, 0)))
    }
}

private class FakeChat : ChatRepository {
    val prefetched = mutableListOf<Pair<List<String>, Int>>()

    override suspend fun prefetch(sessionIds: List<String>, lastMessages: Int) {
        prefetched += sessionIds to lastMessages
    }
    override suspend fun messages(sessionId: String): Result<List<Message>> = Result.success(emptyList())
    override suspend fun prompt(sessionId: String, text: String, delivery: PromptDelivery, attachments: List<PromptAttachment>) =
        Result.success(Unit)
    override suspend fun interrupt(sessionId: String) = Result.success(Unit)
    override suspend fun switchAgent(sessionId: String, agent: String) = Result.success(Unit)
    override suspend fun switchModel(sessionId: String, model: ModelRef) = Result.success(Unit)
    override suspend fun permissions(sessionId: String): Result<List<PermissionRequest>> = Result.success(emptyList())
    override suspend fun replyPermission(sessionId: String, requestId: String, decision: PermissionDecision, message: String?) =
        Result.success(Unit)
}

private class FakeEventRepository : EventRepository {
    override val connection: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Connected)
    val flow = MutableSharedFlow<ServerEvent>()
    override val events = flow
}

private class FakeCatalog : CatalogRepository {
    override suspend fun agents(directory: String?): Result<List<Agent>> = Result.success(
        listOf(
            Agent("build", "build", null, AgentMode.Primary, hidden = false, color = null),
            Agent("explore", "explore", null, AgentMode.Subagent, hidden = false, color = null),
            Agent("title", "title", null, AgentMode.Primary, hidden = true, color = null),
        ),
    )
    override suspend fun models(directory: String?): Result<List<ModelInfo>> = Result.success(emptyList())
}

private class FakeServers : ServerRepository {
    private val profile = ServerProfile("s1", "Desk", "http://x", secret = "x", credential = CredentialKind.Password, addedAt = 0)
    private val other = ServerProfile("s2", "Laptop", "http://y", secret = "y", credential = CredentialKind.Password, addedAt = 1)
    override val servers = MutableStateFlow(listOf(profile, other))
    override val activeServer = MutableStateFlow<ServerProfile?>(profile)
    override suspend fun probe(baseUrl: String, password: String?): Result<ServerInfo> = Result.success(ServerInfo("1", emptyList()))
    override suspend fun pair(baseUrl: String, code: String, name: String?) = Result.success(profile)
    override suspend fun addWithPassword(baseUrl: String, password: String, name: String?) = Result.success(profile)
    override suspend fun activate(serverId: String) {
        activeServer.value = servers.value.firstOrNull { it.id == serverId }
    }
    override suspend fun rename(serverId: String, name: String) = Unit
    override suspend fun remove(serverId: String) {
        servers.value = servers.value.filterNot { it.id == serverId }
        if (activeServer.value?.id == serverId) activeServer.value = servers.value.firstOrNull()
    }
}
