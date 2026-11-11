package io.github.beppe2880.opencodemobile.feature.settings.customize

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatSwipe
import io.github.beppe2880.opencodemobile.core.domain.DEFAULT_LIST_PANE_FRACTION
import io.github.beppe2880.opencodemobile.core.domain.NewChatModel
import io.github.beppe2880.opencodemobile.core.domain.SelectionApply
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.ShareOpens
import io.github.beppe2880.opencodemobile.core.domain.SttProvider
import io.github.beppe2880.opencodemobile.core.domain.TEXT_SCALE_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.core.domain.UnfoldedLayout
import io.github.beppe2880.opencodemobile.core.domain.UpdateNoticeStyle
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.domain.sttModel
import io.github.beppe2880.opencodemobile.core.ui.platform.ScreenCapabilities
import io.github.beppe2880.opencodemobile.core.ui.platform.shareTargetSupported
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_header
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_pending
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_running_line
import io.github.beppe2880.opencodemobile.core.ui.text.UiText
import io.github.beppe2880.opencodemobile.core.ui.theme.OpenCodeThemes
import io.github.beppe2880.opencodemobile.feature.settings.SettingsState
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_agent_outline
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_about_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_appearance_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_chat_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_composer_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_large_screens_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_live_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_notifications_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_servers_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_sessions_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_sharing_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_terminal_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_updates_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_voice_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_demo_server
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_agent_model
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_cloud
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_divider
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_folding
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_gestures
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_layout
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_output
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_group_transcript
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_edits
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_other
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_reads
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_shell
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_thinking
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_new_chat_model_last
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_new_chat_model_server
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_finished
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_permissions
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_questions
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_selection_apply_next
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_selection_apply_now
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_share_opens_choose
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_share_opens_new
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_apply
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_blocked
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_confirm_stop
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_cost
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_custom_keys
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_fullscreen
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_grouped
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_keyboard
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_list_width
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_new_chats
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_no_key
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_no_server
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_notice
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_off
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_on
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_reasoning
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_running
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_server_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_share_menu
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_split
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_swipe
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_swipe_drawer
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_default
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_large
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_largest
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_small
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_dark
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_light
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_system
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfolded_layout_panel
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_update_notice_banner
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_update_notice_off
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_update_notice_window
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_custom
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_mode_cloud
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_mode_system
import org.jetbrains.compose.resources.StringResource
import kotlin.math.roundToInt
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

/*
 * The categories of Settings, shared by both styles and the live demo: every [SettingOption]
 * belongs to exactly one [SettingsArea], listed here in the groups of its page in the List
 * style. The Preview style opens the same area at a [io.github.beppe2880.opencodemobile.core.domain.SettingsPart]
 * of it that offers the option (a test checks that one does).
 */

/** Options shown together under an optional [title] on a category page. */
class OptionGroup(val title: StringResource?, val options: List<SettingOption>)

