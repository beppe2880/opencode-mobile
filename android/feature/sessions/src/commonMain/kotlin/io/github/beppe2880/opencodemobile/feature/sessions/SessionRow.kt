package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.RunningWork
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcProjectAvatar
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.text.relativeTime
import io.github.beppe2880.opencodemobile.core.ui.text.runningSummary
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.sessions.resources.Res
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_delete
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_failed
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_interrupted
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_rename
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_untitled
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SessionRow(
    session: Session,
    running: Boolean,
    selected: Boolean,
    now: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeDelete: () -> Unit,
    modifier: Modifier = Modifier,
    /** A narrow list (beside the chat): the time moves under the title so the title keeps the width. */
    compact: Boolean = false,
    work: RunningWork = RunningWork(),
) {
    val c = OcTheme.colors
    val currentOnDelete by rememberUpdatedState(onSwipeDelete)
    // Only a settled swipe asks for confirmation (onDismiss reacts to the settled value, so a
    // drag crossing back does not cancel anything), then the row slides back: a cancelled
    // delete leaves it in place and swipeable again.
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val renameLabel = stringResource(Res.string.sessions_rename)
    val deleteLabel = stringResource(Res.string.sessions_delete)
    val runningLabel = runningSummary(work, running)
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        onDismiss = {
            currentOnDelete()
            scope.launch { dismissState.reset() }
        },
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(c.danger.bg).padding(horizontal = OcSpacing.xl),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = c.danger.fg)
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) c.layer02 else c.bgBase)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = renameLabel)
                .semantics {
                    if (running || !work.isEmpty) stateDescription = runningLabel
                    customActions = listOf(
                        CustomAccessibilityAction(renameLabel) { onLongClick(); true },
                        CustomAccessibilityAction(deleteLabel) { onSwipeDelete(); true },
                    )
                }
                .heightIn(min = 56.dp)
                .padding(horizontal = OcSpacing.gutter, vertical = OcSpacing.sm)
                .testTag("session_row_${session.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            OcProjectAvatar(session.directoryName)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.displayTitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (session.title.isBlank()) c.textMuted else c.textBase,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (compact) session.directoryName + " · " + relativeTime(session.updatedAt, now) else session.directoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    OutcomeBadge(session.outcome, running)
                }
                if (running || !work.isEmpty) {
                    Text(
                        runningLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.info.fg,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("session_work_${session.id}"),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!compact) {
                    Text(
                        text = relativeTime(session.updatedAt, now),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint,
                        maxLines = 1,
                    )
                }
                if (running) OcPulsingDot(c.info.fg, Modifier.testTag("session_running_${session.id}"))
            }
        }
    }
}

@Composable
private fun OutcomeBadge(outcome: SessionOutcome?, running: Boolean) {
    if (running) return
    val c = OcTheme.colors
    when (outcome) {
        SessionOutcome.Failed -> OcBadge(stringResource(Res.string.sessions_failed), c.danger)
        SessionOutcome.Interrupted -> OcBadge(stringResource(Res.string.sessions_interrupted), c.warning)
        else -> Unit
    }
}

/** The session's title, or a placeholder while the server has not named it yet. */
@Composable
internal fun Session.displayTitle(): String = title.ifBlank { stringResource(Res.string.sessions_untitled) }
