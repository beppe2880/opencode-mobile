package io.github.beppe2880.opencodemobile.core.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Notification preferences on this device, scoped to the active saved server. */
interface MutedSessionRepository {
    val muted: StateFlow<Set<String>>
    fun setMuted(sessionId: String, muted: Boolean)
}

class InMemoryMutedSessions : MutedSessionRepository {
    override val muted = MutableStateFlow<Set<String>>(emptySet())
    override fun setMuted(sessionId: String, muted: Boolean) {
        this.muted.value = if (muted) this.muted.value + sessionId else this.muted.value - sessionId
    }
}
