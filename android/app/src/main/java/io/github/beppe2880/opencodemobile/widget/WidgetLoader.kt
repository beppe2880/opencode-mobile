package io.github.beppe2880.opencodemobile.widget

import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Reads the widget's sessions from the active server, falling back to the ones stored on the device. */
class WidgetLoader(
    private val servers: ServerRepository,
    private val sessions: SessionRepository,
    private val timeoutMillis: Long = LOAD_TIMEOUT_MILLIS,
) {
    suspend fun load(): WidgetModel {
        // The widget can update in a fresh process, before the saved servers are read.
        servers.loaded.first { it }
        val server = servers.activeServer.value ?: return WidgetModel.NoServer
        val fresh = withTimeoutOrNull(timeoutMillis) {
            coroutineScope {
                val running = async { sessions.active() }
                val recent = sessions.list(limit = LIST_LIMIT)
                recent.getOrNull()?.let { list -> list to running.await().getOrDefault(emptySet()) }
            }
        }
        if (fresh != null) {
            val (recent, running) = fresh
            return WidgetModel.Sessions(server.name, widgetSessions(recent, running), stale = false)
        }
        val cached = sessions.cachedList(server.id).orEmpty()
        // Without the server nothing is known to be running.
        return WidgetModel.Sessions(server.name, widgetSessions(cached, emptySet()), stale = true)
    }

    private companion object {
        const val LIST_LIMIT = 30
        const val LOAD_TIMEOUT_MILLIS = 15_000L
    }
}
