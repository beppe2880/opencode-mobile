package io.github.beppe2880.opencodemobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.beppe2880.opencodemobile.core.domain.AppLock
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.platform.DeviceAuthResult
import io.github.beppe2880.opencodemobile.core.ui.platform.rememberDeviceAuthenticator
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.shared.resources.Res
import io.github.beppe2880.opencodemobile.shared.resources.app_lock_failed
import io.github.beppe2880.opencodemobile.shared.resources.app_lock_message
import io.github.beppe2880.opencodemobile.shared.resources.app_lock_prompt_subtitle
import io.github.beppe2880.opencodemobile.shared.resources.app_lock_prompt_title
import io.github.beppe2880.opencodemobile.shared.resources.app_lock_title
import io.github.beppe2880.opencodemobile.shared.resources.app_lock_unlock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Covers the whole app while [lock] is locked, and asks for the
 * system prompt each time the app comes to the foreground; the button asks again after a cancel.
 * A device without any screen lock cannot authenticate (removing it needed the passcode), so it
 * opens.
 */
@Composable
internal fun AppLockScreen(lock: AppLock, darkBars: Boolean) {
    val c = OcTheme.colors
    val authenticator = rememberDeviceAuthenticator()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val promptTitle = stringResource(Res.string.app_lock_prompt_title)
    val promptSubtitle = stringResource(Res.string.app_lock_prompt_subtitle)
    var prompting by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    fun prompt() {
        if (prompting) return
        prompting = true
        failed = false
        lock.authenticating(true)
        scope.launch {
            try {
                when (authenticator.authenticate(promptTitle, promptSubtitle)) {
                    DeviceAuthResult.Success, DeviceAuthResult.Unavailable -> lock.unlock()
                    DeviceAuthResult.Failed -> failed = true
                    DeviceAuthResult.Cancelled -> Unit
                }
            } finally {
                lock.authenticating(false)
                prompting = false
            }
        }
    }

    LaunchedEffect(Unit) { focus.clearFocus(force = true) }
    // Once when it appears and at each return to the foreground: leaving the app for the
    // prompt itself (the device passcode screen) does not count, so a cancel does not loop.
    LaunchedEffect(lock) { lock.returns.collect { prompt() } }
    // A window of its own covers the dialogs and sheets the app had open, and takes Back.
    Dialog(onDismissRequest = {}, properties = lockDialogProperties()) {
        LockWindowBars(darkBars)
        Box(
            Modifier
                .fillMaxSize()
                .background(c.bgBase)
                .safeDrawingPadding()
                .testTag("app_lock_screen"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.widthIn(max = 360.dp).padding(OcSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.iconBase, modifier = Modifier.padding(bottom = OcSpacing.sm).size(40.dp))
                Text(
                    stringResource(Res.string.app_lock_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textBase,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(if (failed) Res.string.app_lock_failed else Res.string.app_lock_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (failed) c.danger.fg else c.textMuted,
                    textAlign = TextAlign.Center,
                )
                OcButton(
                    text = stringResource(Res.string.app_lock_unlock),
                    onClick = ::prompt,
                    loading = prompting,
                    modifier = Modifier.padding(top = OcSpacing.sm).testTag("app_lock_unlock"),
                )
            }
        }
    }
}

/** Full screen and edge to edge, closed only by unlocking. */
internal expect fun lockDialogProperties(): DialogProperties

/** The status and navigation bar icons of the lock screen's own window follow the theme. */
@Composable
internal expect fun LockWindowBars(dark: Boolean)
