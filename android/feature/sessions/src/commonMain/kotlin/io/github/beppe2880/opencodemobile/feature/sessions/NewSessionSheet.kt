package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import io.github.beppe2880.opencodemobile.core.ui.components.OcSearchField
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
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_project_search
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_project_search_empty
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_show_all
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_project_hide
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_project_unhide
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_projects_hidden
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
    /** Ids of the projects hidden on this device. */
    hidden: Set<String> = emptySet(),
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
        } else if (state.browser != null) {
            FolderBrowser(
                state = state.browser,
                onEvent = onEvent,
                modifier = Modifier.imePadding().windowInsetsPadding(WindowInsets.navigationBars),
            )
        } else {
            NewSessionForm(state, projects, onEvent, hidden = hidden)
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
    /** Ids of the projects hidden on this device: listed under "Hidden (N)", not selectable. */
    hidden: Set<String> = emptySet(),
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
            var showHidden by rememberSaveable { mutableStateOf(false) }
            // Local UI state like showAll: the sheet leaves composition when it closes, which resets it.
            var query by rememberSaveable { mutableStateOf("") }
            val shown = projects.filter { it.id !in hidden }
            val hiddenProjects = projects.filter { it.id in hidden }
            val searchable = shown.size > COLLAPSED_PROJECTS
            val searching = searchable && query.isNotBlank()
            val visible = visibleProjects(shown, if (searchable) query else "", showAll, state.directory)
            // While searching, hidden matches show inline (dimmed) so they can be found and unhidden.
            val hiddenMatches = if (searching) visibleProjects(hiddenProjects, query, showAll = true, selectedDirectory = "") else emptyList()
            if (searchable) {
                OcSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(Res.string.sessions_new_project_search),
                    modifier = Modifier.padding(horizontal = OcSpacing.lg).testTag("sessions_new_project_search"),
                )
            }
            if (searching && visible.isEmpty() && hiddenMatches.isEmpty()) {
                Text(
                    stringResource(Res.string.sessions_new_project_search_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    modifier = Modifier
                        .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm)
                        .testTag("sessions_new_project_search_empty"),
                )
            }
            visible.forEach { project ->
                DirectoryOption(
                    title = project.displayName,
                    path = project.directory,
                    selected = !state.customDirectory && state.directory == project.directory,
                    onClick = { onEvent(SessionsEvent.NewSessionDirectory(project.directory, custom = false)) },
                    onHide = { onEvent(SessionsEvent.HideProject(project.id)) },
                    modifier = Modifier.testTag("sessions_new_project_${project.id}"),
                )
            }
            hiddenMatches.forEach { project -> HiddenProjectRow(project, onEvent) }
            // One footer at a time: "Show all" while collapsed, then "Hidden (N)" once everything is listed.
            if (!searching && visible.size < shown.size) {
                FooterLink(
                    text = stringResource(Res.string.sessions_new_show_all, shown.size),
                    onClick = { showAll = true },
                    modifier = Modifier.testTag("sessions_new_show_all"),
                )
            } else if (!searching && hiddenProjects.isNotEmpty()) {
                FooterLink(
                    text = stringResource(Res.string.sessions_projects_hidden, hiddenProjects.size),
                    onClick = { showHidden = !showHidden },
                    trailingIcon = if (showHidden) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    modifier = Modifier.testTag("sessions_new_hidden_toggle"),
                )
                if (showHidden) hiddenProjects.forEach { project -> HiddenProjectRow(project, onEvent) }
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
                    trailingIcon = { BrowseButton { onEvent(SessionsEvent.BrowseFolders) } },
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
private fun FooterLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.textAccent)
        if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectoryOption(
    title: String,
    path: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Offered on long press; null for rows that cannot be hidden. */
    onHide: (() -> Unit)? = null,
) {
    val c = OcTheme.colors
    var menu by remember { mutableStateOf(false) }
    val hideLabel = stringResource(Res.string.sessions_project_hide)
    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { this.selected = selected }
                .combinedClickable(
                    role = Role.RadioButton,
                    onClick = onClick,
                    onLongClick = onHide?.let { { menu = true } },
                    onLongClickLabel = onHide?.let { hideLabel },
                )
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
        if (onHide != null) {
            ProjectMenu(expanded = menu, onDismiss = { menu = false }, onHide = onHide)
        }
    }
}

/** The long-press menu of a project row. */
@Composable
internal fun ProjectMenu(expanded: Boolean, onDismiss: () -> Unit, onHide: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = OcTheme.colors.bgBase) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.sessions_project_hide)) },
            leadingIcon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null, tint = OcTheme.colors.iconBase) },
            onClick = {
                onDismiss()
                onHide()
            },
            modifier = Modifier.testTag("sessions_project_hide"),
        )
    }
}

/** A hidden project: dimmed, not selectable, with Unhide. */
@Composable
private fun HiddenProjectRow(project: Project, onEvent: (SessionsEvent) -> Unit) {
    val c = OcTheme.colors
    val title = project.displayName
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = OcSpacing.lg, end = OcSpacing.sm, top = OcSpacing.xs, bottom = OcSpacing.xs)
            .testTag("sessions_new_hidden_${project.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        Box(Modifier.alpha(0.5f)) { OcProjectAvatar(title) }
        Column(Modifier.weight(1f).alpha(0.6f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(project.directory, style = OcTheme.mono.codeSmall, color = c.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Outlined.VisibilityOff, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
        OcButton(
            text = stringResource(Res.string.sessions_project_unhide),
            onClick = { onEvent(SessionsEvent.UnhideProject(project.id)) },
            variant = OcButtonVariant.Ghost,
            compact = true,
            modifier = Modifier.testTag("sessions_new_unhide_${project.id}"),
        )
    }
}
