package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderStyle
import io.github.beppe2880.opencodemobile.core.domain.ChatSwipe
import io.github.beppe2880.opencodemobile.core.domain.DEFAULT_LIST_PANE_FRACTION
import io.github.beppe2880.opencodemobile.core.domain.FoldChat
import io.github.beppe2880.opencodemobile.core.domain.LIST_PANE_FRACTION_RANGE
import io.github.beppe2880.opencodemobile.core.domain.LIVE_INTERVAL_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.NewChatModel
import io.github.beppe2880.opencodemobile.core.domain.OUTPUT_LINE_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.SelectionApply
import io.github.beppe2880.opencodemobile.core.domain.ShareOpens
import io.github.beppe2880.opencodemobile.core.domain.TEXT_SCALE_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.core.domain.ToolMediaSize
import io.github.beppe2880.opencodemobile.core.domain.UnfoldChat
import io.github.beppe2880.opencodemobile.core.domain.UnfoldedLayout
import io.github.beppe2880.opencodemobile.core.domain.UpdateNoticeStyle
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSliderRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcSwitchRow
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.ChoiceRow
import io.github.beppe2880.opencodemobile.feature.settings.SettingsEvent
import io.github.beppe2880.opencodemobile.feature.settings.SettingsState
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_agent_outline
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_agent_outline_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_back_shows_list
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_back_shows_list_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_close_model_picker
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_close_model_picker_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_confirm_stop
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_confirm_stop_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_dictation
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_dictation_cloud_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_dictation_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_expand_tools
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_expand_tools_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_fold_open_chat
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_fold_open_chat_keep
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_fold_open_chat_list
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_fold_open_chat_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_follow_running
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_follow_running_sub
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
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_keep_live_open
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_keep_live_open_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_handle
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_handle_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_swipe
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_swipe_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_width
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_width_reset
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_width_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_width_value
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_edits
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_edits_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_interval
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_interval_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_other
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_other_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_reads
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_reads_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_shell
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_shell_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_thinking
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_thinking_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_new_chat_model
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_new_chat_model_last
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_new_chat_model_server
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_new_chat_model_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_finished
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_finished_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_permissions
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_permissions_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_questions
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_questions_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_opencode_theme
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_opencode_theme_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_output_lines
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_output_lines_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_permissions_collapsed
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_permissions_collapsed_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_questions_collapsed
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_questions_collapsed_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_refresh_fast
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_refresh_fastest
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_refresh_normal
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_refresh_slow
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_running_chips
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_running_chips_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_running_in_list
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_running_in_list_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_selection_apply
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_selection_apply_next
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_selection_apply_now
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_selection_apply_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_session_notifications
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_session_notifications_blocked
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_session_notifications_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_share_menu
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_share_menu_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_share_opens
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_share_opens_choose
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_share_opens_choose_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_share_opens_new
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_share_opens_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_show_cost
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_show_cost_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_show_reasoning
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_show_reasoning_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_split_view
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_split_view_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_swipe_chats
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_swipe_chats_drawer
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_swipe_chats_drawer_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_swipe_chats_off
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_swipe_chats_off_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_swipe_chats_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_swipe_chats_switch
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal_fullscreen
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal_fullscreen_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal_keyboard
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal_keyboard_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal_keys
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal_keys_reset
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal_keys_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_size
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_size_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_dark
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_light
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_system
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_tool_details
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_tool_details_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_tool_media_size
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_tool_media_size_hidden
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_tool_media_size_large
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_tool_media_size_medium
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_tool_media_size_small
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_tool_media_size_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfold_open_chat
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfold_open_chat_beside
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfold_open_chat_full
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfold_open_chat_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfolded_layout
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfolded_layout_panel
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfolded_layout_panel_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfolded_layout_side
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfolded_layout_side_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfolded_layout_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_update_notice
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_update_notice_banner
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_update_notice_banner_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_update_notice_off
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_update_notice_off_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_update_notice_window
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_update_notice_window_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_hold
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_hold_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_key
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_key_search
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_language
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_language_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_mode
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_mode_cloud
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_mode_cloud_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_mode_system
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_mode_system_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_model
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_model_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_provider
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_provider_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_style
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_style_waveform_sub
import io.github.beppe2880.opencodemobile.feature.settings.update.LocalUpdateCheck
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** The name of the option, as its control and search show it. */
internal val SettingOption.titleRes: StringResource
    get() = when (this) {
        SettingOption.Theme -> Res.string.settings_theme
        SettingOption.TextSize -> Res.string.settings_text_size
        SettingOption.OpenCodeTheme -> Res.string.settings_opencode_theme
        SettingOption.HeaderLine -> Res.string.settings_header_line
        SettingOption.HeaderStyle -> Res.string.settings_header_style
        SettingOption.HeaderOpens -> Res.string.settings_header_opens
        SettingOption.ShowReasoning -> Res.string.settings_show_reasoning
        SettingOption.GroupThoughts -> Res.string.settings_group_thoughts
        SettingOption.GroupActivity -> Res.string.settings_group_activity
        SettingOption.ExpandTools -> Res.string.settings_expand_tools
        SettingOption.ToolDetails -> Res.string.settings_tool_details
        SettingOption.FullCommands -> Res.string.settings_full_commands
        SettingOption.ToolMediaSize -> Res.string.settings_tool_media_size
        SettingOption.OutputLines -> Res.string.settings_output_lines
        SettingOption.ShowCost -> Res.string.settings_show_cost
        SettingOption.PermissionsCollapsed -> Res.string.settings_permissions_collapsed
        SettingOption.QuestionsCollapsed -> Res.string.settings_questions_collapsed
        SettingOption.RunningChips -> Res.string.settings_running_chips
        SettingOption.ConfirmStop -> Res.string.settings_confirm_stop
        SettingOption.FollowRunning -> Res.string.settings_follow_running
        SettingOption.KeepLiveOpen -> Res.string.settings_keep_live_open
        SettingOption.LiveThinking -> Res.string.settings_live_thinking
        SettingOption.LiveShell -> Res.string.settings_live_shell
        SettingOption.LiveInterval -> Res.string.settings_live_interval
        SettingOption.LiveEdits -> Res.string.settings_live_edits
        SettingOption.LiveReads -> Res.string.settings_live_reads
        SettingOption.LiveOther -> Res.string.settings_live_other
        SettingOption.AgentOutline -> Res.string.settings_agent_outline
        SettingOption.Dictation -> Res.string.settings_dictation
        SettingOption.VoiceMode -> Res.string.settings_voice_mode
        SettingOption.VoiceProvider -> Res.string.settings_voice_provider
        SettingOption.VoiceModel -> Res.string.settings_voice_model
        SettingOption.VoiceKey -> Res.string.settings_voice_key
        SettingOption.VoiceLanguage -> Res.string.settings_voice_language
        SettingOption.VoiceStyle -> Res.string.settings_voice_style
        SettingOption.VoiceHold -> Res.string.settings_voice_hold
        SettingOption.SelectionApply -> Res.string.settings_selection_apply
        SettingOption.CloseModelPickerOnSelect -> Res.string.settings_close_model_picker
        SettingOption.NewChatModel -> Res.string.settings_new_chat_model
        SettingOption.ShareMenu -> Res.string.settings_share_menu
        SettingOption.ShareOpens -> Res.string.settings_share_opens
        SettingOption.RunningInList -> Res.string.settings_running_in_list
        SettingOption.SwipeChats -> Res.string.settings_swipe_chats
        SettingOption.SplitView -> Res.string.settings_split_view
        SettingOption.ListWidth -> Res.string.settings_list_width
        SettingOption.ListHandle -> Res.string.settings_list_handle
        SettingOption.BackShowsList -> Res.string.settings_back_shows_list
        SettingOption.UnfoldedLayout -> Res.string.settings_unfolded_layout
        SettingOption.UnfoldOpenChat -> Res.string.settings_unfold_open_chat
        SettingOption.FoldOpenChat -> Res.string.settings_fold_open_chat
        SettingOption.ListSwipe -> Res.string.settings_list_swipe
        SettingOption.TerminalKeys -> Res.string.settings_terminal_keys
        SettingOption.TerminalKeyboard -> Res.string.settings_terminal_keyboard
        SettingOption.TerminalFullscreen -> Res.string.settings_terminal_fullscreen
        SettingOption.Notifications -> Res.string.settings_session_notifications
        SettingOption.NotifyPermissions -> Res.string.settings_notify_permissions
        SettingOption.NotifyQuestions -> Res.string.settings_notify_questions
        SettingOption.NotifyFinished -> Res.string.settings_notify_finished
        SettingOption.UpdateNotice -> Res.string.settings_update_notice
    }

