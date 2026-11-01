package io.github.beppe2880.opencodemobile.core.data.di

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.cache.CacheDatabase
import io.github.beppe2880.opencodemobile.core.data.cache.ListCache
import io.github.beppe2880.opencodemobile.core.data.cache.MessageCache
import io.github.beppe2880.opencodemobile.core.data.cache.RoomListCache
import io.github.beppe2880.opencodemobile.core.data.cache.RoomMessageCache
import io.github.beppe2880.opencodemobile.core.data.catalog.PreferencesRecentModelRepository
import io.github.beppe2880.opencodemobile.core.data.catalog.RemoteCatalogRepository
import io.github.beppe2880.opencodemobile.core.data.chat.RemoteChatRepository
import io.github.beppe2880.opencodemobile.core.data.draft.PreferencesDraftRepository
import io.github.beppe2880.opencodemobile.core.data.events.SseEventRepository
import io.github.beppe2880.opencodemobile.core.data.files.RemoteFileRepository
import io.github.beppe2880.opencodemobile.core.data.mcp.RemoteMcpRepository
import io.github.beppe2880.opencodemobile.core.data.server.StoredServerRepository
import io.github.beppe2880.opencodemobile.core.data.session.RemoteSessionRepository
import io.github.beppe2880.opencodemobile.core.data.settings.PreferencesSettingsRepository
import io.github.beppe2880.opencodemobile.core.data.terminal.RemoteTerminalRepository
import io.github.beppe2880.opencodemobile.core.data.update.GitHubUpdateRepository
import io.github.beppe2880.opencodemobile.core.data.vcs.RemoteVcsRepository
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.DraftRepository
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.FileRepository
import io.github.beppe2880.opencodemobile.core.domain.McpRepository
import io.github.beppe2880.opencodemobile.core.domain.RecentModelRepository
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.TerminalRepository
import io.github.beppe2880.opencodemobile.core.domain.UpdateRepository
import io.github.beppe2880.opencodemobile.core.domain.VcsRepository
import io.github.beppe2880.opencodemobile.core.network.GitHubReleases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Qualifier of the process-wide [CoroutineScope] that owns shared connections. */
val AppScope = named("AppScope")

/** Qualifiers of the two non-sensitive key-value stores, provided by [platformDataModule]. */
internal val SettingsStore = named("settings")
internal val DraftsStore = named("drafts")

/**
 * Platform bindings: `OpenCodeClientFactory`, `ServerStore`, `CacheDatabase`, `AppVisibility`,
 * `ServerDiscovery`, `NetworkMonitor` and the [SettingsStore] / [DraftsStore] [Settings].
 */
internal expect val platformDataModule: Module

val dataModule = module {
    includes(platformDataModule)
    single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { OpenCodeClients(get(), get()) }

    single<MessageCache> { RoomMessageCache(get<CacheDatabase>().messages()) }
    single<ListCache> { RoomListCache(get<CacheDatabase>().lists()) }

    single<ServerRepository> {
        val messages = get<MessageCache>()
        val lists = get<ListCache>()
        val drafts = get<Settings>(DraftsStore)
        val settings = get<Settings>(SettingsStore)
        StoredServerRepository(
            store = get(),
            factory = get(),
            scope = get(AppScope),
            onRemoved = { serverId ->
                messages.clearServer(serverId)
                lists.clearServer(serverId)
                withContext(Dispatchers.IO) {
                    PreferencesDraftRepository.clearServer(drafts, serverId)
                    PreferencesRecentModelRepository.clearServer(settings, serverId)
                }
            },
        )
    }
    single<EventRepository> { SseEventRepository(get(), get(), get(), get(), get(AppScope), lists = get()) }
    single<SessionRepository> { RemoteSessionRepository(get(), get()) }
    single<ChatRepository> { RemoteChatRepository(get(), get()) }
    single<CatalogRepository> { RemoteCatalogRepository(get()) }
    single<TerminalRepository> { RemoteTerminalRepository(get(), network = get()) }
    single<FileRepository> { RemoteFileRepository(get()) }
    single<McpRepository> { RemoteMcpRepository(get()) }
    single<VcsRepository> { RemoteVcsRepository(get()) }
    single<SettingsRepository> { PreferencesSettingsRepository(get(SettingsStore)) }
    single<DraftRepository> { PreferencesDraftRepository(get(DraftsStore), get()) }
    single<RecentModelRepository> { PreferencesRecentModelRepository(get(SettingsStore), get()) }
    single<UpdateRepository> {
        val releases = GitHubReleases()
        GitHubUpdateRepository(releases::recent, get(SettingsStore))
    }
}
