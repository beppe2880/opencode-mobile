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
import io.github.beppe2880.opencodemobile.core.domain.UsageStats
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.FileDiff
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.QueuedPrompt
import io.github.beppe2880.opencodemobile.core.domain.DailyActivity
import io.github.beppe2880.opencodemobile.core.domain.ModelUsage
import java.time.Instant
import java.time.ZoneId
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
import io.github.beppe2880.opencodemobile.core.data.di.AppScope

/**
 * In-memory data layer with realistic sample data for UI work on the emulator without a
 * server. Prompts stream a scripted reply through [FakeEvents] so live states can be inspected.
 * Enable with `./gradlew :app:installDebug -PfakeData=true`.
 */
val fakeDataModule = module {
    single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
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
    val running = mutableSetOf(DotfilesDemo.SESSION, "ses_6")
    val inbox = mutableMapOf(
        "ses_6" to listOf(
            QueuedPrompt("in_1", "Then add a test for expired coupons", PromptDelivery.Queue, now - 60_000),
            QueuedPrompt("in_2", "And mention coupons in the README", PromptDelivery.Queue, now - 30_000),
        ),
    )
    val forms = mutableMapOf("ses_3" to listOf(FakeSamples.checkoutForm))

    init {
        val base = SampleData.transcript.map { shiftMessage(it, shift) }
        sessions.value.forEach { transcripts[it.id] = base }
        permissions["ses_6"] = listOf(SampleData.permission.copy(sessionId = "ses_6"))
        transcripts[DotfilesDemo.SESSION] = DotfilesDemo.transcript()
        sessions.value = sessions.value.map { if (it.id == DotfilesDemo.SESSION) it.copy(model = SampleData.sonnet) else it }
        permissions[DotfilesDemo.SESSION] = listOf(DotfilesDemo.backupPermission)
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
    // Starts signed in to the first sample server; "Add server" still opens the connect flow.
    private val _active = MutableStateFlow(SampleData.servers.firstOrNull())
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

    override suspend fun usage(from: Long, to: Long, timeZone: String): Result<UsageStats> {
        delay(LATENCY)
        val zone = ZoneId.of(timeZone)
        val first = Instant.ofEpochMilli(from).atZone(zone).toLocalDate()
        val last = Instant.ofEpochMilli(to).atZone(zone).toLocalDate()
        // Counted back from today, so today always has activity.
        val pattern = longArrayOf(184, 47, 158, 312, 96, 241, 0, 205, 266, 131)
        val days = generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()
        val activity = days.mapIndexed { i, day -> DailyActivity(day.toString(), pattern[(days.size - 1 - i) % pattern.size]) }
        val steps = activity.sumOf { it.steps }
        fun tokens(share: Double) = TokenUsage(
            input = (steps * 310 * share).toLong(),
            output = (steps * 1_150 * share).toLong(),
            reasoning = (steps * 240 * share).toLong(),
            cacheRead = (steps * 41_000 * share).toLong(),
            cacheWrite = (steps * 2_600 * share).toLong(),
        )
        val models = listOf(
            ModelUsage(SampleData.sonnet, (steps * 0.62).toLong(), steps * 0.031, tokens(0.62)),
            ModelUsage(SampleData.gpt, (steps * 0.27).toLong(), steps * 0.018, tokens(0.27)),
            ModelUsage(SampleData.haiku, steps - (steps * 0.62).toLong() - (steps * 0.27).toLong(), steps * 0.0014, tokens(0.11)),
        )
        return Result.success(
            UsageStats(
                sessions = (steps / 38).coerceAtLeast(if (steps > 0) 1 else 0),
                subagents = steps / 120,
                prompts = steps / 9,
                steps = steps,
                cost = models.sumOf { it.cost },
                tokens = tokens(1.0),
                activeDays = activity.count { it.steps > 0 }.toLong(),
                streak = activity.takeLastWhile { it.steps > 0 }.size.toLong(),
                activity = activity,
                models = models,
            ),
        )
    }
}

internal class FakeChat(private val store: FakeStore, private val events: FakeEvents) : ChatRepository {
    private val demo = DotfilesDemo(store, events)
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
        if (delivery == PromptDelivery.Queue && sessionId in store.running) {
            store.inbox[sessionId] = store.inbox[sessionId].orEmpty() + QueuedPrompt("in_$now", text, delivery, now)
            events.emit(ServerEvent.InboxEnqueued(sessionId, "in_$now", text, now))
            return Result.success(Unit)
        }
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

    override suspend fun shell(sessionId: String, command: String): Result<Unit> {
        delay(600)
        val now = System.currentTimeMillis()
        val output = FakeSamples.shellOutput(command)
        store.transcripts[sessionId] = store.transcripts[sessionId].orEmpty() +
            Message.Shell("sh_$now", now, command, "completed", output, "0")
        events.emit(ServerEvent.TranscriptChanged(sessionId, "shell"))
        return Result.success(Unit)
    }

    override suspend fun inbox(sessionId: String): Result<List<QueuedPrompt>> =
        Result.success(store.inbox[sessionId].orEmpty())

    override suspend fun cancelQueued(sessionId: String, inboxId: String): Result<Unit> {
        delay(150)
        store.inbox[sessionId] = store.inbox[sessionId].orEmpty().filterNot { it.id == inboxId }
        events.emit(ServerEvent.InboxCancelled(sessionId, inboxId))
        return Result.success(Unit)
    }

    override suspend fun steerQueued(sessionId: String, inboxId: String): Result<Unit> {
        delay(150)
        val prompt = store.inbox[sessionId].orEmpty().firstOrNull { it.id == inboxId } ?: return Result.success(Unit)
        store.inbox[sessionId] = store.inbox[sessionId].orEmpty().filterNot { it.id == inboxId }
        events.emit(ServerEvent.InboxDelivered(sessionId, inboxId))
        return prompt(sessionId, prompt.text, PromptDelivery.Steer, emptyList())
    }

    override suspend fun forms(sessionId: String): Result<List<Form>> = Result.success(store.forms[sessionId].orEmpty())

    override suspend fun replyForm(sessionId: String, formId: String, answers: Map<String, FormAnswer>): Result<Unit> {
        delay(LATENCY)
        if (demo.onForm(formId, answers)) return Result.success(Unit)
        store.forms[sessionId] = store.forms[sessionId].orEmpty().filterNot { it.id == formId }
        events.emit(ServerEvent.FormsChanged(sessionId))
        return Result.success(Unit)
    }

    override suspend fun cancelForm(sessionId: String, formId: String): Result<Unit> = replyForm(sessionId, formId, mapOf("ls" to FormAnswer.Text("eza")))

    override suspend fun changes(sessionId: String, scope: ChangesScope): Result<List<FileDiff>> {
        delay(LATENCY)
        val directory = store.sessions.value.firstOrNull { it.id == sessionId }?.directory.orEmpty()
        val diffs = FakeSamples.diffs(directory)
        return Result.success(if (scope == ChangesScope.LastTurn) diffs.take(2) else diffs)
    }

    override suspend fun permissions(sessionId: String): Result<List<PermissionRequest>> =
        Result.success(store.permissions[sessionId].orEmpty())

    override suspend fun replyPermission(sessionId: String, requestId: String, decision: PermissionDecision, message: String?): Result<Unit> {
        delay(LATENCY)
        if (demo.onPermission(requestId, decision)) return Result.success(Unit)
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
