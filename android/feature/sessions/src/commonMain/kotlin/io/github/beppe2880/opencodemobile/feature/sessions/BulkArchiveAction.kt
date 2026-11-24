package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.Session

/** The archive actions the selection menu offers. */
internal enum class BulkArchiveAction { Archive, Unarchive }

/**
 * All archived selections offer Unarchive, all active ones offer Archive, and a mixed selection
 * offers both, so either side can be chosen.
 */
internal fun bulkArchiveActions(selected: Collection<Session>): List<BulkArchiveAction> = when {
    selected.isEmpty() -> emptyList()
    selected.all { it.archivedAt != null } -> listOf(BulkArchiveAction.Unarchive)
    selected.none { it.archivedAt != null } -> listOf(BulkArchiveAction.Archive)
    else -> listOf(BulkArchiveAction.Archive, BulkArchiveAction.Unarchive)
}
