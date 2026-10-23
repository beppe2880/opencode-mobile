package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.text.formatDuration
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/** "Used 2 Read, Shell" summary row; expands into per-tool rows. */
@Composable
internal fun ToolsGroup(
    item: ChatItem.Tools,
    directory: String,
    expanded: Boolean,
    isExpanded: (String) -> Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSession: (String) -> Unit = {},
    onOpenFile: (String) -> Unit = {},
    live: LiveFollow? = null,
    fullCommands: Boolean = false,
    /** The agent is still working on this group: the tools [live] covers stay open. */
    inLiveGroup: Boolean = false,
    /** Rows open on their output when the group opens; tapping one folds it. */
    details: Boolean = false,
) {
    val c = OcTheme.colors
    // Work in progress stays open so it can be followed live; it folds back once done.
    val open = expanded || (live != null && (inLiveGroup || item.running) && item.tools.any(live::covers))
    Column(modifier = modifier.fillMaxWidth()) {
        GroupSummary(
            summary = toolsSummary(item.tools, thinking = null),
            running = item.running,
            failed = item.failed > 0,
            expanded = open,
            onToggle = { onToggle("g:${item.key}") },
            testTag = "chat_tools_${item.key}",
        )
        AnimatedVisibility(visible = open) {
            Column(
                modifier = Modifier
                    .padding(top = OcSpacing.xs)
                    .fillMaxWidth()
                    .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.lg))
                    .padding(vertical = OcSpacing.xs),
            ) {
                item.tools.forEachIndexed { index, tool ->
                    // Call ids repeat across steps; the group key plus position is unique.
                    val id = "${item.key}#$index"
                    ToolRow(
                        tool = tool,
                        directory = directory,
                        expanded = isExpanded("o:$id") != details,
                        full = isExpanded("f:$id"),
                        onToggle = { onToggle("o:$id") },
                        onToggleFull = { onToggle("f:$id") },
                        onOpenSession = onOpenSession,
                        onOpenFile = onOpenFile,
                        live = live,
                        inLiveGroup = inLiveGroup,
                        fullCommand = fullCommands,
                    )
                }
            }
        }
    }
}

