package io.github.beppe2880.opencodemobile.fake

import io.github.beppe2880.opencodemobile.core.domain.ConnectTarget
import io.github.beppe2880.opencodemobile.core.network.ConnectLinkParser
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.DraftRepository
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
import io.github.beppe2880.opencodemobile.core.domain.ServerDiscovery
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import io.github.beppe2880.opencodemobile.core.domain.TranscriptReducer
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import org.koin.dsl.module

/**
 * In-memory data layer with realistic sample data for UI work on the emulator without a
 * server. Prompts stream a scripted reply through [FakeEvents] so live states can be inspected.
 * Enable with `./gradlew :app:installDebug -PfakeData=true`.
 */
val fakeDataModule = module {
    single { FakeStore() }
    single<ServerRepository> { FakeServers() }
    single<ServerDiscovery> {
        object : ServerDiscovery {
            override fun discover(): Flow<String> = flow {
                delay(1_500)
                emit("http://192.168.1.42:4096")
            }
        }
    }
    single { FakeEvents() }
    single<EventRepository> { get<FakeEvents>() }
    single<SessionRepository> { FakeSessions(get(), get()) }
    single<ChatRepository> { FakeChat(get(), get()) }
    single<CatalogRepository> { FakeCatalog() }
    single<SettingsRepository> { FakeSettings() }
    single<DraftRepository> { FakeDrafts() }
}

private const val LATENCY = 350L

internal class FakeStore {
    private val now = System.currentTimeMillis()
    private val shift = now - SampleData.NOW
    val sessions = MutableStateFlow(SampleData.sessions.map { it.copy(createdAt = it.createdAt + shift, updatedAt = it.updatedAt + shift) })
    val transcripts = mutableMapOf<String, List<Message>>()
    val permissions = mutableMapOf<String, List<PermissionRequest>>()
    val running = mutableSetOf("ses_2")

    init {
        val base = SampleData.transcript.map { shiftMessage(it, shift) }
        sessions.value.forEach { transcripts[it.id] = base }
        permissions["ses_2"] = listOf(SampleData.permission.copy(sessionId = "ses_2"))
    }

    private fun shiftMessage(message: Message, shift: Long): Message = when (message) {
        is Message.User -> message.copy(createdAt = message.createdAt + shift)
        is Message.Assistant -> message.copy(
            createdAt = message.createdAt + shift,
            completedAt = message.completedAt?.plus(shift),
            streamedAt = message.streamedAt?.plus(shift),
        )
        is Message.Notice -> message.copy(createdAt = message.createdAt + shift)
        is Message.Shell -> message.copy(createdAt = message.createdAt + shift)
        is Message.Idle -> message.copy(createdAt = message.createdAt + shift)
        is Message.Synthetic -> message.copy(createdAt = message.createdAt + shift)
    }
}

internal class FakeServers : ServerRepository {
    private val _servers = MutableStateFlow(SampleData.servers)
    override val servers: StateFlow<List<ServerProfile>> = _servers.asStateFlow()
    private val _active = MutableStateFlow<ServerProfile?>(null)
    override val activeServer: StateFlow<ServerProfile?> = _active.asStateFlow()

    override suspend fun probe(baseUrl: String, password: String?): Result<ServerInfo> {
        delay(LATENCY)
        return when {
            "unreachable" in baseUrl -> Result.failure(OpenCodeError.Unreachable("refused"))
            password == null -> Result.failure(OpenCodeError.Unauthorized())
            else -> Result.success(ServerInfo("2.0.27", listOf(baseUrl)))
        }
    }

    override suspend fun pair(baseUrl: String, code: String, name: String?): Result<ServerProfile> {
        delay(LATENCY)
        if (code.equals("bad", ignoreCase = true)) return Result.failure(OpenCodeError.PairingFailed("expired"))
        return Result.success(save(baseUrl, CredentialKind.PairingToken, name))
    }

    override suspend fun addWithPassword(baseUrl: String, password: String, name: String?): Result<ServerProfile> {
        delay(LATENCY)
        if (password == "wrong") return Result.failure(OpenCodeError.Unauthorized())
        return Result.success(save(baseUrl, CredentialKind.Password, name))
    }

    private fun save(baseUrl: String, kind: CredentialKind, name: String?): ServerProfile {
        val existing = _servers.value.firstOrNull { it.baseUrl == baseUrl }
        val profile = existing ?: ServerProfile(
            id = "srv-${System.nanoTime()}",
            name = name ?: baseUrl.substringAfter("://").substringBefore(':'),
            baseUrl = baseUrl,
            secret = "x",
            credential = kind,
            addedAt = System.currentTimeMillis(),
        )
        if (existing == null) _servers.value = listOf(profile) + _servers.value
        _active.value = profile
        return profile
    }

