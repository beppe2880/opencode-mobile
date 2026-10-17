package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.FileDiff
import io.github.beppe2880.opencodemobile.core.ui.theme.OcColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

private const val DIFF_PREVIEW_LINES = 60
private const val DIFF_MAX_LINES = 2_000

/** "+12 −3" in success/danger colors, announced as a sentence. */
@Composable
internal fun DiffStats(additions: Int, deletions: Int, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val description = stringResource(R.string.chat_diff_stats, additions, deletions)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = c.success.fg)) { append("+$additions") }
            append(" ")
            withStyle(SpanStyle(color = c.danger.fg)) { append("−$deletions") }
        },
        style = OcTheme.mono.codeSmall,
        maxLines = 1,
        modifier = modifier.semantics { contentDescription = description },
    )
}

/** Red/green unified diff of one file with old/new line numbers, like the OpenCode UI. */
@Composable
internal fun FileDiffView(
    diff: FileDiff,
    directory: String,
    full: Boolean,
    onToggleFull: () -> Unit,
    modifier: Modifier = Modifier,
    /** False when the path and stats already show right above. */
    showHeader: Boolean = true,
) {
    val c = OcTheme.colors
    val lines = remember(diff.patch) { UnifiedDiff.parse(diff.patch) }
    val shown = if (full) lines.take(DIFF_MAX_LINES) else lines.take(DIFF_PREVIEW_LINES)
    val hidden = lines.size - shown.size
    val numberWidth = remember(lines) {
        (lines.maxOfOrNull { maxOf(it.oldLine ?: 0, it.newLine ?: 0) } ?: 0).toString().length
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OcRadius.md))
            .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))
            .background(c.layer01),
    ) {
        if (showHeader) {
            FileDiffHeader(diff, directory, Modifier.fillMaxWidth().padding(horizontal = OcSpacing.sm, vertical = OcSpacing.xs))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val viewport = maxWidth
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .widthIn(min = viewport)
                        .width(IntrinsicSize.Max),
                ) {
                    shown.forEach { line -> DiffLineRow(line, numberWidth, c) }
                }
            }
        }
        if (hidden > 0 || (full && lines.size > DIFF_PREVIEW_LINES)) {
            Text(
                text = if (hidden > 0) pluralStringResource(R.plurals.chat_diff_more_lines, hidden, hidden) else stringResource(R.string.chat_show_less),
                style = MaterialTheme.typography.labelMedium,
                color = c.textAccent,
                modifier = Modifier
                    .clickable(role = Role.Button, enabled = !full || lines.size > DIFF_PREVIEW_LINES, onClick = onToggleFull)
                    .heightIn(min = 40.dp)
                    .padding(horizontal = OcSpacing.sm, vertical = OcSpacing.sm)
                    .testTag("chat_diff_more_${diff.path}"),
            )
        }
    }
}

/** Path relative to [directory], status and line counts of one changed file. */
@Composable
internal fun FileDiffHeader(diff: FileDiff, directory: String, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val path = if (directory.isNotEmpty()) diff.path.removePrefix("$directory/") else diff.path
    val statusLabel = when (diff.status) {
        "added" -> stringResource(R.string.chat_diff_added)
        "deleted" -> stringResource(R.string.chat_diff_deleted)
        FileDiff.WRITTEN -> stringResource(R.string.chat_diff_written)
        else -> null
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Text(
            text = path,
            style = OcTheme.mono.codeSmall,
            color = c.textBase,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        statusLabel?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = c.textFaint) }
        DiffStats(diff.additions, diff.deletions)
    }
}

/** One line rendered as a single Text: line numbers, marker and content over a tinted row. */
@Composable
private fun DiffLineRow(line: DiffLine, numberWidth: Int, c: OcColors) {
    val background = when (line.kind) {
        DiffLineKind.Added -> c.success.bg
        DiffLineKind.Removed -> c.danger.bg
        DiffLineKind.Hunk -> c.layer02
        DiffLineKind.Context -> c.layer01
    }
    Text(
        text = remember(line, numberWidth, c) { diffLineText(line, numberWidth, c) },
        style = OcTheme.mono.codeSmall,
        softWrap = false,
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = OcSpacing.sm, vertical = 1.dp),
    )
}

private fun diffLineText(line: DiffLine, numberWidth: Int, c: OcColors): AnnotatedString = buildAnnotatedString {
    if (line.kind == DiffLineKind.Hunk) {
        withStyle(SpanStyle(color = c.textFaint)) { append(line.text) }
        return@buildAnnotatedString
    }
    withStyle(SpanStyle(color = c.textFaint)) {
        append((line.oldLine?.toString() ?: "").padStart(numberWidth))
        append(" ")
        append((line.newLine?.toString() ?: "").padStart(numberWidth))
        append("  ")
    }
    when (line.kind) {
        DiffLineKind.Added -> withStyle(SpanStyle(color = c.success.fg)) { append("+ ") }
        DiffLineKind.Removed -> withStyle(SpanStyle(color = c.danger.fg)) { append("- ") }
        else -> append("  ")
    }
    withStyle(SpanStyle(color = c.textBase)) { append(line.text.replace("\t", "    ")) }
}

@PreviewLightDark
@Composable
private fun FileDiffViewPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        FileDiffView(
            diff = FileDiff(
                path = "android/core/domain/Session.kt",
                status = "modified",
                additions = 2,
                deletions = 1,
                patch = """
                    @@ -10,4 +10,5 @@
                     data class Session(
                    -    val id: String,
                    +    val id: SessionId,
                    +    val title: String,
                         val directory: String,
                """.trimIndent(),
            ),
            directory = "",
            full = false,
            onToggleFull = {},
            modifier = Modifier.padding(OcSpacing.md),
        )
    }
}
