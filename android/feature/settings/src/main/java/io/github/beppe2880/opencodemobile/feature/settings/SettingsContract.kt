package io.github.beppe2880.opencodemobile.feature.settings

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode

/** Build information provided by the app module. */
data class AppInfo(val versionName: String, val apiVersion: String)

@Immutable
data class SettingsState(
    val servers: List<ServerProfile> = emptyList(),
    val activeServerId: String? = null,
    val settings: AppSettings = AppSettings(),
    val serverVersion: String? = null,
    val serverVersionLoading: Boolean = false,
    val appVersion: String = "",
    val apiVersion: String = "",
    val renaming: ServerProfile? = null,
    val removing: ServerProfile? = null,
    /** The system lets the app post notifications. */
    val notificationsAllowed: Boolean = true,
    /** The permission was denied: only the system settings can grant it now. */
    val notificationsBlocked: Boolean = false,
)

sealed interface SettingsEvent {
    data object Back : SettingsEvent
    data object AddServer : SettingsEvent
    data class SelectServer(val serverId: String) : SettingsEvent
    data class RequestRename(val server: ServerProfile) : SettingsEvent
    data class ConfirmRename(val name: String) : SettingsEvent
    data class RequestRemove(val server: ServerProfile) : SettingsEvent
    data object ConfirmRemove : SettingsEvent
    data object DismissDialog : SettingsEvent
    data class ThemeChanged(val theme: ThemeMode) : SettingsEvent
    data class ShowReasoningChanged(val enabled: Boolean) : SettingsEvent
    data class ExpandToolsChanged(val enabled: Boolean) : SettingsEvent
    data class ShowCostChanged(val enabled: Boolean) : SettingsEvent
    data class SwipeBetweenChatsChanged(val enabled: Boolean) : SettingsEvent
    data class AgentColorOutlineChanged(val enabled: Boolean) : SettingsEvent
    data class GroupActivityChanged(val enabled: Boolean) : SettingsEvent
    data class NotificationsChanged(val enabled: Boolean) : SettingsEvent

    /** Notification access read from the system, or the result of the permission request. */
    data class NotificationAccess(val allowed: Boolean, val blocked: Boolean = false) : SettingsEvent
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object OpenConnect : SettingsEffect
    data object RequestNotificationPermission : SettingsEffect
    data object OpenNotificationSettings : SettingsEffect
}
