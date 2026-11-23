package io.github.beppe2880.opencodemobile.core.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Notification preferences on this device, scoped to the active saved server. */
interface MutedSessionRepository {
    val muted: StateFlow<Set<String>>

    /** Last server idle timestamp when muted, or 0 if the session has never been idle. */
    val temporary: StateFlow<Map<String, Long>>
    fun setMuted(sessionId: String, muted: Boolean, untilIdleAfter: Long? = null)
    fun finishTemporary(sessionId: String, idleAt: Long? = null)
}

class InMemoryMutedSessions : MutedSessionRepository {
    override val muted = MutableStateFlow<Set<String>>(emptySet())
    override val temporary = MutableStateFlow<Map<String, Long>>(emptyMap())
    override fun setMuted(sessionId: String, muted: Boolean, untilIdleAfter: Long?) {
        temporary.value = if (muted && untilIdleAfter != null) temporary.value + (sessionId to untilIdleAfter) else temporary.value - sessionId
        this.muted.value = if (muted) this.muted.value + sessionId else this.muted.value - sessionId
    }

    override fun finishTemporary(sessionId: String, idleAt: Long?) {
        val baseline = temporary.value[sessionId] ?: return
        if (idleAt == null || idleAt > baseline) setMuted(sessionId, false)
    }
}
