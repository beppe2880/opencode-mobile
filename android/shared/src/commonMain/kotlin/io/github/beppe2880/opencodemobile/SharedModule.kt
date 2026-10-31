package io.github.beppe2880.opencodemobile

import io.github.beppe2880.opencodemobile.feature.chat.ChatViewModel
import io.github.beppe2880.opencodemobile.feature.chat.ImageMemory
import io.github.beppe2880.opencodemobile.feature.chat.ScrollMemory
import io.github.beppe2880.opencodemobile.feature.connect.ConnectViewModel
import io.github.beppe2880.opencodemobile.feature.files.FileViewerViewModel
import io.github.beppe2880.opencodemobile.feature.sessions.SessionsViewModel
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import io.github.beppe2880.opencodemobile.feature.settings.SettingsViewModel
import io.github.beppe2880.opencodemobile.feature.settings.usage.UsageViewModel
import io.github.beppe2880.opencodemobile.feature.terminal.TerminalViewModel
import io.github.beppe2880.opencodemobile.widget.WidgetLoader
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** ViewModels and UI state holders shared by the Android and iOS apps. */
fun sharedModule(appInfo: AppInfo): Module = module {
    single { appInfo }
    viewModel { ConnectViewModel(get(), get()) }
    viewModel { SessionsViewModel(get(), get(), get(), get(), get(), get(), get()) }
    // Kept across chats for the whole app run.
    single { ImageMemory() }
    single { ScrollMemory() }
    viewModel { params -> ChatViewModel(params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { UsageViewModel(get(), get()) }
    viewModel { params -> TerminalViewModel(params.get(), get(), get()) }
    viewModel { params -> FileViewerViewModel(params.get(), get()) }
    // Home screen widget: Glance on Android, WidgetKit on iOS.
    single { WidgetLoader(get(), get()) }
}
