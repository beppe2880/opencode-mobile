package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.SessionShortcut
import io.github.beppe2880.opencodemobile.core.domain.UnreadSessions

internal enum class SelectionAction(val shortcut: SessionShortcut, val id: String, val event: SessionsEvent) {
    Rename(SessionShortcut.Rename, "rename", SessionsEvent.RenameSelected),
    Mute(SessionShortcut.Mute, "mute", SessionsEvent.SetSelectedMuted(true)),
    Unmute(SessionShortcut.Mute, "unmute", SessionsEvent.SetSelectedMuted(false)),
    MarkUnread(SessionShortcut.Read, "mark_unread", SessionsEvent.MarkSelectedUnread),
    MarkRead(SessionShortcut.Read, "mark_read", SessionsEvent.MarkSelectedRead),
    Pin(SessionShortcut.Pin, "pin", SessionsEvent.SetSelectedPinned(true)),
    Unpin(SessionShortcut.Pin, "unpin", SessionsEvent.SetSelectedPinned(false)),
    Archive(SessionShortcut.Archive, "archive", SessionsEvent.SetSelectedArchived(true)),
    Unarchive(SessionShortcut.Archive, "unarchive", SessionsEvent.SetSelectedArchived(false)),
    Delete(SessionShortcut.Delete, "delete", SessionsEvent.RequestDeleteSelected),
}

/** Resolve exactly one command per pair before deciding where to display it. */
internal fun selectionActions(state: SessionsState, muteSupported: Boolean): List<SelectionAction> = buildList {
    val selected = state.selectedSessions
    if (selected.isEmpty()) return@buildList
    if (selected.size == 1) add(SelectionAction.Rename)
    if (muteSupported) {
        add(if (selected.all { it.id in state.muted }) SelectionAction.Unmute else SelectionAction.Mute)
    }
    if (state.unreadSessions != UnreadSessions.Off) {
        add(if (selected.all(state::isUnread)) SelectionAction.MarkRead else SelectionAction.MarkUnread)
    }
    add(if (selected.all { it.id in state.pinned }) SelectionAction.Unpin else SelectionAction.Pin)
    if (state.archiveSupported) {
        add(if (selected.all { it.archivedAt != null }) SelectionAction.Unarchive else SelectionAction.Archive)
    }
    add(SelectionAction.Delete)
}

internal data class SelectionActionPlacement(val toolbar: List<SelectionAction>, val overflow: List<SelectionAction>)

/** [slots] includes the overflow button whenever there's anything left to show. */
internal fun placeSelectionActions(
    available: List<SelectionAction>,
    preferences: List<SessionShortcut>,
    slots: Int,
): SelectionActionPlacement {
    val preferred = preferences.distinct().mapNotNull { group -> available.firstOrNull { it.shortcut == group } }
    val toolbar = if (preferred.size == available.size && preferred.size <= slots) {
        preferred
    } else {
        preferred.take((slots - 1).coerceAtLeast(0))
    }
    return SelectionActionPlacement(toolbar, available - toolbar.toSet())
}
