package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.github.beppe2880.opencodemobile.core.domain.APP_LOCK_MAX_MINUTES
import io.github.beppe2880.opencodemobile.core.domain.APP_LOCK_MINUTE_CHOICES
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSwitchRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextInputDialog
import io.github.beppe2880.opencodemobile.core.ui.platform.DeviceAuthResult
import io.github.beppe2880.opencodemobile.core.ui.platform.rememberDeviceAuthenticator
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
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_lock_unavailable
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

/** Every time the app opens, after a few preset minutes away, or after a custom number of minutes. */
@Composable
internal fun AppLockAfterControl(title: String, minutes: Int, onEvent: (SettingsEvent) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    val choices = APP_LOCK_MINUTE_CHOICES.map { value ->
        OcSegment(
            value,
            if (value == 0) stringResource(Res.string.settings_app_lock_always) else stringResource(Res.string.settings_app_lock_minutes, value),
            "settings_app_lock_after_$value",
        )
    } + OcSegment(
        CUSTOM,
        if (minutes in APP_LOCK_MINUTE_CHOICES) {
            stringResource(Res.string.settings_app_lock_custom)
        } else {
            stringResource(Res.string.settings_app_lock_minutes, minutes)
        },
        "settings_app_lock_after_custom",
    )
    ChoiceRow(
        title = title,
        subtitle = if (minutes == 0) {
            stringResource(Res.string.settings_app_lock_after_always_sub)
        } else {
            pluralStringResource(Res.plurals.settings_app_lock_after_sub, minutes, minutes)
        },
        choices = choices,
        selected = if (minutes in APP_LOCK_MINUTE_CHOICES) minutes else CUSTOM,
        onSelect = { if (it == CUSTOM) choosing = true else onEvent(SettingsEvent.AppLockAfterChanged(it)) },
    )
    if (choosing) {
        OcTextInputDialog(
            title = stringResource(Res.string.settings_app_lock_custom_title),
            initialValue = if (minutes in APP_LOCK_MINUTE_CHOICES) "" else minutes.toString(),
            confirmLabel = stringResource(Res.string.settings_save),
            onConfirm = { text ->
                text.toIntOrNull()?.let { onEvent(SettingsEvent.AppLockAfterChanged(it)) }
                choosing = false
            },
            onDismiss = { choosing = false },
            testTagPrefix = "settings_app_lock_custom",
            label = stringResource(Res.string.settings_app_lock_custom_label),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isValid = { it.toIntOrNull() in 1..APP_LOCK_MAX_MINUTES },
        )
    }
}