    override suspend fun activate(serverId: String) {
        _active.value = _servers.value.firstOrNull { it.id == serverId }
    }

    override suspend fun rename(serverId: String, name: String) {
        _servers.value = _servers.value.map { if (it.id == serverId) it.copy(name = name) else it }
        if (_active.value?.id == serverId) _active.value = _servers.value.first { it.id == serverId }
    }

    override suspend fun remove(serverId: String) {
        _servers.value = _servers.value.filterNot { it.id == serverId }
        if (_active.value?.id == serverId) _active.value = null
    }

    override fun parseConnectTarget(raw: String): ConnectTarget? = ConnectLinkParser.parse(raw)
}

internal class FakeEvents : EventRepository {
    override val connection: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Connected)
    private val flow = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 256)
    override val events: Flow<ServerEvent> = flow.onSubscription { emit(ServerEvent.Connected) }
    suspend fun emit(event: ServerEvent) = flow.emit(event)
}

internal class FakeSessions(private val store: FakeStore, private val events: FakeEvents) : SessionRepository {
    override suspend fun list(search: String?, directory: String?, limit: Int): Result<List<Session>> {
        delay(LATENCY)
        return Result.success(
            store.sessions.value
                .filter { search == null || it.title.contains(search, ignoreCase = true) }
                .filter { directory == null || it.directory == directory }
                .sortedByDescending { it.updatedAt }
                .take(limit),
        )
    }

    override suspend fun active(): Result<Set<String>> = Result.success(store.running.toSet())

    override suspend fun get(sessionId: String): Result<Session> =
        store.sessions.value.firstOrNull { it.id == sessionId }?.let { Result.success(it) }
            ?: Result.failure(OpenCodeError.NotFound("session"))

    override suspend fun create(directory: String, agent: String?, model: ModelRef?): Result<Session> {
        delay(LATENCY)
        val now = System.currentTimeMillis()
        val session = Session(
            id = "ses_${now}", parentId = null, projectId = "p", title = "New session",
            directory = directory, agent = agent ?: "build", model = model ?: SampleData.haiku,
            cost = 0.0, tokens = TokenUsage(), outcome = null, createdAt = now, updatedAt = now, archivedAt = null,
        )
        store.sessions.value = listOf(session) + store.sessions.value
        store.transcripts[session.id] = emptyList()
        return Result.success(session)
    }

    override suspend fun rename(sessionId: String, title: String): Result<Unit> {
        delay(LATENCY)
        store.sessions.value = store.sessions.value.map { if (it.id == sessionId) it.copy(title = title) else it }
        events.emit(ServerEvent.SessionChanged(sessionId))
        return Result.success(Unit)
    }

    override suspend fun delete(sessionId: String): Result<Unit> {
        delay(LATENCY)
        store.sessions.value = store.sessions.value.filterNot { it.id == sessionId }
        return Result.success(Unit)
    }

    override suspend fun fork(sessionId: String, beforeMessageId: String?): Result<Session> {
        delay(LATENCY)
        val original = store.sessions.value.firstOrNull { it.id == sessionId }
            ?: return Result.failure(OpenCodeError.NotFound("session"))
        val now = System.currentTimeMillis()
        val fork = original.copy(id = "ses_$now", title = "${original.title} (fork #1)", createdAt = now, updatedAt = now)
        val history = store.transcripts[sessionId].orEmpty()
        store.sessions.value = listOf(fork) + store.sessions.value
        store.transcripts[fork.id] = beforeMessageId?.let { id -> history.takeWhile { it.id != id } } ?: history
        return Result.success(fork)
    }

    override suspend fun projects(): Result<List<Project>> = Result.success(SampleData.projects)
}

