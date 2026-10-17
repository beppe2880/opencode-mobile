package io.github.beppe2880.opencodemobile.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import io.github.beppe2880.opencodemobile.R
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.ui.components.OcConfirmDialog
import kotlinx.coroutines.launch

/**
 * Asked once, when a server is first connected (or on the first start after an update): explains
 * what session notifications do and asks for the permission. Declining turns them off; they can
 * be turned on again in Settings.
 */
@Composable
fun NotificationsIntro(settings: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun answer(enabled: Boolean) {
        scope.launch { settings.update { it.copy(notifications = enabled, notificationsIntroShown = true) } }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), ::answer)
    OcConfirmDialog(
        title = stringResource(R.string.notif_intro_title),
        text = stringResource(R.string.notif_intro_text),
        confirmLabel = stringResource(R.string.notif_intro_allow),
        dismissLabel = stringResource(R.string.notif_intro_later),
        onConfirm = {
            val allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
            if (!allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                answer(true)
            }
        },
        onDismiss = { answer(false) },
        testTagPrefix = "notifications_intro",
    )
}
