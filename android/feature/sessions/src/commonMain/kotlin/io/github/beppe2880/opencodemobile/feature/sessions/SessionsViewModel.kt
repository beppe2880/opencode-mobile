package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.FolderPreferences
import io.github.beppe2880.opencodemobile.core.domain.NewChatModel
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.PendingShare
import io.github.beppe2880.opencodemobile.core.domain.PinnedSessionRepository
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.RECENT_MODELS
import io.github.beppe2880.opencodemobile.core.domain.RecentModelRepository
import io.github.beppe2880.opencodemobile.core.domain.RunningWorkLoader
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.ServerPaths
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.ShareOpens
import io.github.beppe2880.opencodemobile.core.domain.ShareRepository
import io.github.beppe2880.opencodemobile.core.domain.SharedContent
import io.github.beppe2880.opencodemobile.core.domain.newChatAgent
import io.github.beppe2880.opencodemobile.core.domain.newChatModel
import io.github.beppe2880.opencodemobile.core.domain.recentModels
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.core.ui.components.selectablePrimary
import io.github.beppe2880.opencodemobile.core.ui.text.UiText
import io.github.beppe2880.opencodemobile.core.ui.text.toUiText
import io.github.beppe2880.opencodemobile.feature.sessions.resources.Res
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_share_discarded
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class SessionsViewModel(
    private val sessions: SessionRepository,
    private val catalog: CatalogRepository,
    private val events: EventRepository,
    servers: ServerRepository,
    private val chat: ChatRepository,
    private val settings: SettingsRepository,
    private val recents: RecentModelRepository,
    private val shares: ShareRepository,
    private val folderPreferences: FolderPreferences,
    private val pins: PinnedSessionRepository,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : BaseViewModel<SessionsState, SessionsEvent, SessionsEffect>(SessionsState()) {

    private var loadJob: Job? = null
    private var projectsJob: Job? = null
    private var debounceJob: Job? = null
    private var browseJob: Job? = null
    private var newSessionDirectoryCleared = false
    private var sessionList: List<Session> = emptyList()

    /** Sessions pinned on the server shown, as last read from [pins]. */
    private var pinnedIds: Set<String> = emptySet()

    /** Sessions of [sessionList] the server did not list: read one by one because they are pinned. */
    private var listedForPin: Set<String> = emptySet()
    private var workVisible = false
    private var workPoll: Job? = null
    private var workJob: Job? = null
    private var workEventJob: Job? = null
    private val runningWork = RunningWorkLoader(sessions, chat)
    private val showRunningWork: Boolean get() = settings.settings.value.showRunningWorkInList

    /** The saved server shown; its cached lists are shown before each server answers. */
    private var serverId: String? = null

    /** The next load first shows the stored list of the server and filter. */
    private var showCachedFirst = false

    /** Directory filter last chosen on each server while this screen lives. */
    private val filters = mutableMapOf<String, String?>()

    /** The share from another app this screen opened the sheet or the chat choice for. */
    private var shownShare: Long? = null

    /** The share this screen handed to a chat, which takes it when it opens. */
    private var assignedShare: Long? = null

    init {
        viewModelScope.launch {
            servers.activeServer.collect { server -> updateState { it.copy(serverName = server?.name.orEmpty()) } }
        }
        viewModelScope.launch {
            servers.activeServer.map { it?.id }.distinctUntilChanged().collect(::onServerChanged)
        }
        viewModelScope.launch {
            events.connection.collect { connection -> updateState { it.copy(connection = connection) } }
        }
        viewModelScope.launch { events.events.collect(::onServerEvent) }
        viewModelScope.launch { shares.pending.collect(::onShare) }
        viewModelScope.launch { pins.changes.collect { showPins() } }
        viewModelScope.launch {
            settings.settings.map { it.showRunningWorkInList }.distinctUntilChanged().collect { enabled ->
                updateState { it.copy(showRunningWork = enabled, work = if (enabled) it.work else emptyMap()) }
                restartRunningPoll()
            }
        }
        viewModelScope.launch {
            // The order shown (filters included) is the one a chat swipes through.
            state.map { s -> s.groups.flatMap { group -> group.sessions.map { it.id } } }
                .distinctUntilChanged()
                .collect(sessions::setBrowseOrder)
        }
    }

    override fun onEvent(event: SessionsEvent) {
        when (event) {
            is SessionsEvent.Visible -> {
                workVisible = event.visible
                restartRunningPoll()
            }
            SessionsEvent.Refresh -> {
                load(initial = currentState.groups.isEmpty(), pull = currentState.groups.isNotEmpty())
                loadProjects()
            }
            is SessionsEvent.QueryChanged -> {
                updateState { it.copy(query = event.query) }
                scheduleReload(SEARCH_DEBOUNCE_MS)
            }
            is SessionsEvent.FilterChanged -> {
                updateState { it.copy(directoryFilter = event.directory) }
                showCachedFirst = true
                load(initial = false)
            }
            is SessionsEvent.HideProject -> setProjectHidden(event.directory, hidden = true)
            is SessionsEvent.UnhideProject -> setProjectHidden(event.directory, hidden = false)
            is SessionsEvent.Open -> if (currentState.sharing != null) shareTo(event.sessionId) else sendEffect(SessionsEffect.OpenChat(event.sessionId))
            SessionsEvent.OpenSettings -> sendEffect(SessionsEffect.OpenSettings)
            is SessionsEvent.ShowActions -> updateState { it.copy(actionsFor = event.session) }
            SessionsEvent.DismissActions -> updateState { it.copy(actionsFor = null) }
            is SessionsEvent.SetPinned -> setPinned(event.sessionId, event.pinned)
            is SessionsEvent.RequestRename -> updateState { it.copy(actionsFor = null, renaming = event.session) }
            is SessionsEvent.RequestDelete -> updateState { it.copy(actionsFor = null, deleting = event.session) }
            SessionsEvent.DismissDialog -> updateState { it.copy(renaming = null, deleting = null) }
            is SessionsEvent.ConfirmRename -> rename(event.title)
            SessionsEvent.ConfirmDelete -> delete()
            // While a chat is picked for a share, a new session takes it.
            SessionsEvent.StartNewSession -> startNewSession(currentState.sharing)
            SessionsEvent.DismissNewSession -> {
                browseJob?.cancel()
                val shared = currentState.newSession?.shared
                updateState { it.copy(newSession = null) }
                // Opened by the share itself: closing it drops the share; picking a chat goes on.
                if (shared != null && currentState.sharing == null) discardShare()
            }
            SessionsEvent.DiscardShare -> discardShare()
            is SessionsEvent.NewSessionDirectory -> updateNew { it.copy(directory = event.directory, customDirectory = event.custom, error = null) }
            SessionsEvent.BrowseFolders -> browseFolders()
            is SessionsEvent.BrowseTo -> loadFolders(event.path)
            SessionsEvent.CloseFolderBrowser -> {
                browseJob?.cancel()
                updateNew { it.copy(browser = null) }
            }
            SessionsEvent.PickBrowsedFolder -> pickFolder(currentState.newSession?.browser?.path)
            is SessionsEvent.PickFolder -> pickFolder(event.path)
            is SessionsEvent.NewSessionAgent -> updateNew { withDefaultModel(it.copy(agent = event.agent)) }
            is SessionsEvent.NewSessionModelPicker -> updateNew { it.copy(pickingModel = event.open) }
            is SessionsEvent.NewSessionModel -> updateNew { it.copy(model = event.model, pickingModel = false) }
            SessionsEvent.CreateSession -> create()
        }
    }

    /**
     * Shows the new server's stored lists at once instead of the previous server's, then
     * refreshes them from the network. An unreachable server keeps showing what was stored.
     */
    private fun onServerChanged(id: String?) {
        serverId?.let { filters[it] = currentState.directoryFilter }
        serverId = id
        loadJob?.cancel()
        projectsJob?.cancel()
        debounceJob?.cancel()
        browseJob?.cancel()
        workJob?.cancel()
        sessionList = emptyList()
        listedForPin = emptySet()
        pinnedIds = pins.pinned()
        updateState {
            it.copy(
                pinned = pinnedIds,
                query = "",
                projects = emptyList(),
                hiddenDirectories = id?.let(folderPreferences::hiddenDirectories).orEmpty(),
                directoryFilter = id?.let(filters::get),
                groups = emptyList(),
                running = emptySet(),
                work = emptyMap(),
                loading = id != null,
                refreshing = false,
                error = null,
                newSession = null,
                sharing = null,
                actionsFor = null,
                renaming = null,
                deleting = null,
            )
        }
        if (id == null) return
        showCachedFirst = true
        load(initial = true)
        loadProjects(cachedFirst = true)
        // A share still to place opens again on the new server.
        shownShare = null
        onShare(shares.pending.value)
    }

    /**
     * A share from another app (the active server's): opens the new session sheet with it, or
     * asks for its chat (Settings). One handed to a chat before this screen existed, because
     * the app was stopped before the chat opened, opens that chat.
     */
    private fun onShare(share: PendingShare?) {
        if (share == null) {
            shownShare = null
            updateState { it.copy(sharing = null, newSession = it.newSession?.copy(shared = null)) }
            return
        }
        val server = serverId ?: return
        val sessionId = share.sessionId
        if (!share.needsChatOn(server) && sessionId != null) {
            if (assignedShare != share.id) {
                assignedShare = share.id
                sendEffect(SessionsEffect.OpenChat(sessionId))
            }
            return
        }
        if (shownShare == share.id) return
        shownShare = share.id
        when (settings.settings.value.shareOpens) {
            ShareOpens.NewSession -> {
                updateState { it.copy(sharing = null) }
                startNewSession(share.content)
            }
            ShareOpens.ChooseChat -> {
                browseJob?.cancel()
                updateState { it.copy(sharing = share.content, newSession = null) }
            }
        }
    }

    /** The picked chat takes the share into its composer when it opens. */
    private fun shareTo(sessionId: String) {
        val share = shares.pending.value ?: return sendEffect(SessionsEffect.OpenChat(sessionId))
        assignedShare = share.id
        updateState { it.copy(sharing = null) }
        viewModelScope.launch {
            shares.assign(sessionId, serverId)
            sendEffect(SessionsEffect.OpenChat(sessionId))
        }
    }

    private fun discardShare() {
        updateState { it.copy(sharing = null, newSession = it.newSession?.copy(shared = null)) }
        viewModelScope.launch {
            shares.discard()
            sendEffect(SessionsEffect.ShowMessage(UiText.Resource(Res.string.sessions_share_discarded)))
        }
    }

    private fun onServerEvent(event: ServerEvent) {
        // Subagents start and end with executions; commands are found by the poll.
        if (event == ServerEvent.Connected || event is ServerEvent.ExecutionStarted || event is ServerEvent.ExecutionEnded) {
            requestRunningRefresh()
        }
        when (event) {
            ServerEvent.Connected -> load(initial = false)
            is ServerEvent.ExecutionStarted -> updateState { it.copy(running = it.running + event.sessionId) }
            is ServerEvent.ExecutionEnded -> {
                updateState { it.copy(running = it.running - event.sessionId) }
                scheduleReload(EVENT_DEBOUNCE_MS)
            }
            is ServerEvent.SessionChanged -> {
                if (event.deleted) {
                    sessionList = sessionList.filterNot { it.id == event.sessionId }
                    updateState { it.copy(groups = group(sessionList)) }
                    forgetPin(event.sessionId)
                } else {
                    scheduleReload(EVENT_DEBOUNCE_MS)
                }
            }
            else -> Unit
        }
    }

    /** Polls running work only while the list is visible and the setting is on. */
    private fun restartRunningPoll() {
        workPoll?.cancel()
        workJob?.cancel()
        workEventJob?.cancel()
        workPoll = if (workVisible && showRunningWork) {
            viewModelScope.launch {
                while (true) {
                    // Offline or unreachable a refresh only fails; reconnecting refreshes anyway.
                    if (events.connection.value == ConnectionState.Connected) refreshRunning()
                    delay(RUNNING_POLL_MS)
                }
            }
        } else {
            null
        }
    }

    /** Bursts of events (several subagents starting) cause one refresh. */
    private fun requestRunningRefresh() {
        if (!workVisible || !showRunningWork || workEventJob?.isActive == true) return
        workEventJob = viewModelScope.launch {
            delay(RUNNING_EVENT_DEBOUNCE_MS)
            refreshRunning()
        }
    }

    /**
     * Counts only: no transcripts are read. A session whose project could not be read keeps
     * its previous counts; a failure of the whole refresh keeps everything shown.
     */
    private fun refreshRunning() {
        if (!workVisible || !showRunningWork || serverId == null || sessionList.isEmpty() || workJob?.isActive == true) return
        val server = serverId
        val roots = sessionList
        workJob = viewModelScope.launch {
            runningWork.load(roots).onSuccess { snapshot ->
                if (serverId != server) return@onSuccess
                updateState { state ->
                    val work = snapshot.sessions.mapValues { (id, work) -> if (id in snapshot.errors) state.work[id] ?: work else work }
                    state.copy(work = work.filterValues { !it.isEmpty })
                }
            }
        }
    }

    private fun scheduleReload(delayMs: Long) {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(delayMs)
            load(initial = false)
        }
    }

    private fun load(initial: Boolean, pull: Boolean = false) {
        loadJob?.cancel()
        updateState { it.copy(loading = initial && it.groups.isEmpty(), refreshing = pull, error = null) }
        loadJob = viewModelScope.launch {
            val state = currentState
            val search = state.query.trim().ifEmpty { null }
            if (showCachedFirst) {
                if (search == null) showCached(state.directoryFilter) else showCachedFirst = false
            }
            val result = sessions.list(search = search, directory = state.directoryFilter)
            val active = sessions.active().getOrNull()
            result.fold(
                onSuccess = { listed ->
                    val beyond = pinnedBeyond(listed, search, state.directoryFilter)
                    listedForPin = beyond.mapTo(HashSet()) { it.id }
                    val list = listed + beyond
                    sessionList = list
                    updateState {
                        it.copy(
                            groups = group(list),
                            running = active ?: it.running,
                            loading = false,
                            refreshing = false,
                            error = null,
                        )
                    }
                    fillNewSessionDirectory()
                    prefetch(list)
                    refreshRunning()
                },
                onFailure = { error ->
                    updateState { it.copy(loading = false, refreshing = false, error = if (it.groups.isEmpty()) error.toUiText() else null) }
                    // An unreachable server keeps its shown (possibly stored) list; the connection indicator tells.
                    val background = !pull && error is OpenCodeError.Unreachable
                    if (currentState.groups.isNotEmpty() && !background) sendEffect(SessionsEffect.ShowMessage(error.toUiText()))
                },
            )
        }
    }

    /** Caches the last messages of the most recent sessions, so opening them shows content at once. */
    private fun prefetch(list: List<Session>) {
        val recent = list.sortedByDescending { it.updatedAt }.take(PREFETCH_SESSIONS).map { it.id }
        if (recent.isNotEmpty()) viewModelScope.launch { chat.prefetch(recent, PREFETCH_MESSAGES) }
    }

    /** Replaces the list with the stored one of the current server, when there is one. */
    private suspend fun showCached(directory: String?) {
        val id = serverId ?: return
        val cached = sessions.cachedList(id, directory)
        showCachedFirst = false
        if (cached == null) return
        sessionList = cached
        updateState { it.copy(groups = group(cached), loading = false, error = null) }
        fillNewSessionDirectory()
    }

    private fun loadProjects(cachedFirst: Boolean = false) {
        projectsJob?.cancel()
        projectsJob = viewModelScope.launch {
            val id = serverId
            if (cachedFirst && id != null) {
                sessions.cachedProjects(id)?.let { projects ->
                    updateState { it.copy(projects = listed(projects)) }
                    fillNewSessionDirectory()
                }
            }
            sessions.projects().onSuccess { projects ->
                updateState { it.copy(projects = listed(projects)) }
                fillNewSessionDirectory()
            }
        }
    }

    /**
     * Most recent first, each folder once. OpenCode makes a project for every folder a session runs
     * in; its built-in "global" project is no folder of its own, and asking for its "/" creates a real one.
     */
    private fun listed(projects: List<Project>): List<Project> =
        projects.sortedByDescending { it.updatedAt }.filter { it.id != GLOBAL_PROJECT_ID }.distinctBy { it.directory }

    internal fun group(list: List<Session>): List<SessionGroup> = groupSessions(list, pinnedIds, clock(), zone)

    private fun setPinned(sessionId: String, pinned: Boolean) {
        updateState { it.copy(actionsFor = null) }
        pins.setPinned(sessionId, pinned)
        showPins()
    }

    /** A deleted session leaves no pin behind. */
    private fun forgetPin(sessionId: String) {
        pins.setPinned(sessionId, false)
        showPins()
    }

    /**
     * Reads the pins again (changed here or in a chat) and moves the rows between the groups. A
     * session pinned while it is not listed is loaded; one only listed for its pin leaves with it.
     */
    private fun showPins() {
        val added = pins.pinned() - pinnedIds
        pinnedIds = pins.pinned()
        sessionList = sessionList.filterNot { it.id in listedForPin && it.id !in pinnedIds }
        updateState { it.copy(pinned = pinnedIds, groups = group(sessionList)) }
        if (added.any { id -> sessionList.none { it.id == id } }) load(initial = false)
    }

    /**
     * Pinned sessions missing from [listed], which holds only the most recent ones, that the
     * project filter and the search still include. The pin of a session that no longer exists is
     * forgotten; one that cannot be read now stays as last shown.
     */
    private suspend fun pinnedBeyond(listed: List<Session>, search: String?, directory: String?): List<Session> {
        val missing = pinnedIds - listed.mapTo(HashSet()) { it.id }
        if (missing.isEmpty()) return emptyList()
        val shown = sessionList.associateBy { it.id }
        val read = coroutineScope { missing.map { id -> async { id to sessions.get(id) } }.awaitAll() }
        return read
            .mapNotNull { (id, result) ->
                if (result.exceptionOrNull() is OpenCodeError.NotFound) {
                    pins.setPinned(id, false)
                    null
                } else {
                    result.getOrNull() ?: shown[id]
                }
            }
            .filter { it.parentId == null && it.archivedAt == null && it.matches(search, directory) }
    }

    private fun rename(title: String) {
        val session = currentState.renaming ?: return
        if (title.isBlank() || title == session.title) {
            updateState { it.copy(renaming = null) }
            return
        }
        updateState { it.copy(renaming = null) }
        val previous = sessionList
        sessionList = sessionList.map { if (it.id == session.id) it.copy(title = title) else it }
        updateState { it.copy(groups = group(sessionList)) }
        viewModelScope.launch {
            sessions.rename(session.id, title).onFailure { error ->
                sessionList = previous
                updateState { it.copy(groups = group(sessionList)) }
                sendEffect(SessionsEffect.ShowMessage(error.toUiText()))
            }
        }
    }

    private fun delete() {
        val session = currentState.deleting ?: return
        updateState { it.copy(deleting = null) }
        val previous = sessionList
        sessionList = sessionList.filterNot { it.id == session.id }
        updateState { it.copy(groups = group(sessionList)) }
        viewModelScope.launch {
            sessions.delete(session.id).onSuccess { forgetPin(session.id) }.onFailure { error ->
                sessionList = previous
                updateState { it.copy(groups = group(sessionList)) }
                sendEffect(SessionsEffect.ShowMessage(error.toUiText()))
            }
        }
    }

    private fun startNewSession(shared: SharedContent? = null) {
        browseJob?.cancel()
        newSessionDirectoryCleared = false
        val state = currentState
        val directory = defaultDirectory(state)
            ?: ""
        val recent = (recents.recent() + sessionList.recentModels()).distinctBy { it.key }.take(RECENT_MODELS)
        updateState { it.copy(newSession = NewSessionState(directory = directory, recentModels = recent, shared = shared)) }
        // Folders used since the last load are new projects: list them.
        loadProjects()
        viewModelScope.launch {
            val dir = directory.ifBlank { null }
            // A config that cannot be read only skips its step.
            val configuredRequest = async { catalog.configuredModel(dir).getOrNull() }
            val agents = catalog.agents(dir).getOrDefault(emptyList()).selectablePrimary()
            val models = catalog.models(dir).getOrDefault(emptyList())
            val configured = configuredRequest.await()
            updateNew { new ->
                withDefaultModel(
                    new.copy(
                        agents = agents,
                        models = models,
                        configuredModel = configured,
                        agent = new.agent ?: agents.newChatAgent()?.id,
                    ),
                )
            }
        }
    }

    private fun browseFolders() {
        val new = currentState.newSession ?: return
        val start = browseStart(new.directory.trim(), currentState)
        val recent = serverId?.let(folderPreferences::recentFolders).orEmpty()
        updateNew { it.copy(browser = FolderBrowserState(path = start.orEmpty(), recent = recent)) }
        loadFolders(start)
    }

    /**
     * The parent of the selected directory, else of the latest visible project's directory, so
     * siblings are one tap away; null (the server's folder) without an absolute one.
     */
    private fun browseStart(directory: String, state: SessionsState): String? {
        val near = directory.takeIf(ServerPaths::isAbsolute)
            ?: state.visibleProjects.firstOrNull { ServerPaths.isAbsolute(it.directory) }?.directory
            ?: return null
        return ServerPaths.parent(near) ?: ServerPaths.normalize(near)
    }

    /** Until the start folder is listed, loads fall back to the server's folder and set the start. */
    private fun loadFolders(path: String?) {
        val browser = currentState.newSession?.browser ?: return
        val starting = browser.start == null
        browseJob?.cancel()
        updateNew { it.copy(browser = it.browser?.copy(loading = true, error = null)) }
        browseJob = viewModelScope.launch {
            var result = catalog.listFolders(path)
            ensureActive()
            if (result.isFailure && starting && path != null) result = catalog.listFolders(null)
            ensureActive()
            result.fold(
                onSuccess = { listing ->
                    updateNew {
                        it.copy(
                            browser = it.browser?.let { current ->
                                current.copy(
                                    path = listing.path,
                                    parent = listing.parent,
                                    folders = listing.folders,
                                    loading = false,
                                    error = null,
                                    start = current.start ?: listing.path,
                                )
                            },
                        )
                    }
                },
                onFailure = { error ->
                    updateNew { it.copy(browser = it.browser?.copy(loading = false, error = error.toUiText())) }
                },
            )
        }
    }

    /** Picks [path] for the new session, the browsed folder or a recent one, without browsing to it. */
    private fun pickFolder(path: String?) {
        if (currentState.newSession?.browser == null) return
        if (path.isNullOrBlank()) return
        serverId?.let { folderPreferences.folderPicked(it, path) }
        browseJob?.cancel()
        updateNew { it.copy(directory = path, customDirectory = true, error = null, browser = null) }
    }

    /**
     * The model the new chat gets when none is picked, like OpenCode Desktop: the agent's
     * configured one, the server's configured one, then the last one used; each only when available. "Server default" in
     * Settings leaves it to the server.
     */
    private fun withDefaultModel(new: NewSessionState): NewSessionState {
        if (settings.settings.value.newChatModel == NewChatModel.ServerDefault) return new.copy(defaultModel = null)
        val agent = new.agents.firstOrNull { it.id == new.agent }
        return new.copy(defaultModel = newChatModel(agent, new.configuredModel, new.models, new.recentModels))
    }

    /**
     * The sheet can open before anything is loaded (the widget's button at a cold start): once
     * sessions or projects arrive, it gets the directory it would have started with.
     */
    private fun fillNewSessionDirectory() {
        val new = currentState.newSession ?: return
        if (new.directory.isNotBlank() || new.customDirectory || newSessionDirectoryCleared) return
        val directory = defaultDirectory(currentState) ?: return
        updateNew { it.copy(directory = directory) }
    }

    /** The filtered project, else the latest session's or the latest project's directory; never a hidden one. */
    private fun defaultDirectory(state: SessionsState): String? = state.directoryFilter
        ?: sessionList.firstOrNull { !isHidden(it, state) }?.directory
        ?: state.visibleProjects.firstOrNull()?.directory

    /**
     * A session counts as hidden when its own directory is hidden or when its project's directory
     * is: a session in a subfolder of a hidden project (`/same/sub` of `/same`) is hidden too.
     */
    private fun isHidden(session: Session, state: SessionsState): Boolean =
        session.directory in state.hiddenDirectories ||
            state.projects.any { it.id == session.projectId && it.directory in state.hiddenDirectories }

    private fun setProjectHidden(directory: String, hidden: Boolean) {
        val id = serverId ?: return
        folderPreferences.setDirectoryHidden(id, directory, hidden)
        updateState { it.copy(hiddenDirectories = folderPreferences.hiddenDirectories(id)) }
        if (!hidden) return
        val new = currentState.newSession
        if (new != null && !new.customDirectory && new.directory == directory) {
            newSessionDirectoryCleared = true
            updateNew { it.copy(directory = "") }
        }
        if (currentState.directoryFilter == directory) onEvent(SessionsEvent.FilterChanged(null))
    }

    private fun create() {
        val new = currentState.newSession ?: return
        if (!new.canCreate) return
        updateNew { it.copy(creating = true, error = null) }
        viewModelScope.launch {
            sessions.create(new.directory.trim(), new.agent, new.effectiveModel).fold(
                onSuccess = { session ->
                    if (new.shared != null) {
                        assignedShare = shares.pending.value?.id
                        shares.assign(session.id, serverId)
                    }
                    browseJob?.cancel()
                    updateState { it.copy(newSession = null, sharing = null) }
                    sendEffect(SessionsEffect.OpenChat(session.id))
                    load(initial = false)
                    loadProjects()
                },
                onFailure = { error -> updateNew { it.copy(creating = false, error = error.toUiText()) } },
            )
        }
    }

    private fun updateNew(transform: (NewSessionState) -> NewSessionState) =
        updateState { state -> state.newSession?.let { state.copy(newSession = transform(it)) } ?: state }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val EVENT_DEBOUNCE_MS = 600L
        const val PREFETCH_SESSIONS = 5
        const val PREFETCH_MESSAGES = 2
        const val RUNNING_POLL_MS = 15_000L
        const val RUNNING_EVENT_DEBOUNCE_MS = 1_000L
        const val GLOBAL_PROJECT_ID = "global"
    }
}
