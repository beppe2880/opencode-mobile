package io.github.beppe2880.opencodemobile.core.data.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.beppe2880.opencodemobile.core.data.cache.cacheDatabase
import io.github.beppe2880.opencodemobile.core.data.events.AppVisibility
import io.github.beppe2880.opencodemobile.core.data.events.ConnectivityNetworkMonitor
import io.github.beppe2880.opencodemobile.core.data.events.NetworkMonitor
import io.github.beppe2880.opencodemobile.core.data.events.ProcessLifecycleVisibility
import io.github.beppe2880.opencodemobile.core.data.server.EncryptedServerStore
import io.github.beppe2880.opencodemobile.core.data.server.LanServerDiscovery
import io.github.beppe2880.opencodemobile.core.data.server.ServerStore
import io.github.beppe2880.opencodemobile.core.data.share.FileShareStore
import io.github.beppe2880.opencodemobile.core.data.share.ShareStore
import io.github.beppe2880.opencodemobile.core.domain.ServerDiscovery
import io.github.beppe2880.opencodemobile.core.network.OpenCodeClientFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

internal actual val platformDataModule: Module = module {
    single {
        val debuggable = androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        OpenCodeClientFactory(debugLogging = debuggable)
    }
    single<ServerStore> { EncryptedServerStore(androidContext()) }
    single<AppVisibility> { ProcessLifecycleVisibility() }
    single { cacheDatabase(androidContext()) }
    single<ServerDiscovery> { LanServerDiscovery(androidContext().getSystemService(ConnectivityManager::class.java)) }
    single<NetworkMonitor> { ConnectivityNetworkMonitor(androidContext().getSystemService(ConnectivityManager::class.java)) }
    single<Settings>(SettingsStore) { SharedPreferencesSettings(androidContext().getSharedPreferences("settings", Context.MODE_PRIVATE)) }
    // What another app shared, until it is used: private and never backed up.
    single<ShareStore> { FileShareStore(File(androidContext().noBackupFilesDir, "share")) }
    single<Settings>(DraftsStore) { SharedPreferencesSettings(androidContext().getSharedPreferences("drafts", Context.MODE_PRIVATE)) }
}
