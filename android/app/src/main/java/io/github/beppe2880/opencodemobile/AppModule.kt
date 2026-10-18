package io.github.beppe2880.opencodemobile

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.beppe2880.opencodemobile.api.OpenCodeApiVersion
import io.github.beppe2880.opencodemobile.core.data.di.AppScope
import io.github.beppe2880.opencodemobile.feature.chat.ChatViewModel
import io.github.beppe2880.opencodemobile.feature.chat.ImageMemory
import io.github.beppe2880.opencodemobile.feature.chat.ScrollMemory
import io.github.beppe2880.opencodemobile.feature.connect.ConnectViewModel
import io.github.beppe2880.opencodemobile.feature.sessions.SessionsViewModel
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import io.github.beppe2880.opencodemobile.feature.settings.SettingsViewModel
import io.github.beppe2880.opencodemobile.feature.settings.usage.UsageViewModel
import io.github.beppe2880.opencodemobile.notifications.AndroidMonitorNotifier
import io.github.beppe2880.opencodemobile.notifications.SessionMonitor
import io.github.beppe2880.opencodemobile.notifications.SessionMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppInfo(versionName = BuildConfig.VERSION_NAME, apiVersion = OpenCodeApiVersion.OPENCODE) }
    viewModel { ConnectViewModel(get(), get()) }
    viewModel { SessionsViewModel(get(), get(), get(), get(), get()) }
    // Kept across chats for the whole app run.
    single { ImageMemory() }
    single { ScrollMemory() }
    viewModel { params -> ChatViewModel(params.get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { UsageViewModel(get(), get()) }

    single { AndroidMonitorNotifier(androidContext()) }
    single {
        val visible = ProcessLifecycleOwner.get().lifecycle.currentStateFlow
            .map { it.isAtLeast(Lifecycle.State.STARTED) }
            .stateIn(get(AppScope), SharingStarted.Eagerly, false)
        SessionMonitor(
            events = get(),
            sessions = get(),
            chat = get(),
            settings = get(),
            servers = get(),
            notifier = get<AndroidMonitorNotifier>(),
            visible = visible,
            startService = { SessionMonitorService.start(androidContext()) },
        )
    }
}
