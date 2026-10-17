package io.github.beppe2880.opencodemobile.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.beppe2880.opencodemobile.core.data.di.AppScope
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

/** Answers a permission request from the Allow / Reject buttons of its notification. */
class PermissionActionReceiver : BroadcastReceiver(), KoinComponent {

    private val chat: ChatRepository by inject()
    private val notifier: AndroidMonitorNotifier by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REPLY) return
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: return
        val decision = PermissionDecision.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_DECISION) } ?: return
        notifier.cancelAttention(requestId)
        val result = goAsync()
        get<CoroutineScope>(AppScope).launch {
            try {
                chat.replyPermission(sessionId, requestId, decision)
                    .onFailure { notifier.showReplyFailed(requestId) }
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_REPLY = "io.github.beppe2880.opencodemobile.action.REPLY_PERMISSION"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_REQUEST_ID = "request_id"
        const val EXTRA_DECISION = "decision"
    }
}
