package io.github.beppe2880.opencodemobile.notifications

import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.Form
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
) {
    private val _running = MutableStateFlow<Map<String, RunningSession>>(emptyMap())

    /** Running root sessions, in the order they started. */
    val running: StateFlow<Map<String, RunningSession>> = _running.asStateFlow()

    private val lock = Mutex()

    /** Pending request and form ids per session, and those already notified; guarded by [lock]. */
    private val pending = mutableMapOf<String, Set<String>>()
    private val notified = mutableSetOf<String>()
    private val refreshJobs = mutableMapOf<String, Job>()

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(settings.settings.map { it.notifications }, servers.activeServer.map { it?.id }) { on, server ->
                server.takeIf { on }
            }
                .distinctUntilChanged()
                .collectLatest { server ->
                    reset()
                    if (server != null) coroutineScope { follow(this) }
                }
        }
    }

    private suspend fun follow(scope: CoroutineScope) {
        scope.launch {
            combine(_running.map { it.isNotEmpty() }, visible) { running, shown -> running && shown }
                .distinctUntilChanged()
                .filter { it }
                .collect { startService() }
        }
        // Requests that arrived while a screen was open are notified once the app leaves.
        scope.launch {
            visible.drop(1).filter { !it }.collect { (_running.value.keys + pendingSessions()).forEach { refresh(scope, it) } }
        }
        events.events.collect { onEvent(scope, it) }
    }

    private fun onEvent(scope: CoroutineScope, event: ServerEvent) {
        when (event) {
            ServerEvent.Connected -> scope.launch { reconcile(scope) }
            is ServerEvent.ExecutionStarted -> scope.launch { started(event.sessionId) }
            is ServerEvent.ExecutionEnded -> scope.launch { ended(event.sessionId, event.outcome) }
            is ServerEvent.PermissionChanged -> refresh(scope, event.sessionId)
            is ServerEvent.FormsChanged -> refresh(scope, event.sessionId)
            is ServerEvent.ReasoningStarted -> activity(event.sessionId, SessionActivity.Thinking)
            is ServerEvent.TextStarted -> activity(event.sessionId, SessionActivity.Writing)
            is ServerEvent.ToolInputStarted -> activity(event.sessionId, SessionActivity.Tool(event.name))
            is ServerEvent.SessionChanged -> when {
                event.deleted -> scope.launch { forget(event.sessionId) }
                event.sessionId in _running.value -> scope.launch { retitle(event.sessionId) }
            }
            else -> Unit
        }
    }

    private suspend fun started(sessionId: String) {
        val cached = sessions.cached(sessionId)
        if (cached?.parentId != null) return
        _running.update { it + (sessionId to (it[sessionId] ?: RunningSession(sessionId, cached?.title.orEmpty()))) }
        notifier.cancelFinished(sessionId)
        retitle(sessionId)
    }

    /** Refreshes the title (generated after the first prompt) and drops subagent sessions. */
    private suspend fun retitle(sessionId: String) {
        val session = sessions.get(sessionId).getOrNull() ?: return
        _running.update { running ->
            val current = running[sessionId] ?: return@update running
            if (session.parentId != null) running - sessionId else running + (sessionId to current.copy(title = session.title))
        }
    }

    private suspend fun ended(sessionId: String, outcome: SessionOutcome) {
        val session = _running.value[sessionId]
        _running.update { it - sessionId }
        if (session != null && !visible.value && settings.settings.value.notifyFinished) {
            notifier.finished(sessionId, session.title, outcome)
        }
    }

    private suspend fun forget(sessionId: String) {
        _running.update { it - sessionId }
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
    private suspend fun reconcile(scope: CoroutineScope) {
        val active = sessions.active().getOrNull() ?: return
        for (id in _running.value.keys - active) {
            val outcome = sessions.get(id).getOrNull()?.outcome ?: SessionOutcome.Succeeded
            ended(id, outcome)
        }
        for (id in active - _running.value.keys) started(id)
        (active + pendingSessions()).forEach { refresh(scope, it) }
    }

    /** Re-reads the pending permissions and questions of a session; a newer call replaces an older one. */
    private fun refresh(scope: CoroutineScope, sessionId: String) {
        synchronized(refreshJobs) {
            refreshJobs.remove(sessionId)?.cancel()
            refreshJobs[sessionId] = scope.launch { syncAttention(sessionId) }
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
            if (visible.value) return
            val enabled = settings.settings.value
            val newPermissions = if (enabled.notifyPermissions) permissions.filter { it.id !in notified } else emptyList()
            val newForms = if (enabled.notifyQuestions) forms.filter { it.id !in notified } else emptyList()
            if (newPermissions.isEmpty() && newForms.isEmpty()) return
            val title = titleOf(sessionId)
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
        lock.withLock {
            notified.forEach(notifier::cancelAttention)
            notified.clear()
            pending.clear()
        }
    }
}
