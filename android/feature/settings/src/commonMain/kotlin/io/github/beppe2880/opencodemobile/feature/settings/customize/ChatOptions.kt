package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.ChatElement
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderStyle
import io.github.beppe2880.opencodemobile.core.domain.OUTPUT_LINE_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.TEXT_SCALE_CHOICES
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSwitchRow
import io.github.beppe2880.opencodemobile.feature.settings.ChoiceRow
import io.github.beppe2880.opencodemobile.feature.settings.SettingsEvent
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_agent_outline
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_agent_outline_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_dictation
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_dictation_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_expand_tools
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_expand_tools_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_full_commands
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_full_commands_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_activity
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_activity_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_thoughts
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_thoughts_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_line
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_line_all
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_line_all_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_line_branch
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_line_branch_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_line_folder
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_line_folder_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_opens
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_opens_folder
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_opens_session
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_opens_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_style
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_style_full
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_style_full_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_style_icons
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_style_icons_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_style_short
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_header_style_short_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_output_lines
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_output_lines_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_permissions_collapsed
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_permissions_collapsed_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_questions_collapsed
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_questions_collapsed_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_show_cost
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_show_cost_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_show_reasoning
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_show_reasoning_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_default
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_large
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_largest
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_size
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_size_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_small
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_tool_details
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_tool_details_sub
import org.jetbrains.compose.resources.stringResource

/** A setting that changes how the chat looks; the same control in the settings list and in the customizer. */
internal enum class ChatOption {
    HeaderLine,
    HeaderStyle,
    HeaderOpens,
    TextSize,
    ShowReasoning,
    GroupThoughts,
    GroupActivity,
    ExpandTools,
    ToolDetails,
    FullCommands,
    OutputLines,
    ShowCost,
    PermissionsCollapsed,
    QuestionsCollapsed,
    AgentOutline,
    Dictation,
}

/** Every option of this part of the chat, shown or not. */
internal val ChatElement.options: List<ChatOption>
    get() = when (this) {
        ChatElement.Header -> listOf(ChatOption.HeaderLine, ChatOption.HeaderStyle, ChatOption.HeaderOpens)
        ChatElement.Messages -> listOf(ChatOption.TextSize)
        ChatElement.Thoughts -> listOf(ChatOption.ShowReasoning, ChatOption.GroupThoughts, ChatOption.GroupActivity)
        ChatElement.Tools -> listOf(ChatOption.ExpandTools, ChatOption.ToolDetails, ChatOption.FullCommands, ChatOption.OutputLines, ChatOption.GroupActivity)
        ChatElement.Activity -> listOf(
            ChatOption.GroupActivity,
            ChatOption.ShowReasoning,
            ChatOption.GroupThoughts,
            ChatOption.ExpandTools,
            ChatOption.ToolDetails,
            ChatOption.FullCommands,
            ChatOption.OutputLines,
        )
        ChatElement.Footer -> listOf(ChatOption.ShowCost)
        ChatElement.PendingCard -> listOf(ChatOption.PermissionsCollapsed, ChatOption.QuestionsCollapsed)
        ChatElement.Composer -> listOf(ChatOption.AgentOutline, ChatOption.Dictation)
    }

/** The options of this part that apply with [settings]: a folder path alone has no style and is not a line to tap. */
internal fun ChatElement.optionsFor(settings: AppSettings): List<ChatOption> = options.filter { it.appliesTo(settings) }

internal fun ChatOption.appliesTo(settings: AppSettings): Boolean = when (this) {
    ChatOption.HeaderStyle, ChatOption.HeaderOpens -> settings.chatHeaderLine != ChatHeaderLine.Folder
    else -> true
}

