package io.github.beppe2880.opencodemobile.core.data.catalog

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.domain.FolderPreferences
import io.github.beppe2880.opencodemobile.core.domain.RECENT_FOLDERS
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Hidden project directories and picked folders in app-private preferences, per saved server. */
internal class PreferencesFolderPreferences(private val prefs: Settings) : FolderPreferences {
    override fun hiddenDirectories(serverId: String): Set<String> = read("$HIDDEN_PREFIX$serverId").toSet()

    override fun setDirectoryHidden(serverId: String, directory: String, hidden: Boolean) {
        val current = hiddenDirectories(serverId)
        val next = if (hidden) current + directory else current - directory
        prefs.putString("$HIDDEN_PREFIX$serverId", Json.encodeToString(Entries, next.toList()))
    }

    override fun recentFolders(serverId: String): List<String> = read("$RECENT_PREFIX$serverId").distinct().take(RECENT_FOLDERS)

    override fun folderPicked(serverId: String, path: String) {
        val next = (listOf(path) + recentFolders(serverId)).distinct().take(RECENT_FOLDERS)
        prefs.putString("$RECENT_PREFIX$serverId", Json.encodeToString(Entries, next))
    }

    private fun read(key: String): List<String> {
        val stored = prefs.getStringOrNull(key) ?: return emptyList()
        return try {
            Json.decodeFromString(Entries, stored)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    companion object {
        private const val HIDDEN_PREFIX = "hidden_projects/"
        private const val RECENT_PREFIX = "recent_folders/"
        private val Entries = ListSerializer(String.serializer())

        fun clearServer(prefs: Settings, serverId: String) {
            prefs.remove("$HIDDEN_PREFIX$serverId")
            prefs.remove("$RECENT_PREFIX$serverId")
        }
    }
}