/** The groups of options of this category's page, in order; empty for servers and about. */
val SettingsArea.groups: List<OptionGroup>
    get() = when (this) {
        SettingsArea.Servers, SettingsArea.About -> emptyList()
        SettingsArea.Appearance -> listOf(
            OptionGroup(null, listOf(SettingOption.Theme, SettingOption.OpenCodeTheme, SettingOption.TextSize)),
        )
        SettingsArea.Chat -> listOf(
            OptionGroup(
                Res.string.settings_group_transcript,
                listOf(
                    SettingOption.ShowReasoning,
                    SettingOption.GroupThoughts,
                    SettingOption.GroupActivity,
                    SettingOption.ExpandTools,
                    SettingOption.ToolDetails,
                    SettingOption.FullCommands,
                    SettingOption.ToolMediaSize,
                    SettingOption.ShowCost,
                ),
            ),
            OptionGroup(UiRes.string.oc_part_header, listOf(SettingOption.HeaderLine, SettingOption.HeaderStyle, SettingOption.HeaderOpens)),
            OptionGroup(UiRes.string.oc_part_pending, listOf(SettingOption.QuestionsCollapsed, SettingOption.PermissionsCollapsed)),
            OptionGroup(UiRes.string.oc_part_running_line, listOf(SettingOption.RunningChips)),
            OptionGroup(Res.string.settings_group_gestures, listOf(SettingOption.SwipeChats, SettingOption.SwipeSensitivity)),
        )
        SettingsArea.LiveActivity -> listOf(
            OptionGroup(
                null,
                listOf(
                    SettingOption.FollowRunning,
                    SettingOption.LiveThinking,
                    SettingOption.LiveShell,
                    SettingOption.LiveInterval,
                    SettingOption.LiveEdits,
                    SettingOption.LiveReads,
                    SettingOption.LiveOther,
                    SettingOption.KeepLiveOpen,
                ),
            ),
            OptionGroup(Res.string.settings_group_output, listOf(SettingOption.OutputLines)),
        )
        SettingsArea.Composer -> listOf(
            OptionGroup(null, listOf(SettingOption.AgentOutline)),
            OptionGroup(
                Res.string.settings_group_agent_model,
                listOf(SettingOption.SelectionApply, SettingOption.CloseModelPickerOnSelect, SettingOption.NewChatModel),
            ),
        )
        SettingsArea.VoiceInput -> listOf(
            OptionGroup(null, listOf(SettingOption.Dictation, SettingOption.VoiceMode)),
            OptionGroup(
                Res.string.settings_group_cloud,
                listOf(
                    SettingOption.VoiceProvider,
                    SettingOption.VoiceModel,
                    SettingOption.VoiceKey,
                    SettingOption.VoiceLanguage,
                    SettingOption.VoiceStyle,
                    SettingOption.VoiceHold,
                ),
            ),
        )
        SettingsArea.Sessions -> listOf(OptionGroup(null, listOf(SettingOption.RunningInList, SettingOption.ConfirmStop)))
        SettingsArea.LargeScreens -> listOf(
            OptionGroup(Res.string.settings_group_layout, listOf(SettingOption.SplitView, SettingOption.UnfoldedLayout, SettingOption.ListWidth)),
            OptionGroup(Res.string.settings_group_folding, listOf(SettingOption.UnfoldOpenChat, SettingOption.FoldOpenChat)),
            OptionGroup(Res.string.settings_group_divider, listOf(SettingOption.ListHandle, SettingOption.BackShowsList, SettingOption.ListSwipe)),
        )
        SettingsArea.Sharing -> listOf(OptionGroup(null, listOf(SettingOption.ShareMenu, SettingOption.ShareOpens)))
        SettingsArea.Notifications -> listOf(
            OptionGroup(
                null,
                listOf(SettingOption.Notifications, SettingOption.NotifyPermissions, SettingOption.NotifyQuestions, SettingOption.NotifyFinished),
            ),
        )
        SettingsArea.Terminal -> listOf(
            OptionGroup(null, listOf(SettingOption.TerminalKeys, SettingOption.TerminalKeyboard, SettingOption.TerminalFullscreen)),
        )
        SettingsArea.Updates -> listOf(OptionGroup(null, listOf(SettingOption.UpdateNotice)))
    }

/** Every option of this category, in page order. */
val SettingsArea.options: List<SettingOption> get() = groups.flatMap { it.options }

/** The category whose page lists this option. */
val SettingOption.area: SettingsArea get() = SettingsArea.entries.first { this in it.options }

/** The group of its category's page that lists this option. */
val SettingOption.group: OptionGroup get() = area.groups.first { this in it.options }

/**
 * The option this one is nested under on its page: the switch it [requires], or the choice that
 * hides it (a folder path alone has no header style).
 */
val SettingOption.parent: SettingOption?
    get() = requires ?: when (this) {
        SettingOption.HeaderStyle, SettingOption.HeaderOpens -> SettingOption.HeaderLine
        else -> null
    }

/** How deep the option is nested under its parents within [group]. */
fun SettingOption.depthIn(group: OptionGroup): Int = generateSequence(parent) { it.parent }.takeWhile { it in group.options }.count()

/**
 * The categories this device has, in order: sharing to the app only exists on Android,
 * notifications where [notifications] are supported, and foldable and large screens where the
 * [screen] can give the app a wide window (a foldable even while folded, a tablet), not on phones.
 */
fun visibleAreas(notifications: Boolean, screen: ScreenCapabilities, sharing: Boolean = shareTargetSupported): List<SettingsArea> =
    SettingsArea.entries.filter { area ->
        when (area) {
            SettingsArea.Notifications -> notifications
            SettingsArea.Sharing -> sharing
            SettingsArea.LargeScreens -> screen.largeScreen
            else -> true
        }
    }

/**
 * The groups of [area] as its page shows them with the current settings: only the options this
 * device has (see [availableOn]) that apply (dependent ones appear under their parent once it
 * allows them) and the groups left with any. The large screen options show on narrow windows too,
 * to be set before unfolding.
 */
