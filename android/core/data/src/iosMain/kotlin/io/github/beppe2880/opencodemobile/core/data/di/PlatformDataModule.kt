package io.github.beppe2880.opencodemobile.core.data.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.data.cache.cacheDatabase
import io.github.beppe2880.opencodemobile.core.data.events.AppVisibility
import io.github.beppe2880.opencodemobile.core.data.events.ApplicationStateVisibility
import io.github.beppe2880.opencodemobile.core.data.events.NetworkMonitor
import io.github.beppe2880.opencodemobile.core.data.events.PathNetworkMonitor
import io.github.beppe2880.opencodemobile.core.data.server.InterfaceServerDiscovery
import io.github.beppe2880.opencodemobile.core.data.server.KeychainServerStore
import io.github.beppe2880.opencodemobile.core.data.server.ServerStore
import io.github.beppe2880.opencodemobile.core.data.share.MemoryShareStore
import io.github.beppe2880.opencodemobile.core.data.share.ShareStore
import io.github.beppe2880.opencodemobile.core.domain.ServerDiscovery
import io.github.beppe2880.opencodemobile.core.network.OpenCodeClientFactory
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
internal actual val platformDataModule: Module = module {
    single { OpenCodeClientFactory(debugLogging = Platform.isDebugBinary) }
    single<ServerStore> { KeychainServerStore() }
    single<AppVisibility> { ApplicationStateVisibility() }
    single { cacheDatabase() }
    single<ServerDiscovery> { InterfaceServerDiscovery() }
    single<NetworkMonitor> { PathNetworkMonitor() }
    single<Settings>(SettingsStore) { NSUserDefaultsSettings(NSUserDefaults(suiteName = "settings")) }
    // Nothing can be shared to the iOS app yet (no share extension).
    single<ShareStore> { MemoryShareStore() }
    single<Settings>(DraftsStore) { NSUserDefaultsSettings(NSUserDefaults(suiteName = "drafts")) }
}
