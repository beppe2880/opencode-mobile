package io.github.beppe2880.opencodemobile.widget

import io.github.beppe2880.opencodemobile.core.domain.Session

data class WidgetSession(
    val id: String,
    val title: String,
    val project: String,
    val updatedAt: Long,
    val running: Boolean,
)

/** What the home screen widget shows. */
sealed interface WidgetModel {
    /** No server is paired yet, or none is selected. */
    data object NoServer : WidgetModel

    /** [stale]: the server could not be reached, so these are the sessions the app last saw. */
    data class Sessions(
        val serverName: String,
        val sessions: List<WidgetSession>,
        val stale: Boolean,
    ) : WidgetModel {
        val runningCount: Int get() = sessions.count { it.running }
    }
}

/**
 * Running sessions first, in the order of [recent], then the most recently updated others, up
 * to [limit]. A running session missing from [recent] (older than the loaded page) is left out
 * rather than shown without a title.
 */
fun widgetSessions(recent: List<Session>, running: Set<String>, limit: Int = MAX_WIDGET_SESSIONS): List<WidgetSession> =
    recent
        .filter { it.parentId == null && it.archivedAt == null }
        .sortedWith(compareByDescending<Session> { it.id in running }.thenByDescending { it.updatedAt })
        .take(limit)
        .map { WidgetSession(it.id, it.title, it.directoryName, it.updatedAt, running = it.id in running) }

const val MAX_WIDGET_SESSIONS = 12
