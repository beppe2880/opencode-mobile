package io.github.beppe2880.opencodemobile.core.domain

import kotlinx.coroutines.flow.StateFlow

/** Text and images another app shared with OpenCode, on their way into a chat's composer. */
data class SharedContent(
    val text: String = "",
    /** Images as `data:` URI uploads, copied when they were shared. */
    val attachments: List<PromptAttachment> = emptyList(),
    /** Shared items left out: unreadable, not an image, too large or beyond [MAX_SHARED_IMAGES]. */
    val skipped: Int = 0,
    /** The text was cut to [MAX_SHARED_TEXT] characters. */
    val truncated: Boolean = false,
) {
    val isEmpty: Boolean get() = text.isBlank() && attachments.isEmpty()
}

/**
 * A share waiting to be used; [id] differs for every share. Once the user picks the chat that
 * receives it, [sessionId] and [serverId] name it, and that chat takes the share into its composer.
 */
data class PendingShare(
    val id: Long,
    val content: SharedContent,
    val sessionId: String? = null,
    val serverId: String? = null,
) {
    /** Whether the share still needs a chat on server [activeServerId]: not picked yet, or picked on another server. */
    fun needsChatOn(activeServerId: String?): Boolean = sessionId == null || serverId != activeServerId
}

/** What sharing to OpenCode from another app opens. */
enum class ShareOpens {
    /** The new session sheet, with the shared content shown. */
    NewSession,

    /** The session list, to pick the chat that receives it, or start a new one. */
    ChooseChat,
}

/** Longer shared text is cut. */
const val MAX_SHARED_TEXT = 100_000

/** Images of one share; more are skipped. Matches what the chat's photo picker accepts at once. */
const val MAX_SHARED_IMAGES = 5

/**
 * The share from another app waiting to be used, kept on the device (until it is used or
 * discarded) so it survives a restart of the app and waits for a server to be connected.
 */
interface ShareRepository {
    /** The waiting share; null when there is none or while it is read from storage. */
    val pending: StateFlow<PendingShare?>

    /** Replaces any waiting share with [content]. */
    suspend fun offer(content: SharedContent)

    /** The share goes to [sessionId] on server [serverId]: its chat takes it with [take]. */
    suspend fun assign(sessionId: String, serverId: String?)

    /** Removes and returns the share assigned to [sessionId]; null when there is none. */
    suspend fun take(sessionId: String): SharedContent?

    suspend fun discard()
}
