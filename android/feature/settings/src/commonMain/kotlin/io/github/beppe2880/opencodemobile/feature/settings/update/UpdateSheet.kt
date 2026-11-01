package io.github.beppe2880.opencodemobile.feature.settings.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.ReleaseNote
import io.github.beppe2880.opencodemobile.core.domain.ReleaseSection
import io.github.beppe2880.opencodemobile.core.domain.ReleaseSectionKind
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_download
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_install
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_notes_breaking
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_notes_features
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_notes_fixes
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_release_notes
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_sheet_later
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_sheet_skip
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_sheet_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_sheet_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_sheet_versions_description
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_sheet_whats_new
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The sheet announcing a newer release with what changed, shown once per app start when the
 * update notice is a window; draws nothing otherwise. Shares its ViewModel with
 * [UpdateNoticeRoute] in the same screen.
 */
@Composable
fun UpdateSheetRoute() {
    val viewModel: UpdateNoticeViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is UpdateNoticeEffect.OpenUrl -> runCatching { uriHandler.openUri(effect.url) }
        }
    }
    if (state.sheetVisible) UpdateSheet(state, viewModel::onEvent)
}

@Composable
fun UpdateSheet(state: UpdateNoticeState, onEvent: (UpdateNoticeEvent) -> Unit) {
    // Swiping it away, Back or a tap outside count as Later.
    OcBottomSheet(onDismiss = { onEvent(UpdateNoticeEvent.Later) }, testTag = "update_sheet") {
        UpdateSheetContent(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        )
    }
}

/**
 * Title, versions, "What's new" and the actions; also drawn by the Settings preview. With
 * [scrollNotes] the notes scroll between the title and the actions, which stay in sight.
 */
@Composable
fun UpdateSheetContent(
    state: UpdateNoticeState,
    onEvent: (UpdateNoticeEvent) -> Unit,
    modifier: Modifier = Modifier,
    scrollNotes: Boolean = true,
) {
    val release = state.release ?: return
    val action = state.action ?: return
    val c = OcTheme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg).padding(bottom = OcSpacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            Icon(Icons.Outlined.NewReleases, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(22.dp))
            Text(
                stringResource(Res.string.update_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                color = c.textBase,
                modifier = Modifier.semantics { heading() }.testTag("update_sheet_title"),
            )
        }
        if (state.installedVersion.isNotEmpty()) {
            val description = stringResource(Res.string.update_sheet_versions_description, state.installedVersion, release.version)
            Text(
                // Versions read left to right in every language.
                "${state.installedVersion} → ${release.version}",
                style = OcTheme.mono.codeSmall.copy(textDirection = TextDirection.Ltr),
                color = c.textMuted,
                modifier = Modifier
                    .padding(top = OcSpacing.xs)
                    .clearAndSetSemantics { contentDescription = description }
                    .testTag("update_sheet_versions"),
            )
        }
        Text(
            stringResource(Res.string.update_sheet_whats_new),
            style = MaterialTheme.typography.titleSmall,
            color = c.textBase,
            modifier = Modifier.padding(top = OcSpacing.lg, bottom = OcSpacing.xs).semantics { heading() },
        )
        Column(
            (if (scrollNotes) Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()) else Modifier)
                .testTag("update_sheet_notes"),
        ) {
            val several = state.changes.size > 1
            state.changes.forEach { changes -> VersionNotes(changes, showVersion = several) }
        }
        // Marks where the notes scroll under the actions.
        OcDivider(Modifier.padding(top = OcSpacing.sm))
        Column(Modifier.padding(top = OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
            OcButton(
                text = stringResource(
                    when (action.kind) {
                        UpdateActionKind.Download -> Res.string.update_download
                        UpdateActionKind.ReleaseNotes -> Res.string.update_release_notes
                        UpdateActionKind.Store -> Res.string.update_install
                    },
                ),
                onClick = { onEvent(UpdateNoticeEvent.Open) },
                modifier = Modifier.fillMaxWidth().testTag("update_sheet_open"),
            )
            OcButton(
                text = stringResource(Res.string.update_sheet_later),
                onClick = { onEvent(UpdateNoticeEvent.Later) },
                variant = OcButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth().testTag("update_sheet_later"),
            )
            OcButton(
                text = stringResource(Res.string.update_sheet_skip),
                onClick = { onEvent(UpdateNoticeEvent.Dismiss) },
                variant = OcButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth().testTag("update_sheet_skip"),
            )
        }
    }
}

@Composable
private fun VersionNotes(changes: VersionChanges, showVersion: Boolean) {
    val c = OcTheme.colors
    if (showVersion) {
        Text(
            stringResource(Res.string.update_sheet_version, changes.version),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = c.textBase,
            modifier = Modifier.padding(top = OcSpacing.md).semantics { heading() },
        )
    }
    changes.sections.forEach { section ->
        val title = section.label()
        if (title.isNotEmpty()) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = c.textMuted,
                modifier = Modifier.padding(top = OcSpacing.sm, bottom = OcSpacing.xxs).semantics { heading() },
            )
        }
        section.items.forEach { NoteRow(it) }
    }
}

@Composable
private fun NoteRow(note: ReleaseNote) {
    val c = OcTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = OcSpacing.xxs), horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.clearAndSetSemantics { })
        Text(
            buildAnnotatedString {
                note.scope?.let { scope ->
                    withStyle(SpanStyle(color = c.textMuted, fontWeight = FontWeight.Medium)) { append(scope) }
                    append("  ")
                }
                append(note.text)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = c.textBase,
            modifier = Modifier.weight(1f),
        )
    }
}

/** The heading of a group of notes: translated when it is a known one, as published otherwise. */
@Composable
private fun ReleaseSection.label(): String = when (kind) {
    ReleaseSectionKind.Features -> stringResource(Res.string.update_notes_features)
    ReleaseSectionKind.Fixes -> stringResource(Res.string.update_notes_fixes)
    ReleaseSectionKind.Breaking -> stringResource(Res.string.update_notes_breaking)
    ReleaseSectionKind.Other -> title
}

@PreviewLightDark
@Composable
private fun UpdateSheetContentPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        val release = AppRelease("0.9.0", "https://github.com/beppe2880/opencode-mobile/releases/tag/v0.9.0")
        UpdateSheetContent(
            state = UpdateNoticeState(
                release = release,
                action = UpdateTarget.Android.actionFor(release),
                installedVersion = "0.8.0",
                changes = listOf(
                    VersionChanges(
                        "0.9.0",
                        listOf(
                            ReleaseSection(ReleaseSectionKind.Features, "Features", listOf(ReleaseNote("Follow the agent's work live", "chat"))),
                            ReleaseSection(ReleaseSectionKind.Fixes, "Bug fixes", listOf(ReleaseNote("Restore touch scrolling in the terminal", "terminal"))),
                        ),
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}
