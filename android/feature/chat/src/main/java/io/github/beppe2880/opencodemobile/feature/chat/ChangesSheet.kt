package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.FileDiff
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcEmptyState
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegmentedControl
import io.github.beppe2880.opencodemobile.core.ui.components.OcSkeleton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/** Files with fewer changes than this open expanded. */
private const val EXPANDED_FILES = 3

/** Files the session changed, each expandable to its diff, for the last turn or the whole session. */
@Composable
internal fun ChangesSheet(state: ChatState, panel: ChangesPanel, onEvent: (ChatEvent) -> Unit) {
    val c = OcTheme.colors
    val files = panel.files
    OcBottomSheet(onDismiss = { onEvent(ChatEvent.CloseChanges) }, testTag = "chat_changes_sheet") {
        LazyColumn(
            contentPadding = PaddingValues(
                start = OcSpacing.lg,
                end = OcSpacing.lg,
                bottom = OcSpacing.lg + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                        Text(
                            stringResource(R.string.chat_changes),
                            style = MaterialTheme.typography.titleSmall,
                            color = c.textBase,
                            modifier = Modifier.weight(1f),
                        )
                        if (panel.loading && files != null) OcSpinner()
                        if (!files.isNullOrEmpty()) {
                            DiffStats(files.sumOf { it.additions }, files.sumOf { it.deletions })
                        }
                    }
                    OcSegmentedControl(
                        segments = listOf(
                            OcSegment(ChangesScope.Session, stringResource(R.string.chat_changes_session), "chat_changes_scope_session"),
                            OcSegment(ChangesScope.LastTurn, stringResource(R.string.chat_changes_last_turn), "chat_changes_scope_turn"),
                        ),
                        selected = panel.scope,
                        onSelect = { onEvent(ChatEvent.LoadChanges(it)) },
                    )
                    if (!files.isNullOrEmpty()) {
                        Text(
                            pluralStringResource(R.plurals.chat_changes_files, files.size, files.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                        )
                    }
                }
            }
            when {
                files == null && panel.error != null -> item(key = "error") {
                    OcErrorState(
                        title = stringResource(R.string.chat_changes_error),
                        message = panel.error.resolve(),
                        onRetry = { onEvent(ChatEvent.LoadChanges(panel.scope)) },
                        retryTestTag = "chat_changes_retry",
                    )
                }
                files == null -> items(2, key = { "skeleton$it" }) {
                    OcSkeleton(Modifier.fillMaxWidth().heightIn(min = 44.dp))
                }
                files.isEmpty() -> item(key = "empty") {
                    OcEmptyState(
                        icon = Icons.Outlined.FolderOpen,
                        title = stringResource(R.string.chat_changes_empty),
                        message = stringResource(
                            if (panel.scope == ChangesScope.LastTurn) R.string.chat_changes_empty_turn else R.string.chat_changes_empty_session,
                        ),
                        modifier = Modifier.testTag("chat_changes_empty"),
                    )
                }
                else -> items(files, key = { "file:${it.path}" }) { file ->
                    val expanded = state.isExpanded("changes:${file.path}", defaultExpanded = files.size <= EXPANDED_FILES)
                    ChangedFile(
                        file = file,
                        directory = state.directory,
                        expanded = expanded,
                        full = state.isExpanded("changes-full:${file.path}"),
                        onToggle = { onEvent(ChatEvent.Toggle("changes:${file.path}")) },
                        onToggleFull = { onEvent(ChatEvent.Toggle("changes-full:${file.path}")) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangedFile(
    file: FileDiff,
    directory: String,
    expanded: Boolean,
    full: Boolean,
    onToggle: () -> Unit,
    onToggleFull: () -> Unit,
) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.md)
    val stateLabel = stringResource(if (expanded) R.string.chat_expanded else R.string.chat_collapsed)
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, c.borderBase, shape)
                .background(c.layer01)
                .clickable(role = Role.Button, onClick = onToggle)
                .heightIn(min = 44.dp)
                .padding(horizontal = OcSpacing.sm)
                .semantics { stateDescription = stateLabel }
                .testTag("chat_changes_file_${file.path}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            FileDiffHeader(file, directory, Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = c.iconMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        if (expanded) {
            FileDiffView(diff = file, directory = directory, full = full, onToggleFull = onToggleFull, showHeader = false)
        }
    }
}
