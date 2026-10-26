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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcProjectAvatar
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.text.relativeTime
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import kotlinx.coroutines.launch

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
) {
    val c = OcTheme.colors
    val currentOnDelete by rememberUpdatedState(onSwipeDelete)
    // Only a settled swipe asks for confirmation (onDismiss reacts to the settled value, so a
    // drag crossing back does not cancel anything), then the row slides back: a cancelled
    // delete leaves it in place and swipeable again.
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val renameLabel = stringResource(R.string.sessions_rename)
    val deleteLabel = stringResource(R.string.sessions_delete)
    val runningLabel = stringResource(R.string.sessions_running_a11y)
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
                    if (running) stateDescription = runningLabel
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
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textBase,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = session.directoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    OutcomeBadge(session.outcome, running)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = relativeTime(session.updatedAt, now),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint,
                    maxLines = 1,
                )
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
        SessionOutcome.Failed -> OcBadge(stringResource(R.string.sessions_failed), c.danger)
        SessionOutcome.Interrupted -> OcBadge(stringResource(R.string.sessions_interrupted), c.warning)
        else -> Unit
    }
}
