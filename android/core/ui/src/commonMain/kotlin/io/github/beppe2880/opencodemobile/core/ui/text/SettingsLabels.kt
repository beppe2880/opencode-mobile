package io.github.beppe2880.opencodemobile.core.ui.text

import androidx.compose.runtime.Composable
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.ui.resources.Res
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_area_appearance
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_area_chat
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_area_composer
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_area_live
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_area_notifications
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_area_sessions
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_area_terminal
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_area_updates
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_activity
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_composer
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_footer
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_fullscreen
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_header
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_key_bar
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_keyboard
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_live_edits
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_live_other
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_live_reads
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_live_shell
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_live_step
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_live_thinking
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_messages
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_new_chat
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_notifications
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_notify_finished
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_notify_permission
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_notify_question
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_pending
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_running_line
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_selection
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_session_rows
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_split
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_swipe
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_text_size
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_theme
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_thoughts
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_tools
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_update
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_voice
import org.jetbrains.compose.resources.stringResource

/** The name of a group of settings, as Settings lists it. */
@Composable
fun SettingsArea.label(): String = stringResource(
    when (this) {
        SettingsArea.Appearance -> Res.string.oc_area_appearance
        SettingsArea.Chat -> Res.string.oc_area_chat
        SettingsArea.LiveActivity -> Res.string.oc_area_live
        SettingsArea.Composer -> Res.string.oc_area_composer
        SettingsArea.Sessions -> Res.string.oc_area_sessions
        SettingsArea.Terminal -> Res.string.oc_area_terminal
        SettingsArea.Notifications -> Res.string.oc_area_notifications
        SettingsArea.Updates -> Res.string.oc_area_updates
    },
)

/** The name of a part of a settings preview, as its preview and its options call it. */
@Composable
fun SettingsPart.label(): String = stringResource(
    when (this) {
        SettingsPart.Theme -> Res.string.oc_part_theme
        SettingsPart.TextSize -> Res.string.oc_part_text_size
        SettingsPart.Header -> Res.string.oc_part_header
        SettingsPart.Messages -> Res.string.oc_part_messages
        SettingsPart.Thoughts -> Res.string.oc_part_thoughts
        SettingsPart.Tools -> Res.string.oc_part_tools
        SettingsPart.Activity -> Res.string.oc_part_activity
        SettingsPart.Footer -> Res.string.oc_part_footer
        SettingsPart.PendingCards -> Res.string.oc_part_pending
        SettingsPart.RunningLine -> Res.string.oc_part_running_line
        SettingsPart.LiveStep -> Res.string.oc_part_live_step
        SettingsPart.LiveThinking -> Res.string.oc_part_live_thinking
        SettingsPart.LiveShell -> Res.string.oc_part_live_shell
        SettingsPart.LiveEdits -> Res.string.oc_part_live_edits
        SettingsPart.LiveReads -> Res.string.oc_part_live_reads
        SettingsPart.LiveOther -> Res.string.oc_part_live_other
        SettingsPart.ComposerBox -> Res.string.oc_part_composer
        SettingsPart.VoiceInput -> Res.string.oc_part_voice
        SettingsPart.SelectionTiming -> Res.string.oc_part_selection
        SettingsPart.NewChat -> Res.string.oc_part_new_chat
        SettingsPart.SessionRows -> Res.string.oc_part_session_rows
        SettingsPart.SwipeChats -> Res.string.oc_part_swipe
        SettingsPart.SplitView -> Res.string.oc_part_split
        SettingsPart.KeyBar -> Res.string.oc_part_key_bar
        SettingsPart.TerminalKeyboard -> Res.string.oc_part_keyboard
        SettingsPart.TerminalFullscreen -> Res.string.oc_part_fullscreen
        SettingsPart.NotificationShade -> Res.string.oc_part_notifications
        SettingsPart.PermissionNotification -> Res.string.oc_part_notify_permission
        SettingsPart.QuestionNotification -> Res.string.oc_part_notify_question
        SettingsPart.FinishedNotification -> Res.string.oc_part_notify_finished
        SettingsPart.UpdateNotice -> Res.string.oc_part_update
    },
)
