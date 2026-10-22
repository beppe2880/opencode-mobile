package io.github.beppe2880.opencodemobile.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Redraws placed widgets when sessions start, end or are deleted, when the active
 * server changes and when the app goes to the background. Events only arrive while the app is
 * open or the session notifications keep the stream alive; otherwise the widget refreshes on
 * its own every 30 minutes or from its refresh button.
 */
@OptIn(FlowPreview::class)
class WidgetUpdater(
    private val context: Context,
    private val events: EventRepository,
    private val servers: ServerRepository,
    private val visible: StateFlow<Boolean>,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            merge(
                events.events.filter { it.changesSessionList() }.map { },
                servers.activeServer.map { it?.id }.distinctUntilChanged().drop(1).map { },
                visible.drop(1).filter { !it }.map { },
            )
                .debounce(UPDATE_DEBOUNCE_MILLIS)
                .collect { updateIfPlaced() }
        }
    }

    private suspend fun updateIfPlaced() {
        try {
            if (GlanceAppWidgetManager(context).getGlanceIds(SessionsWidget::class.java).isNotEmpty()) {
                SessionsWidget().updateAll(context)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IllegalStateException) {
            // The widget host is unavailable, for example while the launcher restarts.
        }
    }

    private fun ServerEvent.changesSessionList(): Boolean = when (this) {
        is ServerEvent.ExecutionStarted, is ServerEvent.ExecutionEnded -> true
        // Every message updates its session; only deletions change the list.
        is ServerEvent.SessionChanged -> deleted
        else -> false
    }

    private companion object {
        const val UPDATE_DEBOUNCE_MILLIS = 1_000L
    }
}