fun visibleGroups(area: SettingsArea, settings: AppSettings, notificationsOn: Boolean, screen: ScreenCapabilities): List<OptionGroup> =
    area.groups.mapNotNull { group ->
        val shown = group.options.filter { it.availableOn(screen) && it.appliesTo(settings, notificationsOn) }
        if (shown.isEmpty()) null else OptionGroup(group.title, shown)
    }

/** Whether a search can offer the option: in one of the categories shown, [areas], and on this device. */
fun SettingOption.findableIn(areas: List<SettingsArea>, screen: ScreenCapabilities): Boolean = area in areas && availableOn(screen)

/**
 * Where to point at [option] on its page: the option itself when it is shown, or else the
 * nearest parent that is, which brings it back.
 */
fun SettingOption.anchor(settings: AppSettings, notificationsOn: Boolean): SettingOption =
    generateSequence(this) { it.parent }.firstOrNull { it.appliesTo(settings, notificationsOn) } ?: this

/** What the category covers, on its card in the Preview style and for search. */
val SettingsArea.description: StringResource
    get() = when (this) {
        SettingsArea.Servers -> Res.string.settings_area_servers_sub
        SettingsArea.Appearance -> Res.string.settings_area_appearance_sub
        SettingsArea.Chat -> Res.string.settings_area_chat_sub
        SettingsArea.LiveActivity -> Res.string.settings_area_live_sub
        SettingsArea.Composer -> Res.string.settings_area_composer_sub
        SettingsArea.VoiceInput -> Res.string.settings_area_voice_sub
        SettingsArea.Sessions -> Res.string.settings_area_sessions_sub
        SettingsArea.LargeScreens -> Res.string.settings_area_large_screens_sub
        SettingsArea.Sharing -> Res.string.settings_area_sharing_sub
        SettingsArea.Terminal -> Res.string.settings_area_terminal_sub
        SettingsArea.Notifications -> Res.string.settings_area_notifications_sub
        SettingsArea.Updates -> Res.string.settings_area_updates_sub
        SettingsArea.About -> Res.string.settings_area_about_sub
    }

/**
 * The current values of [area] in a few words, joined into one line by its row on the first
 * page; empty when there is nothing to say, and the row shows [description] instead.
 */
