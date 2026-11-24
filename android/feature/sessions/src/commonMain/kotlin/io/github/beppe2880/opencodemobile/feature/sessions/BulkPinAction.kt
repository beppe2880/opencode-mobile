package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.Session

/** The pin actions the selection menu offers. */
internal enum class BulkPinAction { Pin, Unpin }

/**
 * All pinned selections offer Unpin, all unpinned ones offer Pin, and a mixed selection offers
 * both, so either side can be chosen.
 */
internal fun bulkPinActions(selected: Collection<Session>, pinned: Set<String>): List<BulkPinAction> = when {
    selected.isEmpty() -> emptyList()
    selected.all { it.id in pinned } -> listOf(BulkPinAction.Unpin)
    selected.none { it.id in pinned } -> listOf(BulkPinAction.Pin)
    else -> listOf(BulkPinAction.Pin, BulkPinAction.Unpin)
}
