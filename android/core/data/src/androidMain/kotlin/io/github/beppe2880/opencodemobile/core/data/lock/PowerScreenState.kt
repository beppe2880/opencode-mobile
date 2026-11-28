package io.github.beppe2880.opencodemobile.core.data.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** The screen state from the system's screen on and off broadcasts. */
internal class PowerScreenState(private val context: Context) : ScreenState {
    override val on: Flow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> trySend(false)
                    Intent.ACTION_SCREEN_ON -> trySend(true)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        trySend(context.getSystemService(PowerManager::class.java)?.isInteractive != false)
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
