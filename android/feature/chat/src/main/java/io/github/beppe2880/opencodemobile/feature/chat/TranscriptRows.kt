package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.NoticeKind
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.markdown.MarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.markdown.OcMarkdown
import io.github.beppe2880.opencodemobile.core.ui.text.formatCost
import io.github.beppe2880.opencodemobile.core.ui.text.formatDuration
import io.github.beppe2880.opencodemobile.core.ui.text.formatTokenTotal
import io.github.beppe2880.opencodemobile.core.ui.text.formatTokensPerSecond
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/** Lines of tool and shell output shown before "Show more". */
internal const val OUTPUT_PREVIEW_LINES = 12

@Composable
internal fun UserBubble(
    item: ChatItem.User,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    onOpenImage: (Int) -> Unit = {},
) {
    val c = OcTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(start = 48.dp),
        horizontalAlignment = Alignment.End,
    ) {
        if (item.images.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = OcSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            ) {
                item.images.forEachIndexed { index, image ->
                    ImageThumbnail(
                        dataUri = image.dataUri,
                        onClick = { onOpenImage(index) },
                        // A prompt of images alone has no text bubble to long press.
                        onLongClick = onLongPress?.takeIf { item.text.isBlank() },
                        modifier = Modifier.testTag("chat_user_image_${item.key}_$index"),
                    )
                }
            }
        }
        val bubble = Modifier
            .alpha(if (item.pending) 0.75f else 1f)
            .clip(RoundedCornerShape(OcRadius.lg))
            .background(c.userBubbleBg)
        if (item.text.isBlank() && item.images.isNotEmpty()) {
            // Images only: no empty bubble below them.
        } else if (onLongPress != null) {
            // Long press opens the message actions (copy, undo), replacing text selection.
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = c.userBubbleText,
                modifier = bubble
                    .combinedClickable(
                        onClickLabel = null,
                        onLongClickLabel = stringResource(R.string.chat_message_actions),
                        onLongClick = onLongPress,
                        onClick = {},
                    )
                    .padding(horizontal = OcSpacing.md, vertical = 10.dp)
                    .testTag("chat_user_message_${item.key}"),
            )
        } else {
            SelectionContainer {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.userBubbleText,
                    modifier = bubble.padding(horizontal = OcSpacing.md, vertical = 10.dp),
                )
            }
        }
        // Images show as thumbnails above; only the other files are counted.
        val files = item.attachments - item.images.size
        if (item.pending || files > 0) {
            Row(
                modifier = Modifier.padding(top = OcSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            ) {
                if (item.pending) {
                    Icon(Icons.Outlined.Schedule, null, tint = c.textFaint, modifier = Modifier.size(12.dp))
                    Text(stringResource(R.string.chat_queued), style = MaterialTheme.typography.labelSmall, color = c.textFaint)
                }
                if (files > 0) {
                    Text(
                        pluralStringResource(R.plurals.chat_attachments, files, files),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AssistantText(item: ChatItem.Text, cache: MarkdownCache, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (item.text.isNotEmpty()) {
            SelectionContainer {
                OcMarkdown(id = item.key, content = item.text, cache = cache)
            }
        }
        if (item.streaming) {
            OcPulsingDot(OcTheme.colors.textMuted, Modifier.padding(top = OcSpacing.xs), size = 6.dp)
        }
    }
}

@Composable
internal fun ReasoningBlock(item: ChatItem.Reasoning, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val label = when {
        item.streaming -> stringResource(R.string.chat_thinking)
        item.durationMs != null && item.durationMs >= 1_000 -> stringResource(R.string.chat_thought_for, formatDuration(item.durationMs))
        else -> stringResource(R.string.chat_thought)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        DisclosureRow(
            expanded = expanded,
            onToggle = onToggle,
            testTag = "chat_reasoning_${item.key}",
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            if (item.streaming) OcSpinner(size = 10.dp)
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
                modifier = Modifier
                    .padding(start = OcSpacing.xs, top = OcSpacing.xs)
                    .background(c.layer01, RoundedCornerShape(OcRadius.md))
                    .padding(OcSpacing.md),
            )
        }
    }
}

/** A collapsible summary row with a chevron that turns when [expanded]. */
@Composable
internal fun DisclosureRow(
    expanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable RowScope.() -> Unit,
) {
    val c = OcTheme.colors
    val rotation by animateFloatAsState(if (expanded) 0f else -90f, label = "chevron")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(OcRadius.sm))
            .clickable(role = Role.Button, onClickLabel = null, onClick = onToggle)
            .heightIn(min = 36.dp)
            .padding(end = OcSpacing.sm)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
        Icon(
            Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = c.iconMuted,
            modifier = Modifier.size(16.dp).rotate(rotation),
        )
    }
}

@Composable
internal fun NoticeRow(item: ChatItem.Notice, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val color = if (item.kind == NoticeKind.Error) c.danger.fg else c.textFaint
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = OcSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Box(Modifier.weight(1f).heightIn(min = 1.dp, max = 1.dp).background(c.borderMuted))
        Icon(if (item.kind == NoticeKind.Error) Icons.Outlined.ErrorOutline else Icons.Outlined.Info, null, tint = color, modifier = Modifier.size(12.dp))
        Text(
            when (item.kind) {
                // The completed notice carries the whole summary: too long for a divider label.
                NoticeKind.Compacting -> stringResource(R.string.chat_compacting)
                NoticeKind.Compaction -> stringResource(R.string.chat_compacted)
                else -> item.text
            },
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 280.dp),
        )
        Box(Modifier.weight(1f).heightIn(min = 1.dp, max = 1.dp).background(c.borderMuted))
    }
}

