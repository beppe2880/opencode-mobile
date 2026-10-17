package io.github.beppe2880.opencodemobile.notifications

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Keeps the process and the event stream alive while sessions run, so [SessionMonitor] can
 * notify with the app in the background. Shows the running sessions in its ongoing
 * notification and stops shortly after the last one ends.
 */
class SessionMonitorService : Service() {

    private val monitor: SessionMonitor by inject()
    private val events: EventRepository by inject()
    private val notifier: AndroidMonitorNotifier by inject()
    private var scope: CoroutineScope? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, AndroidMonitorNotifier.ONGOING_ID, notifier.ongoing(running()), type)
        if (scope == null) {
            events.keepAliveInBackground(true)
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also(::observe)
        }
        return START_NOT_STICKY
    }

    private fun observe(scope: CoroutineScope) {
        scope.launch {
            monitor.running.map { it.values.toList() }.distinctUntilChanged().conflate().collect { running ->
                if (running.isNotEmpty()) {
                    update(running)
                    delay(UPDATE_INTERVAL_MS)
                }
            }
        }
        // A queued prompt ends one execution and starts the next: wait before stopping.
        scope.launch {
            monitor.running.map { it.isEmpty() }.distinctUntilChanged().collectLatest { idle ->
                if (idle) {
                    delay(STOP_DELAY_MS)
                    stopSelf()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun update(running: List<RunningSession>) {
        val manager = NotificationManagerCompat.from(this)
        if (manager.areNotificationsEnabled()) manager.notify(AndroidMonitorNotifier.ONGOING_ID, notifier.ongoing(running))
    }

    private fun running() = monitor.running.value.values.toList()

    /** Android 15+ limits data sync services to about 6 hours a day. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        scope?.cancel()
        scope = null
        events.keepAliveInBackground(false)
        super.onDestroy()
    }

    companion object {
        private const val UPDATE_INTERVAL_MS = 1_000L
        private const val STOP_DELAY_MS = 3_000L

        /** Starts the service; only allowed while the app is in the foreground. */
        fun start(context: Context) {
            // Without notifications the user could not see anything it does.
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, SessionMonitorService::class.java))
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException: the app went to the background meanwhile.
                Log.w("SessionMonitorService", "Not started: ${e.javaClass.simpleName}")
            }
        }
    }
}
