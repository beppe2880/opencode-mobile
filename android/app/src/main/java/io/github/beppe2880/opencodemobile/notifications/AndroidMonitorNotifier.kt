package io.github.beppe2880.opencodemobile.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.beppe2880.opencodemobile.MainActivity
import io.github.beppe2880.opencodemobile.R
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome

/**
 * Notification channels and builders. Attention notifications are tagged with the request or
 * form id and finished ones with the session id, so each can be replaced or removed alone.
 */
class AndroidMonitorNotifier(private val context: Context) : MonitorNotifier {

    private val manager = NotificationManagerCompat.from(context)

    /** Creates or renames (after a locale change) the channels; cheap to call at every start. */
    fun createChannels() {
        manager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_RUNNING, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.notif_channel_running))
                    .setShowBadge(false)
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_ATTENTION, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.notif_channel_attention))
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_FINISHED, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.notif_channel_finished))
                    .build(),
            ),
        )
    }

    /** The foreground service notification listing the running sessions. */
    fun ongoing(running: List<RunningSession>): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(0, 0, true)
        val single = running.singleOrNull()
        if (single != null || running.isEmpty()) {
            builder.setContentTitle(titleOf(single?.title))
                .setContentText(activityLabel(single?.activity))
                .setContentIntent(openSession(single?.id))
        } else {
            val title = context.resources.getQuantityString(R.plurals.notif_running_count, running.size, running.size)
            val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
            running.forEach { style.addLine("${titleOf(it.title)} · ${activityLabel(it.activity)}") }
            builder.setContentTitle(title)
                .setContentText(running.joinToString(", ") { titleOf(it.title) })
                .setStyle(style)
                .setContentIntent(openSession(null))
        }
        return builder.build()
    }

    override fun permission(sessionTitle: String, request: PermissionRequest) {
        val text = context.getString(R.string.notif_permission_text, request.action)
        val details = listOfNotNull(request.message?.takeIf { it.isNotBlank() }) + request.resources
        val notification = attention(sessionTitle, text, request.sessionId)
            .setStyle(NotificationCompat.BigTextStyle().bigText((listOf(text) + details).joinToString("\n")))
            .addAction(reply(request, PermissionDecision.Once, R.string.notif_allow_once))
            .addAction(reply(request, PermissionDecision.Reject, R.string.notif_reject))
            .build()
        post(request.id, ATTENTION_ID, notification)
    }

    override fun question(sessionTitle: String, form: Form) {
        val text = context.getString(R.string.notif_question_text, form.title)
        post(form.id, ATTENTION_ID, attention(sessionTitle, text, form.sessionId).build())
    }

    override fun cancelAttention(id: String) = manager.cancel(id, ATTENTION_ID)

    override fun finished(sessionId: String, title: String, outcome: SessionOutcome) {
        val text = context.getString(
            when (outcome) {
                SessionOutcome.Succeeded -> R.string.notif_finished
                SessionOutcome.Failed -> R.string.notif_failed
                SessionOutcome.Interrupted -> R.string.notif_interrupted
            },
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_FINISHED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleOf(title))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openSession(sessionId))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(CHANNEL_FINISHED, text))
            .build()
        post(sessionId, FINISHED_ID, notification)
    }

    override fun cancelFinished(sessionId: String) = manager.cancel(sessionId, FINISHED_ID)

    private fun attention(sessionTitle: String, text: String, sessionId: String) =
        NotificationCompat.Builder(context, CHANNEL_ATTENTION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleOf(sessionTitle))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openSession(sessionId))
            // Requests can name files and commands: the lock screen only says that one waits.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(CHANNEL_ATTENTION, context.getString(R.string.notif_attention_public)))

    private fun publicVersion(channel: String, text: String) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .build()

    private fun reply(request: PermissionRequest, decision: PermissionDecision, label: Int): NotificationCompat.Action {
        val intent = Intent(context, PermissionActionReceiver::class.java)
            .setAction(PermissionActionReceiver.ACTION_REPLY)
            .putExtra(PermissionActionReceiver.EXTRA_SESSION_ID, request.sessionId)
            .putExtra(PermissionActionReceiver.EXTRA_REQUEST_ID, request.id)
            .putExtra(PermissionActionReceiver.EXTRA_DECISION, decision.name)
        val pending = PendingIntent.getBroadcast(
            context,
            (request.id + decision.name).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Action.Builder(null, context.getString(label), pending)
            // Answering from the lock screen needs the device unlocked first.
            .setAuthenticationRequired(true)
            .build()
    }

    /** Opens the app, on the chat of [sessionId] when given. */
    private fun openSession(sessionId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (sessionId != null) intent.putExtra(MainActivity.EXTRA_SESSION_ID, sessionId)
        return PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun titleOf(title: String?) = title?.takeIf { it.isNotBlank() } ?: context.getString(R.string.notif_untitled)

    private fun activityLabel(activity: SessionActivity?) = when (activity) {
        SessionActivity.Thinking -> context.getString(R.string.notif_activity_thinking)
        SessionActivity.Writing -> context.getString(R.string.notif_activity_writing)
        is SessionActivity.Tool -> context.getString(R.string.notif_activity_tool, activity.name)
        null -> context.getString(R.string.notif_activity_working)
    }

    /** Shows the notification when the user allows notifications; does nothing otherwise. */
    @SuppressLint("MissingPermission")
    private fun post(tag: String, id: Int, notification: Notification) {
        if (manager.areNotificationsEnabled()) manager.notify(tag, id, notification)
    }

    fun showReplyFailed(requestId: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ATTENTION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notif_reply_failed))
            .setAutoCancel(true)
            .setContentIntent(openSession(null))
            .build()
        post(requestId, ATTENTION_ID, notification)
    }

    companion object {
        const val ONGOING_ID = 1
        private const val ATTENTION_ID = 2
        private const val FINISHED_ID = 3
        private const val CHANNEL_RUNNING = "running"
        private const val CHANNEL_ATTENTION = "attention"
        private const val CHANNEL_FINISHED = "finished"
    }
}