/** What the option does, for search; the controls may show a variant for the current value. */
internal val SettingOption.descriptionRes: StringResource
    get() = when (this) {
        SettingOption.Theme -> Res.string.settings_theme_sub
        SettingOption.TextSize -> Res.string.settings_text_size_sub
        SettingOption.OpenCodeTheme -> Res.string.settings_opencode_theme_sub
        SettingOption.HeaderLine -> Res.string.settings_header_line_all_sub
        SettingOption.HeaderStyle -> Res.string.settings_header_style_icons_sub
        SettingOption.HeaderOpens -> Res.string.settings_header_opens_sub
        SettingOption.ShowReasoning -> Res.string.settings_show_reasoning_sub
        SettingOption.GroupThoughts -> Res.string.settings_group_thoughts_sub
        SettingOption.GroupActivity -> Res.string.settings_group_activity_sub
        SettingOption.ExpandTools -> Res.string.settings_expand_tools_sub
        SettingOption.ToolDetails -> Res.string.settings_tool_details_sub
        SettingOption.FullCommands -> Res.string.settings_full_commands_sub
        SettingOption.ToolMediaSize -> Res.string.settings_tool_media_size_sub
        SettingOption.OutputLines -> Res.string.settings_output_lines_sub
        SettingOption.ShowCost -> Res.string.settings_show_cost_sub
        SettingOption.PermissionsCollapsed -> Res.string.settings_permissions_collapsed_sub
        SettingOption.QuestionsCollapsed -> Res.string.settings_questions_collapsed_sub
        SettingOption.RunningChips -> Res.string.settings_running_chips_sub
        SettingOption.ConfirmStop -> Res.string.settings_confirm_stop_sub
        SettingOption.FollowRunning -> Res.string.settings_follow_running_sub
        SettingOption.KeepLiveOpen -> Res.string.settings_keep_live_open_sub
        SettingOption.LiveThinking -> Res.string.settings_live_thinking_sub
        SettingOption.LiveShell -> Res.string.settings_live_shell_sub
        SettingOption.LiveInterval -> Res.string.settings_live_interval_sub
        SettingOption.LiveEdits -> Res.string.settings_live_edits_sub
        SettingOption.LiveReads -> Res.string.settings_live_reads_sub
        SettingOption.LiveOther -> Res.string.settings_live_other_sub
        SettingOption.AgentOutline -> Res.string.settings_agent_outline_sub
        SettingOption.Dictation -> Res.string.settings_dictation_sub
        SettingOption.VoiceMode -> Res.string.settings_voice_mode_cloud_sub
        SettingOption.VoiceProvider -> Res.string.settings_voice_provider_sub
        SettingOption.VoiceModel -> Res.string.settings_voice_model_sub
        SettingOption.VoiceKey -> Res.string.settings_voice_key_search
        SettingOption.VoiceLanguage -> Res.string.settings_voice_language_sub
        SettingOption.VoiceStyle -> Res.string.settings_voice_style_waveform_sub
        SettingOption.VoiceHold -> Res.string.settings_voice_hold_sub
        SettingOption.SelectionApply -> Res.string.settings_selection_apply_sub
        SettingOption.CloseModelPickerOnSelect -> Res.string.settings_close_model_picker_sub
        SettingOption.NewChatModel -> Res.string.settings_new_chat_model_sub
        SettingOption.ShareMenu -> Res.string.settings_share_menu_sub
        SettingOption.ShareOpens -> Res.string.settings_share_opens_sub
        SettingOption.RunningInList -> Res.string.settings_running_in_list_sub
        SettingOption.SwipeChats -> Res.string.settings_swipe_chats_sub
        SettingOption.SplitView -> Res.string.settings_split_view_sub
        SettingOption.ListWidth -> Res.string.settings_list_width_sub
        SettingOption.ListHandle -> Res.string.settings_list_handle_sub
        SettingOption.BackShowsList -> Res.string.settings_back_shows_list_sub
        SettingOption.UnfoldedLayout -> Res.string.settings_unfolded_layout_sub
        SettingOption.UnfoldOpenChat -> Res.string.settings_unfold_open_chat_sub
        SettingOption.FoldOpenChat -> Res.string.settings_fold_open_chat_sub
        SettingOption.ListSwipe -> Res.string.settings_list_swipe_sub
        SettingOption.TerminalKeys -> Res.string.settings_terminal_keys_sub
        SettingOption.TerminalKeyboard -> Res.string.settings_terminal_keyboard_sub
        SettingOption.TerminalFullscreen -> Res.string.settings_terminal_fullscreen_sub
        SettingOption.Notifications -> Res.string.settings_session_notifications_sub
        SettingOption.NotifyPermissions -> Res.string.settings_notify_permissions_sub
        SettingOption.NotifyQuestions -> Res.string.settings_notify_questions_sub
        SettingOption.NotifyFinished -> Res.string.settings_notify_finished_sub
        SettingOption.UpdateNotice -> Res.string.settings_update_notice_window_sub
    }

