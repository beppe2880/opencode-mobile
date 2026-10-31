package io.github.beppe2880.opencodemobile

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.beppe2880.opencodemobile.api.OpenCodeApiVersion
import io.github.beppe2880.opencodemobile.core.data.di.AppScope
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateTarget
import io.github.beppe2880.opencodemobile.notifications.AndroidMonitorNotifier
import io.github.beppe2880.opencodemobile.notifications.SessionMonitor
import io.github.beppe2880.opencodemobile.notifications.SessionMonitorService
import io.github.beppe2880.opencodemobile.widget.WidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val AppVisible = named("AppVisible")

val appModule = module {
    includes(
        sharedModule(
            AppInfo(versionName = BuildConfig.VERSION_NAME, apiVersion = OpenCodeApiVersion.OPENCODE, updateTarget = UpdateTarget.Android),
        ),
    )

    single { AndroidMonitorNotifier(androidContext()) }
    // Whether a screen of the app is shown.
    single(AppVisible) {
        ProcessLifecycleOwner.get().lifecycle.currentStateFlow
            .map { it.isAtLeast(Lifecycle.State.STARTED) }
            .stateIn(get(AppScope), SharingStarted.Eagerly, false)
    }
    single { WidgetUpdater(androidContext(), get(), get(), get(AppVisible)) }
    single {
        SessionMonitor(
            events = get(),
            sessions = get(),
            chat = get(),
            settings = get(),
            servers = get(),
            notifier = get<AndroidMonitorNotifier>(),
            visible = get(AppVisible),
            startService = { SessionMonitorService.start(androidContext()) },
        )
    }
}
