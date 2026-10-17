package io.github.beppe2880.opencodemobile.notifications

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.ServerInfo
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionMonitorTest {

    private val events = FakeEvents()
    private val sessions = FakeSessions()
    private val chat = FakeChat()
    private val settings = FakeSettings()
    private val servers = FakeServers()
    private val notifier = RecordingNotifier()
    private val visible = MutableStateFlow(false)
    private var serviceStarts = 0

    private fun TestScope.monitor() = SessionMonitor(
        events, sessions, chat, settings, servers, notifier, visible, startService = { serviceStarts++ },
    ).also {
        it.start(backgroundScope)
        runCurrent()
    }

    private suspend fun TestScope.emit(event: ServerEvent) {
        events.flow.emit(event)
        runCurrent()
    }

    @Test
    fun `does nothing while notifications are off`() = runTest {
        settings.settings.value = AppSettings(notifications = false)
        val monitor = monitor()
        assertEquals(0, events.flow.subscriptionCount.value)

        settings.settings.value = AppSettings(notifications = true)
        runCurrent()
        emit(ServerEvent.ExecutionStarted("ses_a"))
        assertEquals(listOf("ses_a"), monitor.running.value.keys.toList())
    }

    @Test
    fun `follows root sessions and their activity, ignoring subagents`() = runTest {
        sessions.add(session("ses_a", "Fix the bug"))
        sessions.add(session("ses_child", "Explore", parentId = "ses_a"))
        val monitor = monitor()

        emit(ServerEvent.ExecutionStarted("ses_a"))
        emit(ServerEvent.ExecutionStarted("ses_child"))
        emit(ServerEvent.ToolInputStarted("ses_a", "msg_1", "call_1", "bash", 0))

        assertEquals(
            mapOf("ses_a" to RunningSession("ses_a", "Fix the bug", SessionActivity.Tool("bash"))),
            monitor.running.value,
        )
    }

    @Test
    fun `notifies finished sessions only while the app is in the background`() = runTest {
        sessions.add(session("ses_a", "First"))
        sessions.add(session("ses_b", "Second"))
        monitor()

        visible.value = true
        emit(ServerEvent.ExecutionStarted("ses_a"))
        emit(ServerEvent.ExecutionEnded("ses_a", SessionOutcome.Succeeded))
        visible.value = false
        emit(ServerEvent.ExecutionStarted("ses_b"))
        emit(ServerEvent.ExecutionEnded("ses_b", SessionOutcome.Failed))

        assertEquals(listOf("finished ses_b Second Failed"), notifier.log.filter { it.startsWith("finished") })
    }

    @Test
    fun `notifies a permission request once and removes it when answered`() = runTest {
        sessions.add(session("ses_a", "Fix the bug"))
        monitor()
        emit(ServerEvent.ExecutionStarted("ses_a"))

        chat.permissions["ses_a"] = listOf(permission("per_1"))
        emit(ServerEvent.PermissionChanged("ses_a"))
        emit(ServerEvent.PermissionChanged("ses_a"))
        chat.permissions["ses_a"] = emptyList()
        emit(ServerEvent.PermissionChanged("ses_a"))

        assertEquals(listOf("permission Fix the bug per_1", "cancel per_1"), notifier.attention())
    }

    @Test
    fun `a request seen while the app is open is notified when the app leaves`() = runTest {
        sessions.add(session("ses_a", "Fix the bug"))
        visible.value = true
        monitor()
        emit(ServerEvent.ExecutionStarted("ses_a"))
        chat.forms["ses_a"] = listOf(Form("frm_1", "ses_a", "Which database?", emptyList()))
        emit(ServerEvent.FormsChanged("ses_a"))
        assertEquals(emptyList<String>(), notifier.attention())

        visible.value = false
        runCurrent()

        assertEquals(listOf("question Fix the bug frm_1"), notifier.attention())
    }

    @Test
    fun `a reconnect catches up with sessions that finished meanwhile`() = runTest {
        sessions.add(session("ses_a", "Fix the bug", outcome = SessionOutcome.Succeeded))
        sessions.add(session("ses_b", "Write tests"))
        val monitor = monitor()
        emit(ServerEvent.ExecutionStarted("ses_a"))

        sessions.activeIds = setOf("ses_b")
        emit(ServerEvent.Connected)

        assertEquals(listOf("ses_b"), monitor.running.value.keys.toList())
        assertTrue("finished ses_a Fix the bug Succeeded" in notifier.log)
    }

    @Test
    fun `starts the service while the app is visible and something runs`() = runTest {
        sessions.add(session("ses_a", "Fix the bug"))
        monitor()
        emit(ServerEvent.ExecutionStarted("ses_a"))
        assertEquals(0, serviceStarts)

        visible.value = true
        runCurrent()
        assertEquals(1, serviceStarts)
    }

    @Test
    fun `switching server forgets running sessions and pending requests`() = runTest {
        sessions.add(session("ses_a", "Fix the bug"))
        val monitor = monitor()
        emit(ServerEvent.ExecutionStarted("ses_a"))
        chat.permissions["ses_a"] = listOf(permission("per_1"))
        emit(ServerEvent.PermissionChanged("ses_a"))

        servers.activeServer.value = servers.activeServer.value!!.copy(id = "other")
        runCurrent()

        assertTrue(monitor.running.value.isEmpty())
        assertEquals("cancel per_1", notifier.attention().last())
    }

    private fun session(id: String, title: String, parentId: String? = null, outcome: SessionOutcome? = null) =
        Session(id, parentId, "prj", title, "/home/user/project", null, null, 0.0, TokenUsage(), outcome, 0, 0, null)

    private fun permission(id: String) = PermissionRequest(id, "ses_a", "edit", listOf("src/todos.ts"), null, canSave = true)
}