/** The switch, choice or slider of [option]; [onEvent] receives the same events everywhere in Settings. */
@Composable
internal fun SettingOptionControl(option: SettingOption, state: SettingsState, onEvent: (SettingsEvent) -> Unit, modifier: Modifier = Modifier) {
    val settings = state.settings
    val title = stringResource(option.titleRes)
    val subtitle = stringResource(option.descriptionRes)
    val tag = "settings_${option.tag}"
    fun switch(checked: Boolean, event: (Boolean) -> SettingsEvent, sub: String = subtitle): @Composable () -> Unit = {
        OcSwitchRow(title = title, subtitle = sub, checked = checked, onCheckedChange = { onEvent(event(it)) }, modifier = modifier.testTag("${tag}_switch"))
    }
    when (option) {
        SettingOption.Theme -> ChoiceRow(
            title = title,
            subtitle = subtitle,
            choices = listOf(
                OcSegment(ThemeMode.System, stringResource(Res.string.settings_theme_system), "settings_theme_system"),
                OcSegment(ThemeMode.Light, stringResource(Res.string.settings_theme_light), "settings_theme_light"),
                OcSegment(ThemeMode.Dark, stringResource(Res.string.settings_theme_dark), "settings_theme_dark"),
            ),
            selected = settings.theme,
            onSelect = { onEvent(SettingsEvent.ThemeChanged(it)) },
        )
        SettingOption.TextSize -> ChoiceRow(
            title = title,
            subtitle = subtitle,
            choices = TEXT_SCALE_CHOICES.mapIndexed { i, scale -> OcSegment(scale, stringResource(TEXT_SIZE_LABELS[i]), "settings_text_size_$i") },
            selected = settings.chatTextScale,
            onSelect = { onEvent(SettingsEvent.ChatTextScaleChanged(it)) },
        )
        SettingOption.OpenCodeTheme -> OpenCodeThemeControl(title, subtitle, settings.openCodeTheme, onEvent, modifier)
        SettingOption.HeaderLine -> ChoiceRow(
            title = title,
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
        SettingOption.HeaderStyle -> ChoiceRow(
            title = title,
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
        SettingOption.HeaderOpens -> ChoiceRow(
            title = title,
            subtitle = subtitle,
            choices = listOf(
                OcSegment(ChangesScope.Folder, stringResource(Res.string.settings_header_opens_folder), "settings_header_opens_folder"),
                OcSegment(ChangesScope.Session, stringResource(Res.string.settings_header_opens_session), "settings_header_opens_session"),
            ),
            selected = settings.headerOpens,
            onSelect = { onEvent(SettingsEvent.HeaderOpensChanged(it)) },
        )
        SettingOption.ShowReasoning -> switch(settings.showReasoning, SettingsEvent::ShowReasoningChanged)()
        SettingOption.GroupThoughts -> switch(settings.groupThoughts, SettingsEvent::GroupThoughtsChanged)()
        SettingOption.GroupActivity -> switch(settings.groupActivity, SettingsEvent::GroupActivityChanged)()
        SettingOption.ExpandTools -> switch(settings.expandToolsByDefault, SettingsEvent::ExpandToolsChanged)()
        SettingOption.ToolDetails -> switch(settings.showToolDetails, SettingsEvent::ShowToolDetailsChanged)()
        SettingOption.FullCommands -> switch(settings.showFullCommands, SettingsEvent::ShowFullCommandsChanged)()
        SettingOption.ToolMediaSize -> ChoiceRow(
            title = title,
            subtitle = subtitle,
            choices = listOf(
                OcSegment(ToolMediaSize.Hidden, stringResource(Res.string.settings_tool_media_size_hidden), "settings_tool_media_size_hidden"),
                OcSegment(ToolMediaSize.Small, stringResource(Res.string.settings_tool_media_size_small), "settings_tool_media_size_small"),
                OcSegment(ToolMediaSize.Medium, stringResource(Res.string.settings_tool_media_size_medium), "settings_tool_media_size_medium"),
                OcSegment(ToolMediaSize.Large, stringResource(Res.string.settings_tool_media_size_large), "settings_tool_media_size_large"),
            ),
            selected = settings.toolMediaSize,
            onSelect = { onEvent(SettingsEvent.ToolMediaSizeChanged(it)) },
        )
        SettingOption.OutputLines -> ChoiceRow(
            title = title,
            subtitle = subtitle,
            choices = OUTPUT_LINE_CHOICES.map { OcSegment(it, it.toString(), "settings_output_lines_$it") },
            selected = settings.outputPreviewLines,
            onSelect = { onEvent(SettingsEvent.OutputPreviewLinesChanged(it)) },
        )
        SettingOption.ShowCost -> switch(settings.showCost, SettingsEvent::ShowCostChanged)()
        SettingOption.PermissionsCollapsed -> switch(settings.permissionsCollapsed, SettingsEvent::PermissionsCollapsedChanged)()
        SettingOption.QuestionsCollapsed -> switch(settings.questionsCollapsed, SettingsEvent::QuestionsCollapsedChanged)()
        SettingOption.RunningChips -> switch(settings.runningChips, SettingsEvent::RunningChipsChanged)()
        SettingOption.ConfirmStop -> switch(settings.confirmStopCommand, SettingsEvent::ConfirmStopChanged)()
        SettingOption.FollowRunning -> switch(settings.followRunningTools, SettingsEvent::FollowRunningToolsChanged)()
        SettingOption.KeepLiveOpen -> switch(settings.keepLiveStepsOpen, SettingsEvent::KeepLiveStepsOpenChanged)()
        SettingOption.LiveThinking -> switch(settings.liveThinking, SettingsEvent::LiveThinkingChanged)()
        SettingOption.LiveShell -> switch(settings.liveShell, SettingsEvent::LiveShellChanged)()
        SettingOption.LiveInterval -> {
            val speeds = listOf(
                Res.string.settings_refresh_fastest,
                Res.string.settings_refresh_fast,
                Res.string.settings_refresh_normal,
                Res.string.settings_refresh_slow,
            )
            ChoiceRow(
                title = title,
                subtitle = subtitle,
                choices = LIVE_INTERVAL_CHOICES.mapIndexed { i, ms -> OcSegment(ms, stringResource(speeds[i]), "settings_live_interval_$ms") },
                selected = settings.liveOutputIntervalMs,
                onSelect = { onEvent(SettingsEvent.LiveOutputIntervalChanged(it)) },
            )
        }
        SettingOption.LiveEdits -> switch(settings.liveEdits, SettingsEvent::LiveEditsChanged)()
        SettingOption.LiveReads -> switch(settings.liveReads, SettingsEvent::LiveReadsChanged)()
        SettingOption.LiveOther -> switch(settings.liveOther, SettingsEvent::LiveOtherChanged)()
        SettingOption.AgentOutline -> switch(settings.agentColorOutline, SettingsEvent::AgentColorOutlineChanged)()
        SettingOption.Dictation -> switch(
            settings.dictation,
            SettingsEvent::DictationChanged,
            if (settings.voiceEngine == VoiceEngine.Cloud) stringResource(Res.string.settings_dictation_cloud_sub) else subtitle,
        )()
        SettingOption.VoiceMode -> ChoiceRow(
            title = title,
            subtitle = stringResource(
                when (settings.voiceEngine) {
                    VoiceEngine.System -> Res.string.settings_voice_mode_system_sub
                    VoiceEngine.Cloud -> Res.string.settings_voice_mode_cloud_sub
                },
            ),
            choices = listOf(
                OcSegment(VoiceEngine.System, stringResource(Res.string.settings_voice_mode_system), "settings_voice_mode_system"),
                OcSegment(VoiceEngine.Cloud, stringResource(Res.string.settings_voice_mode_cloud), "settings_voice_mode_cloud"),
            ),
            selected = settings.voiceEngine,
            onSelect = { onEvent(SettingsEvent.VoiceEngineChanged(it)) },
        )
        SettingOption.VoiceProvider -> VoiceProviderControl(title, subtitle, state, onEvent, modifier)
        SettingOption.VoiceModel -> VoiceModelControl(title, subtitle, state, onEvent, modifier)
        SettingOption.VoiceKey -> VoiceKeyControl(title, state, onEvent, modifier)
        SettingOption.VoiceLanguage -> switch(settings.sttLanguageHint, SettingsEvent::SttLanguageHintChanged)()
        SettingOption.VoiceStyle -> VoiceStyleControl(title, settings, onEvent)
        SettingOption.VoiceHold -> switch(settings.voiceHoldToRecord, SettingsEvent::VoiceHoldToRecordChanged)()
        SettingOption.CloseModelPickerOnSelect -> switch(settings.closeModelPickerOnSelect, SettingsEvent::CloseModelPickerOnSelectChanged)()
        SettingOption.SelectionApply -> ChoiceRow(
            title = title,
            subtitle = subtitle,
            choices = listOf(
                OcSegment(SelectionApply.NextMessage, stringResource(Res.string.settings_selection_apply_next), "settings_selection_apply_next"),
                OcSegment(SelectionApply.Immediately, stringResource(Res.string.settings_selection_apply_now), "settings_selection_apply_now"),
            ),
            selected = settings.selectionApply,
            onSelect = { onEvent(SettingsEvent.SelectionApplyChanged(it)) },
        )
        SettingOption.NewChatModel -> ChoiceRow(
            title = title,
            subtitle = subtitle,
            choices = listOf(
                OcSegment(NewChatModel.LastUsed, stringResource(Res.string.settings_new_chat_model_last), "settings_new_chat_model_last"),
                OcSegment(NewChatModel.ServerDefault, stringResource(Res.string.settings_new_chat_model_server), "settings_new_chat_model_server"),
            ),
            selected = settings.newChatModel,
            onSelect = { onEvent(SettingsEvent.NewChatModelChanged(it)) },
        )
        SettingOption.ShareMenu -> switch(settings.showInShareMenu, SettingsEvent::ShowInShareMenuChanged)()
        SettingOption.ShareOpens -> ChoiceRow(
            title = title,
            subtitle = stringResource(
                when (settings.shareOpens) {
                    ShareOpens.NewSession -> Res.string.settings_share_opens_sub
                    ShareOpens.ChooseChat -> Res.string.settings_share_opens_choose_sub
                },
            ),
            choices = listOf(
                OcSegment(ShareOpens.NewSession, stringResource(Res.string.settings_share_opens_new), "settings_share_opens_new"),
                OcSegment(ShareOpens.ChooseChat, stringResource(Res.string.settings_share_opens_choose), "settings_share_opens_choose"),
            ),
            selected = settings.shareOpens,
            onSelect = { onEvent(SettingsEvent.ShareOpensChanged(it)) },
        )
        SettingOption.RunningInList -> switch(settings.showRunningWorkInList, SettingsEvent::RunningInListChanged)()
        SettingOption.SwipeChats -> ChoiceRow(
            title = title,
            subtitle = stringResource(
                when (settings.chatSwipe) {
                    ChatSwipe.SwitchChats -> Res.string.settings_swipe_chats_sub
                    ChatSwipe.SessionDrawer -> Res.string.settings_swipe_chats_drawer_sub
                    ChatSwipe.Off -> Res.string.settings_swipe_chats_off_sub
                },
            ),
            choices = listOf(
                OcSegment(ChatSwipe.SessionDrawer, stringResource(Res.string.settings_swipe_chats_drawer), "settings_swipe_chats_drawer"),
                OcSegment(ChatSwipe.SwitchChats, stringResource(Res.string.settings_swipe_chats_switch), "settings_swipe_chats_next_previous"),
                OcSegment(ChatSwipe.Off, stringResource(Res.string.settings_swipe_chats_off), "settings_swipe_chats_off"),
            ),
            selected = settings.chatSwipe,
            onSelect = { onEvent(SettingsEvent.ChatSwipeChanged(it)) },
        )
        SettingOption.SplitView -> switch(settings.splitView, SettingsEvent::SplitViewChanged)()
        SettingOption.ListWidth -> androidx.compose.foundation.layout.Column(modifier) {
            val fraction = settings.listPaneFraction ?: DEFAULT_LIST_PANE_FRACTION
            OcSliderRow(
                title = title,
                subtitle = subtitle,
                value = fraction.coerceIn(LIST_PANE_FRACTION_RANGE),
                onValueChange = { onEvent(SettingsEvent.ListPaneFractionChanged(it)) },
                valueRange = LIST_PANE_FRACTION_RANGE,
                // 5% steps.
                steps = ((LIST_PANE_FRACTION_RANGE.endInclusive - LIST_PANE_FRACTION_RANGE.start) * 20).roundToInt() - 1,
                valueLabel = stringResource(Res.string.settings_list_width_value, (fraction * 100).roundToInt()),
                modifier = Modifier.testTag("settings_list_width_slider"),
            )
            // Also changed by dragging the divider; the default follows the window width within bounds.
            OcButton(
                text = stringResource(Res.string.settings_list_width_reset),
                onClick = { onEvent(SettingsEvent.ListPaneFractionChanged(null)) },
                variant = OcButtonVariant.Secondary,
                compact = true,
                enabled = settings.listPaneFraction != null,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = OcSpacing.sm, bottom = OcSpacing.sm)
                    .testTag("settings_list_width_reset"),
            )
        }
        SettingOption.UnfoldedLayout -> ChoiceRow(
            title = title,
            subtitle = stringResource(
                when (settings.unfoldedLayout) {
                    UnfoldedLayout.SideBySide -> Res.string.settings_unfolded_layout_side_sub
                    UnfoldedLayout.ListPanel -> Res.string.settings_unfolded_layout_panel_sub
                },
            ),
            choices = listOf(
                OcSegment(UnfoldedLayout.SideBySide, stringResource(Res.string.settings_unfolded_layout_side), "settings_unfolded_layout_side"),
                OcSegment(UnfoldedLayout.ListPanel, stringResource(Res.string.settings_unfolded_layout_panel), "settings_unfolded_layout_panel"),
            ),
            selected = settings.unfoldedLayout,
            onSelect = { onEvent(SettingsEvent.UnfoldedLayoutChanged(it)) },
        )
        SettingOption.UnfoldOpenChat -> ChoiceRow(
            title = title,
            subtitle = subtitle,
            choices = listOf(
                OcSegment(UnfoldChat.BesideList, stringResource(Res.string.settings_unfold_open_chat_beside), "settings_unfold_open_chat_beside"),
                OcSegment(UnfoldChat.FullScreen, stringResource(Res.string.settings_unfold_open_chat_full), "settings_unfold_open_chat_full"),
            ),
            selected = settings.unfoldOpenChat,
            onSelect = { onEvent(SettingsEvent.UnfoldOpenChatChanged(it)) },
        )
        SettingOption.FoldOpenChat -> ChoiceRow(
            title = title,
            subtitle = subtitle,
            choices = listOf(
                OcSegment(FoldChat.KeepChat, stringResource(Res.string.settings_fold_open_chat_keep), "settings_fold_open_chat_keep"),
                OcSegment(FoldChat.ShowList, stringResource(Res.string.settings_fold_open_chat_list), "settings_fold_open_chat_list"),
            ),
            selected = settings.foldOpenChat,
            onSelect = { onEvent(SettingsEvent.FoldOpenChatChanged(it)) },
        )
        SettingOption.ListHandle -> switch(settings.listPaneHandle, SettingsEvent::ListPaneHandleChanged)()
        SettingOption.BackShowsList -> switch(settings.backShowsList, SettingsEvent::BackShowsListChanged)()
        SettingOption.ListSwipe -> switch(settings.listSwipe, SettingsEvent::ListSwipeChanged)()
        SettingOption.TerminalKeys -> Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = OcSpacing.lg, end = OcSpacing.sm, top = OcSpacing.sm, bottom = OcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = OcTheme.colors.textBase)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OcTheme.colors.textMuted)
            }
            OcButton(
                text = stringResource(Res.string.settings_terminal_keys_reset),
                onClick = { onEvent(SettingsEvent.ResetTerminalKeys) },
                variant = OcButtonVariant.Secondary,
                compact = true,
                enabled = settings.terminalKeys != null,
                modifier = Modifier.testTag("settings_terminal_keys_reset"),
            )
        }
        SettingOption.TerminalKeyboard -> switch(settings.terminalOpenKeyboard, SettingsEvent::TerminalOpenKeyboardChanged)()
        SettingOption.TerminalFullscreen -> switch(settings.terminalFullscreenByDefault, SettingsEvent::TerminalFullscreenByDefaultChanged)()
        SettingOption.Notifications -> {
            val blocked = state.notificationsBlocked && !state.notificationsAllowed
            switch(
                settings.notifications && state.notificationsAllowed,
                SettingsEvent::NotificationsChanged,
                if (blocked) stringResource(Res.string.settings_session_notifications_blocked) else subtitle,
            )()
        }
        SettingOption.NotifyPermissions -> switch(settings.notifyPermissions, SettingsEvent::NotifyPermissionsChanged)()
        SettingOption.NotifyQuestions -> switch(settings.notifyQuestions, SettingsEvent::NotifyQuestionsChanged)()
        SettingOption.NotifyFinished -> switch(settings.notifyFinished, SettingsEvent::NotifyFinishedChanged)()
        SettingOption.UpdateNotice -> androidx.compose.foundation.layout.Column(modifier) {
            ChoiceRow(
                title = title,
                subtitle = stringResource(
                    when (settings.updateNotice) {
                        UpdateNoticeStyle.Window -> Res.string.settings_update_notice_window_sub
                        UpdateNoticeStyle.Banner -> Res.string.settings_update_notice_banner_sub
                        UpdateNoticeStyle.Off -> Res.string.settings_update_notice_off_sub
                    },
                ),
                choices = listOf(
                    OcSegment(UpdateNoticeStyle.Window, stringResource(Res.string.settings_update_notice_window), "settings_update_notice_window"),
                    OcSegment(UpdateNoticeStyle.Banner, stringResource(Res.string.settings_update_notice_banner), "settings_update_notice_banner"),
                    OcSegment(UpdateNoticeStyle.Off, stringResource(Res.string.settings_update_notice_off), "settings_update_notice_off"),
                ),
                selected = settings.updateNotice,
                onSelect = { onEvent(SettingsEvent.UpdateNoticeChanged(it)) },
            )
            // Also with Off: that only stops the automatic checks.
            LocalUpdateCheck.current?.invoke()
        }
    }
}