@Composable
internal fun ErrorRow(item: ChatItem.Error, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.danger.bg, RoundedCornerShape(OcRadius.md))
            .border(1.dp, c.danger.border, RoundedCornerShape(OcRadius.md))
            .padding(OcSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = c.danger.fg, modifier = Modifier.size(16.dp))
        SelectionContainer { Text(item.text, style = MaterialTheme.typography.bodySmall, color = c.danger.fg) }
    }
}

@Composable
internal fun ShellBlock(item: ChatItem.Shell, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.layer01, RoundedCornerShape(OcRadius.md))
            .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))
            .padding(OcSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            Text("$ ${item.command}", style = OcTheme.mono.code, color = c.textBase, modifier = Modifier.weight(1f, fill = false))
            if (item.status == "running") OcSpinner(Modifier.testTag("chat_shell_running"))
        }
        if (item.output.isNotBlank()) {
            Text(
                item.output.trimEnd().lines().takeLast(OUTPUT_PREVIEW_LINES).joinToString("\n"),
                style = OcTheme.mono.codeSmall,
                color = c.textMuted,
            )
        }
        item.exit?.let { Text(stringResource(R.string.chat_shell_exit, it), style = OcTheme.mono.codeSmall, color = c.textFaint) }
    }
}

/** "Build · Claude Haiku 5.5 · Anthropic · 3s · 48 tok/s · $0.002" with a copy action. */
@Composable
internal fun TurnFooter(
    item: ChatItem.Footer,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
    showCost: Boolean = true,
) {
    val c = OcTheme.colors
    val parts = buildList {
        add(item.agent.replaceFirstChar { it.uppercase() })
        add(item.model)
        add(item.provider)
        item.durationMs?.let { add(formatDuration(it)) }
        if (item.totalTokens > 0) add(formatTokenTotal(item.totalTokens))
        item.tokensPerSecond?.let { add(formatTokensPerSecond(it)) }
        if (showCost) formatCost(item.cost).takeIf { it.isNotEmpty() }?.let { add(it) }
    }
    val interrupted = stringResource(R.string.chat_interrupted)
    val label = buildAnnotatedString {
        append(parts.joinToString(" · "))
        if (item.interrupted) {
            append(" · ")
            withStyle(SpanStyle(color = c.warning.fg)) { append(interrupted) }
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.copyText.isNotEmpty()) {
            OcIconButton(
                icon = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.chat_copy_response),
                onClick = { onCopy(item.copyText) },
                tint = c.iconMuted,
                iconSize = 14.dp,
                modifier = Modifier.offset(x = (-14).dp).testTag("chat_copy_${item.key}"),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = c.textFaint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(x = if (item.copyText.isNotEmpty()) (-14).dp else 0.dp),
        )
    }
}

@Composable
internal fun WorkingRow(modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = OcSpacing.xs).testTag("chat_working"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        OcPulsingDot(c.agents.build, size = 8.dp)
        Text(stringResource(R.string.chat_working), style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}
