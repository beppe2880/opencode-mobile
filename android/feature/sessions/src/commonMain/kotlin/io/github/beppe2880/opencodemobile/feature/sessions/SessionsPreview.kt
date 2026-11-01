package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.DEFAULT_LIST_PANE_FRACTION
import io.github.beppe2880.opencodemobile.core.domain.LIST_PANE_FRACTION_RANGE
import io.github.beppe2880.opencodemobile.core.domain.NewChatModel
import io.github.beppe2880.opencodemobile.core.domain.RunningWork
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.domain.ShareOpens
import io.github.beppe2880.opencodemobile.core.ui.components.OcCustomizable
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.PartPreview
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.sessions.resources.Res
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_preview_back_leave
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_preview_back_list
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_preview_share_choose
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_preview_share_new
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_preview_share_off
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_preview_split_off
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_preview_swipe_off
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_preview_swipe_on
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_preview_wide
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The session list for Settings: real rows from the sample sessions, one running with work in the background. */
@Composable
fun SessionsPreview(preview: PartPreview, modifier: Modifier = Modifier) {
    val s = preview.settings
    Column(modifier.fillMaxWidth().padding(vertical = OcSpacing.xs).testTag("sessions_preview")) {
        OcCustomizable(SettingsPart.SessionRows, preview, Modifier.padding(horizontal = OcSpacing.sm)) {
            SampleRows(work = s.showRunningWorkInList)
        }
    }
}

/** How swiping moves between chats. */
@Composable
fun SwipeChatsPreview(preview: PartPreview, modifier: Modifier = Modifier) {
    OcCustomizable(SettingsPart.SwipeChats, preview, modifier.padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs)) {
        SwipeSketch(preview.settings.swipeBetweenChats)
    }
}

/** A sketch of the list beside the chat on wide screens that follows its width, handle and Back settings. */
@Composable
fun SplitViewPreview(preview: PartPreview, modifier: Modifier = Modifier) {
    val s = preview.settings
    OcCustomizable(SettingsPart.SplitView, preview, modifier.padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs)) {
        SplitSketch(
            split = s.splitView,
            fraction = (s.listPaneFraction ?: DEFAULT_LIST_PANE_FRACTION).coerceIn(LIST_PANE_FRACTION_RANGE),
            handle = s.listPaneHandle,
            backShowsList = s.backShowsList,
        )
    }
}

/** The new chat sheet with the model it starts with: the last used one or the server's default. */
@Composable
fun NewChatPreview(preview: PartPreview, modifier: Modifier = Modifier) {
    OcCustomizable(SettingsPart.NewChat, preview, modifier.padding(OcSpacing.sm).testTag("new_chat_preview")) {
        SampleNewSessionForm(lastUsed = preview.settings.newChatModel == NewChatModel.LastUsed)
    }
}

/** What sharing to OpenCode from another app opens: the new chat sheet or the session list, or nothing. */
@Composable
fun SharingPreview(preview: PartPreview, modifier: Modifier = Modifier) {
    val s = preview.settings
    OcCustomizable(SettingsPart.ShareTarget, preview, modifier.padding(OcSpacing.sm).testTag("sharing_preview")) {
        Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
            Caption(
                when {
                    !s.showInShareMenu -> Res.string.sessions_preview_share_off
                    s.shareOpens == ShareOpens.ChooseChat -> Res.string.sessions_preview_share_choose
                    else -> Res.string.sessions_preview_share_new
                },
                Modifier.padding(start = OcSpacing.md, end = OcSpacing.md, top = OcSpacing.sm),
            )
            when {
                !s.showInShareMenu -> Unit
                s.shareOpens == ShareOpens.ChooseChat -> SampleRows(work = false)
                else -> SampleNewSessionForm(lastUsed = s.newChatModel == NewChatModel.LastUsed)
            }
        }
    }
}

@Composable
private fun SampleRows(work: Boolean) {
    Column {
        SampleData.sessions.take(3).forEachIndexed { i, session ->
            if (i > 0) OcDivider()
            SessionRow(
                session = session,
                running = i == 0,
                selected = false,
                now = SampleData.NOW,
                onClick = {},
                onLongClick = {},
                onSwipeDelete = {},
                work = if (!work) null else if (i == 0) SampleData.runningWork else RunningWork(),
            )
        }
    }
}

