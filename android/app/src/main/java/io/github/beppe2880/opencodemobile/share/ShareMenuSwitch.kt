package io.github.beppe2880.opencodemobile.share

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Shows or hides OpenCode in the share menu of other apps, following the Settings switch. */
class ShareMenuSwitch(private val context: Context, private val settings: SettingsRepository) {
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            settings.settings.map { it.showInShareMenu }.distinctUntilChanged().collect { shown ->
                context.packageManager.setComponentEnabledSetting(
                    ComponentName(context, ShareActivity::class.java),
                    if (shown) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
