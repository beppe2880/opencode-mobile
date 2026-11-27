package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.RunningWork
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.SessionShortcut
import io.github.beppe2880.opencodemobile.core.domain.SharedContent
import io.github.beppe2880.opencodemobile.core.domain.UnreadSessions
import io.github.beppe2880.opencodemobile.core.ui.text.UiText

/**
 * Groups of the list, in the order shown. [Pinned] holds the pinned sessions of any day, [Unread] the
 * unread sessions that are not pinned, both above the days.
 */
enum class DayBucket { Pinned, Unread, Today, Yesterday, ThisWeek, Older }

@Immutable
data class SessionGroup(val bucket: DayBucket, val sessions: List<Session>)

@Immutable
data class FolderBrowserState(
    val path: String = "",
    val parent: String? = null,
    val folders: List<String> = emptyList(),
    val loading: Boolean = true,
    val error: UiText? = null,
    val recent: List<String> = emptyList(),
    /** The folder the browser opened at once listed; back stops there and recent folders show there. */
    val start: String? = null,
) {
    /** Recent folders are offered here: at the start folder, or while it is not listed yet. */
    val atStart: Boolean get() = start == null || path == start

    /** Where back goes: one folder up until the start folder or a root, then null (back closes the browser). */
    val backTo: String? get() = parent?.takeIf { path != start }
}

@Immutable
data class NewSessionState(
    val directory: String = "",
    val customDirectory: Boolean = false,
    /** Open while the user browses the server's folders. */
    val browser: FolderBrowserState? = null,
    val agents: List<Agent> = emptyList(),
    val models: List<ModelInfo> = emptyList(),
    /** Models last used on this device, then those of the most recently updated sessions; listed first in the picker. */
    val recentModels: List<ModelRef> = emptyList(),
    val agent: String? = null,
    /** Set explicitly in the server configuration; null when not set or not readable. */
    val configuredModel: ModelRef? = null,
    /** Picked by the user; null when not picked. */
    val model: ModelRef? = null,
    /** The model used when none is picked (Settings: model for new chats); null lets the server decide. */
    val defaultModel: ModelRef? = null,
    val pickingModel: Boolean = false,
    val creating: Boolean = false,
    val error: UiText? = null,
    /** Shared from another app: the new chat's composer gets it. */
    val shared: SharedContent? = null,
) {
    val canCreate: Boolean get() = directory.isNotBlank() && !creating

    /** The model the new chat starts with; null lets the server decide. */
    val effectiveModel: ModelRef? get() = model ?: defaultModel
}

/** What a running session waits for; a permission request wins over a question. */
enum class SessionWaiting { Permission, Answer }

@Immutable
data class SessionsState(
    val serverName: String = "",
    val connection: ConnectionState = ConnectionState.Disconnected,
    val query: String = "",
    val projects: List<Project> = emptyList(),
    val hiddenDirectories: Set<String> = emptySet(),
    val directoryFilter: String? = null,
    /** The filters chosen in the filter sheet. */
    val filters: SessionFilters = SessionFilters(),
    /** False on servers before OpenCode 2.1.0: archive and the archived filter are hidden. */
    val archiveSupported: Boolean = true,
    val filterSheetOpen: Boolean = false,
    val groups: List<SessionGroup> = emptyList(),
    /** Sessions pinned on this server; those listed are in the [DayBucket.Pinned] group. */
    val pinned: Set<String> = emptySet(),
    val muted: Set<String> = emptySet(),
    val running: Set<String> = emptySet(),
    val work: Map<String, RunningWork> = emptyMap(),
    /** Running sessions blocked on the user (their own requests or their subagents'). */
    val waiting: Map<String, SessionWaiting> = emptyMap(),
    /** Rows show running subagents and commands (setting). */
    val showRunningWork: Boolean = true,
    /** How unread sessions are marked and grouped (setting). */
    val unreadSessions: UnreadSessions = UnreadSessions.DotAndGroup,
    val sessionShortcuts: List<SessionShortcut> = SessionShortcut.defaults,
    /** Marking every listed unread session as read is under way. */
    val markingAllRead: Boolean = false,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: UiText? = null,
    val newSession: NewSessionState? = null,
    /**
     * Shared from another app, waiting for the user to pick its chat (Settings: sharing opens
     * Choose a chat): tapping a session sends it there, a new session takes it too.
     */
    val sharing: SharedContent? = null,
    /** Sessions selected for bulk actions; empty outside selection mode. */
    val selection: Set<String> = emptySet(),
    /** Sessions marked unread by hand on this device, on top of the server's unread state. */
    val manualUnread: Set<String> = emptySet(),
    /** How many sessions the bulk delete confirmation covers; null when it is closed. */
    val bulkDeleteCount: Int? = null,
    /** Sessions a bulk delete could not remove; shown in a dialog until dismissed. */
    val deleteFailures: List<Session> = emptyList(),
    val renaming: Session? = null,
    val deleting: Session? = null,
) {
    val isEmpty: Boolean get() = groups.isEmpty()

    /**
     * The unread sessions the list shows, those [SessionsEvent.MarkAllRead] marks; none while unread
     * state is off. Sessions waiting for a permission or an answer are left out: marking them read
     * would hide that they need the user.
     */
    val unreadShown: List<Session>
        get() = if (unreadSessions == UnreadSessions.Off) {
            emptyList()
        } else {
            groups.flatMap { it.sessions }.filter { isUnread(it) && it.id !in waiting }
        }

    /** The sessions of the Unread group, those [SessionsEvent.MarkUnreadGroupRead] marks. */
    val unreadGroup: List<Session> get() = groups.firstOrNull { it.bucket == DayBucket.Unread }?.sessions.orEmpty()

    /** Whether [session] counts as unread here: the server says so, or it was marked unread on this device. */
    fun isUnread(session: Session): Boolean = session.isUnread || session.id in manualUnread
    val visibleProjects: List<Project> get() = projects.filter { it.directory !in hiddenDirectories }
    val filterLabel: String? get() = directoryFilter?.let { dir -> projects.firstOrNull { it.directory == dir }?.displayName ?: dir }

    /** The selected sessions of the list shown, in list order. */
    val selectedSessions: List<Session>
        get() = groups.flatMap { it.sessions }.filter { it.id in selection }

    /** Selection mode is on while at least one visible session is selected. */
    val selectionMode: Boolean get() = selectedSessions.isNotEmpty()
}

