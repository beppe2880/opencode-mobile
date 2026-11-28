package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.github.beppe2880.opencodemobile.core.domain.APP_LOCK_MAX_MINUTES
import io.github.beppe2880.opencodemobile.core.domain.APP_LOCK_MINUTE_CHOICES
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSwitchRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextInputDialog
import io.github.beppe2880.opencodemobile.core.ui.platform.DeviceAuthResult
import io.github.beppe2880.opencodemobile.core.ui.platform.rememberDeviceAuthenticator
import io.github.beppe2880.opencodemobile.core.ui.platform.rememberLocationPermission
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.ChoiceRow
import io.github.beppe2880.opencodemobile.feature.settings.SettingsEvent
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_after_always_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_after_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_always
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_confirm_subtitle
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_confirm_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_custom
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_custom_label
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_custom_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_minutes
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_screen_off_always_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_screen_off_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_unavailable
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_wifi_add
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_wifi_denied
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_wifi_missing
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_wifi_remove
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_save
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The app lock switch. Turning it on asks for the system prompt first, so it can't be turned on
 * where it would not open; without any screen lock on the device it is off and says why (checked
 * again when the user comes back from setting one up).
 */
@Composable
internal fun AppLockControl(title: String, subtitle: String, enabled: Boolean, onEvent: (SettingsEvent) -> Unit, modifier: Modifier = Modifier) {
    val authenticator = rememberDeviceAuthenticator()
    val scope = rememberCoroutineScope()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val available = remember(lifecycleState) { authenticator.isAvailable() }
    val confirmTitle = stringResource(Res.string.settings_app_lock_confirm_title)
    val confirmSubtitle = stringResource(Res.string.settings_app_lock_confirm_subtitle)
    var confirming by remember { mutableStateOf(false) }
    OcSwitchRow(
        title = title,
        subtitle = if (available || enabled) subtitle else stringResource(Res.string.settings_app_lock_unavailable),
        checked = enabled,
        enabled = (available || enabled) && !confirming,
        onCheckedChange = { on ->
            if (!on) {
                onEvent(SettingsEvent.AppLockChanged(false))
            } else {
                confirming = true
                scope.launch {
                    try {
                        if (authenticator.authenticate(confirmTitle, confirmSubtitle) == DeviceAuthResult.Success) {
                            onEvent(SettingsEvent.AppLockChanged(true))
                        }
                    } finally {
                        confirming = false
                    }
                }
            }
        },
        modifier = modifier,
    )
}

/** Value of the custom segment: the minutes come from a dialog. */
private const val CUSTOM = -1

/** Which absence a [AppLockMinutesControl] times: leaving for other apps, or the screen off. */
internal enum class LockAbsence { Leave, ScreenOff }

/** Every time, after a few preset minutes, or after a custom number of minutes of [absence]. */
@Composable
internal fun AppLockMinutesControl(title: String, absence: LockAbsence, minutes: Int, onChange: (Int) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    val tag = if (absence == LockAbsence.Leave) "settings_app_lock_after" else "settings_app_lock_screen_off_after"
    val choices = APP_LOCK_MINUTE_CHOICES.map { value ->
        OcSegment(
            value,
            if (value == 0) stringResource(Res.string.settings_app_lock_always) else stringResource(Res.string.settings_app_lock_minutes, value),
            "${tag}_$value",
        )
    } + OcSegment(
        CUSTOM,
        if (minutes in APP_LOCK_MINUTE_CHOICES) {
            stringResource(Res.string.settings_app_lock_custom)
        } else {
            stringResource(Res.string.settings_app_lock_minutes, minutes)
        },
        "${tag}_custom",
    )
    ChoiceRow(
        title = title,
        subtitle = when {
            absence == LockAbsence.Leave && minutes == 0 -> stringResource(Res.string.settings_app_lock_after_always_sub)
            absence == LockAbsence.Leave -> pluralStringResource(Res.plurals.settings_app_lock_after_sub, minutes, minutes)
            minutes == 0 -> stringResource(Res.string.settings_app_lock_screen_off_always_sub)
            else -> pluralStringResource(Res.plurals.settings_app_lock_screen_off_sub, minutes, minutes)
        },
        choices = choices,
        selected = if (minutes in APP_LOCK_MINUTE_CHOICES) minutes else CUSTOM,
        onSelect = { if (it == CUSTOM) choosing = true else onChange(it) },
    )
    if (choosing) {
        OcTextInputDialog(
            title = stringResource(Res.string.settings_app_lock_custom_title),
            initialValue = if (minutes in APP_LOCK_MINUTE_CHOICES) "" else minutes.toString(),
            confirmLabel = stringResource(Res.string.settings_save),
            onConfirm = { text ->
                text.toIntOrNull()?.let(onChange)
                choosing = false
            },
            onDismiss = { choosing = false },
            testTagPrefix = "${tag}_custom",
            label = stringResource(Res.string.settings_app_lock_custom_label),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isValid = { it.toIntOrNull() in 1..APP_LOCK_MAX_MINUTES },
        )
    }
}

/**
 * The trusted Wi-Fi networks, each with a remove button, and a button that adds the current one.
 * Android only tells the network name with the location permission, asked here.
 */
@Composable
internal fun TrustedWifiControl(title: String, subtitle: String, networks: List<String>, nameMissing: Boolean, onEvent: (SettingsEvent) -> Unit) {
    val c = OcTheme.colors
    val permission = rememberLocationPermission()
    var denied by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        networks.forEach { name ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Wifi, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
                Text(name, style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f).padding(start = OcSpacing.sm))
                OcIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(Res.string.settings_app_lock_wifi_remove, name),
                    onClick = { onEvent(SettingsEvent.RemoveTrustedWifi(name)) },
                    modifier = Modifier.testTag("settings_app_lock_wifi_remove_$name"),
                )
            }
        }
        OcButton(
            text = stringResource(Res.string.settings_app_lock_wifi_add),
            onClick = {
                permission.request { granted ->
                    denied = !granted
                    if (granted) onEvent(SettingsEvent.AddCurrentWifi)
                }
            },
            variant = OcButtonVariant.Secondary,
            leadingIcon = Icons.Outlined.Add,
            modifier = Modifier.testTag("settings_app_lock_wifi_add"),
        )
        when {
            denied -> Res.string.settings_app_lock_wifi_denied
            nameMissing -> Res.string.settings_app_lock_wifi_missing
            else -> null
        }?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = c.danger.fg) }
    }
}