private class RecordingNotifier : MonitorNotifier {
    val log = mutableListOf<String>()
    fun attention() = log.filter { it.startsWith("permission") || it.startsWith("question") || it.startsWith("cancel ") }
    override fun permission(sessionTitle: String, request: PermissionRequest) { log += "permission $sessionTitle ${request.id}" }
    override fun question(sessionTitle: String, form: Form) { log += "question $sessionTitle ${form.id}" }
    override fun cancelAttention(id: String) { log += "cancel $id" }
    override fun finished(sessionId: String, title: String, outcome: SessionOutcome) { log += "finished $sessionId $title $outcome" }
    override fun cancelFinished(sessionId: String) { log += "cancelFinished $sessionId" }
}

private class FakeEvents : EventRepository {
    val flow = MutableSharedFlow<ServerEvent>()
    override val connection = MutableStateFlow(ConnectionState.Connected)
    override val events = flow
}

private class FakeSessions : SessionRepository {
    private val byId = mutableMapOf<String, Session>()
    var activeIds: Set<String> = emptySet()
    fun add(session: Session) { byId[session.id] = session }
    override suspend fun list(search: String?, directory: String?, limit: Int) = Result.success(byId.values.toList())
    override suspend fun active() = Result.success(activeIds)
    override suspend fun get(sessionId: String) =
        byId[sessionId]?.let { Result.success(it) } ?: Result.failure(IllegalStateException(sessionId))
    override suspend fun create(directory: String, agent: String?, model: ModelRef?) = error("unused")
    override suspend fun rename(sessionId: String, title: String) = Result.success(Unit)
    override suspend fun delete(sessionId: String) = Result.success(Unit)
    override suspend fun fork(sessionId: String, beforeMessageId: String?) = error("unused")
    override suspend fun projects() = Result.success(emptyList<Project>())
}

private class FakeChat : ChatRepository {
    val permissions = mutableMapOf<String, List<PermissionRequest>>()
    val forms = mutableMapOf<String, List<Form>>()
    override suspend fun messages(sessionId: String) = Result.success(emptyList<Message>())
    override suspend fun prompt(
        sessionId: String,
        text: String,
        delivery: io.github.beppe2880.opencodemobile.core.domain.PromptDelivery,
        attachments: List<io.github.beppe2880.opencodemobile.core.domain.PromptAttachment>,
    ) = Result.success(Unit)
    override suspend fun interrupt(sessionId: String) = Result.success(Unit)
    override suspend fun switchAgent(sessionId: String, agent: String) = Result.success(Unit)
    override suspend fun switchModel(sessionId: String, model: ModelRef) = Result.success(Unit)
    override suspend fun permissions(sessionId: String) = Result.success(permissions[sessionId].orEmpty())
    override suspend fun forms(sessionId: String) = Result.success(forms[sessionId].orEmpty())
    override suspend fun replyPermission(sessionId: String, requestId: String, decision: PermissionDecision, message: String?) =
        Result.success(Unit)
}

private class FakeSettings : SettingsRepository {
    override val settings = MutableStateFlow(AppSettings(notifications = true))
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.value = transform(settings.value)
    }
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
