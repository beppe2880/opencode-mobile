package io.github.beppe2880.opencodemobile.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.MutedSessionRepository
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Redraws placed widgets on [widgetRefreshes]. Otherwise the widget refreshes on its own every
 * 30 minutes or from its refresh button.
 */
class WidgetUpdater(
    private val context: Context,
    private val events: EventRepository,
    private val servers: ServerRepository,
    private val visible: StateFlow<Boolean>,
    private val mutes: MutedSessionRepository,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            widgetRefreshes(events, servers, visible, mutes.muted).collect { updateIfPlaced() }
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
}
