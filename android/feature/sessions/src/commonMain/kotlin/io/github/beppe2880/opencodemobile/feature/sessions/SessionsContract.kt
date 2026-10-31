package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.ui.text.UiText

enum class DayBucket { Today, Yesterday, ThisWeek, Older }

@Immutable
data class SessionGroup(val bucket: DayBucket, val sessions: List<Session>)

@Immutable
data class NewSessionState(
    val directory: String = "",
    val customDirectory: Boolean = false,
    val agents: List<Agent> = emptyList(),
    val models: List<ModelInfo> = emptyList(),
    /** Models last used on this device, then those of the most recently updated sessions; listed first in the picker. */
    val recentModels: List<ModelRef> = emptyList(),
    val agent: String? = null,
    /** Picked by the user; null when not picked. */
    val model: ModelRef? = null,
    /** The model used when none is picked (Settings: model for new chats); null lets the server decide. */
    val defaultModel: ModelRef? = null,
    val pickingModel: Boolean = false,
    val creating: Boolean = false,
    val error: UiText? = null,
) {
    val canCreate: Boolean get() = directory.isNotBlank() && !creating

    /** The model the new chat starts with; null lets the server decide. */
    val effectiveModel: ModelRef? get() = model ?: defaultModel
}

@Immutable
data class SessionsState(
    val serverName: String = "",
    val connection: ConnectionState = ConnectionState.Disconnected,
    val query: String = "",
    val projects: List<Project> = emptyList(),
    val directoryFilter: String? = null,
    val groups: List<SessionGroup> = emptyList(),
    val running: Set<String> = emptySet(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: UiText? = null,
    val newSession: NewSessionState? = null,
    val actionsFor: Session? = null,
    val renaming: Session? = null,
    val deleting: Session? = null,
) {
    val isEmpty: Boolean get() = groups.isEmpty()
    val filterLabel: String? get() = directoryFilter?.let { dir -> projects.firstOrNull { it.directory == dir }?.displayName ?: dir }
}

sealed interface SessionsEvent {
    data object Refresh : SessionsEvent
    data class QueryChanged(val query: String) : SessionsEvent
    data class FilterChanged(val directory: String?) : SessionsEvent
    data class Open(val sessionId: String) : SessionsEvent
    data object OpenSettings : SessionsEvent

    data class ShowActions(val session: Session) : SessionsEvent
    data object DismissActions : SessionsEvent
    data class RequestRename(val session: Session) : SessionsEvent
    data class ConfirmRename(val title: String) : SessionsEvent
    data class RequestDelete(val session: Session) : SessionsEvent
    data object ConfirmDelete : SessionsEvent
    data object DismissDialog : SessionsEvent

    data object StartNewSession : SessionsEvent
    data object DismissNewSession : SessionsEvent
    data class NewSessionDirectory(val directory: String, val custom: Boolean) : SessionsEvent
    data class NewSessionAgent(val agent: String) : SessionsEvent
    data class NewSessionModelPicker(val open: Boolean) : SessionsEvent
    data class NewSessionModel(val model: ModelRef) : SessionsEvent
    data object CreateSession : SessionsEvent
}

sealed interface SessionsEffect {
    data class OpenChat(val sessionId: String) : SessionsEffect
    data object OpenSettings : SessionsEffect
    data class ShowMessage(val message: UiText) : SessionsEffect
}
