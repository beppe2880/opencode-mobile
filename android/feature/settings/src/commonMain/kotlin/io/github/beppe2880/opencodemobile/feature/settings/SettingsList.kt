package io.github.beppe2880.opencodemobile.feature.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegmentedControl
import io.github.beppe2880.opencodemobile.feature.settings.customize.SettingOption
import io.github.beppe2880.opencodemobile.feature.settings.customize.SettingOptionControl
import io.github.beppe2880.opencodemobile.feature.settings.customize.appliesTo
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_appearance
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_conversation
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_activity
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notifications
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_dark
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_light
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_system
import org.jetbrains.compose.resources.stringResource

/**
 * The classic settings list, section by section as it always was: every option in a card with
 * the same controls, order, conditions and test tags as the previews use.
 */
internal fun LazyListScope.settingsList(state: SettingsState, onEvent: (SettingsEvent) -> Unit, showNotifications: Boolean, demo: Boolean) {
    item(key = "servers") { ServersSection(state, onEvent, demo) }
    if (state.activeServerId != null) item(key = "usage") { UsageSection(onEvent) }
    item(key = "appearance") { AppearanceSection(state, onEvent) }
    item(key = "conversation") {
        OptionsSection(
            stringResource(Res.string.settings_conversation),
            state,
            onEvent,
            listOf(
                SettingOption.ShowReasoning,
                SettingOption.ExpandTools,
                SettingOption.GroupActivity,
                SettingOption.GroupThoughts,
                SettingOption.ShowCost,
                SettingOption.HeaderLine,
                SettingOption.HeaderStyle,
                SettingOption.HeaderOpens,
                SettingOption.SwipeChats,
                SettingOption.RunningInList,
                SettingOption.ConfirmStop,
                SettingOption.AgentOutline,
                SettingOption.QuestionsCollapsed,
                SettingOption.PermissionsCollapsed,
                SettingOption.Dictation,
                SettingOption.VoiceMode,
                SettingOption.VoiceProvider,
                SettingOption.VoiceModel,
                SettingOption.VoiceKey,
                SettingOption.VoiceLanguage,
                SettingOption.SelectionApply,
                SettingOption.NewChatModel,
                SettingOption.ShareMenu,
                SettingOption.ShareOpens,
            ),
        )
    }
    item(key = "live") {
        OptionsSection(
            stringResource(Res.string.settings_live_activity),
            state,
            onEvent,
            listOf(
                SettingOption.FollowRunning,
                SettingOption.LiveThinking,
                SettingOption.LiveShell,
                SettingOption.LiveEdits,
                SettingOption.LiveReads,
                SettingOption.LiveOther,
                SettingOption.FullCommands,
                SettingOption.ToolDetails,
                SettingOption.OutputLines,
                SettingOption.KeepLiveOpen,
                SettingOption.LiveInterval,
            ),
        )
    }
    if (showNotifications) {
        item(key = "notifications") {
            OptionsSection(
                stringResource(Res.string.settings_notifications),
                state,
                onEvent,
                listOf(SettingOption.Notifications, SettingOption.NotifyPermissions, SettingOption.NotifyQuestions, SettingOption.NotifyFinished),
            )
        }
    }
    item(key = "terminal") {
        OptionsSection(
            stringResource(Res.string.settings_terminal),
            state,
            onEvent,
            listOf(SettingOption.TerminalKeyboard, SettingOption.TerminalFullscreen),
        )
    }
    item(key = "reset") { ResetSection(onEvent) }
    item(key = "about") { AboutSection(state, onEvent, showUpdateNotice = true) }
}

/** [options] that apply in one card, divided by hairlines; the others are left out, like their switch says. */
@Composable
private fun OptionsSection(title: String, state: SettingsState, onEvent: (SettingsEvent) -> Unit, options: List<SettingOption>) {
    Section(title) {
        OcCard {
            options.filter { it.appliesTo(state.settings, state.notificationsOn) }.forEachIndexed { i, option ->
                if (i > 0) OcDivider()
                SettingOptionControl(option, state, onEvent)
            }
        }
    }
}

@Composable
private fun AppearanceSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val settings = state.settings
    // Only wide windows (unfolded foldables, tablets) can show the chat beside the list.
    val wide = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } >= SplitViewMinWidth
    Section(stringResource(Res.string.settings_appearance)) {
        OcSegmentedControl(
            segments = listOf(
                OcSegment(ThemeMode.System, stringResource(Res.string.settings_theme_system), "settings_theme_system"),
                OcSegment(ThemeMode.Light, stringResource(Res.string.settings_theme_light), "settings_theme_light"),
                OcSegment(ThemeMode.Dark, stringResource(Res.string.settings_theme_dark), "settings_theme_dark"),
            ),
            selected = settings.theme,
            onSelect = { onEvent(SettingsEvent.ThemeChanged(it)) },
        )
        OcCard {
            SettingOptionControl(SettingOption.TextSize, state, onEvent)
            if (wide) {
                listOf(SettingOption.SplitView, SettingOption.ListWidth, SettingOption.ListHandle, SettingOption.BackShowsList)
                    .filter { it.appliesTo(settings) }
                    .forEach { option ->
                        OcDivider()
                        SettingOptionControl(option, state, onEvent)
                    }
            }
        }
    }
}

/** Matches the width at which the app shows the chat beside the session list. */
private val SplitViewMinWidth = 600.dp
