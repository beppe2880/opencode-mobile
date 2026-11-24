package io.github.beppe2880.opencodemobile.widget

import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * When the home screen widget should redraw: sessions start, end or are deleted, the active
 * server changes, or the app leaves the screen. [visible] starts with the current state.
 * Events only arrive while the app is open or the session notifications keep the stream alive.
 */
@OptIn(FlowPreview::class)
fun widgetRefreshes(events: EventRepository, servers: ServerRepository, visible: Flow<Boolean>, muted: Flow<Set<String>> = emptyFlow()): Flow<Unit> = merge(
    events.events.filter { it.changesSessionList() }.map { },
    servers.activeServer.map { it?.id }.distinctUntilChanged().drop(1).map { },
    visible.drop(1).filter { !it }.map { },
    muted.drop(1).map { },
).debounce(UPDATE_DEBOUNCE_MILLIS)

private fun ServerEvent.changesSessionList(): Boolean = when (this) {
    is ServerEvent.ExecutionStarted, is ServerEvent.ExecutionEnded -> true
    // Every message updates its session; only deletions change the list.
    is ServerEvent.SessionChanged -> deleted
    else -> false
}

private const val UPDATE_DEBOUNCE_MILLIS = 1_000L