/** "Thought for 9s · Used 6 Shell, Edit" summary; expands into the thinking and tool rows in order. */
@Composable
internal fun ActivityGroup(
    item: ChatItem.Activity,
    directory: String,
    expanded: Boolean,
    defaultToolsExpanded: Boolean,
    isExpanded: (String) -> Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSession: (String) -> Unit = {},
    onOpenFile: (String) -> Unit = {},
    live: LiveFollow? = null,
    fullCommands: Boolean = false,
    /** The agent is still working on this group: its newest tools stay open between calls. */
    inLiveGroup: Boolean = false,
    /** Inner tool groups open with every tool's output when this group opens. */
    details: Boolean = false,
) {
    val thinking = when {
        item.thoughts.isEmpty() -> null
        item.thoughts.any { it.streaming } -> stringResource(R.string.chat_thinking)
        item.thinkingMs >= 1_000 -> stringResource(R.string.chat_thought_for, formatDuration(item.thinkingMs))
        else -> stringResource(R.string.chat_thought)
    }
    val followed = live != null &&
        (item.tools.any(live::covers) || (live.thinking && item.thoughts.any { it.streaming }))
    val open = expanded || (followed && (inLiveGroup || item.running))
    Column(modifier = modifier.fillMaxWidth()) {
        GroupSummary(
            summary = toolsSummary(item.tools, thinking),
            running = item.running,
            failed = item.failed > 0,
            expanded = open,
            onToggle = { onToggle("a:${item.key}") },
            testTag = "chat_activity_${item.key}",
        )
        AnimatedVisibility(visible = open) {
            Column(
                modifier = Modifier.padding(start = OcSpacing.md, top = OcSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            ) {
                val latestTools = item.entries.lastOrNull { it is ChatItem.Tools }
                item.entries.forEach { entry ->
                    when (entry) {
                        is ChatItem.Reasoning -> ReasoningBlock(
                            item = entry,
                            expanded = isExpanded("r:${entry.key}"),
                            onToggle = { onToggle("r:${entry.key}") },
                            live = live,
                        )
                        is ChatItem.Tools -> ToolsGroup(
                            item = entry,
                            directory = directory,
                            expanded = isExpanded("g:${entry.key}") != (defaultToolsExpanded || details),
                            isExpanded = isExpanded,
                            onToggle = onToggle,
                            onOpenSession = onOpenSession,
                            onOpenFile = onOpenFile,
                            live = live,
                            fullCommands = fullCommands,
                            inLiveGroup = inLiveGroup && entry === latestTools,
                            details = details,
                        )
                        else -> Unit
                    }
                }
            }
        }
    }
}

/** "[thinking · ]Used N Title, Title" with the counts and titles emphasized. */
@Composable
private fun toolsSummary(tools: List<Part.Tool>, thinking: String?): AnnotatedString {
    val c = OcTheme.colors
    val used = stringResource(R.string.chat_used_tools)
    return buildAnnotatedString {
        thinking?.let { append(it) }
        if (tools.isNotEmpty()) {
            if (thinking != null) append(" · ")
            append("$used ")
            withStyle(SpanStyle(color = c.textBase, fontWeight = FontWeight.Medium)) {
                append("${tools.size} ")
                append(tools.map { ToolInfo.title(it.name) }.distinct().joinToString(", "))
            }
        }
    }
}

@Composable
private fun GroupSummary(
    summary: AnnotatedString,
    running: Boolean,
    failed: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
) {
    val c = OcTheme.colors
    DisclosureRow(expanded = expanded, onToggle = onToggle, testTag = testTag) {
        Text(
            summary,
            style = MaterialTheme.typography.bodySmall,
            color = c.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        when {
            running -> OcSpinner(size = 10.dp)
            failed -> Icon(Icons.Outlined.ErrorOutline, stringResource(R.string.chat_tool_failed), tint = c.danger.fg, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ToolRow(
    tool: Part.Tool,
    directory: String,
    expanded: Boolean,
    full: Boolean,
    onToggle: () -> Unit,
    onToggleFull: () -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    live: LiveFollow?,
    inLiveGroup: Boolean,
    fullCommand: Boolean,
) {
    val c = OcTheme.colors
    val child = SessionInsights.childSession(tool)
    val file = ToolInfo.filePath(tool)
    val argument = ToolInfo.primaryArgument(tool.input)?.let { arg ->
        if (directory.isNotEmpty() && arg.startsWith("$directory/")) arg.removePrefix("$directory/") else arg
    }
    val running = tool.status == ToolStatus.Running || tool.status == ToolStatus.Streaming
    // Followed kinds open while they run and, in the group the agent is still working on, once done.
    val open = expanded || (live?.covers(tool) == true && (running || inLiveGroup))
    val statusLabel = when (tool.status) {
        ToolStatus.Streaming, ToolStatus.Running -> stringResource(R.string.chat_tool_running)
        ToolStatus.Completed -> stringResource(R.string.chat_tool_completed)
        ToolStatus.Error -> stringResource(R.string.chat_tool_failed)
    }
    // With full commands on, the argument gets its own full-width lines under the title.
    val wrapArgument = fullCommand && !argument.isNullOrEmpty()
    Column(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = statusLabel }
                .testTag("chat_tool_${tool.callId}"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                Icon(ToolInfo.icon(tool.name), contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
                Text(ToolInfo.title(tool.name), style = OcTheme.mono.label, color = c.textBase)
                if (wrapArgument) {
                    Spacer(Modifier.weight(1f))
                } else {
                    Text(
                        text = argument.orEmpty(),
                        style = OcTheme.mono.codeSmall,
                        color = c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (tool.diffs.isNotEmpty()) {
                    DiffStats(tool.diffs.sumOf { it.additions }, tool.diffs.sumOf { it.deletions })
                }
                if (file != null) {
                    OcIconButton(
                        icon = Icons.Outlined.FileOpen,
                        contentDescription = stringResource(R.string.chat_open_file),
                        onClick = { onOpenFile(file) },
                        modifier = Modifier.size(36.dp).testTag("chat_open_file_${tool.callId}"),
                    )
                }
                if (child != null) {
                    OcIconButton(
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = stringResource(R.string.chat_open_subagent),
                        onClick = { onOpenSession(child) },
                        modifier = Modifier.size(36.dp).testTag("chat_open_subagent_${tool.callId}"),
                    )
                }
                ToolStatusIcon(tool.status)
            }
            if (wrapArgument) {
                Text(
                    text = argument.orEmpty(),
                    style = OcTheme.mono.codeSmall,
                    color = c.textMuted,
                    modifier = Modifier.padding(start = OcSpacing.md, end = OcSpacing.md, bottom = OcSpacing.sm),
                )
            }
        }
        // A running tool has nothing to show until its output starts arriving.
        AnimatedVisibility(visible = open && !(running && tool.output.isNullOrEmpty())) {
            if (tool.diffs.isNotEmpty() && tool.error == null) {
                // Patches change several files and have no button of their own: each diff gets one.
                ToolDiffs(tool, directory, full, onToggleFull, onOpenFile.takeIf { file == null })
            } else {
                ToolOutput(tool, full, onToggleFull, live = running)
            }
        }
    }
}

@Composable
private fun ToolDiffs(tool: Part.Tool, directory: String, full: Boolean, onToggleFull: () -> Unit, onOpenFile: ((String) -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = OcSpacing.md, end = OcSpacing.md, bottom = OcSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        tool.diffs.forEach { diff ->
            FileDiffView(diff, directory, full, onToggleFull, onOpenFile = onOpenFile?.takeIf { diff.status != "deleted" }?.let { open -> { open(diff.path) } })
        }
    }
}

@Composable
private fun ToolStatusIcon(status: ToolStatus) {
    val c = OcTheme.colors
    when (status) {
        ToolStatus.Streaming, ToolStatus.Running -> OcSpinner(size = 12.dp)
        ToolStatus.Completed -> Icon(Icons.Outlined.CheckCircle, null, tint = c.success.fg, modifier = Modifier.size(14.dp))
        ToolStatus.Error -> Icon(Icons.Outlined.ErrorOutline, null, tint = c.danger.fg, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun ToolOutput(tool: Part.Tool, full: Boolean, onToggleFull: () -> Unit, live: Boolean = false) {
    val c = OcTheme.colors
    val raw = (tool.error ?: tool.output).orEmpty().trimEnd()
    val lines = raw.lines()
    val truncated = !full && lines.size > OUTPUT_PREVIEW_LINES
    // While the tool runs, the newest lines are the interesting ones.
    val shown = when {
        !truncated -> raw
        live -> lines.takeLast(OUTPUT_PREVIEW_LINES).joinToString("\n")
        else -> lines.take(OUTPUT_PREVIEW_LINES).joinToString("\n")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = OcSpacing.md, end = OcSpacing.md, bottom = OcSpacing.sm)
            .background(if (tool.error != null) c.danger.bg else c.layer01, RoundedCornerShape(OcRadius.md)),
    ) {
        SelectionContainer {
            Text(
                text = shown.ifEmpty { stringResource(R.string.chat_no_output) },
                style = OcTheme.mono.codeSmall,
                color = if (tool.error != null) c.danger.fg else c.textMuted,
                softWrap = false,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(OcSpacing.sm),
            )
        }
        if (lines.size > OUTPUT_PREVIEW_LINES) {
            Text(
                text = stringResource(if (full) R.string.chat_show_less else R.string.chat_show_more),
                style = MaterialTheme.typography.labelMedium,
                color = c.textAccent,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onToggleFull)
                    .heightIn(min = 40.dp)
                    .padding(horizontal = OcSpacing.sm, vertical = OcSpacing.sm)
                    .testTag("chat_tool_more_${tool.callId}"),
            )
        }
    }
}