/** Test tags keep the names the settings list used before the previews. */
private val SettingOption.tag: String
    get() = when (this) {
        SettingOption.OpenCodeTheme -> "opencode_theme"
        SettingOption.ShowReasoning -> "show_reasoning"
        SettingOption.GroupThoughts -> "group_thoughts"
        SettingOption.GroupActivity -> "group_activity"
        SettingOption.ExpandTools -> "expand_tools"
        SettingOption.ToolDetails -> "tool_details"
        SettingOption.FullCommands -> "full_commands"
        SettingOption.ToolMediaSize -> "tool_media_size"
        SettingOption.ShowCost -> "show_cost"
        SettingOption.PermissionsCollapsed -> "permissions_collapsed"
        SettingOption.QuestionsCollapsed -> "questions_collapsed"
        SettingOption.RunningChips -> "running_chips"
        SettingOption.ConfirmStop -> "confirm_stop"
        SettingOption.FollowRunning -> "follow_running"
        SettingOption.KeepLiveOpen -> "keep_live_open"
        SettingOption.LiveThinking -> "live_thinking"
        SettingOption.LiveShell -> "live_shell"
        SettingOption.LiveEdits -> "live_edits"
        SettingOption.LiveReads -> "live_reads"
        SettingOption.LiveOther -> "live_other"
        SettingOption.AgentOutline -> "agent_outline"
        SettingOption.Dictation -> "dictation"
        SettingOption.VoiceMode -> "voice_mode"
        SettingOption.VoiceProvider -> "voice_provider"
        SettingOption.VoiceModel -> "voice_model"
        SettingOption.VoiceKey -> "voice_key"
        SettingOption.VoiceLanguage -> "voice_language"
        SettingOption.VoiceStyle -> "voice_style"
        SettingOption.VoiceHold -> "voice_hold"
        SettingOption.RunningInList -> "running_in_list"
        SettingOption.ShareMenu -> "share_menu"
        SettingOption.SwipeChats -> "swipe_chats"
        SettingOption.SplitView -> "split_view"
        SettingOption.ListHandle -> "list_handle"
        SettingOption.BackShowsList -> "back_shows_list"
        SettingOption.UnfoldedLayout -> "unfolded_layout"
        SettingOption.UnfoldOpenChat -> "unfold_open_chat"
        SettingOption.FoldOpenChat -> "fold_open_chat"
        SettingOption.ListSwipe -> "list_swipe"
        SettingOption.TerminalKeyboard -> "terminal_keyboard"
        SettingOption.TerminalFullscreen -> "terminal_fullscreen"
        SettingOption.Notifications -> "notifications"
        SettingOption.NotifyPermissions -> "notify_permissions"
        SettingOption.NotifyQuestions -> "notify_questions"
        SettingOption.NotifyFinished -> "notify_finished"
        else -> name.lowercase()
    }
