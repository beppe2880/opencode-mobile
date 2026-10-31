package io.github.beppe2880.opencodemobile

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import io.github.beppe2880.opencodemobile.api.OpenCodeApiVersion
import io.github.beppe2880.opencodemobile.core.data.di.AppScope
import io.github.beppe2880.opencodemobile.core.data.di.dataModule
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateTarget
import io.github.beppe2880.opencodemobile.widget.WidgetSnapshots
import org.koin.core.context.startKoin
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

private var koinStarted = false

/**
 * Entry point used by iosApp: starts Koin once and returns the Compose root controller.
 * [reloadWidgets] asks WidgetKit to redraw the home screen widget (a Swift-only API).
 */
@Suppress("FunctionName", "unused")
fun MainViewController(reloadWidgets: () -> Unit): UIViewController {
    if (!koinStarted) {
        val version = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "dev"
        val appInfo = AppInfo(versionName = version, apiVersion = OpenCodeApiVersion.OPENCODE, updateTarget = UpdateTarget.Ios)
        val koin = startKoin { modules(dataModule, sharedModule(appInfo)) }.koin
        WidgetSnapshots(koin.get(), koin.get(), koin.get(), koin.get(), reloadWidgets).start(koin.get(AppScope))
        koinStarted = true
    }
    // The screens move content above the keyboard themselves (imePadding); the default
    // behavior would shift the focused field a second time.
    return ComposeUIViewController(configure = { onFocusBehavior = OnFocusBehavior.DoNothing }) {
        val sessionToOpen by AppLinks.sessionToOpen.collectAsState()
        val newSession by AppLinks.newSession.collectAsState()
        OpenCodeApp(
            openSessionId = sessionToOpen,
            onSessionOpened = { AppLinks.sessionToOpen.value = null },
            openNewSession = newSession,
            onNewSessionOpened = { AppLinks.newSession.value = false },
        )
    }
}
