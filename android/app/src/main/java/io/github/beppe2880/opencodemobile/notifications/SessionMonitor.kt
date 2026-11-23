package io.github.beppe2880.opencodemobile.notifications

import androidx.annotation.VisibleForTesting
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.MutedSessionRepository
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What the agent of a running session is doing, shown in the ongoing notification. */
sealed interface SessionActivity {
    data object Thinking : SessionActivity
    data object Writing : SessionActivity
    data class Tool(val name: String) : SessionActivity
}

data class RunningSession(val id: String, val title: String, val activity: SessionActivity? = null)

/** Posts and removes the notifications decided by [SessionMonitor]. */
interface MonitorNotifier {
    fun permission(sessionTitle: String, request: PermissionRequest)
    fun question(sessionTitle: String, form: Form)

    /** Removes a [permission] or [question] notification by request or form id. */
    fun cancelAttention(id: String)
    fun finished(sessionId: String, title: String, outcome: SessionOutcome)
    fun cancelFinished(sessionId: String)
}

/**
 * Follows the root sessions running on the active server while notifications are enabled.
 * While the app is in the background it notifies the permission requests and questions that
 * wait for the user, and the sessions that finish; a pending request is notified once and its
 * notification removed when it is answered anywhere.
 *
 * The event stream only flows in the background while [SessionMonitorService] keeps it alive;
 * [startService] is called while the app is visible and something runs, because Android does
 * not let a background app start a foreground service.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionMonitor(
    private val events: EventRepository,
    private val sessions: SessionRepository,
    private val chat: ChatRepository,
    private val settings: SettingsRepository,
    private val servers: ServerRepository,
    private val notifier: MonitorNotifier,
    private val visible: StateFlow<Boolean>,
    private val startService: () -> Unit,
    private val mutes: MutedSessionRepository,
) {
    private val _running = MutableStateFlow<Map<String, RunningSession>>(emptyMap())

    /** Running root sessions, in the order they started. */
    val running: StateFlow<Map<String, RunningSession>> = _running.asStateFlow()

    private val lock = Mutex()

    /** Pending request and form ids per session, and those already notified; guarded by [lock]. */
    private val pending = mutableMapOf<String, Set<String>>()
    private val notified = mutableSetOf<String>()
    private val refreshJobs = mutableMapOf<String, Job>()

    @VisibleForTesting
    internal val refreshJobCount: Int get() = synchronized(refreshJobs) { refreshJobs.size }

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(settings.settings.map { it.notifications }, servers.activeServer.map { it?.id }, mutes.temporary) { on, server, temporary ->
                // Temporary mutes still need to expire when notifications are disabled.
                server.takeIf { on || temporary.isNotEmpty() }
            }
                .distinctUntilChanged()
                .collectLatest { server ->
                    reset()
                    if (server != null) coroutineScope { follow(this, server) }
                }
        }
    }

    private suspend fun follow(scope: CoroutineScope, serverId: String) {
        scope.launch {
            var previous = emptySet<String>()
            mutes.muted.collect { muted ->
                val unmutedPending = lock.withLock {
                    for (id in muted - previous) {
                        notifier.cancelFinished(id)
                        pending[id].orEmpty().filter { notified.remove(it) }.forEach(notifier::cancelAttention)
                    }
                    (previous - muted).filter { it in pending }
                }
                previous = muted
                unmutedPending.forEach { refresh(scope, it) }
            }
        }
        scope.launch {
            combine(_running, visible, mutes.muted, settings.settings) { running, shown, muted, preferences ->
                preferences.notifications && shown && running.keys.any { it !in muted }
            }
                .distinctUntilChanged()
                .filter { it }
                .collect { startService() }
        }
        // Requests that arrived while a screen was open are notified once the app leaves.
        scope.launch {
            visible.drop(1).filter { !it }.collect { (_running.value.keys + pendingSessions()).forEach { refresh(scope, it) } }
        }
        // An authorization failure invalidates the locally tracked running sessions.
        scope.launch {
            events.connection.filter { it == ConnectionState.Unauthorized }.collect { reset() }
        }
        // There may already be a live stream when the monitor starts (e.g. after enabling it).
        if (events.connection.value == ConnectionState.Connected) scope.launch { reconcile(scope, serverId) }
        events.events.collect { if (servers.activeServer.value?.id == serverId) onEvent(scope, serverId, it) }
    }

    private fun onEvent(scope: CoroutineScope, serverId: String, event: ServerEvent) {
        when (event) {
            ServerEvent.Connected -> scope.launch { reconcile(scope, serverId) }
            is ServerEvent.ExecutionStarted -> started(scope, event.sessionId)
            is ServerEvent.ExecutionEnded -> ended(event.sessionId, event.outcome, event.at.takeIf { it > 0 })
            is ServerEvent.PermissionChanged -> refresh(scope, event.sessionId)
            is ServerEvent.FormsChanged -> refresh(scope, event.sessionId)
            is ServerEvent.ReasoningStarted -> activity(event.sessionId, SessionActivity.Thinking)
            is ServerEvent.TextStarted -> activity(event.sessionId, SessionActivity.Writing)
            is ServerEvent.ToolInputStarted -> activity(event.sessionId, SessionActivity.Tool(event.name))
            is ServerEvent.SessionChanged -> when {
                event.deleted -> {
                    mutes.setMuted(event.sessionId, false)
                    scope.launch { forget(event.sessionId) }
                }
                event.sessionId in _running.value -> scope.launch { retitle(event.sessionId) }
            }
            else -> Unit
        }
    }

    /** Record the start before reading another event; only the title lookup can suspend. */
    private fun started(scope: CoroutineScope, sessionId: String) {
        val cached = sessions.cached(sessionId)
        if (cached?.parentId != null) return
        _running.update { it + (sessionId to (it[sessionId] ?: RunningSession(sessionId, cached?.title.orEmpty()))) }
        notifier.cancelFinished(sessionId)
        scope.launch { retitle(sessionId) }
    }

    /** Refreshes the title (generated after the first prompt) and drops subagent sessions. */
    private suspend fun retitle(sessionId: String) {
        val session = sessions.get(sessionId).getOrNull() ?: return
        _running.update { running ->
            val current = running[sessionId] ?: return@update running
            if (session.parentId != null) running - sessionId else running + (sessionId to current.copy(title = session.title))
        }
    }

    private fun ended(sessionId: String, outcome: SessionOutcome, idleAt: Long? = null) {
        val muted = sessionId in mutes.muted.value
        val session = _running.value[sessionId]
        _running.update { it - sessionId }
        val preferences = settings.settings.value
        if (session != null && !muted && !visible.value && preferences.notifications && preferences.notifyFinished) {
            notifier.finished(sessionId, session.title, outcome)
        }
        // The completion itself belongs to the muted turn, not to the next one.
        mutes.finishTemporary(sessionId, idleAt)
    }

    private suspend fun forget(sessionId: String) {
        _running.update { it - sessionId }
        synchronized(refreshJobs) { refreshJobs.remove(sessionId)?.cancel() }
        lock.withLock {
            pending.remove(sessionId)?.filter { notified.remove(it) }?.forEach(notifier::cancelAttention)
        }
    }

    private fun activity(sessionId: String, activity: SessionActivity) {
        _running.update { running ->
            val current = running[sessionId]
            if (current == null || current.activity == activity) running else running + (sessionId to current.copy(activity = activity))
        }
    }

    /** After a (re)connect: catches up with executions and requests missed while disconnected. */
    private suspend fun reconcile(scope: CoroutineScope, serverId: String) {
        val active = sessions.active().getOrNull() ?: return
        if (servers.activeServer.value?.id != serverId) return
        for (id in _running.value.keys - active) {
            val outcome = sessions.get(id).getOrNull()?.outcome ?: SessionOutcome.Succeeded
            if (servers.activeServer.value?.id != serverId) return
            ended(id, outcome)
        }
        // A saved temporary mute may outlive the process or the SSE connection. Check for an
        // intervening idle even if a new turn is already running by the time we reconnect.
        for (id in mutes.temporary.value.keys) {
            val idleAt = if (id in active) sessions.get(id).getOrNull()?.idleAt ?: continue else null
            if (servers.activeServer.value?.id != serverId) return
            mutes.finishTemporary(id, idleAt)
        }
        for (id in active - _running.value.keys) started(scope, id)
        (active + pendingSessions()).forEach { refresh(scope, it) }
    }

    /** Re-reads the pending permissions and questions of a session; a newer call replaces an older one. */
    private fun refresh(scope: CoroutineScope, sessionId: String) {
        synchronized(refreshJobs) {
            refreshJobs.remove(sessionId)?.cancel()
            val job = scope.launch { syncAttention(sessionId) }
            refreshJobs[sessionId] = job
            // Finished jobs must not keep their session id in the map for the process lifetime.
            job.invokeOnCompletion {
                synchronized(refreshJobs) { if (refreshJobs[sessionId] === job) refreshJobs.remove(sessionId) }
            }
        }
    }

    private suspend fun syncAttention(sessionId: String) {
        val permissions = chat.permissions(sessionId).getOrNull() ?: return
        val forms = chat.forms(sessionId).getOrNull() ?: return
        lock.withLock {
            val now = permissions.map { it.id }.toSet() + forms.map { it.id }
            for (gone in pending[sessionId].orEmpty() - now) {
                if (notified.remove(gone)) notifier.cancelAttention(gone)
            }
            if (now.isEmpty()) pending.remove(sessionId) else pending[sessionId] = now
            if (visible.value || sessionId in mutes.muted.value) return
            val enabled = settings.settings.value
            if (!enabled.notifications) return
            val newPermissions = if (enabled.notifyPermissions) permissions.filter { it.id !in notified } else emptyList()
            val newForms = if (enabled.notifyQuestions) forms.filter { it.id !in notified } else emptyList()
            if (newPermissions.isEmpty() && newForms.isEmpty()) return
            val title = titleOf(sessionId)
            if (visible.value || sessionId in mutes.muted.value || !settings.settings.value.notifications) return
            newPermissions.forEach { notifier.permission(title, it) }
            newForms.forEach { notifier.question(title, it) }
            notified += newPermissions.map { it.id } + newForms.map { it.id }
        }
    }

    private suspend fun titleOf(sessionId: String): String =
        _running.value[sessionId]?.title?.takeIf { it.isNotBlank() }
            ?: sessions.cached(sessionId)?.title
            ?: sessions.get(sessionId).getOrNull()?.title.orEmpty()

    private suspend fun pendingSessions(): Set<String> = lock.withLock { pending.keys.toSet() }

    private suspend fun reset() {
        _running.value = emptyMap()
        synchronized(refreshJobs) {
            refreshJobs.values.forEach { it.cancel() }
            refreshJobs.clear()
        }
        lock.withLock {
            notified.forEach(notifier::cancelAttention)
            notified.clear()
            pending.clear()
        }
    }
}
