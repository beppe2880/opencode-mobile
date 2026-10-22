package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.recentModels
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.core.ui.components.selectablePrimary
import io.github.beppe2880.opencodemobile.core.ui.text.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class SessionsViewModel(
    private val sessions: SessionRepository,
    private val catalog: CatalogRepository,
    private val events: EventRepository,
    servers: ServerRepository,
    private val chat: ChatRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : BaseViewModel<SessionsState, SessionsEvent, SessionsEffect>(SessionsState()) {

    private var loadJob: Job? = null
    private var projectsJob: Job? = null
    private var debounceJob: Job? = null
    private var sessionList: List<Session> = emptyList()

    /** The saved server shown; its cached lists are shown before each server answers. */
    private var serverId: String? = null

    /** The next load first shows the stored list of the server and filter. */
    private var showCachedFirst = false

    /** Directory filter last chosen on each server while this screen lives. */
    private val filters = mutableMapOf<String, String?>()

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
        viewModelScope.launch {
            // The order shown (filters included) is the one a chat swipes through.
            state.map { s -> s.groups.flatMap { group -> group.sessions.map { it.id } } }
                .distinctUntilChanged()
                .collect(sessions::setBrowseOrder)
        }
    }

    override fun onEvent(event: SessionsEvent) {
        when (event) {
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
            is SessionsEvent.Open -> sendEffect(SessionsEffect.OpenChat(event.sessionId))
            SessionsEvent.OpenSettings -> sendEffect(SessionsEffect.OpenSettings)
            is SessionsEvent.ShowActions -> updateState { it.copy(actionsFor = event.session) }
            SessionsEvent.DismissActions -> updateState { it.copy(actionsFor = null) }
            is SessionsEvent.RequestRename -> updateState { it.copy(actionsFor = null, renaming = event.session) }
            is SessionsEvent.RequestDelete -> updateState { it.copy(actionsFor = null, deleting = event.session) }
            SessionsEvent.DismissDialog -> updateState { it.copy(renaming = null, deleting = null) }
            is SessionsEvent.ConfirmRename -> rename(event.title)
            SessionsEvent.ConfirmDelete -> delete()
            SessionsEvent.StartNewSession -> startNewSession()
            SessionsEvent.DismissNewSession -> updateState { it.copy(newSession = null) }
            is SessionsEvent.NewSessionDirectory -> updateNew { it.copy(directory = event.directory, customDirectory = event.custom, error = null) }
            is SessionsEvent.NewSessionAgent -> updateNew { it.copy(agent = event.agent) }
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
        sessionList = emptyList()
        updateState {
            it.copy(
                query = "",
                projects = emptyList(),
                directoryFilter = id?.let(filters::get),
                groups = emptyList(),
                running = emptySet(),
                loading = id != null,
                refreshing = false,
                error = null,
                newSession = null,
                actionsFor = null,
                renaming = null,
                deleting = null,
            )
        }
        if (id == null) return
        showCachedFirst = true
        load(initial = true)
        loadProjects(cachedFirst = true)
    }

    private fun onServerEvent(event: ServerEvent) {
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
                } else {
                    scheduleReload(EVENT_DEBOUNCE_MS)
                }
            }
            else -> Unit
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
                onSuccess = { list ->
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
                    updateState { it.copy(projects = projects.sortedByDescending { p -> p.updatedAt }) }
                    fillNewSessionDirectory()
                }
            }
            sessions.projects().onSuccess { projects ->
                updateState { it.copy(projects = projects.sortedByDescending { p -> p.updatedAt }) }
                fillNewSessionDirectory()
            }
        }
    }

    internal fun group(list: List<Session>): List<SessionGroup> {
        val today = Instant.ofEpochMilli(clock()).atZone(zone).toLocalDate()
        return list
            .sortedByDescending { it.updatedAt }
            .groupBy { session ->
                val day = Instant.ofEpochMilli(session.updatedAt).atZone(zone).toLocalDate()
                when {
                    !day.isBefore(today) -> DayBucket.Today
                    day == today.minusDays(1) -> DayBucket.Yesterday
                    day.isAfter(today.minusDays(7)) -> DayBucket.ThisWeek
                    else -> DayBucket.Older
                }
            }
            .toSortedMap()
            .map { (bucket, items) -> SessionGroup(bucket, items) }
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
            sessions.delete(session.id).onFailure { error ->
                sessionList = previous
                updateState { it.copy(groups = group(sessionList)) }
                sendEffect(SessionsEffect.ShowMessage(error.toUiText()))
            }
        }
    }

    private fun startNewSession() {
        val state = currentState
        val directory = state.directoryFilter
            ?: sessionList.firstOrNull()?.directory
            ?: state.projects.firstOrNull()?.directory
            ?: ""
        updateState { it.copy(newSession = NewSessionState(directory = directory, recentModels = sessionList.recentModels())) }
        viewModelScope.launch {
            val dir = directory.ifBlank { null }
            val agents = catalog.agents(dir).getOrDefault(emptyList()).selectablePrimary()
            val models = catalog.models(dir).getOrDefault(emptyList())
            updateNew { new ->
                new.copy(
                    agents = agents,
                    models = models,
                    agent = new.agent ?: agents.firstOrNull { it.id == "build" }?.id ?: agents.firstOrNull()?.id,
                )
            }
        }
    }

    /**
     * The sheet can open before anything is loaded (the widget's button at a cold start): once
     * sessions or projects arrive, it gets the directory it would have started with.
     */
    private fun fillNewSessionDirectory() {
        val new = currentState.newSession ?: return
        if (new.directory.isNotBlank() || new.customDirectory) return
        val directory = currentState.directoryFilter
            ?: sessionList.firstOrNull()?.directory
            ?: currentState.projects.firstOrNull()?.directory
            ?: return
        updateNew { it.copy(directory = directory) }
    }

    private fun create() {
        val new = currentState.newSession ?: return
        if (!new.canCreate) return
        updateNew { it.copy(creating = true, error = null) }
        viewModelScope.launch {
            sessions.create(new.directory.trim(), new.agent, new.model).fold(
                onSuccess = { session ->
                    updateState { it.copy(newSession = null) }
                    sendEffect(SessionsEffect.OpenChat(session.id))
                    load(initial = false)
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
    }
}