fun summary(area: SettingsArea, state: SettingsState, demo: Boolean = false): List<UiText> {
    val s = state.settings
    fun res(id: StringResource, vararg args: Any) = UiText.Resource(id, args.toList())
    fun on(vararg features: Pair<Boolean, StringResource>) = features.filter { it.first }.map { res(it.second) }
    return when (area) {
        SettingsArea.Servers -> when {
            demo -> listOf(res(Res.string.settings_demo_server))
            state.servers.isEmpty() -> listOf(res(Res.string.settings_summary_no_server))
            // The active server first, then the others.
            else -> state.servers.sortedByDescending { it.id == state.activeServerId }.map { UiText.Raw(it.name) }
        }
        SettingsArea.Appearance -> listOfNotNull(
            res(s.theme.labelRes),
            OpenCodeThemes.byId(s.openCodeTheme)?.let { UiText.Raw(it.displayName) },
            res(textSizeLabel(s.chatTextScale)),
        )
        SettingsArea.Chat -> on(
            s.showReasoning to Res.string.settings_summary_reasoning,
            s.groupActivity to Res.string.settings_summary_grouped,
            s.showCost to Res.string.settings_summary_cost,
            (s.chatSwipe == ChatSwipe.SwitchChats) to Res.string.settings_summary_swipe,
            (s.chatSwipe == ChatSwipe.SessionDrawer) to Res.string.settings_summary_swipe_drawer,
        )
        SettingsArea.LiveActivity -> if (!s.followRunningTools) {
            listOf(res(Res.string.settings_summary_off))
        } else {
            on(
                s.liveThinking to Res.string.settings_live_thinking,
                s.liveShell to Res.string.settings_live_shell,
                s.liveEdits to Res.string.settings_live_edits,
                s.liveReads to Res.string.settings_live_reads,
                s.liveOther to Res.string.settings_live_other,
            ).ifEmpty { listOf(res(Res.string.settings_summary_on)) }
        }
        SettingsArea.Composer -> on(s.agentColorOutline to Res.string.settings_agent_outline) + listOf(
            res(
                Res.string.settings_summary_apply,
                res(if (s.selectionApply == SelectionApply.NextMessage) Res.string.settings_selection_apply_next else Res.string.settings_selection_apply_now),
            ),
            res(
                Res.string.settings_summary_new_chats,
                res(if (s.newChatModel == NewChatModel.LastUsed) Res.string.settings_new_chat_model_last else Res.string.settings_new_chat_model_server),
            ),
        )
        SettingsArea.VoiceInput -> when {
            !s.dictation -> listOf(res(Res.string.settings_summary_off))
            s.voiceEngine == VoiceEngine.System -> listOf(res(Res.string.settings_voice_mode_system))
            else -> listOfNotNull(
                res(Res.string.settings_voice_mode_cloud),
                if (s.sttProvider == SttProvider.Custom) res(Res.string.settings_voice_custom) else UiText.Raw(s.sttProvider.label),
                UiText.Raw(s.sttModel()),
                if (s.sttProvider != SttProvider.Custom && s.sttProvider !in state.sttKeys) res(Res.string.settings_summary_no_key) else null,
            )
        }
        SettingsArea.Sessions -> on(
            s.showRunningWorkInList to Res.string.settings_summary_running,
            s.confirmStopCommand to Res.string.settings_summary_confirm_stop,
        )
        SettingsArea.LargeScreens -> when {
            !s.splitView -> listOf(res(Res.string.settings_summary_off))
            s.unfoldedLayout == UnfoldedLayout.ListPanel -> listOf(res(Res.string.settings_unfolded_layout_panel))
            else -> listOf(
                res(Res.string.settings_summary_split),
                res(Res.string.settings_summary_list_width, ((s.listPaneFraction ?: DEFAULT_LIST_PANE_FRACTION) * 100).roundToInt()),
            )
        }
        SettingsArea.Sharing -> if (!s.showInShareMenu) {
            listOf(res(Res.string.settings_summary_off))
        } else {
            listOf(
                res(Res.string.settings_summary_share_menu),
                res(if (s.shareOpens == ShareOpens.NewSession) Res.string.settings_share_opens_new else Res.string.settings_share_opens_choose),
            )
        }
        SettingsArea.Notifications -> when {
            state.notificationsBlocked && !state.notificationsAllowed -> listOf(res(Res.string.settings_summary_blocked))
            !state.notificationsOn -> listOf(res(Res.string.settings_summary_off))
            else -> on(
                s.notifyPermissions to Res.string.settings_notify_permissions,
                s.notifyQuestions to Res.string.settings_notify_questions,
                s.notifyFinished to Res.string.settings_notify_finished,
            ).ifEmpty { listOf(res(Res.string.settings_summary_on)) }
        }
        SettingsArea.Terminal -> on(
            (s.terminalKeys != null) to Res.string.settings_summary_custom_keys,
            s.terminalOpenKeyboard to Res.string.settings_summary_keyboard,
            s.terminalFullscreenByDefault to Res.string.settings_summary_fullscreen,
        )
        SettingsArea.Updates -> listOf(res(Res.string.settings_summary_notice, res(s.updateNotice.labelRes)))
        SettingsArea.About -> listOfNotNull(
            res(Res.string.settings_summary_version, state.appVersion),
            state.serverVersion?.let { res(Res.string.settings_summary_server_version, it) },
        )
    }
}

internal val ThemeMode.labelRes: StringResource
    get() = when (this) {
        ThemeMode.System -> Res.string.settings_theme_system
        ThemeMode.Light -> Res.string.settings_theme_light
        ThemeMode.Dark -> Res.string.settings_theme_dark
    }

/** The name of the chat text size [scale], as its choice shows it. */
internal fun textSizeLabel(scale: Float): StringResource = TEXT_SIZE_LABELS[TEXT_SCALE_CHOICES.indexOf(scale).takeIf { it >= 0 } ?: 1]

internal val TEXT_SIZE_LABELS =
    listOf(Res.string.settings_text_small, Res.string.settings_text_default, Res.string.settings_text_large, Res.string.settings_text_largest)

internal val UpdateNoticeStyle.labelRes: StringResource
    get() = when (this) {
        UpdateNoticeStyle.Window -> Res.string.settings_update_notice_window
        UpdateNoticeStyle.Banner -> Res.string.settings_update_notice_banner
        UpdateNoticeStyle.Off -> Res.string.settings_update_notice_off
    }
