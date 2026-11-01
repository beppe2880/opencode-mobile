package io.github.beppe2880.opencodemobile.core.data.server

import android.content.Context
import android.content.SharedPreferences

/**
 * Keeps profiles in EncryptedSharedPreferences backed by an Android Keystore key. When the file
 * cannot be decrypted (for example after a backup restore to a new device) it is reset, so the
 * user pairs again instead of the app failing to start.
 */
internal class EncryptedServerStore(private val context: Context) : ServerStore {

    private val prefs: SharedPreferences by lazy { encryptedPreferences(context, FILE_NAME) }

    override fun load(): StoredServers = StoredServers(
        profiles = ServerProfilesCodec.decode(prefs.getString(KEY_PROFILES, null)),
        activeId = prefs.getString(KEY_ACTIVE, null),
    )

    override fun save(servers: StoredServers) {
        prefs.edit()
            .putString(KEY_PROFILES, ServerProfilesCodec.encode(servers.profiles))
            .putString(KEY_ACTIVE, servers.activeId)
            .commit()
    }

    private companion object {
        const val FILE_NAME = "opencode_servers"
        const val KEY_PROFILES = "profiles"
        const val KEY_ACTIVE = "active_id"
    }
}