/** The switch or choice of [option]; [onEvent] receives the same events as the settings list. */
@Composable
internal fun ChatOptionControl(option: ChatOption, settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    when (option) {
        ChatOption.HeaderLine -> ChoiceRow(
            title = stringResource(Res.string.settings_header_line),
            subtitle = stringResource(
                when (settings.chatHeaderLine) {
                    ChatHeaderLine.FolderBranchFiles -> Res.string.settings_header_line_all_sub
                    ChatHeaderLine.BranchFiles -> Res.string.settings_header_line_branch_sub
                    ChatHeaderLine.Folder -> Res.string.settings_header_line_folder_sub
                },
            ),
            choices = listOf(
                OcSegment(ChatHeaderLine.FolderBranchFiles, stringResource(Res.string.settings_header_line_all), "settings_header_line_all"),
                OcSegment(ChatHeaderLine.BranchFiles, stringResource(Res.string.settings_header_line_branch), "settings_header_line_branch"),
                OcSegment(ChatHeaderLine.Folder, stringResource(Res.string.settings_header_line_folder), "settings_header_line_folder"),
            ),
            selected = settings.chatHeaderLine,
            onSelect = { onEvent(SettingsEvent.ChatHeaderLineChanged(it)) },
        )
        ChatOption.HeaderStyle -> ChoiceRow(
            title = stringResource(Res.string.settings_header_style),
            subtitle = stringResource(
                when (settings.chatHeaderStyle) {
                    ChatHeaderStyle.Icons -> Res.string.settings_header_style_icons_sub
                    ChatHeaderStyle.Short -> Res.string.settings_header_style_short_sub
                    ChatHeaderStyle.Full -> Res.string.settings_header_style_full_sub
                },
            ),
            choices = listOf(
                OcSegment(ChatHeaderStyle.Icons, stringResource(Res.string.settings_header_style_icons), "settings_header_style_icons"),
                OcSegment(ChatHeaderStyle.Short, stringResource(Res.string.settings_header_style_short), "settings_header_style_short"),
                OcSegment(ChatHeaderStyle.Full, stringResource(Res.string.settings_header_style_full), "settings_header_style_full"),
            ),
            selected = settings.chatHeaderStyle,
            onSelect = { onEvent(SettingsEvent.ChatHeaderStyleChanged(it)) },
        )
        ChatOption.HeaderOpens -> ChoiceRow(
            title = stringResource(Res.string.settings_header_opens),
            subtitle = stringResource(Res.string.settings_header_opens_sub),
            choices = listOf(
                OcSegment(ChangesScope.Folder, stringResource(Res.string.settings_header_opens_folder), "settings_header_opens_folder"),
                OcSegment(ChangesScope.Session, stringResource(Res.string.settings_header_opens_session), "settings_header_opens_session"),
            ),
            selected = settings.headerOpens,
            onSelect = { onEvent(SettingsEvent.HeaderOpensChanged(it)) },
        )
        ChatOption.TextSize -> {
            val sizes =
                listOf(Res.string.settings_text_small, Res.string.settings_text_default, Res.string.settings_text_large, Res.string.settings_text_largest)
            ChoiceRow(
                title = stringResource(Res.string.settings_text_size),
                subtitle = stringResource(Res.string.settings_text_size_sub),
                choices = TEXT_SCALE_CHOICES.mapIndexed { i, scale -> OcSegment(scale, stringResource(sizes[i]), "settings_text_size_$i") },
                selected = settings.chatTextScale,
                onSelect = { onEvent(SettingsEvent.ChatTextScaleChanged(it)) },
            )
        }
        ChatOption.ShowReasoning -> OcSwitchRow(
            title = stringResource(Res.string.settings_show_reasoning),
            subtitle = stringResource(Res.string.settings_show_reasoning_sub),
            checked = settings.showReasoning,
            onCheckedChange = { onEvent(SettingsEvent.ShowReasoningChanged(it)) },
            modifier = Modifier.testTag("settings_show_reasoning_switch"),
        )
        ChatOption.GroupThoughts -> OcSwitchRow(
            title = stringResource(Res.string.settings_group_thoughts),
            subtitle = stringResource(Res.string.settings_group_thoughts_sub),
            checked = settings.groupThoughts,
            onCheckedChange = { onEvent(SettingsEvent.GroupThoughtsChanged(it)) },
            modifier = Modifier.testTag("settings_group_thoughts_switch"),
        )
        ChatOption.GroupActivity -> OcSwitchRow(
            title = stringResource(Res.string.settings_group_activity),
            subtitle = stringResource(Res.string.settings_group_activity_sub),
            checked = settings.groupActivity,
            onCheckedChange = { onEvent(SettingsEvent.GroupActivityChanged(it)) },
            modifier = Modifier.testTag("settings_group_activity_switch"),
        )
        ChatOption.ExpandTools -> OcSwitchRow(
            title = stringResource(Res.string.settings_expand_tools),
            subtitle = stringResource(Res.string.settings_expand_tools_sub),
            checked = settings.expandToolsByDefault,
            onCheckedChange = { onEvent(SettingsEvent.ExpandToolsChanged(it)) },
            modifier = Modifier.testTag("settings_expand_tools_switch"),
        )
        ChatOption.ToolDetails -> OcSwitchRow(
            title = stringResource(Res.string.settings_tool_details),
            subtitle = stringResource(Res.string.settings_tool_details_sub),
            checked = settings.showToolDetails,
            onCheckedChange = { onEvent(SettingsEvent.ShowToolDetailsChanged(it)) },
            modifier = Modifier.testTag("settings_tool_details_switch"),
        )
        ChatOption.FullCommands -> OcSwitchRow(
            title = stringResource(Res.string.settings_full_commands),
            subtitle = stringResource(Res.string.settings_full_commands_sub),
            checked = settings.showFullCommands,
            onCheckedChange = { onEvent(SettingsEvent.ShowFullCommandsChanged(it)) },
            modifier = Modifier.testTag("settings_full_commands_switch"),
        )
        ChatOption.OutputLines -> ChoiceRow(
            title = stringResource(Res.string.settings_output_lines),
            subtitle = stringResource(Res.string.settings_output_lines_sub),
            choices = OUTPUT_LINE_CHOICES.map { OcSegment(it, it.toString(), "settings_output_lines_$it") },
            selected = settings.outputPreviewLines,
            onSelect = { onEvent(SettingsEvent.OutputPreviewLinesChanged(it)) },
        )
        ChatOption.ShowCost -> OcSwitchRow(
            title = stringResource(Res.string.settings_show_cost),
            subtitle = stringResource(Res.string.settings_show_cost_sub),
            checked = settings.showCost,
            onCheckedChange = { onEvent(SettingsEvent.ShowCostChanged(it)) },
            modifier = Modifier.testTag("settings_show_cost_switch"),
        )
        ChatOption.PermissionsCollapsed -> OcSwitchRow(
            title = stringResource(Res.string.settings_permissions_collapsed),
            subtitle = stringResource(Res.string.settings_permissions_collapsed_sub),
            checked = settings.permissionsCollapsed,
            onCheckedChange = { onEvent(SettingsEvent.PermissionsCollapsedChanged(it)) },
            modifier = Modifier.testTag("settings_permissions_collapsed_switch"),
        )
        ChatOption.QuestionsCollapsed -> OcSwitchRow(
            title = stringResource(Res.string.settings_questions_collapsed),
            subtitle = stringResource(Res.string.settings_questions_collapsed_sub),
            checked = settings.questionsCollapsed,
            onCheckedChange = { onEvent(SettingsEvent.QuestionsCollapsedChanged(it)) },
            modifier = Modifier.testTag("settings_questions_collapsed_switch"),
        )
        ChatOption.AgentOutline -> OcSwitchRow(
            title = stringResource(Res.string.settings_agent_outline),
            subtitle = stringResource(Res.string.settings_agent_outline_sub),
            checked = settings.agentColorOutline,
            onCheckedChange = { onEvent(SettingsEvent.AgentColorOutlineChanged(it)) },
            modifier = Modifier.testTag("settings_agent_outline_switch"),
        )
        ChatOption.Dictation -> OcSwitchRow(
            title = stringResource(Res.string.settings_dictation),
            subtitle = stringResource(Res.string.settings_dictation_sub),
            checked = settings.dictation,
            onCheckedChange = { onEvent(SettingsEvent.DictationChanged(it)) },
            modifier = Modifier.testTag("settings_dictation_switch"),
        )
    }
}
