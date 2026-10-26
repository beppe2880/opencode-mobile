package io.github.beppe2880.opencodemobile.core.data.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.cache.CacheDatabase
import io.github.beppe2880.opencodemobile.core.data.cache.ListCache
import io.github.beppe2880.opencodemobile.core.data.cache.MessageCache
import io.github.beppe2880.opencodemobile.core.data.cache.RoomListCache
import io.github.beppe2880.opencodemobile.core.data.cache.RoomMessageCache
import io.github.beppe2880.opencodemobile.core.data.catalog.RemoteCatalogRepository
import io.github.beppe2880.opencodemobile.core.data.chat.RemoteChatRepository
import io.github.beppe2880.opencodemobile.core.data.draft.PreferencesDraftRepository
import io.github.beppe2880.opencodemobile.core.data.events.AppVisibility
import io.github.beppe2880.opencodemobile.core.data.events.ConnectivityNetworkMonitor
import io.github.beppe2880.opencodemobile.core.data.events.NetworkMonitor
import io.github.beppe2880.opencodemobile.core.data.events.ProcessLifecycleVisibility
import io.github.beppe2880.opencodemobile.core.data.events.SseEventRepository
import io.github.beppe2880.opencodemobile.core.data.files.RemoteFileRepository
import io.github.beppe2880.opencodemobile.core.data.server.EncryptedServerStore
import io.github.beppe2880.opencodemobile.core.data.server.LanServerDiscovery
import io.github.beppe2880.opencodemobile.core.data.server.ServerStore
import io.github.beppe2880.opencodemobile.core.data.server.StoredServerRepository
import io.github.beppe2880.opencodemobile.core.data.session.RemoteSessionRepository
import io.github.beppe2880.opencodemobile.core.data.settings.PreferencesSettingsRepository
import io.github.beppe2880.opencodemobile.core.data.terminal.RemoteTerminalRepository
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.DraftRepository
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.FileRepository
import io.github.beppe2880.opencodemobile.core.domain.ServerDiscovery
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.TerminalRepository
import io.github.beppe2880.opencodemobile.core.network.OpenCodeClientFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Qualifier of the process-wide [CoroutineScope] that owns shared connections. */
val AppScope = named("AppScope")

private const val DRAFTS = "drafts"

val dataModule = module {
    single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single {
        val debuggable = androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        OpenCodeClientFactory(debugLogging = debuggable)
    }
    single<ServerStore> { EncryptedServerStore(androidContext()) }
    single { OpenCodeClients(get(), get()) }
    single<AppVisibility> { ProcessLifecycleVisibility() }

    single { CacheDatabase.create(androidContext()) }
    single<MessageCache> { RoomMessageCache(get<CacheDatabase>().messages()) }
    single<ListCache> { RoomListCache(get<CacheDatabase>().lists()) }

    single<ServerRepository> {
        val messages = get<MessageCache>()
        val lists = get<ListCache>()
        val drafts = androidContext().getSharedPreferences(DRAFTS, Context.MODE_PRIVATE)
        StoredServerRepository(
            store = get(),
            factory = get(),
            scope = get(AppScope),
            onRemoved = { serverId ->
                messages.clearServer(serverId)
                lists.clearServer(serverId)
                withContext(Dispatchers.IO) { PreferencesDraftRepository.clearServer(drafts, serverId) }
            },
        )
    }
    single<ServerDiscovery> { LanServerDiscovery(androidContext().getSystemService(ConnectivityManager::class.java)) }
    single<NetworkMonitor> { ConnectivityNetworkMonitor(androidContext().getSystemService(ConnectivityManager::class.java)) }
    single<EventRepository> { SseEventRepository(get(), get(), get(), get(), get(AppScope), lists = get()) }
    single<SessionRepository> { RemoteSessionRepository(get(), get()) }
    single<ChatRepository> { RemoteChatRepository(get(), get()) }
    single<CatalogRepository> { RemoteCatalogRepository(get()) }
    single<TerminalRepository> { RemoteTerminalRepository(get()) }
    single<FileRepository> { RemoteFileRepository(get()) }
    single<SettingsRepository> {
        PreferencesSettingsRepository(androidContext().getSharedPreferences("settings", Context.MODE_PRIVATE))
    }
    single<DraftRepository> {
        PreferencesDraftRepository(androidContext().getSharedPreferences(DRAFTS, Context.MODE_PRIVATE), get())
    }
}
