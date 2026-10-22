package io.github.beppe2880.opencodemobile.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.beppe2880.opencodemobile.MainActivity
import io.github.beppe2880.opencodemobile.R
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.core.ui.theme.OcColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcDarkColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcLightColors
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Home screen widget: running and recent sessions of the active server, and a new session button. */
class SessionsWidget :
    GlanceAppWidget(),
    KoinComponent {
    private val loader: WidgetLoader by inject()
    private val settings: SettingsRepository by inject()

    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, LIST))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = loader.load()
        val colors = WidgetColors.forTheme(settings.settings.value.theme)
        val updatedAt = System.currentTimeMillis()
        provideContent { WidgetContent(model, colors, updatedAt) }
    }

    companion object {
        val COMPACT = DpSize(180.dp, 56.dp)
        val LIST = DpSize(180.dp, 150.dp)
    }
}

class SessionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SessionsWidget()
}

/** The refresh button: loads the sessions again. */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        SessionsWidget().update(context, glanceId)
    }
}

/** App colors for the widget; with the System theme they follow the launcher's light or dark mode. */
internal class WidgetColors(
    val background: ColorProvider,
    val surface: ColorProvider,
    val text: ColorProvider,
    val muted: ColorProvider,
    val accent: ColorProvider,
    val onAccent: ColorProvider,
    val running: ColorProvider,
) {
    companion object {
        fun forTheme(theme: ThemeMode): WidgetColors {
            val day = if (theme == ThemeMode.Dark) OcDarkColors else OcLightColors
            val night = if (theme == ThemeMode.Light) OcLightColors else OcDarkColors
            fun pick(token: (OcColors) -> Color) = ColorProvider(day = token(day), night = token(night))
            return WidgetColors(
                background = pick { it.bgBase },
                surface = pick { it.layer02 },
                text = pick { it.textBase },
                muted = pick { it.textMuted },
                accent = pick { it.bgAccent },
                onAccent = pick { it.onAccent },
                running = pick { it.success.fg },
            )
        }
    }
}

@Composable
private fun WidgetContent(model: WidgetModel, colors: WidgetColors, updatedAt: Long) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Header(model, colors)
        if (LocalSize.current.height >= SessionsWidget.LIST.height) {
            Spacer(GlanceModifier.height(8.dp))
            when (model) {
                WidgetModel.NoServer -> Message(context.getString(R.string.widget_no_server), colors, openApp(context))
                is WidgetModel.Sessions ->
                    if (model.sessions.isEmpty()) {
                        Message(context.getString(R.string.widget_empty), colors, newSession(context))
                    } else {
                        LazyColumn(GlanceModifier.fillMaxSize()) {
                            items(model.sessions, itemId = { it.id.hashCode().toLong() }) { session ->
                                SessionRow(session, colors, updatedAt)
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun Header(model: WidgetModel, colors: WidgetColors) {
    val context = LocalContext.current
    val subtitle = when (model) {
        WidgetModel.NoServer -> context.getString(R.string.widget_not_connected)
        is WidgetModel.Sessions -> when {
            model.stale -> context.getString(R.string.widget_offline, model.serverName)
            model.runningCount > 0 ->
                context.resources.getQuantityString(R.plurals.widget_running, model.runningCount, model.serverName, model.runningCount)
            else -> model.serverName
        }
    }
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(GlanceModifier.defaultWeight().clickable(openApp(context))) {
            Text(
                text = context.getString(R.string.widget_title),
                style = TextStyle(color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Text(text = subtitle, style = TextStyle(color = colors.muted, fontSize = 12.sp), maxLines = 1)
        }
        if (model is WidgetModel.Sessions) {
            IconButton(R.drawable.ic_widget_refresh, context.getString(R.string.widget_refresh), colors.muted, actionRunCallback<RefreshWidgetAction>())
            Spacer(GlanceModifier.width(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier
                    .height(36.dp)
                    .background(colors.accent)
                    .cornerRadius(18.dp)
                    .padding(horizontal = 12.dp)
                    .clickable(newSession(context)),
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.onAccent),
                    modifier = GlanceModifier.size(18.dp),
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = context.getString(R.string.widget_new_session),
                    style = TextStyle(color = colors.onAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun IconButton(icon: Int, description: String, tint: ColorProvider, onClick: Action) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.size(36.dp).cornerRadius(18.dp).clickable(onClick).semantics { contentDescription = description },
    ) {
        Image(provider = ImageProvider(icon), contentDescription = description, colorFilter = ColorFilter.tint(tint), modifier = GlanceModifier.size(20.dp))
    }
}

@Composable
private fun SessionRow(session: WidgetSession, colors: WidgetColors, now: Long) {
    val context = LocalContext.current
    Column(GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(colors.surface)
                .cornerRadius(10.dp)
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .clickable(openSession(context, session.id)),
        ) {
            if (session.running) {
                Box(GlanceModifier.size(8.dp).background(colors.running).cornerRadius(4.dp)) {}
                Spacer(GlanceModifier.width(8.dp))
            }
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = session.title.ifBlank { context.getString(R.string.widget_untitled) },
                    style = TextStyle(color = colors.text, fontSize = 14.sp),
                    maxLines = 1,
                )
                val detail = if (session.running) context.getString(R.string.widget_session_running) else relativeTime(session.updatedAt, now)
                Text(
                    text = "${session.project} · $detail",
                    style = TextStyle(color = colors.muted, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Message(text: String, colors: WidgetColors, onClick: Action) {
    Box(GlanceModifier.fillMaxSize().clickable(onClick), contentAlignment = Alignment.Center) {
        Text(text = text, style = TextStyle(color = colors.muted, fontSize = 13.sp))
    }
}

private fun relativeTime(time: Long, now: Long = System.currentTimeMillis()): String =
    DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()

// Each target has its own data URI: pending intents that differ only in extras would be merged.
private fun openApp(context: Context): Action = actionStartActivity(Intent(context, MainActivity::class.java))

private fun openSession(context: Context, sessionId: String): Action = actionStartActivity(
    Intent(context, MainActivity::class.java)
        .setData(Uri.parse("opencode-widget://session/$sessionId"))
        .putExtra(MainActivity.EXTRA_SESSION_ID, sessionId),
)

private fun newSession(context: Context): Action = actionStartActivity(
    Intent(context, MainActivity::class.java)
        .setData(Uri.parse("opencode-widget://new-session"))
        .putExtra(MainActivity.EXTRA_NEW_SESSION, true),
)
