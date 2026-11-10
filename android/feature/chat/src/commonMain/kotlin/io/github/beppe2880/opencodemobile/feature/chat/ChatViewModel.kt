package io.github.beppe2880.opencodemobile.feature.chat

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.DraftRepository
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.ImageAttachment
import io.github.beppe2880.opencodemobile.core.domain.McpRepository
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.NoticeKind
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import io.github.beppe2880.opencodemobile.core.domain.PromptDelivery
import io.github.beppe2880.opencodemobile.core.domain.QueuedPrompt
import io.github.beppe2880.opencodemobile.core.domain.RECENT_MODELS
import io.github.beppe2880.opencodemobile.core.domain.RecentModelRepository
import io.github.beppe2880.opencodemobile.core.domain.RunningWork
import io.github.beppe2880.opencodemobile.core.domain.RunningWorkLoader
import io.github.beppe2880.opencodemobile.core.domain.SelectionApply
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.ShareRepository
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import io.github.beppe2880.opencodemobile.core.domain.TranscriptReducer
import io.github.beppe2880.opencodemobile.core.domain.VcsBranch
import io.github.beppe2880.opencodemobile.core.domain.VcsRepository
import io.github.beppe2880.opencodemobile.core.domain.initialAnswers
import io.github.beppe2880.opencodemobile.core.domain.isComplete
import io.github.beppe2880.opencodemobile.core.domain.recentModels
import io.github.beppe2880.opencodemobile.core.domain.reply
import io.github.beppe2880.opencodemobile.core.domain.shellOwners
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.core.ui.components.selectablePrimary
import io.github.beppe2880.opencodemobile.core.ui.text.UiText
import io.github.beppe2880.opencodemobile.core.ui.text.toUiText
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_attachment_failed
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_nothing_to_redo
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_nothing_to_undo
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_agents_failed
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_commands_failed
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_session_deleted
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.PluralStringResource
import kotlin.time.Clock

