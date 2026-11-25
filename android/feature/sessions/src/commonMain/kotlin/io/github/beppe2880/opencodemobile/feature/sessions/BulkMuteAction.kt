package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.Session

/** The mute actions the selection menu offers. */
internal enum class BulkMuteAction { Mute, Unmute }

/**
 * All muted selections offer Unmute, all unmuted ones offer Mute, and a mixed selection offers
 * both, so either side can be chosen, like Mark as read and Mark as unread.
 */
internal fun bulkMuteActions(selected: Collection<Session>, muted: Set<String>): List<BulkMuteAction> = when {
    selected.isEmpty() -> emptyList()
    selected.all { it.id in muted } -> listOf(BulkMuteAction.Unmute)
    selected.none { it.id in muted } -> listOf(BulkMuteAction.Mute)
    else -> listOf(BulkMuteAction.Mute, BulkMuteAction.Unmute)
}
