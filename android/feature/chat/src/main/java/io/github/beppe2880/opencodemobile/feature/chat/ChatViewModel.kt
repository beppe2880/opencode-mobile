package io.github.beppe2880.opencodemobile.feature.chat

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.DraftRepository
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.NoticeKind
import io.github.beppe2880.opencodemobile.core.domain.RECENT_MODELS
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.TranscriptReducer
import io.github.beppe2880.opencodemobile.core.domain.initialAnswers
import io.github.beppe2880.opencodemobile.core.domain.recentModels
import io.github.beppe2880.opencodemobile.core.domain.isComplete
import io.github.beppe2880.opencodemobile.core.domain.reply
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.core.ui.components.selectablePrimary
import io.github.beppe2880.opencodemobile.core.ui.text.UiText
import io.github.beppe2880.opencodemobile.core.ui.text.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
    private val clock: () -> Long = System::currentTimeMillis,
) : BaseViewModel<ChatState, ChatEvent, ChatEffect>(ChatState(sessionId = sessionId, savedScroll = scroll.get(sessionId))) {

    private var messages: List<Message> = emptyList()
    private var showReasoning: Boolean = settings.settings.value.showReasoning
    private var groupActivity: Boolean = settings.settings.value.groupActivity
    private var reloadJob: Job? = null
    private var olderJob: Job? = null
    private var previewJob: Job? = null
    /** Messages whose images were requested in this chat; a failure is retried on reopen. */
    private val imageRequests = HashSet<String>()
    private var suggestJob: Job? = null
    private var changesJob: Job? = null

    /** Models of the most recently updated sessions, across every project. */
    private var recentFromSessions: List<ModelRef> = emptyList()

    /** Completed compactions when `/compact` was requested; null when none is pending. */
    private var compactRequestedWith: Int? = null

    /** Keys of [BackgroundableWork] already sent to the background, so the hint hides at once. */
    private val movedToBackground = mutableSetOf<String>()

    /** First message of a staged revert; it and everything after are hidden. */
    private var revertMessageId: String? = null
    private var localIds = 0

    init {
        // Header from the session list, so the title shows before anything loads.
        sessions.cached(sessionId)?.let { s ->
            revertMessageId = s.revertMessageId
            updateState { it.copy(title = s.title, directory = s.directory, agent = s.agent, model = s.model) }
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
                groupActivity = s.groupActivity
                updateState {
                    it.copy(expandToolsByDefault = s.expandToolsByDefault, showCost = s.showCost, swipeEnabled = s.swipeBetweenChats, agentColorOutline = s.agentColorOutline)
                }
                rebuild()
            }
        }
        viewModelScope.launch { events.connection.collect { c -> updateState { it.copy(connection = c) } } }
        viewModelScope.launch { events.events.collect(::onServerEvent) }
        loadAll()
    }

    override fun onEvent(event: ChatEvent) {
        when (event) {
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
            is ChatEvent.OpenSession -> sendEffect(ChatEffect.OpenSession(event.sessionId))
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
            ChatEvent.Back -> sendEffect(ChatEffect.NavigateBack)
            is ChatEvent.AddAttachments -> updateState { s -> s.copy(attachments = (s.attachments + event.attachments).distinctBy { it.uri }) }
            is ChatEvent.RemoveAttachment -> updateState { s -> s.copy(attachments = s.attachments.filterNot { it.uri == event.uri }) }
            is ChatEvent.PreviewAttachment -> currentState.attachments.firstOrNull { it.uri == event.uri }?.let { a ->
                updateState { it.copy(preview = ImagePreview(a.name, a.uri)) }
            }
            is ChatEvent.OpenImage -> openImage(event.messageId, event.index)
            ChatEvent.ClosePreview -> {
                previewJob?.cancel()
                updateState { it.copy(preview = null) }
            }
            ChatEvent.AttachmentFailed -> sendEffect(ChatEffect.ShowMessage(UiText.Res(R.string.chat_attachment_failed)))
            is ChatEvent.PickSuggestion -> pickSuggestion(event.suggestion)
            is ChatEvent.FormAnswerChanged -> updateState { s ->
                s.copy(formAnswers = if (event.answer == null) s.formAnswers - event.key else s.formAnswers + (event.key to event.answer))
            }
            ChatEvent.SubmitForm -> submitForm()
            ChatEvent.DismissForm -> dismissForm()
            is ChatEvent.Revert -> revert(event.messageId)
            ChatEvent.RestoreRevert -> restoreRevert()
            is ChatEvent.Fork -> fork(event.messageId)
            ChatEvent.OpenChanges -> loadChanges(currentState.changes?.scope ?: ChangesScope.Session)
            ChatEvent.CloseChanges -> {
                changesJob?.cancel()
                updateState { it.copy(changes = null) }
            }
            is ChatEvent.LoadChanges -> loadChanges(event.scope)
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
            messages = olderThan(server) + TranscriptReducer.merge(server, localPending(server))
            val lastAssistant = messages.lastOrNull { it is Message.Assistant } as? Message.Assistant
            val activeIds = active.await().getOrNull()
            val permissionList = perms.await().getOrNull()
            updateState {
                val model = s.model ?: lastAssistant?.model ?: it.model
                it.copy(
                    title = s.title,
                    directory = s.directory,
                    agent = s.agent ?: lastAssistant?.agent ?: it.agent,
                    model = model,
                    running = activeIds?.contains(sessionId) ?: it.running,
                    permissions = permissionList ?: it.permissions,
                    recentModels = recentModels(model),
                    loading = false,
                    synced = true,
                    error = null,
                )
            }
            rebuild()
            loadCatalog(s.directory)
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

    /**
     * Shows a sent image. The local cache keeps prompts without their image payload (too big
     * to store), so a missing one is fetched from the server with its message.
     */
    private fun openImage(messageId: String, index: Int) {
        val image = (messages.firstOrNull { it.id == messageId } as? Message.User)?.images?.getOrNull(index) ?: return
        val known = image.dataUri ?: images.get(messageId)?.getOrNull(index)
        previewJob?.cancel()
        updateState { it.copy(preview = ImagePreview(image.name, known)) }
        if (known != null) return
        previewJob = viewModelScope.launch {
            val uri = chat.messageImages(sessionId, messageId).getOrNull()?.getOrNull(index)
            updateState { s -> s.preview?.let { s.copy(preview = it.copy(dataUri = uri, failed = uri == null)) } ?: s }
        }
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
        updateState {
            it.copy(
                agent = it.agent ?: lastAssistant?.agent,
                model = it.model ?: lastAssistant?.model,
                loading = false,
            )
        }
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
        if (event == ServerEvent.Connected) {
            loadAll()
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
            }
            is ServerEvent.SessionChanged -> if (event.deleted) {
                sendEffect(ChatEffect.ShowMessage(UiText.Res(R.string.chat_session_deleted)))
                sendEffect(ChatEffect.NavigateBack)
            } else {
                viewModelScope.launch {
                    sessions.get(sessionId).onSuccess { s ->
                        updateState { it.copy(title = s.title, agent = s.agent ?: it.agent, model = s.model ?: it.model) }
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
            is ServerEvent.TranscriptChanged -> scheduleReload()
            is ServerEvent.StepStarted -> {
                applyEvent(event)
                updateState { it.copy(running = true, agent = event.agent, model = event.model) }
            }
            is ServerEvent.StepEnded -> {
                applyEvent(event)
                scheduleReload()
                // A step may have edited files: the open sheet follows along.
                currentState.changes?.let { loadChanges(it.scope) }
            }
            is ServerEvent.InboxEnqueued -> {
                messages = messages.filterNot { it is Message.User && it.id.startsWith(LOCAL_PREFIX) && it.text == event.text }
                applyEvent(event)
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

    /** Messages before the staged revert point; all of them when nothing is reverted. */
    private fun visibleMessages(): List<Message> {
        val cut = revertMessageId?.let { id -> messages.indexOfFirst { it.id == id } }?.takeIf { it >= 0 }
        return if (cut == null) messages else messages.subList(0, cut)
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

    private fun rebuild() {
        val state = currentState
        val visible = visibleMessages()
        val reverted = messages.subList(visible.size, messages.size).count { it is Message.User }
        val items = ChatItemsBuilder.build(withImages(visible), state.running, showReasoning, state.models::labelFor, groupActivity)
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
            )
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

    /** The current model, then the models of recently updated sessions, then this transcript's. */
    private fun recentModels(current: ModelRef? = currentState.model): List<ModelRef> {
        val fromTranscript = messages.asReversed().filterIsInstance<Message.Assistant>().map { it.model }
        return (listOfNotNull(current) + recentFromSessions + fromTranscript).distinctBy { it.key }.take(RECENT_MODELS)
    }

    private fun send() {
        val state = currentState
        if (!state.canSend) return
        val text = state.draft.trim()
        val slash = Completions.slash(text)
        Completions.Builtin.of(slash?.name)?.let { return runBuiltin(it) }
        val command = slash?.name?.takeIf { name -> state.commands.any { it.name == name } }
        val arguments = slash?.arguments.orEmpty()
        val attachments = state.attachments
        val delivery = state.delivery
        val revert = revertMessageId
        val local = Message.User("$LOCAL_PREFIX${localIds++}", clock(), text, pending = true, attachments = state.uploads.size)
        // A new prompt makes a staged revert permanent, so the hidden messages stay hidden.
        revertMessageId = null
        messages = messages + local
        updateState { it.copy(draft = "", attachments = emptyList(), suggestions = emptyList(), sending = true) }
        rebuild()
        sendEffect(ChatEffect.PromptSent)
        viewModelScope.launch {
            val committed = if (revert != null) chat.commitRevert(sessionId) else Result.success(Unit)
            val result = committed.fold(
                onSuccess = {
                    if (command != null) chat.command(sessionId, command, arguments, delivery, attachments)
                    else chat.prompt(sessionId, text, delivery, attachments)
                },
                onFailure = { Result.failure(it) },
            )
            result.fold(
                onSuccess = {
                    updateState { it.copy(sending = false, running = true) }
                    rebuild()
                    scheduleReload()
                },
                onFailure = { error ->
                    if (committed.isFailure) revertMessageId = revert
                    messages = messages.filterNot { it.id == local.id }
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

    private fun runBuiltin(builtin: Completions.Builtin) {
        updateState { it.copy(draft = "", suggestions = emptyList()) }
        when (builtin) {
            Completions.Builtin.Undo -> undo()
            Completions.Builtin.Redo -> restoreRevert()
            Completions.Builtin.Compact -> compact()
            Completions.Builtin.Fork -> fork(null)
        }
    }

    /** `/undo`: reverts the last prompt, like the OpenCode TUI. */
    private fun undo() {
        val last = visibleMessages().lastOrNull { it is Message.User && !it.pending && !it.id.startsWith(LOCAL_PREFIX) }
        if (last == null) {
            sendEffect(ChatEffect.ShowMessage(UiText.Res(R.string.chat_nothing_to_undo)))
            return
        }
        revert(last.id)
    }

    private fun revert(messageId: String) {
        if (currentState.reverting) return
        val message = messages.firstOrNull { it.id == messageId } as? Message.User ?: return
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
                    updateState { it.copy(reverting = false, draft = message.text) }
                    rebuild()
                },
                onFailure = { error ->
                    updateState { it.copy(reverting = false) }
                    sendEffect(ChatEffect.ShowMessage(error.toUiText()))
                },
            )
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
            sendEffect(ChatEffect.ShowMessage(UiText.Res(R.string.chat_nothing_to_redo)))
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
        val previous = currentState.agent
        if (agent == previous) return
        updateState { it.copy(agent = agent) }
        viewModelScope.launch {
            chat.switchAgent(sessionId, agent).onFailure { error ->
                updateState { it.copy(agent = previous) }
                sendEffect(ChatEffect.ShowMessage(error.toUiText()))
            }
        }
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
        val previous = currentState.model
        updateState { it.copy(model = model, recentModels = (listOf(model) + it.recentModels).distinctBy { m -> m.key }) }
        viewModelScope.launch {
            chat.switchModel(sessionId, model).onFailure { error ->
                updateState { it.copy(model = previous) }
                sendEffect(ChatEffect.ShowMessage(error.toUiText()))
            }
        }
    }

    /** Loads [scope] into the changes sheet; files of the same scope stay shown while it reloads. */
    private fun loadChanges(scope: ChangesScope) {
        changesJob?.cancel()
        updateState { s ->
            val kept = s.changes?.takeIf { it.scope == scope }?.files
            s.copy(changes = ChangesPanel(scope, files = kept, loading = true))
        }
        changesJob = viewModelScope.launch {
            val result = chat.changes(sessionId, scope)
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
        const val LOCAL_PREFIX = "local-"
        const val RELOAD_DEBOUNCE_MS = 400L
        const val LOCAL_MATCH_WINDOW = 10
        const val SUGGEST_DEBOUNCE_MS = 150L

        /** Sessions scanned for recently used models. */
        const val RECENT_SESSIONS = 30

        /** Newest messages cached for the neighbors in the list: about a screen of transcript. */
        const val NEIGHBOR_MESSAGES = 50
    }
}