class ChatViewModel(
    private val sessionId: String,
    private val sessions: SessionRepository,
    private val chat: ChatRepository,
    private val catalog: CatalogRepository,
    private val events: EventRepository,
    private val settings: SettingsRepository,
    private val drafts: DraftRepository,
    private val images: ImageMemory,
    private val scroll: ScrollMemory,
    private val mcps: McpRepository,
    private val recents: RecentModelRepository,
    private val vcs: VcsRepository,
    private val shares: ShareRepository,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : BaseViewModel<ChatState, ChatEvent, ChatEffect>(ChatState(sessionId = sessionId, savedScroll = scroll.get(sessionId))) {

    private var messages: List<Message> = emptyList()
    private var showReasoning: Boolean = settings.settings.value.showReasoning
    private var groupActivity: Boolean = settings.settings.value.groupActivity
    private var groupThoughts: Boolean = settings.settings.value.groupThoughts
    private var reloadJob: Job? = null
    private var olderJob: Job? = null

    /** Fetches of the open viewer's missing images, by message or tool call. */
    private val previewJobs = mutableMapOf<String, Job>()

    /** Where each image of the open viewer comes from, in the same order. */
    private var previewSources: List<PreviewSource> = emptyList()
    private var workPoll: Job? = null
    private var workVisible = false
    private var workJob: Job? = null
    private var workEventJob: Job? = null
    private val runningWork = RunningWorkLoader(sessions, chat)
    private val commandStops = mutableMapOf<String, Job>()
    private val agentStops = mutableMapOf<String, Job>()
    private var confirmStop: Boolean = settings.settings.value.confirmStopCommand

    /** Messages whose images were requested in this chat; a failure is retried on reopen. */
    private val imageRequests = HashSet<String>()

    /**
     * Images of prompts sent from here, by the server id of the prompt, shown in its place until
     * the server's copy has its own: it may come without them for a while ([withSentImages]).
     */
    private val sentImages = HashMap<String, List<ImageAttachment>>()

    /** Tool calls whose files were requested in this chat ([ToolMedia.fetchKey]). */
    private val toolMediaRequests = HashSet<String>()
    private var suggestJob: Job? = null
    private var changesJob: Job? = null
    private var queueJob: Job? = null
    private var mcpJob: Job? = null
    private var branchJob: Job? = null
    private var changedJob: Job? = null
    private val changingMcps = mutableSetOf<String>()

    /** Models of the most recently updated sessions, across every project. */
    private var recentFromSessions: List<ModelRef> = emptyList()

    /** The session's agent and model as the server last reported them. */
    private var sessionAgent: String? = null
    private var sessionModel: ModelRef? = null

    /**
     * Picked in the composer but not sent to the server yet ([SelectionApply.NextMessage]):
     * applied right before the next prompt. Null when it matches the session.
     */
    private var pendingAgent: String? = null
    private var pendingModel: ModelRef? = null

    /** [pendingModel] was picked by the user rather than taken from the picked agent's configuration. */
    private var modelPicked = false
    private var selectionApply: SelectionApply = settings.settings.value.selectionApply

    /** Completed compactions when `/compact` was requested; null when none is pending. */
    private var compactRequestedWith: Int? = null

    /** Keys of [BackgroundableWork] already sent to the background, so the hint hides at once. */
    private val movedToBackground = mutableSetOf<String>()

    /** What running shell tools have printed so far, by call id, and the jobs reading it. */
    private val liveOutput = HashMap<String, String>()
    private val liveJobs = HashMap<String, Job>()

    /** Shell of each followed tool, by call id: kept across reloads, which drop it from the metadata. */
    private val toolShells = HashMap<String, String>()
    private var liveOutputIntervalMs: Long = settings.settings.value.liveOutputIntervalMs

    private var headerLine: ChatHeaderLine = settings.settings.value.chatHeaderLine
    private var headerOpens: ChangesScope = settings.settings.value.headerOpens

    /** The header line shows the branch and the changed files, which are then kept up to date. */
    private val vcsInHeader: Boolean get() = headerLine != ChatHeaderLine.Folder

    /** Whether the project is in a repository; null until known. */
    private var inRepository: Boolean? = null
    private var branch: VcsBranch? = null
    private var changedFiles: Int = 0

    /** Shell readers wait on it before every read, so a hidden chat makes no output requests. */
    private val chatVisible = MutableStateFlow(true)

    /** First message of a staged revert; it and everything after are hidden. */
    private var revertMessageId: String? = null
    private var localIds = 0

    init {
        // Header from the session list, so the title shows before anything loads.
        sessions.cached(sessionId)?.let { s ->
            revertMessageId = s.revertMessageId
            updateState { it.copy(title = s.title, directory = s.directory) }
            sessionSelection(s.agent, s.model)
        }
        drafts.load(sessionId).takeIf { it.isNotEmpty() }?.let { text -> updateState { it.copy(draft = text) } }
        updateState { it.copy(previousSession = neighbor(-1), nextSession = neighbor(+1)) }
        viewModelScope.launch {
            // Every change of the composer text (typing, send, revert, picked mention) is kept.
            state.map { it.draft }.distinctUntilChanged().collect { drafts.save(sessionId, it) }
        }
        viewModelScope.launch {
            settings.settings.collect { s ->
                showReasoning = s.showReasoning
                liveOutputIntervalMs = s.liveOutputIntervalMs
                groupActivity = s.groupActivity
                groupThoughts = s.groupThoughts
                selectionApply = s.selectionApply
                confirmStop = s.confirmStopCommand
                val vcsTurnedOn = s.chatHeaderLine != ChatHeaderLine.Folder && !vcsInHeader
                headerLine = s.chatHeaderLine
                headerOpens = s.headerOpens
                if (vcsTurnedOn) refreshBranch()
                updateState { it.withSettings(s) }
                rebuild()
            }
        }
        viewModelScope.launch { events.connection.collect { c -> updateState { it.copy(connection = c) } } }
        viewModelScope.launch {
            // Content shared from another app and sent to this chat: into the composer, to edit and send.
            shares.pending.filter { it?.sessionId == sessionId }.collect {
                val shared = shares.take(sessionId) ?: return@collect
                updateState { s ->
                    s.copy(
                        draft = listOf(s.draft, shared.text).filter { it.isNotBlank() }.joinToString("\n\n"),
                        attachments = (s.attachments + shared.attachments).distinctBy { it.uri },
                    )
                }
            }
        }
        viewModelScope.launch { events.events.collect(::onServerEvent) }
        loadAll()
    }

    override fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.Visible -> {
                workVisible = event.visible
                restartRunningPoll()
            }
            is ChatEvent.RunningSheet -> {
                updateState { it.copy(workOpen = event.open) }
                restartRunningPoll()
            }
            ChatEvent.RefreshRunning -> refreshRunning()
            is ChatEvent.StopCommand -> if (confirmStop) {
                updateState { it.copy(confirmingStop = event.id) }
            } else {
                stopCommands(listOf(event.id))
            }
            ChatEvent.ConfirmStopCommand -> {
                val id = currentState.confirmingStop
                updateState { it.copy(confirmingStop = null) }
                if (id != null) stopCommands(listOf(id))
            }
            ChatEvent.DismissStopCommand -> updateState { it.copy(confirmingStop = null) }
            is ChatEvent.StopAgent -> if (confirmStop) {
                updateState { it.copy(confirmingStopAgent = event.sessionId) }
            } else {
                stopAgents(listOf(event.sessionId))
            }
            ChatEvent.ConfirmStopAgent -> {
                val id = currentState.confirmingStopAgent
                updateState { it.copy(confirmingStopAgent = null) }
                if (id != null) stopAgents(listOf(id))
            }
            ChatEvent.DismissStopAgent -> updateState { it.copy(confirmingStopAgent = null) }
            is ChatEvent.StopAll -> updateState { it.copy(confirmingStopAll = event.target) }
            ChatEvent.ConfirmStopAll -> {
                val groups = when (val target = currentState.confirmingStopAll) {
                    is StopAllTarget.Group -> listOf(target.group)
                    StopAllTarget.Everything -> RunningGroup.entries
                    null -> emptyList()
                }
                updateState { it.copy(confirmingStopAll = null) }
                val work = currentState.work
                // Only commands proven to belong to this session; unassigned ones keep their own Stop.
                if (RunningGroup.Commands in groups) stopCommands(work.commands.map { it.id })
                if (RunningGroup.Agents in groups) stopAgents(work.agents.map { it.session.id })
            }
            ChatEvent.DismissStopAll -> updateState { it.copy(confirmingStopAll = null) }
            is ChatEvent.ShowRunningList -> {
                updateState { s -> s.copy(runningPanel = if (s.runningChips) event.group?.let { RunningPanel(it) } else null) }
                restartRunningPoll()
            }
            is ChatEvent.ShowRunningItem -> {
                val entry = currentState.work.entries(event.group).firstOrNull { it.id == event.id } ?: return
                updateState { it.copy(runningPanel = RunningPanel(event.group, entry)) }
                restartRunningPoll()
            }
            ChatEvent.CloseRunningItem -> {
                updateState { it.copy(runningPanel = null) }
                restartRunningPoll()
            }
            ChatEvent.Retry -> loadAll()
            is ChatEvent.DraftChanged -> {
                updateState { it.copy(draft = event.text) }
                updateSuggestions(event.text)
            }
            is ChatEvent.DeliveryChanged -> updateState { it.copy(delivery = event.delivery) }
            ChatEvent.Send -> send()
            ChatEvent.Stop -> stop()
            ChatEvent.MoveToBackground -> moveToBackground()
            ChatEvent.Compact -> compact()
            is ChatEvent.OpenSession -> {
                // Otherwise the Running sheet is still open on return, and the first Back only closes it.
                updateState { it.copy(workOpen = false, runningPanel = null) }
                sendEffect(ChatEffect.OpenSession(event.sessionId))
            }
            is ChatEvent.OpenFile -> sendEffect(ChatEffect.OpenFile(currentState.directory, event.path))
            is ChatEvent.SwitchSession -> sendEffect(ChatEffect.SwitchSession(event.sessionId))
            ChatEvent.LoadOlder -> loadOlder()
            // Not part of the state: only read back when the chat opens again.
            is ChatEvent.ScrollMoved -> scroll.put(sessionId, event.anchor)
            is ChatEvent.Toggle -> updateState {
                it.copy(toggled = if (event.key in it.toggled) it.toggled - event.key else it.toggled + event.key)
            }
            is ChatEvent.SelectAgent -> selectAgent(event.agent)
            is ChatEvent.ModelSheet -> updateState { it.copy(modelSheetOpen = event.open) }
            is ChatEvent.SelectModel -> selectModel(event.model)
            is ChatEvent.SelectVariant -> selectVariant(event.variant)
            is ChatEvent.ReplyPermission -> reply(event.requestId, event.decision)
            is ChatEvent.CollapsePermission -> updateState { s ->
                s.copy(toggledPermissions = s.toggledPermissions.toggle(event.requestId, event.collapsed != s.permissionsCollapsedByDefault))
            }
            ChatEvent.Back -> sendEffect(ChatEffect.NavigateBack)
            is ChatEvent.AddAttachments -> updateState { s -> s.copy(attachments = (s.attachments + event.attachments).distinctBy { it.uri }) }
            is ChatEvent.RemoveAttachment -> updateState { s -> s.copy(attachments = s.attachments.filterNot { it.uri == event.uri }) }
            is ChatEvent.PreviewAttachment -> previewAttachment(event.uri)
            is ChatEvent.OpenImage -> openImage(event.messageId, event.index)
            is ChatEvent.OpenToolImage -> openToolImage(event.messageId, event.callId, event.index)
            is ChatEvent.LoadToolMedia -> loadToolMedia(event.messageId, event.callId)
            is ChatEvent.PreviewPage -> showPreviewPage(event.index)
            ChatEvent.ClosePreview -> closePreview()
            ChatEvent.AttachmentFailed -> sendEffect(ChatEffect.ShowMessage(UiText.Resource(Res.string.chat_attachment_failed)))
            is ChatEvent.PickSuggestion -> pickSuggestion(event.suggestion)
            is ChatEvent.FormAnswerChanged -> updateState { s ->
                s.copy(formAnswers = if (event.answer == null) s.formAnswers - event.key else s.formAnswers + (event.key to event.answer))
            }
            ChatEvent.SubmitForm -> submitForm()
            ChatEvent.DismissForm -> dismissForm()
            is ChatEvent.CollapseForm -> updateState { s ->
                s.copy(toggledForms = s.toggledForms.toggle(event.formId, event.collapsed != s.questionsCollapsedByDefault))
            }
            is ChatEvent.Revert -> revert(event.messageId)
            ChatEvent.RestoreRevert -> restoreRevert()
            is ChatEvent.Fork -> fork(event.messageId)
            ChatEvent.OpenChanges -> {
                loadChanges(currentState.changes?.scope ?: ChangesScope.Session)
                // Offers the folder's changes once it is known to be a repository.
                if (inRepository == null) refreshBranch(force = true)
            }
            ChatEvent.OpenHeaderChanges -> loadChanges(
                if (headerOpens == ChangesScope.Folder && inRepository == true) ChangesScope.Folder else ChangesScope.Session,
            )
            ChatEvent.OpenProviders -> if (currentState.directory.isNotEmpty()) sendEffect(ChatEffect.OpenProviders)
            ChatEvent.ProvidersConnected -> {
                viewModelScope.launch {
                    catalog.models(currentState.directory).onSuccess { models -> updateState { it.copy(models = models) } }
                }
            }
            ChatEvent.OpenMcps -> {
                updateState { it.copy(mcps = it.mcps ?: McpPanel(changing = changingMcps.toSet())) }
                refreshMcps()
            }
            ChatEvent.RefreshMcps -> refreshMcps()
            ChatEvent.CloseMcps -> {
                mcpJob?.cancel()
                updateState { it.copy(mcps = null) }
            }
            is ChatEvent.SetMcpConnected -> setMcpConnected(event.name, event.connected)
            ChatEvent.OpenTerminal -> currentState.directory.takeIf { it.isNotEmpty() }?.let { sendEffect(ChatEffect.OpenTerminal(it)) }
            ChatEvent.CloseChanges -> {
                changesJob?.cancel()
                updateState { it.copy(changes = null) }
            }
            is ChatEvent.LoadChanges -> loadChanges(event.scope)
            is ChatEvent.SteerQueued -> steerQueued(event.id)
            is ChatEvent.EditQueued -> editQueued(event.id)
            is ChatEvent.RemoveQueued -> removeQueued(event.id)
        }
    }

    private fun refreshRunning() {
        if (!workVisible || workJob?.isActive == true) return
        workJob = viewModelScope.launch {
            val root = sessions.get(sessionId).getOrElse {
                updateState { s -> s.copy(workLoading = false, workError = it.toUiText()) }
                return@launch
            }
            // Commands started by this transcript's tools are owned through the messages already in memory.
            val owners = shellOwners(sessionId, messages) + toolShells.values.associateWith { sessionId }
            // Subagent titles and start times are read for the sheet and for the chips' lists.
            val details = currentState.workOpen || currentState.runningPanel != null
            runningWork.load(listOf(root), details = details, shellOwners = owners).fold(
                onSuccess = { snapshot ->
                    val work = snapshot.sessions[sessionId] ?: RunningWork()
                    val offset = chat.serverClockOffsetMs()
                    // The item open in its sheet keeps its last output once it ended.
                    val ids = (work.commands + snapshot.unassigned).map { it.id }.toSet() + listOfNotNull(currentState.runningPanel?.item?.id)
                    val newShells = work.commands.map { it.id } != currentState.work.commands.map { it.id }
                    updateState {
                        it.copy(
                            work = work,
                            clockOffsetMs = offset,
                            unassignedCommands = snapshot.unassigned,
                            workLoading = false,
                            workError = snapshot.errors[sessionId]?.toUiText(),
                            workOutput = it.workOutput.filterKeys { id -> id in ids },
                            workOutputErrors = it.workOutputErrors.filterKeys { id -> id in ids },
                            runningPanel = it.runningPanel?.follow(work, clock() + offset),
                        )
                    }
                    // Running tools whose shell id the transcript lost can follow these shells.
                    if (newShells) rebuild()
                    // The sheet shows every output; a chip's sheet only its own shell's.
                    val tails = if (currentState.workOpen) {
                        work.commands + snapshot.unassigned
                    } else {
                        val open = currentState.runningPanel?.takeIf { it.endedAt == null }?.item?.takeIf { it.group == RunningGroup.Commands }
                        work.commands.filter { it.id == open?.id }
                    }
                    for (command in tails) {
                        chat.shellTail(command.directory, command.id).fold(
                            onSuccess = { output ->
                                updateState {
                                    it.copy(workOutput = it.workOutput + (command.id to output), workOutputErrors = it.workOutputErrors - command.id)
                                }
                            },
                            onFailure = { error ->
                                updateState {
                                    it.copy(workOutputErrors = it.workOutputErrors + (command.id to error.toUiText()))
                                }
                            },
                        )
                    }
                },
                onFailure = { error -> updateState { it.copy(workLoading = false, workError = error.toUiText()) } },
            )
        }
    }

    /**
     * Refreshes running work now, then while the chat is visible: often with the sheet open, so
     * its output stays fresh, rarely otherwise, as events keep the summary current.
     */
    private fun restartRunningPoll() {
        workPoll?.cancel()
        workJob?.cancel()
        workEventJob?.cancel()
        workPoll = if (workVisible) {
            viewModelScope.launch {
                while (true) {
                    // Offline or unreachable a refresh only fails; reconnecting refreshes anyway.
                    if (events.connection.value == ConnectionState.Connected) refreshRunning()
                    val outputShown = currentState.workOpen || currentState.runningPanel?.item != null
                    delay(if (outputShown) RUNNING_SHEET_POLL_MS else RUNNING_POLL_MS)
                }
            }
        } else {
            null
        }
    }

    /** Streaming tools send many events: one refresh per burst. */
    private fun requestRunningRefresh() {
        if (!workVisible || workEventJob?.isActive == true) return
        workEventJob = viewModelScope.launch {
            delay(RUNNING_EVENT_DEBOUNCE_MS)
            refreshRunning()
        }
    }

    /**
     * Stops the listed commands concurrently, skipping those already stopping. Stopped ones leave
     * the list at once, failed ones stay; then the list is refreshed.
     */
    private fun stopCommands(ids: List<String>) {
        val known = currentState.work.commands + currentState.unassignedCommands
        val targets = ids.distinct().mapNotNull { id -> known.firstOrNull { it.id == id } }.filter { commandStops[it.id]?.isActive != true }
        if (targets.isEmpty()) return
        val targetIds = targets.map { it.id }.toSet()
        updateState { it.copy(stoppingCommands = it.stoppingCommands + targetIds) }
        val job = viewModelScope.launch {
            try {
                val failures = stopConcurrently(targets, { it.id }) { chat.stopShell(it.directory, it.id) }
                val stopped = targetIds - failures.keys
                if (stopped.isNotEmpty()) {
                    workJob?.cancel()
                    updateState { s ->
                        s.copy(
                            work = s.work.copy(commands = s.work.commands.filterNot { it.id in stopped }),
                            unassignedCommands = s.unassignedCommands.filterNot { it.id in stopped },
                        )
                    }
                    refreshRunning()
                }
                reportStopFailures(failures.values, targets.size, Res.plurals.chat_running_stop_commands_failed)
            } finally {
                updateState { it.copy(stoppingCommands = it.stoppingCommands - targetIds) }
                targetIds.forEach { commandStops.remove(it) }
            }
        }
        targetIds.forEach { commandStops[it] = job }
    }

    /** Interrupts the listed subagents' child sessions concurrently; never this session itself. */
    private fun stopAgents(ids: List<String>) {
        val running = currentState.work.agents.map { it.session.id }.toSet()
        val targets = ids.distinct().filter { it != sessionId && it in running && agentStops[it]?.isActive != true }
        if (targets.isEmpty()) return
        val targetIds = targets.toSet()
        updateState { it.copy(stoppingAgents = it.stoppingAgents + targetIds) }
        val job = viewModelScope.launch {
            try {
                val failures = stopConcurrently(targets, { it }) { chat.interrupt(it) }
                val stopped = targetIds - failures.keys
                if (stopped.isNotEmpty()) {
                    workJob?.cancel()
                    updateState { s -> s.copy(work = s.work.copy(agents = s.work.agents.filterNot { it.session.id in stopped })) }
                    refreshRunning()
                }
                reportStopFailures(failures.values, targets.size, Res.plurals.chat_running_stop_agents_failed)
            } finally {
                updateState { it.copy(stoppingAgents = it.stoppingAgents - targetIds) }
                targetIds.forEach { agentStops.remove(it) }
            }
        }
        targetIds.forEach { agentStops[it] = job }
    }

    /** Runs [stop] for every item at once; the failures by item id. */
    private suspend fun <T> stopConcurrently(items: List<T>, id: (T) -> String, stop: suspend (T) -> Result<Unit>): Map<String, Throwable> =
        coroutineScope {
            items.map { item -> async { id(item) to stop(item).exceptionOrNull() } }
                .awaitAll()
                .mapNotNull { (key, error) -> error?.let { key to it } }
                .toMap()
        }

    /** A single stop shows its error; several are counted in one message. */
    private fun reportStopFailures(failures: Collection<Throwable>, attempted: Int, counted: PluralStringResource) {
        when {
            failures.isEmpty() -> Unit
            attempted == 1 -> sendEffect(ChatEffect.ShowMessage(failures.first().toUiText()))
            else -> sendEffect(ChatEffect.ShowMessage(UiText.Plural(counted, failures.size, listOf(failures.size))))
        }
    }

    private fun loadAll() {
        updateState { it.copy(loading = messages.isEmpty(), error = null) }
        viewModelScope.launch {
            if (messages.isEmpty()) showCached()
            val session = async { sessions.get(sessionId) }
            val transcript = async { chat.messages(sessionId) }
            val active = async { sessions.active() }
            val perms = async { chat.permissions(sessionId) }
            val pendingForms = async { chat.forms(sessionId) }
            val inbox = async { chat.inbox(sessionId) }

            val sessionResult = session.await()
            val transcriptResult = transcript.await()
            val failure = sessionResult.exceptionOrNull() ?: transcriptResult.exceptionOrNull()
            if (failure != null) {
                updateState { it.copy(loading = false, error = if (messages.isEmpty()) failure.toUiText() else null) }
                if (messages.isNotEmpty()) sendEffect(ChatEffect.ShowMessage(failure.toUiText()))
                return@launch
            }
            val s = sessionResult.getOrThrow()
            revertMessageId = s.revertMessageId
            pendingForms.await().onSuccess(::showForms)
            val server = transcriptResult.getOrThrow()
            keepSentImages(server)
            messages = olderThan(server) + TranscriptReducer.merge(server, localPending(server))
            val lastAssistant = messages.lastOrNull { it is Message.Assistant } as? Message.Assistant
            val activeIds = active.await().getOrNull()
            val permissionList = perms.await().getOrNull()
            val queuedList = inbox.await().getOrNull()?.queuedOnly()
            sessionSelection(s.agent ?: lastAssistant?.agent, s.model ?: lastAssistant?.model)
            updateState {
                it.copy(
                    title = s.title,
                    directory = s.directory,
                    running = activeIds?.contains(sessionId) ?: it.running,
                    // Stop only asks the server to interrupt. If its ExecutionEnded was lost with
                    // the connection, a fresh server view must not leave the button disabled.
                    stopping = if (activeIds != null) false else it.stopping,
                    permissions = permissionList ?: it.permissions,
                    queued = queuedList ?: it.queued,
                    recentModels = recentModels(),
                    loading = false,
                    synced = true,
                    error = null,
                )
            }
            rebuild()
            // `/mcps` can open the panel before the project directory is known.
            if (currentState.mcps?.servers == null && mcpJob?.isActive != true) refreshMcps()
            loadCatalog(s.directory)
            refreshBranch()
            prefetchNeighbors()
        }
    }

    /**
     * Loads one older page above what is shown, when the user scrolls near the top. Loading
     * the whole history up front rebuilt the list once per page and made opening chats stutter.
     */
    private fun loadOlder() {
        if (olderJob?.isActive == true || !currentState.hasOlder || !currentState.synced) return
        updateState { it.copy(loadingOlder = true, olderFailed = false) }
        olderJob = viewModelScope.launch {
            val page = chat.olderMessages(sessionId)
            val older = page.getOrNull()
            if (older != null) {
                val known = messages.mapTo(HashSet()) { it.id }
                val added = older.filter { it.id !in known }
                if (added.isNotEmpty()) {
                    messages = added + messages
                    rebuild()
                }
            }
            // A failed page is offered again with a retry row; null means the history is complete.
            updateState { it.copy(loadingOlder = false, olderFailed = page.isFailure, hasOlder = page.isFailure || older != null) }
        }
    }

    /**
     * Shown messages older than a reloaded transcript: history loaded beyond the local cache
     * (or dropped from it at its cap) stays on screen instead of vanishing on every reload.
     */
    private fun olderThan(server: List<Message>): List<Message> {
        val oldestId = server.firstOrNull()?.id ?: return emptyList()
        val index = messages.indexOfFirst { it.id == oldestId }
        return if (index > 0) messages.subList(0, index).toList() else emptyList()
    }

    /** Shows the composer's images, opened at the attachment [uri]. */
    private fun previewAttachment(uri: String) {
        val shown = currentState.attachments.filter { it.uri == uri || it.mime?.startsWith("image/") == true }
        val index = shown.indexOfFirst { it.uri == uri }.takeIf { it >= 0 } ?: return
        openPreview(shown.map { PreviewImage(it.name, it.uri) to PreviewSource.Local }, index)
    }

    /**
     * Shows the images of a sent prompt, opened at [index]. The local cache keeps prompts without
     * their image payload (too big to store), so a missing one is fetched with its message.
     */
    private fun openImage(messageId: String, index: Int) {
        val message = messages.firstOrNull { it.id == messageId } as? Message.User ?: return
        val prompt = withSentImages(message).images
        if (index !in prompt.indices) return
        val fetched = images.get(messageId)
        openPreview(
            prompt.mapIndexed { i, image ->
                PreviewImage(image.name, image.dataUri ?: fetched?.getOrNull(i)) to PreviewSource.Prompt(messageId, i)
            },
            index,
        )
    }

    /**
     * Shows the images the tools of one group returned, opened at the [index]th file of tool call
     * [callId]; payloads the cache dropped are fetched again.
     */
    private fun openToolImage(messageId: String, callId: String, index: Int) {
        val group = currentState.items.firstNotNullOfOrNull { item ->
            val media = when (item) {
                is ChatItem.Tools -> item.media
                is ChatItem.Activity -> item.media
                else -> return@firstNotNullOfOrNull null
            }
            media.takeIf { list -> list.any { it.messageId == messageId && it.callId == callId && it.index == index } }
        } ?: return
        val viewable = group.filter { it.kind == MediaKind.Image && (it.file.isDecodable() || it.file.needsFetch()) }
        val opened = viewable.indexOfFirst { it.messageId == messageId && it.callId == callId && it.index == index }
        if (opened < 0) return
        openPreview(
            viewable.map { media ->
                val known = media.file.uri.takeIf { media.file.isDecodable() } ?: images.get(media.fetchKey)?.getOrNull(media.index)
                PreviewImage(media.file.displayName, known) to PreviewSource.Tool(media.messageId, media.callId, media.index)
            },
            opened,
        )
    }

    private fun openPreview(shown: List<Pair<PreviewImage, PreviewSource>>, index: Int) {
        closePreview()
        previewSources = shown.map { it.second }
        updateState { it.copy(preview = ImagePreview(shown.map { image -> image.first }, index)) }
        showPreviewPage(index)
    }

    private fun closePreview() {
        previewJobs.values.forEach { it.cancel() }
        previewJobs.clear()
        previewSources = emptyList()
        updateState { it.copy(preview = null) }
    }

    /** Remembers the image on screen and fetches it, with the others of its message or tool call, when missing. */
    private fun showPreviewPage(index: Int) {
        val preview = currentState.preview ?: return
        val image = preview.images.getOrNull(index) ?: return
        if (preview.index != index) updateState { s -> s.copy(preview = s.preview?.copy(index = index)) }
        if (image.dataUri != null || image.failed) return
        val source = previewSources.getOrNull(index) ?: return
        val key = when (source) {
            is PreviewSource.Prompt -> source.messageId
            is PreviewSource.Tool -> toolMediaKey(source.messageId, source.callId)
            PreviewSource.Local -> return
        }
        if (previewJobs[key]?.isActive == true) return
        previewJobs[key] = viewModelScope.launch {
            val uris = when (source) {
                is PreviewSource.Prompt -> chat.messageImages(sessionId, source.messageId).getOrNull()
                is PreviewSource.Tool -> chat.toolFileData(sessionId, currentState.directory, source.messageId, source.callId).getOrNull()
                    ?.also { images.put(key, it) }
            }
            updateState { s ->
                val open = s.preview ?: return@updateState s
                s.copy(
                    preview = open.copy(
                        images = open.images.mapIndexed { i, shown ->
                            val from = previewSources.getOrNull(i)
                            val position = when {
                                from is PreviewSource.Prompt && key == from.messageId -> from.index
                                from is PreviewSource.Tool && key == toolMediaKey(from.messageId, from.callId) -> from.index
                                else -> null
                            }
                            if (position == null || shown.dataUri != null) {
                                shown
                            } else {
                                val uri = uris?.getOrNull(position)
                                shown.copy(dataUri = uri, failed = uri == null)
                            }
                        },
                    ),
                )
            }
        }
    }

    /**
     * Fetches the files of one tool call whose image cards have no content (the message cache
     * drops inline payloads; images by path are read from the server), once per chat.
     */
    private fun loadToolMedia(messageId: String, callId: String) {
        val key = toolMediaKey(messageId, callId)
        if (images.get(key) != null) {
            // Fetched in an earlier visit: what is still missing is not available.
            updateState { it.copy(toolMediaLoaded = it.toolMediaLoaded + key) }
            return
        }
        if (!toolMediaRequests.add(key)) return
        viewModelScope.launch {
            chat.toolFileData(sessionId, currentState.directory, messageId, callId).onSuccess { images.put(key, it) }
            updateState { it.copy(toolMediaLoaded = it.toolMediaLoaded + key) }
            rebuild()
        }
    }

    /** Fills in tool images fetched by [loadToolMedia], kept in [images] across chats. */
    private fun withToolMedia(visible: List<Message>): List<Message> = visible.map { message ->
        if (message !is Message.Assistant || message.parts.none { it is Part.Tool && it.files.any { f -> f.needsFetch() } }) {
            return@map message
        }
        message.copy(
            parts = message.parts.map { part ->
                val tool = part as? Part.Tool
                val fetched = tool?.takeIf { it.files.any { f -> f.needsFetch() } }?.let { images.get(toolMediaKey(message.id, it.callId)) }
                if (tool == null || fetched == null) {
                    part
                } else {
                    tool.copy(
                        files = tool.files.mapIndexed { i, file ->
                            val uri = fetched.getOrNull(i)
                            if (uri != null && file.needsFetch()) file.copy(uri = uri) else file
                        },
                    )
                }
            },
        )
    }

    /** Caches the newest messages of the sessions a swipe would open, so they show at once. */
    private fun prefetchNeighbors() {
        val ids = listOfNotNull(currentState.previousSession?.id, currentState.nextSession?.id)
        if (ids.isNotEmpty()) viewModelScope.launch { chat.prefetch(ids, NEIGHBOR_MESSAGES) }
    }

    /** Shows the locally stored transcript right away; the server sync then brings it up to date. */
    private suspend fun showCached() {
        val cached = chat.cachedMessages(sessionId)
        if (cached.isEmpty() || messages.isNotEmpty()) return
        messages = cached
        val lastAssistant = cached.lastOrNull { it is Message.Assistant } as? Message.Assistant
        sessionSelection(sessionAgent ?: lastAssistant?.agent, sessionModel ?: lastAssistant?.model)
        updateState { it.copy(loading = false) }
        rebuild()
    }

    private fun loadCatalog(directory: String) {
        if (currentState.models.isNotEmpty()) return
        viewModelScope.launch {
            val agents = async { catalog.agents(directory) }
            val models = async { catalog.models(directory) }
            val commands = async { catalog.commands(directory) }
            // The session list already has every session's model; ask the server only without it.
            val listed = sessions.browseOrder().mapNotNull(sessions::cached)
            val recent = async { if (listed.isNotEmpty()) listed else sessions.list(limit = RECENT_SESSIONS).getOrDefault(emptyList()) }
            val agentList = agents.await().getOrDefault(emptyList()).selectablePrimary()
            val modelList = models.await().getOrDefault(emptyList())
            val commandList = commands.await().getOrDefault(emptyList())
            recentFromSessions = recent.await().recentModels()
            updateState { it.copy(agents = agentList, models = modelList, commands = commandList, recentModels = recentModels()) }
            rebuild()
        }
    }

    private fun reloadMessages() {
        viewModelScope.launch {
            chat.messages(sessionId).onSuccess { server ->
                keepSentImages(server)
                messages = olderThan(server) + TranscriptReducer.merge(server, localPending(server))
                updateState { it.copy(recentModels = recentModels()) }
                rebuild()
            }
        }
    }

    private fun scheduleReload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            delay(RELOAD_DEBOUNCE_MS)
            reloadMessages()
        }
    }

    private fun onServerEvent(event: ServerEvent) {
        if (event == ServerEvent.Connected || event is ServerEvent.ExecutionStarted || event is ServerEvent.ExecutionEnded ||
            event is ServerEvent.ToolProgress || event is ServerEvent.ToolSucceeded
        ) {
            requestRunningRefresh()
        }
        if (event == ServerEvent.Connected) {
            loadAll()
            refreshMcps()
            return
        }
        if (event == ServerEvent.McpsChanged) {
            refreshMcps()
            return
        }
        if (event is ServerEvent.BranchChanged) {
            onBranchChanged(event)
            return
        }
        if (event !is ServerEvent.SessionScoped || event.sessionId != sessionId) return
        when (event) {
            is ServerEvent.ExecutionStarted -> {
                updateState { it.copy(running = true) }
                rebuild()
            }
            is ServerEvent.ExecutionEnded -> {
                compactRequestedWith = null
                updateState { it.copy(running = false, stopping = false) }
                // Inserts the idle marker that closes the turn, before the reload confirms it.
                applyEvent(event)
                scheduleReload()
                refreshChangedFiles()
            }
            is ServerEvent.SessionChanged -> if (event.deleted) {
                sendEffect(ChatEffect.ShowMessage(UiText.Resource(Res.string.chat_session_deleted)))
                sendEffect(ChatEffect.NavigateBack)
            } else {
                viewModelScope.launch {
                    sessions.get(sessionId).onSuccess { s ->
                        updateState { it.copy(title = s.title) }
                        sessionSelection(s.agent, s.model)
                    }
                }
            }
            is ServerEvent.PermissionChanged -> refreshPermissions()
            is ServerEvent.FormsChanged -> refreshForms()
            is ServerEvent.RevertChanged -> {
                revertMessageId = event.messageId
                rebuild()
                if (event.committed) scheduleReload()
            }
            is ServerEvent.TranscriptChanged -> {
                scheduleReload()
                // A `!command` may have changed files.
                if (event.type == SHELL_ENDED) refreshChangedFiles()
            }
            is ServerEvent.StepStarted -> {
                applyEvent(event)
                updateState { it.copy(running = true) }
                sessionSelection(event.agent, event.model)
            }
            is ServerEvent.StepEnded -> {
                applyEvent(event)
                scheduleReload()
                // A step may have edited files: the open sheet and the branch line follow along.
                currentState.changes?.let { loadChanges(it.scope) }
                refreshChangedFiles()
            }
            is ServerEvent.InboxEnqueued -> {
                val sent = messages.filter { it is Message.User && it.id.startsWith(LOCAL_PREFIX) && it.text == event.text }
                // The inbox entry takes the local prompt's place with its images and files, in
                // one update, so the thumbnails do not blink.
                val local = sent.firstOrNull() as? Message.User
                messages = TranscriptReducer.reduce(messages - sent.toSet(), event).map { message ->
                    if (local == null || message !is Message.User || message.id != event.inboxId || message.attachments > 0) return@map message
                    if (local.images.isNotEmpty()) sentImages[message.id] = local.images
                    message.copy(attachments = local.attachments, images = local.images, files = local.files)
                }
                rebuild()
                refreshQueue()
            }
            is ServerEvent.InboxDelivered, is ServerEvent.InboxCancelled, is ServerEvent.InboxDeliveryChanged -> {
                applyEvent(event)
                refreshQueue()
            }
            else -> applyEvent(event)
        }
    }

    private fun applyEvent(event: ServerEvent.SessionScoped) {
        val next = TranscriptReducer.reduce(messages, event)
        if (next !== messages) {
            messages = next
            rebuild()
        }
    }

    private fun refreshQueue() {
        queueJob?.cancel()
        queueJob = viewModelScope.launch {
            chat.inbox(sessionId).onSuccess { list ->
                updateState { it.copy(queued = list.queuedOnly()) }
                rebuild()
            }
        }
    }

    private fun steerQueued(id: String) {
        val prompt = currentState.queued.firstOrNull { it.id == id } ?: return
        updateState { s -> s.copy(queued = s.queued - prompt) }
        // Shown in the transcript as pending until delivered at the next step.
        if (messages.none { it.id == id }) messages = messages + Message.User(id, prompt.createdAt, prompt.text, pending = true)
        rebuild()
        viewModelScope.launch {
            chat.steerQueued(sessionId, id).onFailure { error ->
                sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                refreshQueue()
            }
        }
    }

    /** Cancels the queued prompt and puts its text back in the composer, before what is typed there. */
    private fun editQueued(id: String) {
        val prompt = currentState.queued.firstOrNull { it.id == id } ?: return
        removeQueued(id)
        updateState { s -> s.copy(draft = listOf(prompt.text, s.draft).filter { it.isNotBlank() }.joinToString("\n\n")) }
    }

    private fun removeQueued(id: String) {
        val prompt = currentState.queued.firstOrNull { it.id == id } ?: return
        updateState { s -> s.copy(queued = s.queued - prompt) }
        messages = messages.filterNot { it.id == id }
        rebuild()
        viewModelScope.launch {
            chat.cancelQueued(sessionId, id).onFailure { error ->
                sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                refreshQueue()
            }
        }
    }

    private fun List<QueuedPrompt>.queuedOnly(): List<QueuedPrompt> = filter { it.delivery == PromptDelivery.Queue }

    private fun refreshPermissions() {
        viewModelScope.launch {
            chat.permissions(sessionId).onSuccess { list -> updateState { it.copy(permissions = list) } }
        }
    }

    private fun refreshForms() {
        viewModelScope.launch { chat.forms(sessionId).onSuccess(::showForms) }
    }

    /** Shows [forms]; answers restart from the defaults when the first form changes. */
    private fun showForms(forms: List<Form>) = updateState { s ->
        val first = forms.firstOrNull()
        val answers = if (first?.id == s.forms.firstOrNull()?.id) s.formAnswers else first?.initialAnswers().orEmpty()
        s.copy(forms = forms, formAnswers = answers)
    }

    private fun submitForm() {
        val state = currentState
        val form = state.forms.firstOrNull() ?: return
        if (state.submittingForm || !form.isComplete(state.formAnswers)) return
        updateState { it.copy(submittingForm = true) }
        viewModelScope.launch {
            chat.replyForm(sessionId, form.id, form.reply(state.formAnswers)).fold(
                onSuccess = { showForms(currentState.forms.filterNot { it.id == form.id }) },
                onFailure = { error -> sendEffect(ChatEffect.ShowMessage(error.toUiText())) },
            )
            updateState { it.copy(submittingForm = false) }
        }
    }

    private fun dismissForm() {
        val form = currentState.forms.firstOrNull() ?: return
        showForms(currentState.forms.drop(1))
        viewModelScope.launch {
            chat.cancelForm(sessionId, form.id).onFailure { error ->
                sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                refreshForms()
            }
        }
    }

    /**
     * Messages before the staged revert point; all of them when nothing is reverted. Server ids
     * sort by creation, so the cut also works when the reverted prompt is older than the
     * loaded history (then everything loaded is hidden).
     */
    private fun visibleMessages(): List<Message> {
        val id = revertMessageId ?: return messages
        val cut = messages.indexOfFirst { !it.id.startsWith(LOCAL_PREFIX) && it.id >= id }
        return if (cut < 0) messages else messages.subList(0, cut)
    }

    /**
     * A new prompt or shell command commits the staged revert, which deletes its messages on the
     * server: they leave the transcript now instead of showing again while the commit runs.
     * Returns them for [restoreReverted].
     */
    private fun takeReverted(): List<Message> {
        if (revertMessageId == null) return emptyList()
        val visible = visibleMessages()
        val reverted = messages.subList(visible.size, messages.size).toList()
        messages = visible.toList()
        revertMessageId = null
        return reverted
    }

    /** The commit failed, so the revert is still only staged: its messages are hidden again. */
    private fun restoreReverted(messageId: String, reverted: List<Message>) {
        val known = messages.mapTo(HashSet()) { it.id }
        messages = messages + reverted.filter { it.id !in known }
        revertMessageId = messageId
    }

    /**
     * Fills in prompt images the message cache dropped, from [images]; the missing ones are
     * fetched in the background (once per message) and the transcript rebuilt when they arrive.
     */
    private fun withImages(visible: List<Message>): List<Message> = visible.map { message ->
        if (message !is Message.User || message.images.none { it.dataUri == null }) return@map message
        val fetched = images.get(message.id)
        if (fetched == null) {
            fetchImages(message.id)
            message
        } else {
            message.copy(images = message.images.mapIndexed { i, image -> image.copy(dataUri = image.dataUri ?: fetched.getOrNull(i)) })
        }
    }

    private fun fetchImages(messageId: String) {
        if (!imageRequests.add(messageId)) return
        viewModelScope.launch {
            chat.messageImages(sessionId, messageId).onSuccess { uris ->
                images.put(messageId, uris)
                rebuild()
            }
        }
    }

    /** Running tools show what their shell has printed so far; finished ones keep their final output. */
    private fun withLiveOutput(visible: List<Message>): List<Message> {
        if (liveOutput.isEmpty()) return visible
        return visible.map { message ->
            if (message !is Message.Assistant || message.parts.none { it is Part.Tool && it.callId in liveOutput }) return@map message
            message.copy(
                parts = message.parts.map { part ->
                    val live = (part as? Part.Tool)?.takeIf { it.status == ToolStatus.Running }?.let { liveOutput[it.callId] }
                    if (live != null) (part as Part.Tool).copy(output = live) else part
                },
            )
        }
    }

    fun setChatVisible(visible: Boolean) {
        chatVisible.value = visible
    }

    /**
     * The server streams a `bash` tool's output only to its shell, not to the event stream:
     * while the tool runs, its shell output is read every [liveOutputIntervalMs] (setting).
     * The shell is found as described in [LiveShells].
     */
    private fun followShells(visible: List<Message>) {
        val running = if (currentState.live?.shell == true) runningShells(visible) else emptyMap()
        // Ended, or now known to print to another shell.
        liveJobs.keys.filter { running[it] != toolShells[it] }.forEach { callId ->
            liveJobs.remove(callId)?.cancel()
            liveOutput.remove(callId)
        }
        toolShells.clear()
        toolShells.putAll(running)
        running.forEach { (callId, shellId) ->
            if (callId in liveJobs) return@forEach
            // Registered before it starts: the first read can rebuild, and with it call this again.
            val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
                var cursor = 0L
                while (true) {
                    // Hidden, offline or with the server unreachable, a read cannot show anything.
                    combine(chatVisible, events.connection) { shown, link -> shown && link == ConnectionState.Connected }.first { it }
                    val directory = currentState.directory
                    chat.shellOutput(directory, shellId, cursor).onSuccess { page ->
                        if (page.text.isNotEmpty()) {
                            liveOutput[callId] = liveOutput[callId].orEmpty() + page.text
                            cursor = page.cursor
                            rebuild()
                        }
                    }
                    delay(liveOutputIntervalMs)
                }
            }
            liveJobs[callId] = job
            job.start()
        }
    }

    /** Shell ids of the running `bash` tools, by call id; remembered, as a reload drops them from the metadata. */
    private fun runningShells(visible: List<Message>): Map<String, String> {
        val running = visible.asSequence()
            .filterIsInstance<Message.Assistant>()
            .flatMap { it.parts }
            .filterIsInstance<Part.Tool>()
            .filter { it.status == ToolStatus.Running }
            .toList()
        return LiveShells.match(sessionId, running, currentState.work.commands, toolShells)
    }

    private fun rebuild() {
        val state = currentState
        val visible = visibleMessages()
        val reverted = messages.subList(visible.size, messages.size).count { it is Message.User }
        // Queued prompts show above the composer instead of as pending bubbles.
        val queuedIds = state.queued.mapTo(HashSet()) { it.id }
        val shown = if (queuedIds.isEmpty()) visible else visible.filterNot { it is Message.User && it.id in queuedIds }
        followShells(shown)
        val items = ChatItemsBuilder.build(
            withLiveOutput(withToolMedia(withImages(withSentImages(shown)))),
            state.running,
            showReasoning,
            state.models::labelFor,
            groupActivity,
            groupThoughts,
        )
        val backgroundable = BackgroundableWork.find(visible)?.takeIf { it.key !in movedToBackground }
        val compactions = visible.filterIsInstance<Message.Notice>()
            .filter { it.kind == NoticeKind.Compacting || it.kind == NoticeKind.Compaction }
        val requestedWith = compactRequestedWith
        if (requestedWith != null && compactions.count { it.kind == NoticeKind.Compaction } > requestedWith) {
            compactRequestedWith = null
        }
        val compacting = compactRequestedWith != null || compactions.lastOrNull()?.kind == NoticeKind.Compacting
        updateState {
            it.copy(
                items = items,
                revertedCount = reverted,
                backgroundable = backgroundable,
                context = SessionInsights.context(visible, state.models),
                compacting = compacting,
                vcs = vcsLine(visible),
            )
        }
        // The revert point is older than the loaded history: bring it in so the chat is not blank.
        if (revertMessageId != null && visible.isEmpty() && messages.isNotEmpty()) loadOlder()
    }

    /**
     * Prompts sent from here already have their images on the device, so the thumbnails do not
     * blink when a prompt is confirmed. Local prompts are matched to their copies in [server] by
     * text like [localPending] (inbox ones were matched by id when enqueued) and shown with
     * their images until the server's copy has its own ([withSentImages]). Then the images go to
     * [images], as the message cache drops their payload, instead of being fetched again.
     */
    private fun keepSentImages(server: List<Message>) {
        val recent = server.takeLast(LOCAL_MATCH_WINDOW).filterIsInstance<Message.User>()
        messages.forEach { sent ->
            if (sent !is Message.User || sent.images.isEmpty() || !sent.id.startsWith(LOCAL_PREFIX)) return@forEach
            recent.lastOrNull { it.text.trim() == sent.text }?.let { confirmed -> sentImages.getOrPut(confirmed.id) { sent.images } }
        }
        if (sentImages.isEmpty()) return
        val users = server.filterIsInstance<Message.User>().filter { it.id in sentImages }.associateBy { it.id }
        sentImages.entries.removeAll { (id, sent) ->
            val confirmed = users[id] ?: return@removeAll false
            // The same images in the same order; mentioned image files would add more on the server.
            if (confirmed.images.size == sent.size && confirmed.images.any { it.dataUri == null }) images.put(id, sent.map { it.dataUri })
            confirmed.images.size >= sent.size
        }
    }

    /** [withSentImages] of each message. */
    private fun withSentImages(visible: List<Message>): List<Message> =
        if (sentImages.isEmpty()) visible else visible.map { if (it is Message.User) withSentImages(it) else it }

    /** [message] with the images it was sent with from here, while the server's copy lacks them or their payload. */
    private fun withSentImages(message: Message.User): Message.User {
        val sent = sentImages[message.id] ?: return message
        return when {
            message.images.size < sent.size -> message.copy(images = sent)
            message.images.size == sent.size && message.images.any { it.dataUri == null } ->
                message.copy(images = message.images.mapIndexed { i, image -> image.copy(dataUri = image.dataUri ?: sent[i].dataUri) })
            else -> message
        }
    }

    /** Local optimistic prompts and live streaming state not yet reflected by [server]. */
    private fun localPending(server: List<Message>): List<Message> {
        val serverTexts = server.takeLast(LOCAL_MATCH_WINDOW).filterIsInstance<Message.User>().mapTo(HashSet()) { it.text.trim() }
        return messages.filter { message ->
            when {
                message is Message.User && message.id.startsWith(LOCAL_PREFIX) -> message.text !in serverTexts
                else -> true
            }
        }
    }

    /**
     * The session's model, then those this device sent prompts with, then the models of recently
     * updated sessions, then this transcript's. A model picked but not sent yet does not move up.
     */
    private fun recentModels(): List<ModelRef> {
        val fromTranscript = messages.asReversed().filterIsInstance<Message.Assistant>().map { it.model }
        return (listOfNotNull(sessionModel) + recents.recent() + recentFromSessions + fromTranscript)
            .distinctBy { it.key }
            .take(RECENT_MODELS)
    }

    private fun send() {
        val state = currentState
        if (!state.canSend) return
        val text = state.draft.trim()
        Completions.shell(text)?.let { return runShell(it, text) }
        val slash = Completions.slash(text)
        Completions.Builtin.of(slash?.name)?.let { return runBuiltin(it) }
        val command = slash?.name?.takeIf { name -> state.commands.any { it.name == name } }
        val arguments = slash?.arguments.orEmpty()
        val attachments = state.attachments
        val delivery = state.delivery
        val revert = revertMessageId
        val local = Message.User(
            "$LOCAL_PREFIX${localIds++}",
            clock(),
            text,
            pending = true,
            attachments = attachments.size,
            // Shown at once, while they upload.
            images = attachments.sentImages(),
            files = attachments.filter { it.mention != null }.map { it.uri.removePrefix("file://") },
        )
        val reverted = takeReverted()
        messages = messages + local
        updateState { it.copy(draft = "", attachments = emptyList(), suggestions = emptyList(), sending = true) }
        rebuild()
        sendEffect(ChatEffect.PromptSent)
        viewModelScope.launch {
            var committed = revert == null
            val settle: (suspend () -> Result<Unit>)? = revert?.let { { chat.commitRevert(sessionId).onSuccess { committed = true } } }
            val result = prepareSend(apply = delivery == PromptDelivery.Steer, settle).fold(
                onSuccess = {
                    if (command != null) chat.command(sessionId, command, arguments, delivery, attachments)
                    else chat.prompt(sessionId, text, delivery, attachments)
                },
                onFailure = { Result.failure(it) },
            )
            result.fold(
                onSuccess = {
                    sent()
                    updateState { it.copy(sending = false, running = true) }
                    rebuild()
                    scheduleReload()
                },
                onFailure = { error ->
                    messages = messages.filterNot { it.id == local.id }
                    if (revert != null && !committed) restoreReverted(revert, reverted)
                    updateState {
                        it.copy(
                            sending = false,
                            draft = if (it.draft.isEmpty()) text else it.draft,
                            attachments = if (it.attachments.isEmpty()) attachments else it.attachments,
                        )
                    }
                    rebuild()
                    sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                },
            )
        }
    }

    /**
     * `!command`: runs it in the session's shell like the TUI. Its output joins the transcript
     * through the event stream; attachments stay in the composer for the next prompt.
     */
    private fun runShell(command: String, draft: String) {
        val revert = revertMessageId
        val reverted = takeReverted()
        updateState { it.copy(draft = "", suggestions = emptyList(), sending = true) }
        rebuild()
        sendEffect(ChatEffect.PromptSent)
        viewModelScope.launch {
            var committed = revert == null
            // Like OpenCode Desktop, a shell command runs with the session's selection.
            val ready = prepareSend(apply = false, revert?.let { { chat.commitRevert(sessionId).onSuccess { committed = true } } })
            // The server answers when the command exits; the composer is free again at once.
            updateState { it.copy(sending = false) }
            ready.fold(onSuccess = { chat.shell(sessionId, command) }, onFailure = { Result.failure(it) }).fold(
                onSuccess = { scheduleReload() },
                onFailure = { error ->
                    if (revert != null && !committed) restoreReverted(revert, reverted)
                    rebuild()
                    updateState { if (it.draft.isEmpty()) it.copy(draft = draft) else it }
                    sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                },
            )
        }
    }

    private fun runBuiltin(builtin: Completions.Builtin) {
        if (currentState.creatingSession) return
        val draft = currentState.draft
        updateState { it.copy(draft = "", suggestions = emptyList()) }
        when (builtin) {
            Completions.Builtin.New -> newSession(draft)
            Completions.Builtin.Undo -> undo()
            Completions.Builtin.Redo -> restoreRevert()
            Completions.Builtin.Compact -> compact()
            Completions.Builtin.Fork -> fork(null)
            Completions.Builtin.Mcps -> onEvent(ChatEvent.OpenMcps)
            Completions.Builtin.Connect -> onEvent(ChatEvent.OpenProviders)
        }
    }

    /** A fresh session in this folder; the server resolves its agent and model, without history. */
    private fun newSession(draft: String) {
        val directory = currentState.directory
        updateState { it.copy(creatingSession = true) }
        viewModelScope.launch {
            sessions.create(directory, agent = null, model = null).fold(
                onSuccess = { session ->
                    updateState { it.copy(creatingSession = false) }
                    sendEffect(ChatEffect.OpenSession(session.id))
                },
                onFailure = { error ->
                    updateState { it.copy(creatingSession = false, draft = it.draft.ifEmpty { draft }) }
                    sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                },
            )
        }
    }

    /**
     * `/undo`: reverts the last prompt, like the OpenCode TUI. A long turn can push the prompt
     * before it out of the loaded history; then it is looked up on the server.
     */
    private fun undo() {
        if (currentState.reverting) return
        val last = visibleMessages().lastOrNull { it is Message.User && !it.pending && !it.id.startsWith(LOCAL_PREFIX) } as? Message.User
        if (last != null) return revert(last)
        updateState { it.copy(reverting = true) }
        viewModelScope.launch {
            val previous = chat.previousPrompt(sessionId, revertMessageId).getOrElse { error ->
                updateState { it.copy(reverting = false) }
                sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                return@launch
            }
            updateState { it.copy(reverting = false) }
            if (previous == null) sendEffect(ChatEffect.ShowMessage(UiText.Resource(Res.string.chat_nothing_to_undo))) else revert(previous)
        }
    }

    private fun revert(messageId: String) {
        (messages.firstOrNull { it.id == messageId } as? Message.User)?.let(::revert)
    }

    private fun revert(message: Message.User) {
        val messageId = message.id
        if (currentState.reverting) return
        updateState { it.copy(reverting = true) }
        viewModelScope.launch {
            // The server refuses to revert a busy session: stop it first.
            if (currentState.running) {
                chat.interrupt(sessionId)
                chat.awaitIdle(sessionId)
            }
            chat.revert(sessionId, messageId).fold(
                onSuccess = {
                    revertMessageId = messageId
                    val restored = restoredImages(message)
                    updateState { s ->
                        s.copy(
                            reverting = false,
                            draft = message.text,
                            attachments = (s.attachments + restored).distinctBy { it.uri },
                        )
                    }
                    rebuild()
                },
                onFailure = { error ->
                    updateState { it.copy(reverting = false) }
                    sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                },
            )
        }
    }

    /** The images of an undone prompt, back in the composer like its text; those not available are skipped. */
    private suspend fun restoredImages(undone: Message.User): List<PromptAttachment> {
        val message = withSentImages(undone)
        if (message.images.isEmpty()) return emptyList()
        val known = message.images.map { it.dataUri }
        val uris = if (known.all { it != null }) known
        else images.get(message.id) ?: chat.messageImages(sessionId, message.id).getOrNull().orEmpty()
        return message.images.mapIndexedNotNull { i, image ->
            val uri = known[i] ?: uris.getOrNull(i) ?: return@mapIndexedNotNull null
            PromptAttachment(uri = uri, name = image.name ?: "image-${i + 1}", mime = image.mime)
        }
    }

    /**
     * Like the OpenCode TUI: the fork holds the history before [messageId], whose text waits in
     * its composer; `/fork` (null) copies the whole session.
     */
    private fun fork(messageId: String?) {
        if (currentState.forking || messageId?.startsWith(LOCAL_PREFIX) == true) return
        val message = messageId?.let { id -> messages.firstOrNull { it.id == id } as? Message.User ?: return }
        updateState { it.copy(forking = true) }
        viewModelScope.launch {
            sessions.fork(sessionId, messageId).fold(
                onSuccess = { fork ->
                    message?.let { drafts.save(fork.id, it.text) }
                    updateState { it.copy(forking = false) }
                    sendEffect(ChatEffect.OpenSession(fork.id))
                },
                onFailure = { error ->
                    updateState { it.copy(forking = false) }
                    sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                },
            )
        }
    }

    private fun restoreRevert() {
        val previous = revertMessageId
        if (previous == null) {
            sendEffect(ChatEffect.ShowMessage(UiText.Resource(Res.string.chat_nothing_to_redo)))
            return
        }
        revertMessageId = null
        rebuild()
        viewModelScope.launch {
            chat.clearRevert(sessionId).onFailure { error ->
                revertMessageId = previous
                rebuild()
                sendEffect(ChatEffect.ShowMessage(error.toUiText()))
            }
        }
    }

    /** Offers `/` commands at the start of the draft and `@` files for the word being typed. */
    private fun updateSuggestions(draft: String) {
        suggestJob?.cancel()
        if (Completions.shell(draft) != null) {
            if (currentState.suggestions.isNotEmpty()) updateState { it.copy(suggestions = emptyList()) }
            return
        }
        Completions.slashSuggestions(draft, currentState.commands)?.let { commands ->
            updateState { it.copy(suggestions = commands) }
            return
        }
        val query = Completions.mentionQuery(draft)
        val directory = currentState.directory
        if (query == null || directory.isEmpty()) {
            if (currentState.suggestions.isNotEmpty()) updateState { it.copy(suggestions = emptyList()) }
            return
        }
        suggestJob = viewModelScope.launch {
            delay(SUGGEST_DEBOUNCE_MS)
            catalog.findFiles(directory, query).onSuccess { paths ->
                updateState { it.copy(suggestions = paths.map(Suggestion::File)) }
            }
        }
    }

    private fun pickSuggestion(suggestion: Suggestion) {
        suggestJob?.cancel()
        val state = currentState
        when (suggestion) {
            // Built-ins run on tap, like the TUI; server commands may take arguments.
            is Suggestion.Slash -> Completions.Builtin.of(suggestion.name)?.let(::runBuiltin)
                ?: updateState { it.copy(draft = "/${suggestion.name} ", suggestions = emptyList()) }
            is Suggestion.File -> {
                val (draft, attachment) = Completions.pickFile(state.draft, state.directory, suggestion.path) ?: return
                updateState { s ->
                    s.copy(
                        draft = draft,
                        suggestions = emptyList(),
                        attachments = s.attachments.filterNot { it.uri == attachment.uri } + attachment,
                    )
                }
            }
        }
    }

    /** The session [step] places away from this one in the session list, for swiping. */
    private fun neighbor(step: Int): SessionPeek? {
        val order = sessions.browseOrder()
        val index = order.indexOf(sessionId).takeIf { it >= 0 } ?: return null
        val id = order.getOrNull(index + step) ?: return null
        return SessionPeek(id, sessions.cached(id)?.title.orEmpty())
    }

    /**
     * Reads the branch of the project, then its changed files for the header. Without them in
     * the header nothing is asked unless [force]d by the changes sheet, which needs to know
     * whether the folder is a repository.
     */
    private fun refreshBranch(force: Boolean = false) {
        val directory = currentState.directory
        if ((!vcsInHeader && !force) || directory.isEmpty()) return
        branchJob?.cancel()
        branchJob = viewModelScope.launch {
            vcs.branch(directory).onSuccess { found ->
                inRepository = found != null
                branch = found
                updateState { it.copy(folderChanges = found != null) }
                if (found != null && vcsInHeader) loadChangedFiles(directory) else showVcs()
            }
        }
    }

    /** Files change with every step: re-read once the step settles. Not needed outside a repository. */
    private fun refreshChangedFiles() {
        val directory = currentState.directory
        if (!vcsInHeader || inRepository != true || directory.isEmpty()) return
        changedJob?.cancel()
        changedJob = viewModelScope.launch {
            delay(RELOAD_DEBOUNCE_MS)
            loadChangedFiles(directory)
        }
    }

    private suspend fun loadChangedFiles(directory: String) {
        vcs.changedFiles(directory).onSuccess { files ->
            changedFiles = files.size
            showVcs()
        }
    }

    private fun onBranchChanged(event: ServerEvent.BranchChanged) {
        if (event.directory != currentState.directory) return
        if (inRepository != true) return refreshBranch()
        branch = branch?.copy(current = event.branch) ?: VcsBranch(event.branch, null)
        showVcs()
        // Another branch has another working copy.
        refreshChangedFiles()
        currentState.changes?.takeIf { it.scope == ChangesScope.Folder }?.let { loadChanges(it.scope) }
    }

    private fun showVcs() = updateState { it.copy(vcs = vcsLine(visibleMessages())) }

    private fun vcsLine(visible: List<Message>): VcsLine? {
        if (!vcsInHeader) return null
        val folder = headerFolder(headerLine, currentState.directory)
        return when (inRepository) {
            true -> VcsLine(folder, branch?.current, changedFiles)
            false -> agentEditedFiles(visible).takeIf { it > 0 }?.let { VcsLine(folder, null, it, agentEdits = true) }
            null -> null
        }
    }

    /** Distinct files the agent's edit, write and patch tools changed. */
    private fun agentEditedFiles(visible: List<Message>): Int = visible.asSequence()
        .filterIsInstance<Message.Assistant>()
        .flatMap { it.parts }
        .filterIsInstance<Part.Tool>()
        .filter { it.status == ToolStatus.Completed }
        .flatMap { it.diffs }
        .mapTo(HashSet()) { it.path }
        .size

    private fun refreshMcps() {
        if (currentState.mcps == null || currentState.directory.isEmpty()) return
        mcpJob?.cancel()
        updateState { it.copy(mcps = it.mcps?.copy(loading = true, error = null)) }
        val directory = currentState.directory
        mcpJob = viewModelScope.launch {
            mcps.list(directory).fold(
                onSuccess = { servers -> updateState { it.copy(mcps = it.mcps?.copy(servers = servers, loading = false)) } },
                onFailure = { error ->
                    updateState { it.copy(mcps = it.mcps?.copy(loading = false, error = error.toUiText())) }
                },
            )
        }
    }

    private fun setMcpConnected(name: String, connected: Boolean) {
        if (currentState.mcps?.servers?.none { it.name == name } != false || !changingMcps.add(name)) return
        val directory = currentState.directory
        updateState { it.copy(mcps = it.mcps?.copy(changing = changingMcps.toSet())) }
        viewModelScope.launch {
            try {
                mcps.setConnected(directory, name, connected).onFailure { error ->
                    sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                }
            } finally {
                changingMcps.remove(name)
                updateState { it.copy(mcps = it.mcps?.copy(changing = changingMcps.toSet())) }
            }
            // A successful HTTP response need not mean connected (e.g. OAuth is required).
            refreshMcps()
        }
    }

    private fun compact() {
        if (currentState.compacting) return
        compactRequestedWith = messages.count { it is Message.Notice && it.kind == NoticeKind.Compaction }
        updateState { it.copy(compacting = true) }
        viewModelScope.launch {
            chat.compact(sessionId).fold(
                onSuccess = { scheduleReload() },
                onFailure = { error ->
                    compactRequestedWith = null
                    updateState { it.copy(compacting = false) }
                    sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                },
            )
        }
    }

    private fun moveToBackground() {
        val work = currentState.backgroundable ?: return
        movedToBackground += work.key
        updateState { it.copy(backgroundable = null) }
        viewModelScope.launch {
            chat.background(sessionId).onFailure { error ->
                movedToBackground -= work.key
                rebuild()
                sendEffect(ChatEffect.ShowMessage(error.toUiText()))
            }
        }
    }

    private fun stop() {
        if (currentState.stopping) return
        updateState { it.copy(stopping = true) }
        viewModelScope.launch {
            chat.interrupt(sessionId).onFailure { error ->
                updateState { it.copy(stopping = false) }
                sendEffect(ChatEffect.ShowMessage(error.toUiText()))
            }
        }
    }

    private fun selectAgent(agent: String) {
        if (agent == currentState.agent) return
        if (selectionApply == SelectionApply.NextMessage) {
            pendingAgent = agent.takeIf { it != sessionAgent }
            // Like OpenCode Desktop, an agent with a configured model brings it along unless one was picked.
            if (!modelPicked) {
                pendingModel = configuredModel(agent)?.takeIf { pendingAgent != null && it != sessionModel }
            }
            showSelection()
            return
        }
        // A choice left pending before the setting changed gives way to this one.
        pendingAgent = null
        val previous = sessionAgent
        sessionSelection(agent, null)
        viewModelScope.launch {
            chat.switchAgent(sessionId, agent).onFailure { error ->
                if (sessionAgent == agent) sessionAgent = previous
                showSelection()
                sendEffect(ChatEffect.ShowMessage(error.toUiText()))
            }
        }
    }

    /** The model [agent] is configured with on the server, when it is available. */
    private fun configuredModel(agent: String): ModelRef? {
        val model = currentState.agents.firstOrNull { it.id == agent }?.model ?: return null
        return model.takeIf { currentState.models.any { it.ref.key == model.key } }
    }

    private fun selectModel(model: ModelRef) {
        val previous = currentState.model
        updateState { it.copy(modelSheetOpen = false) }
        // Picking the same model again keeps its variant; another model starts from its default.
        if (model.key != previous?.key) switchModel(model.copy(variant = null))
    }

    /** Changes the reasoning level of the current model; the sheet stays open. */
    private fun selectVariant(variant: String?) {
        val current = currentState.model ?: return
        if (variant != current.variant) switchModel(current.copy(variant = variant))
    }

    private fun switchModel(model: ModelRef) {
        if (selectionApply == SelectionApply.NextMessage) {
            pendingModel = model.takeIf { it != sessionModel }
            modelPicked = pendingModel != null
            showSelection()
            return
        }
        pendingModel = null
        val previous = sessionModel
        sessionSelection(null, model)
        viewModelScope.launch {
            chat.switchModel(sessionId, model).onFailure { error ->
                if (sessionModel == model) sessionModel = previous
                showSelection()
                sendEffect(ChatEffect.ShowMessage(error.toUiText()))
            }
        }
    }

    /**
     * The agent and model reported by the server; null keeps what was known. A selection picked
     * but not sent yet stays shown over them.
     */
    private fun sessionSelection(agent: String?, model: ModelRef?) {
        if (agent != null) sessionAgent = agent
        if (model != null) sessionModel = model
        if (pendingAgent == sessionAgent) pendingAgent = null
        if (pendingModel == sessionModel) pendingModel = null
        showSelection()
    }

    /** Shows the agent and model the next prompt will use. */
    private fun showSelection() {
        if (pendingModel == null) modelPicked = false
        updateState { it.copy(agent = pendingAgent ?: sessionAgent, model = pendingModel ?: sessionModel) }
    }

    /**
     * Runs before a prompt, command or shell command. When [apply] is set, the selection picked
     * in the composer is applied in OpenCode Desktop's order: the agent, then [settle] when
     * given (committing a staged revert, which would otherwise delete the model switch), then
     * the model. What was applied stops being pending, so after a failure only the rest is
     * applied on the next try. Like Desktop, only a steered prompt or command applies it: a
     * queued one must not reconfigure the work it waits behind, and a shell command runs with
     * the session's selection, so the choice stays pending for the next steered prompt.
     */
    private suspend fun prepareSend(apply: Boolean, settle: (suspend () -> Result<Unit>)?): Result<Unit> {
        if (!apply) return settle?.invoke() ?: Result.success(Unit)
        pendingAgent?.let { agent ->
            chat.switchAgent(sessionId, agent).onFailure { return Result.failure(it) }
            sessionSelection(agent, null)
        }
        settle?.invoke()?.onFailure { return Result.failure(it) }
        pendingModel?.let { model ->
            chat.switchModel(sessionId, model).onFailure { return Result.failure(it) }
            sessionSelection(null, model)
        }
        return Result.success(Unit)
    }

    /** A prompt went out: its model becomes the most recently used one. */
    private fun sent() {
        sessionModel?.let(recents::used)
        updateState { it.copy(recentModels = recentModels()) }
    }

    /** Loads [scope] into the changes sheet; files of the same scope stay shown while it reloads. */
    private fun loadChanges(scope: ChangesScope) {
        changesJob?.cancel()
        updateState { s ->
            val kept = s.changes?.takeIf { it.scope == scope }?.files
            s.copy(changes = ChangesPanel(scope, files = kept, loading = true))
        }
        val directory = currentState.directory
        changesJob = viewModelScope.launch {
            val result = if (scope == ChangesScope.Folder) {
                vcs.workingChanges(directory).onSuccess { files ->
                    // Read together with the header count: both show the same files.
                    if (vcsInHeader && inRepository == true) {
                        changedFiles = files.size
                        showVcs()
                    }
                }
            } else {
                chat.changes(sessionId, scope)
            }
            updateState { s ->
                val panel = s.changes ?: return@updateState s
                s.copy(
                    changes = result.fold(
                        onSuccess = { panel.copy(files = it, loading = false, error = null) },
                        onFailure = { panel.copy(loading = false, error = it.toUiText()) },
                    ),
                )
            }
        }
    }

    private fun reply(requestId: String, decision: PermissionDecision) {
        if (currentState.replyingPermission != null) return
        updateState { it.copy(replyingPermission = requestId) }
        viewModelScope.launch {
            chat.replyPermission(sessionId, requestId, decision).fold(
                onSuccess = {
                    updateState { s -> s.copy(replyingPermission = null, permissions = s.permissions.filterNot { it.id == requestId }) }
                },
                onFailure = { error ->
                    updateState { it.copy(replyingPermission = null) }
                    sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                    refreshPermissions()
                },
            )
        }
    }

    private companion object {
        const val RELOAD_DEBOUNCE_MS = 400L
        const val SHELL_ENDED = "session.shell.ended"
        const val LOCAL_MATCH_WINDOW = 10
        const val SUGGEST_DEBOUNCE_MS = 150L
        const val RUNNING_POLL_MS = 15_000L
        const val RUNNING_SHEET_POLL_MS = 5_000L
        const val RUNNING_EVENT_DEBOUNCE_MS = 1_000L

        /** Sessions scanned for recently used models. */
        const val RECENT_SESSIONS = 30

        /** Newest messages cached for the neighbors in the list: about a screen of transcript. */
        const val NEIGHBOR_MESSAGES = 50
    }
}

/** Id prefix of prompts sent from here that the server has not acknowledged yet. */
internal const val LOCAL_PREFIX = "local-"

/** The uploaded images of a prompt, as its transcript row shows them. */
private fun List<PromptAttachment>.sentImages(): List<ImageAttachment> = mapNotNull { attachment ->
    val mime = attachment.mime?.takeIf { it.startsWith("image/") }
    if (attachment.mention != null || mime == null || !attachment.uri.startsWith("data:", ignoreCase = true)) null
    else ImageAttachment(attachment.name, mime, attachment.uri)
}

private fun Set<String>.toggle(id: String, present: Boolean): Set<String> = if (present) this + id else this - id

/** Where an image of the viewer comes from, to fetch it when the cache dropped it. */
private sealed interface PreviewSource {
    data class Prompt(val messageId: String, val index: Int) : PreviewSource
    data class Tool(val messageId: String, val callId: String, val index: Int) : PreviewSource

    /** An attachment of the composer: always here. */
    data object Local : PreviewSource
}