@Composable
private fun SampleNewSessionForm(lastUsed: Boolean) {
    val projects = SampleData.projects.take(2)
    val state = NewSessionState(
        directory = projects.first().directory,
        agents = SampleData.agents.filter { !it.hidden },
        models = SampleData.models,
        recentModels = listOf(SampleData.sonnet),
        agent = "build",
        defaultModel = if (lastUsed) SampleData.sonnet else null,
    )
    NewSessionForm(state, projects, onEvent = {}, inSheet = false)
}

/** The open chat between its neighbors in the list, which a swipe brings in. */
@Composable
private fun SwipeSketch(on: Boolean) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.md)
    val sessions = SampleData.sessions
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
            val peek = if (on) 1f else 0.25f
            MiniChat(sessions[0].title, Modifier.weight(0.6f).alpha(peek))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = if (on) c.textAccent else c.iconMuted, modifier = Modifier.alpha(peek))
            MiniChat(sessions[1].title, Modifier.weight(1f).border(1.dp, c.borderStrong, shape))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = if (on) c.textAccent else c.iconMuted, modifier = Modifier.alpha(peek))
            MiniChat(sessions[2].title, Modifier.weight(0.6f).alpha(peek))
        }
        Caption(if (on) Res.string.sessions_preview_swipe_on else Res.string.sessions_preview_swipe_off)
    }
}

@Composable
private fun MiniChat(title: String, modifier: Modifier) {
    val c = OcTheme.colors
    Column(
        modifier.background(c.layer01, RoundedCornerShape(OcRadius.md)).padding(OcSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.xxs),
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = c.textBase, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.align(Alignment.End).fillMaxWidth(0.6f).height(8.dp).background(c.userBubbleBg, RoundedCornerShape(2.dp)))
        Box(Modifier.fillMaxWidth(0.9f).height(4.dp).background(c.layer04, RoundedCornerShape(2.dp)))
        Box(Modifier.fillMaxWidth(0.7f).height(4.dp).background(c.layer04, RoundedCornerShape(2.dp)))
    }
}

/** A wide window: the list at [fraction] of the width beside the chat, the divider handle, and what Back does. */
@Composable
private fun SplitSketch(split: Boolean, fraction: Float, handle: Boolean, backShowsList: Boolean) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.md)
    val listWidth by animateFloatAsState(if (split) fraction else 0f, label = "split_list")
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
        Caption(Res.string.sessions_preview_wide)
        Row(
            Modifier
                .fillMaxWidth()
                .height(88.dp)
                .border(1.dp, c.borderStrong, shape)
                .padding(OcSpacing.xxs),
        ) {
            if (listWidth > 0.01f) {
                Column(Modifier.weight(listWidth).fillMaxHeight().padding(OcSpacing.xxs), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    repeat(4) { i ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xxs)) {
                            Box(Modifier.size(8.dp).background(c.layer04, RoundedCornerShape(2.dp)))
                            Box(Modifier.fillMaxWidth(if (i % 2 == 0) 0.9f else 0.6f).height(4.dp).background(c.layer04, RoundedCornerShape(2.dp)))
                        }
                    }
                }
                Box(Modifier.fillMaxHeight().width(1.dp).background(c.borderStrong), contentAlignment = Alignment.Center) {
                    if (handle) {
                        Box(
                            Modifier.size(16.dp).background(c.bgBase, CircleShape).border(1.dp, c.borderStrong, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.ChevronLeft, null, tint = c.iconMuted, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
            Column(Modifier.weight(1f - listWidth).fillMaxHeight().padding(OcSpacing.xs), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                Box(Modifier.align(Alignment.End).fillMaxWidth(0.5f).height(10.dp).background(c.userBubbleBg, RoundedCornerShape(2.dp)))
                Box(Modifier.fillMaxWidth(0.9f).height(4.dp).background(c.layer04, RoundedCornerShape(2.dp)))
                Box(Modifier.fillMaxWidth(0.8f).height(4.dp).background(c.layer04, RoundedCornerShape(2.dp)))
                Box(Modifier.fillMaxWidth().height(14.dp).border(1.dp, c.borderStrong, RoundedCornerShape(4.dp)))
            }
        }
        when {
            !split -> Caption(Res.string.sessions_preview_split_off)
            handle -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = c.iconMuted, modifier = Modifier.padding(start = OcSpacing.xs).size(14.dp))
                Caption(if (backShowsList) Res.string.sessions_preview_back_list else Res.string.sessions_preview_back_leave)
            }
        }
    }
}

@Composable
private fun Caption(text: StringResource, modifier: Modifier = Modifier) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodySmall,
        color = OcTheme.colors.textMuted,
        modifier = modifier.padding(horizontal = OcSpacing.xs),
    )
}