internal class FakeChat(private val store: FakeStore, private val events: FakeEvents) : ChatRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun messages(sessionId: String): Result<List<Message>> {
        delay(LATENCY)
        return Result.success(store.transcripts[sessionId].orEmpty())
    }

    override suspend fun prompt(
        sessionId: String,
        text: String,
        delivery: PromptDelivery,
        attachments: List<PromptAttachment>,
    ): Result<Unit> {
        delay(150)
        val now = System.currentTimeMillis()
        val uploads = attachments.count { it.mention == null }
        store.transcripts[sessionId] = store.transcripts[sessionId].orEmpty() + Message.User("u_$now", now, text, uploads)
        scope.launch { simulateReply(sessionId, text) }
        return Result.success(Unit)
    }

    private suspend fun simulateReply(sessionId: String, prompt: String) {
        val id = "a_${System.currentTimeMillis()}"
        val session = store.sessions.value.firstOrNull { it.id == sessionId }
        val agent = session?.agent ?: "build"
        val model = session?.model ?: SampleData.haiku
        val emitted = mutableListOf<ServerEvent.SessionScoped>()
        suspend fun send(event: ServerEvent.SessionScoped) {
            emitted += event
            events.emit(event)
        }
        store.running += sessionId
        events.emit(ServerEvent.ExecutionStarted(sessionId))
        val start = System.currentTimeMillis()
        send(ServerEvent.StepStarted(sessionId, id, agent, model, start))
        send(ServerEvent.ReasoningStarted(sessionId, id, 0, start))
        "Let me look at the project before answering.".chunked(6).forEach {
            delay(60)
            send(ServerEvent.ReasoningDelta(sessionId, id, 0, it))
        }
        send(ServerEvent.ReasoningEnded(sessionId, id, 0, "", System.currentTimeMillis()))
        send(ServerEvent.ToolInputStarted(sessionId, id, "c_$id", "read", System.currentTimeMillis()))
        send(ServerEvent.ToolCalled(sessionId, id, "c_$id", """{"filePath":"${session?.directory}/README.md"}"""))
        delay(700)
        send(ServerEvent.ToolSucceeded(sessionId, id, "c_$id", "1: # Project\n2: Sample readme", emptyMap(), System.currentTimeMillis()))
        val answer = "You asked: **${prompt.take(60)}**.\n\nHere is a streamed reply from the fake data layer, with `inline code` and a block:\n\n```kotlin\nfun main() = println(\"hello\")\n```\n\nDone."
        send(ServerEvent.TextStarted(sessionId, id, 1))
        answer.chunked(8).forEach {
            delay(45)
            send(ServerEvent.TextDelta(sessionId, id, 1, it))
        }
        send(ServerEvent.TextEnded(sessionId, id, 1, answer))
        val end = System.currentTimeMillis()
        send(ServerEvent.StepEnded(sessionId, id, "stop", 0.0042, TokenUsage(input = 1200, output = 180), end))
        val final = emitted.fold(store.transcripts[sessionId].orEmpty()) { acc, e -> TranscriptReducer.reduce(acc, e) }
        store.transcripts[sessionId] = final
        store.running -= sessionId
        events.emit(ServerEvent.ExecutionEnded(sessionId, SessionOutcome.Succeeded))
    }

    override suspend fun interrupt(sessionId: String): Result<Unit> {
        store.running -= sessionId
        events.emit(ServerEvent.ExecutionEnded(sessionId, SessionOutcome.Interrupted))
        return Result.success(Unit)
    }

    override suspend fun switchAgent(sessionId: String, agent: String): Result<Unit> {
        store.sessions.value = store.sessions.value.map { if (it.id == sessionId) it.copy(agent = agent) else it }
        return Result.success(Unit)
    }

    override suspend fun switchModel(sessionId: String, model: ModelRef): Result<Unit> {
        store.sessions.value = store.sessions.value.map { if (it.id == sessionId) it.copy(model = model) else it }
        return Result.success(Unit)
    }

    override suspend fun permissions(sessionId: String): Result<List<PermissionRequest>> =
        Result.success(store.permissions[sessionId].orEmpty())

    override suspend fun replyPermission(sessionId: String, requestId: String, decision: PermissionDecision, message: String?): Result<Unit> {
        delay(LATENCY)
        store.permissions[sessionId] = store.permissions[sessionId].orEmpty().filterNot { it.id == requestId }
        return Result.success(Unit)
    }
}

internal class FakeCatalog : CatalogRepository {
    override suspend fun agents(directory: String?): Result<List<Agent>> = Result.success(SampleData.agents)
    override suspend fun models(directory: String?): Result<List<ModelInfo>> = Result.success(SampleData.models)
}

internal class FakeDrafts : DraftRepository {
    private val drafts = mutableMapOf<String, String>()
    override fun load(sessionId: String): String = drafts[sessionId].orEmpty()
    override fun save(sessionId: String, text: String) {
        if (text.isBlank()) drafts -= sessionId else drafts[sessionId] = text
    }
}

internal class FakeSettings : SettingsRepository {
    // Sample data is for screenshots and UI work: skip the notification explanation.
    private val _settings = MutableStateFlow(AppSettings(notificationsIntroShown = true))
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        _settings.value = transform(_settings.value)
    }
}
