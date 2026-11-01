package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.domain.SharedContent
import io.github.beppe2880.opencodemobile.core.ui.components.LocalEditMode
import io.github.beppe2880.opencodemobile.core.ui.components.OcAgentChips
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcModelPicker
import io.github.beppe2880.opencodemobile.core.ui.components.OcProjectAvatar
import io.github.beppe2880.opencodemobile.core.ui.components.OcSharedContent
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextField
import io.github.beppe2880.opencodemobile.core.ui.components.editable
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.sessions.resources.Res
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_agent
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_create
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_directory
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_model
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_model_default
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_other_directory
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_path_label
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_path_placeholder
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_show_all
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_share_choose
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_share_discard
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_share_new
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_share_sheet_hint
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NewSessionSheet(
    state: NewSessionState,
    projects: List<Project>,
    onEvent: (SessionsEvent) -> Unit,
) {
    OcBottomSheet(onDismiss = { onEvent(SessionsEvent.DismissNewSession) }, testTag = "sessions_new_sheet") {
        if (state.pickingModel) {
            OcModelPicker(
                models = state.models,
                selected = state.effectiveModel,
                recent = state.recentModels,
                onSelect = { onEvent(SessionsEvent.NewSessionModel(it)) },
                onBack = { onEvent(SessionsEvent.NewSessionModelPicker(false)) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        } else {
            NewSessionForm(state, projects, onEvent)
        }
    }
}

@Composable
internal fun NewSessionForm(
    state: NewSessionState,
    projects: List<Project>,
    onEvent: (SessionsEvent) -> Unit,
    /** False for the preview in Settings: no own scrolling or insets, and no directories. */
    inSheet: Boolean = true,
) {
    val c = OcTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (inSheet) {
                    Modifier.imePadding().verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.navigationBars)
                } else {
                    Modifier
                },
            )
            .padding(bottom = OcSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Text(
            stringResource(Res.string.sessions_new_title),
            style = MaterialTheme.typography.titleMedium,
            color = c.textBase,
            modifier = Modifier.padding(horizontal = OcSpacing.lg),
        )
        if (state.shared != null && inSheet) {
            OcSharedContent(
                content = state.shared,
                message = stringResource(Res.string.sessions_share_sheet_hint),
                modifier = Modifier.padding(horizontal = OcSpacing.lg).testTag("sessions_new_shared"),
            )
        }
        // The preview in Settings shows only what its setting changes: the model.
        if (inSheet) {
            Label(stringResource(Res.string.sessions_new_directory))
            var showAll by rememberSaveable { mutableStateOf(false) }
            val visible = if (showAll || projects.size <= COLLAPSED_PROJECTS) {
                projects
            } else {
                (projects.take(COLLAPSED_PROJECTS) + projects.filter { it.directory == state.directory }).distinctBy { it.id }
            }
            visible.forEach { project ->
                DirectoryOption(
                    title = project.displayName,
                    path = project.directory,
                    selected = !state.customDirectory && state.directory == project.directory,
                    onClick = { onEvent(SessionsEvent.NewSessionDirectory(project.directory, custom = false)) },
                    modifier = Modifier.testTag("sessions_new_project_${project.id}"),
                )
            }
            if (visible.size < projects.size) {
                Text(
                    text = stringResource(Res.string.sessions_new_show_all, projects.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textAccent,
                    modifier = Modifier
                        .clickable(role = Role.Button) { showAll = true }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md)
                        .testTag("sessions_new_show_all"),
                )
            }
            DirectoryOption(
                title = stringResource(Res.string.sessions_new_other_directory),
                path = null,
                selected = state.customDirectory || projects.none { it.directory == state.directory },
                onClick = { onEvent(SessionsEvent.NewSessionDirectory(if (state.customDirectory) state.directory else "", custom = true)) },
                modifier = Modifier.testTag("sessions_new_other_directory"),
            )
            if (state.customDirectory || projects.none { it.directory == state.directory }) {
                OcTextField(
                    value = state.directory,
                    onValueChange = { onEvent(SessionsEvent.NewSessionDirectory(it, custom = true)) },
                    label = stringResource(Res.string.sessions_new_path_label),
                    placeholder = stringResource(Res.string.sessions_new_path_placeholder),
                    mono = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                    modifier = Modifier.padding(horizontal = OcSpacing.lg).testTag("sessions_new_path_field"),
                )
            }
        }
        if (state.agents.isNotEmpty()) {
            Label(stringResource(Res.string.sessions_new_agent))
            OcAgentChips(
                agents = state.agents,
                selected = state.agent,
                onSelect = { onEvent(SessionsEvent.NewSessionAgent(it)) },
                modifier = Modifier.padding(horizontal = OcSpacing.lg),
                testTagPrefix = "sessions_new_agent",
            )
        }
        Label(stringResource(Res.string.sessions_new_model))
        val model = state.effectiveModel
        val selectedModel = model?.let { ref -> state.models.firstOrNull { it.ref.key == ref.key } }
        OcChip(
            text = selectedModel?.name ?: model?.id ?: stringResource(Res.string.sessions_new_model_default),
            secondaryText = selectedModel?.providerName ?: model?.providerId,
            onClick = { onEvent(SessionsEvent.NewSessionModelPicker(true)) },
            trailingIcon = Icons.Outlined.ExpandMore,
            modifier = Modifier.padding(horizontal = OcSpacing.lg).editable(SettingsPart.NewChat, LocalEditMode.current).testTag("sessions_new_model"),
        )
        state.error?.let {
            Text(
                it.resolve(),
                color = c.danger.fg,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = OcSpacing.lg),
            )
        }
        OcButton(
            text = stringResource(Res.string.sessions_new_create),
            onClick = { onEvent(SessionsEvent.CreateSession) },
            enabled = state.canCreate,
            loading = state.creating,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm)
                .testTag("sessions_new_create_button"),
        )
    }
}

private const val COLLAPSED_PROJECTS = 5

/** Above the session list while a chat is picked for content shared from another app. */
@Composable
internal fun ShareChoice(content: SharedContent, onEvent: (SessionsEvent) -> Unit) {
    OcSharedContent(
        content = content,
        message = stringResource(Res.string.sessions_share_choose),
        modifier = Modifier.padding(start = OcSpacing.gutter, end = OcSpacing.gutter, top = OcSpacing.md).testTag("sessions_share_choice"),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm, Alignment.End)) {
            OcButton(
                text = stringResource(Res.string.sessions_share_discard),
                onClick = { onEvent(SessionsEvent.DiscardShare) },
                variant = OcButtonVariant.Ghost,
                compact = true,
                modifier = Modifier.testTag("sessions_share_discard"),
            )
            OcButton(
                text = stringResource(Res.string.sessions_share_new),
                onClick = { onEvent(SessionsEvent.StartNewSession) },
                variant = OcButtonVariant.Secondary,
                compact = true,
                modifier = Modifier.testTag("sessions_share_new"),
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = OcTheme.colors.textMuted,
        modifier = Modifier.padding(start = OcSpacing.lg, end = OcSpacing.lg, top = OcSpacing.sm),
    )
}

@Composable
private fun DirectoryOption(
    title: String,
    path: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        if (path != null) {
            OcProjectAvatar(title)
        } else {
            Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (path != null) {
                Text(path, style = OcTheme.mono.codeSmall, color = c.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = c.bgAccent, unselectedColor = c.iconMuted),
        )
    }
}
