package io.github.beppe2880.opencodemobile.feature.settings

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderStyle
import io.github.beppe2880.opencodemobile.core.domain.NewChatModel
import io.github.beppe2880.opencodemobile.core.domain.SelectionApply
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.domain.SettingsStyle
import io.github.beppe2880.opencodemobile.core.domain.ShareOpens
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.feature.settings.customize.SettingOption
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateTarget

/** Build information provided by the app module. */
data class AppInfo(
    val versionName: String,
    val apiVersion: String,
    /** Where this platform's app is updated from. */
    val updateTarget: UpdateTarget,
)

@Immutable
data class SettingsState(
    val servers: List<ServerProfile> = emptyList(),
    val activeServerId: String? = null,
    val settings: AppSettings = AppSettings(),
    val serverVersion: String? = null,
    val serverVersionLoading: Boolean = false,
    val appVersion: String = "",
    val apiVersion: String = "",
    /** Where this platform's app is updated from, for the update banner's preview. */
    val updateTarget: UpdateTarget = UpdateTarget.Android,
    val renaming: ServerProfile? = null,
    val removing: ServerProfile? = null,
    /** The confirmation to reset every setting is open. */
    val resetting: Boolean = false,
    /** The system lets the app post notifications. */
    val notificationsAllowed: Boolean = true,
    /** The permission was denied: only the system settings can grant it now. */
    val notificationsBlocked: Boolean = false,
    /** The group of settings open with its preview; null on the list of groups. */
    val area: SettingsArea? = null,
    /** The part of the preview whose options are in view. */
    val selected: SettingsPart? = null,
    /** The option a search result opened, highlighted until another part is picked. */
    val focus: SettingOption? = null,
    /** Chat previews show the agent at work instead of a finished answer. */
    val running: Boolean = false,
    /** Text of the search field; options matching it replace the groups. */
    val query: String = "",
) {
    /** Notifications are on and the system lets the app post them. */
    val notificationsOn: Boolean get() = settings.notifications && notificationsAllowed
}

sealed interface SettingsEvent {
    data object Back : SettingsEvent
    data object AddServer : SettingsEvent
    data object OpenUsage : SettingsEvent

    /** Opens a group of settings with its preview, at [part] or at the part of [option]. */
    data class OpenArea(val area: SettingsArea, val part: SettingsPart? = null, val option: SettingOption? = null) : SettingsEvent
    data object CloseArea : SettingsEvent

    /** A part of the preview was tapped, or its options scrolled into view. */
    data class SelectPart(val part: SettingsPart) : SettingsEvent
    data class PreviewRunning(val running: Boolean) : SettingsEvent
    data class QueryChanged(val query: String) : SettingsEvent
    data class EditMarksChanged(val visible: Boolean) : SettingsEvent
    data class SettingsStyleChanged(val style: SettingsStyle) : SettingsEvent

    /** The divider between preview and options was dragged, or a size was picked; 0 hides the preview. */
    data class PreviewFractionChanged(val fraction: Float) : SettingsEvent

    /** The terminal key bar goes back to its default keys. */
    data object ResetTerminalKeys : SettingsEvent
    data class SelectServer(val serverId: String) : SettingsEvent
    data class RequestRename(val server: ServerProfile) : SettingsEvent
    data class ConfirmRename(val name: String) : SettingsEvent
    data class RequestRemove(val server: ServerProfile) : SettingsEvent
    data object ConfirmRemove : SettingsEvent
    data object DismissDialog : SettingsEvent
    data class ThemeChanged(val theme: ThemeMode) : SettingsEvent
    data class ShowReasoningChanged(val enabled: Boolean) : SettingsEvent
    data class ExpandToolsChanged(val enabled: Boolean) : SettingsEvent
    data class FollowRunningToolsChanged(val enabled: Boolean) : SettingsEvent
    data class LiveThinkingChanged(val enabled: Boolean) : SettingsEvent
    data class LiveShellChanged(val enabled: Boolean) : SettingsEvent
    data class LiveEditsChanged(val enabled: Boolean) : SettingsEvent
    data class LiveReadsChanged(val enabled: Boolean) : SettingsEvent
    data class LiveOtherChanged(val enabled: Boolean) : SettingsEvent
    data class ShowFullCommandsChanged(val enabled: Boolean) : SettingsEvent
    data class ShowToolDetailsChanged(val enabled: Boolean) : SettingsEvent
    data class TerminalOpenKeyboardChanged(val enabled: Boolean) : SettingsEvent
    data class TerminalFullscreenByDefaultChanged(val enabled: Boolean) : SettingsEvent
    data class SplitViewChanged(val enabled: Boolean) : SettingsEvent
    data class ListPaneFractionChanged(val fraction: Float) : SettingsEvent
    data class ListPaneHandleChanged(val enabled: Boolean) : SettingsEvent
    data class BackShowsListChanged(val enabled: Boolean) : SettingsEvent
    data class OutputPreviewLinesChanged(val lines: Int) : SettingsEvent
    data class LiveOutputIntervalChanged(val millis: Long) : SettingsEvent
    data class KeepLiveStepsOpenChanged(val enabled: Boolean) : SettingsEvent
    data class ChatTextScaleChanged(val scale: Float) : SettingsEvent
    data object RequestReset : SettingsEvent
    data object ConfirmReset : SettingsEvent
    data class ShowCostChanged(val enabled: Boolean) : SettingsEvent
    data class ChatHeaderLineChanged(val line: ChatHeaderLine) : SettingsEvent
    data class HeaderOpensChanged(val scope: ChangesScope) : SettingsEvent
    data class ChatHeaderStyleChanged(val style: ChatHeaderStyle) : SettingsEvent
    data class SwipeBetweenChatsChanged(val enabled: Boolean) : SettingsEvent
    data class RunningInListChanged(val enabled: Boolean) : SettingsEvent
    data class ConfirmStopChanged(val enabled: Boolean) : SettingsEvent
    data class AgentColorOutlineChanged(val enabled: Boolean) : SettingsEvent
    data class DictationChanged(val enabled: Boolean) : SettingsEvent
    data class SelectionApplyChanged(val apply: SelectionApply) : SettingsEvent
    data class NewChatModelChanged(val model: NewChatModel) : SettingsEvent
    data class ShowInShareMenuChanged(val enabled: Boolean) : SettingsEvent
    data class ShareOpensChanged(val opens: ShareOpens) : SettingsEvent
    data class GroupActivityChanged(val enabled: Boolean) : SettingsEvent
    data class GroupThoughtsChanged(val enabled: Boolean) : SettingsEvent
    data class QuestionsCollapsedChanged(val enabled: Boolean) : SettingsEvent
    data class PermissionsCollapsedChanged(val enabled: Boolean) : SettingsEvent
    data class NotificationsChanged(val enabled: Boolean) : SettingsEvent
    data class NotifyPermissionsChanged(val enabled: Boolean) : SettingsEvent
    data class NotifyQuestionsChanged(val enabled: Boolean) : SettingsEvent
    data class NotifyFinishedChanged(val enabled: Boolean) : SettingsEvent
    data class CheckForUpdatesChanged(val enabled: Boolean) : SettingsEvent

    /** Notification access read from the system, or the result of the permission request. */
    data class NotificationAccess(val allowed: Boolean, val blocked: Boolean = false) : SettingsEvent
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object OpenConnect : SettingsEffect
    data object OpenUsage : SettingsEffect
    data object RequestNotificationPermission : SettingsEffect
    data object OpenNotificationSettings : SettingsEffect
}