sealed interface SessionsEvent {
    data class Visible(val visible: Boolean) : SessionsEvent
    data object Refresh : SessionsEvent
    data class QueryChanged(val query: String) : SessionsEvent
    data class FilterChanged(val directory: String?) : SessionsEvent
    data object OpenFilters : SessionsEvent
    data object DismissFilters : SessionsEvent

    /** Adds or removes a status from the list; reloads it. */
    data class ToggleStatus(val status: SessionStatus) : SessionsEvent
    data class SetDateFilter(val filter: SessionDateFilter) : SessionsEvent
    data class ToggleOutcome(val outcome: SessionOutcome) : SessionsEvent

    /** Narrows the list to [state], or clears the filter when it is already picked. */
    data class ToggleRead(val state: SessionRead) : SessionsEvent

    /** Marks every unread session the list shows (filters and search applied) as viewed. */
    data object MarkAllRead : SessionsEvent

    /** Marks exactly the sessions of the Unread group as viewed, the count its header shows. */
    data object MarkUnreadGroupRead : SessionsEvent
    data object ResetFilters : SessionsEvent

    data class HideProject(val directory: String) : SessionsEvent
    data class UnhideProject(val directory: String) : SessionsEvent
    data class Open(val sessionId: String) : SessionsEvent
    data object OpenSettings : SessionsEvent

    /** Pins the session to the top of the list or unpins it. */
    data class SetPinned(val sessionId: String, val pinned: Boolean) : SessionsEvent
    data class SetMuted(val sessionId: String, val muted: Boolean, val untilFinished: Boolean = false) : SessionsEvent

    /** Mutes or unmutes every selected session, permanently. */
    data class SetSelectedMuted(val muted: Boolean) : SessionsEvent

    /** Archives the session or restores it. */
    data class SetArchived(val session: Session, val archived: Boolean) : SessionsEvent
    data class ConfirmRename(val title: String) : SessionsEvent
    data class RequestDelete(val session: Session) : SessionsEvent
    data object ConfirmDelete : SessionsEvent
    data object DismissDialog : SessionsEvent

    /** Starts selection mode with this session selected. */
    data class StartSelection(val sessionId: String) : SessionsEvent

    /** Adds or removes a session from the selection; an empty selection leaves selection mode. */
    data class ToggleSelection(val sessionId: String) : SessionsEvent
    data object ExitSelection : SessionsEvent

    /** Pins or unpins every selected session, then leaves selection mode. */
    data class SetSelectedPinned(val pinned: Boolean) : SessionsEvent

    /** Archives or restores every selected session, then leaves selection mode. */
    data class SetSelectedArchived(val archived: Boolean) : SessionsEvent

    /** Marks every selected session read, also on the server; leaves selection mode. */
    data object MarkSelectedRead : SessionsEvent

    /** Marks every selected session unread on this device; leaves selection mode. */
    data object MarkSelectedUnread : SessionsEvent

    /** Opens the rename dialog for the only selected session. */
    data object RenameSelected : SessionsEvent

    /** Asks to delete the selected sessions; one saves its title, several show the count. */
    data object RequestDeleteSelected : SessionsEvent

    /** Closes the list of sessions a bulk delete could not remove. */
    data object DismissDeleteFailures : SessionsEvent

    data object StartNewSession : SessionsEvent
    data object DismissNewSession : SessionsEvent
    data class NewSessionDirectory(val directory: String, val custom: Boolean) : SessionsEvent
    data object BrowseFolders : SessionsEvent

    /** Null opens the server's own folder. */
    data class BrowseTo(val path: String?) : SessionsEvent
    data object CloseFolderBrowser : SessionsEvent
    data object PickBrowsedFolder : SessionsEvent

    /** Picks [path] (e.g. a recent folder) without browsing to it first. */
    data class PickFolder(val path: String) : SessionsEvent

    data class NewSessionAgent(val agent: String) : SessionsEvent
    data class NewSessionModelPicker(val open: Boolean) : SessionsEvent
    data class NewSessionModel(val model: ModelRef) : SessionsEvent
    data object CreateSession : SessionsEvent

    /** Drops the content shared from another app. */
    data object DiscardShare : SessionsEvent
}

sealed interface SessionsEffect {
    data class OpenChat(val sessionId: String) : SessionsEffect
    data object OpenSettings : SessionsEffect
    data class ShowMessage(val message: UiText) : SessionsEffect

    /** Shows the archive/restore confirmation with an Undo action. */
    data class ShowArchiveUndo(val session: Session, val archived: Boolean) : SessionsEffect
}
