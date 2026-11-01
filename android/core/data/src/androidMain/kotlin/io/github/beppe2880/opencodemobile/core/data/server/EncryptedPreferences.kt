package io.github.beppe2880.opencodemobile.core.data.server

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * EncryptedSharedPreferences backed by an Android Keystore key. When the file cannot be
 * decrypted (for example after a restore to a new device) it is reset: its secrets are lost, but
 * the app keeps working and asks for them again.
 */
internal fun encryptedPreferences(context: Context, fileName: String): SharedPreferences {
    fun open(): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        fileName,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    fun reset(): SharedPreferences {
        context.deleteSharedPreferences(fileName)
        return open()
    }
    return try {
        open()
    } catch (e: GeneralSecurityException) {
        reset()
    } catch (e: IOException) {
        reset()
    }
}
