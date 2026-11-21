package io.github.beppe2880.opencodemobile.voice

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.beppe2880.opencodemobile.MainActivity
import io.github.beppe2880.opencodemobile.R
import io.github.beppe2880.opencodemobile.core.data.stt.BackgroundRecording

/**
 * Holds the microphone while a voice message is recorded, so the recording goes on with the app
 * in the background. Its ongoing notification reopens the app, and its Stop action ends the
 * recording, which is then transcribed as when stopped in the chat.
 */
class VoiceRecordingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
        } catch (e: RuntimeException) {
            // The microphone type needs the app in the foreground; the recording then ends with it.
            Log.w(TAG, "Not in the foreground: ${e.javaClass.simpleName}")
            stopSelf()
            return START_NOT_STICKY
        }
        // The recording may have ended before the service started.
        if (onStop == null) stopSelf() else running = this
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (running === this) running = null
        super.onDestroy()
    }

    private fun notification(): Notification {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(getString(R.string.voice_channel))
                .setShowBadge(false)
                .build(),
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, StopReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.voice_recording_title))
            .setContentText(getString(R.string.voice_recording_text))
            .setContentIntent(open)
            .addAction(0, getString(R.string.voice_recording_stop), stop)
            .setOngoing(true)
            .setSilent(true)
            .setUsesChronometer(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /** The Stop action of the notification. */
    class StopReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onStop?.invoke()
        }
    }

    companion object {
        private const val TAG = "VoiceRecordingService"
        private const val CHANNEL = "voice_recording"
        private const val NOTIFICATION_ID = 4

        /** Ends the recording in progress; null when none is. */
        @Volatile
        private var onStop: (() -> Unit)? = null

        @Volatile
        private var running: VoiceRecordingService? = null

        private val main = Handler(Looper.getMainLooper())

        fun recording(context: Context) = BackgroundRecording { stop ->
            onStop = stop
            try {
                ContextCompat.startForegroundService(context, Intent(context, VoiceRecordingService::class.java))
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException: the app went to the background meanwhile.
                Log.w(TAG, "Not started: ${e.javaClass.simpleName}")
            }
            val end = {
                if (onStop === stop) onStop = null
                // On the main thread like onStartCommand: a service still starting stops itself there.
                main.post { if (onStop == null) running?.stopSelf() }
                Unit
            }
            end
        }
    }
}
