package io.github.beppe2880.opencodemobile.core.domain

/**
 * Typed subset of the OpenCode `/api/event` stream that the app reacts to.
 * Unknown event types are dropped by the network layer.
 */
sealed interface ServerEvent {
    data object Connected : ServerEvent

    sealed interface SessionScoped : ServerEvent {
        val sessionId: String
    }

    data class SessionChanged(override val sessionId: String, val deleted: Boolean = false) : SessionScoped

    data class ExecutionStarted(override val sessionId: String) : SessionScoped
    /**
     * End of an execution. The server stores it as an idle message whose id is the event id with
     * `msg_` instead of `evt_`; [idleId] lets the live transcript insert the same [Message.Idle].
     */
    data class ExecutionEnded(
        override val sessionId: String,
        val outcome: SessionOutcome,
        val idleId: String? = null,
        val at: Long = 0,
        val error: String? = null,
    ) : SessionScoped

    data class StepStarted(
        override val sessionId: String,
        val messageId: String,
        val agent: String,
        val model: ModelRef,
        val startedAt: Long,
    ) : SessionScoped

    data class StepEnded(
        override val sessionId: String,
        val messageId: String,
        val finish: String?,
        val cost: Double,
        val tokens: TokenUsage,
        val at: Long,
    ) : SessionScoped

    data class StepStreamed(override val sessionId: String, val messageId: String, val at: Long) : SessionScoped

    data class StepFailed(override val sessionId: String, val messageId: String, val error: String) : SessionScoped

    data class TextStarted(override val sessionId: String, val messageId: String, val ordinal: Int) : SessionScoped
    data class TextDelta(override val sessionId: String, val messageId: String, val ordinal: Int, val delta: String) : SessionScoped
    data class TextEnded(override val sessionId: String, val messageId: String, val ordinal: Int, val text: String) : SessionScoped

    data class ReasoningStarted(override val sessionId: String, val messageId: String, val ordinal: Int, val at: Long) : SessionScoped
    data class ReasoningDelta(override val sessionId: String, val messageId: String, val ordinal: Int, val delta: String) : SessionScoped
    data class ReasoningEnded(override val sessionId: String, val messageId: String, val ordinal: Int, val text: String, val at: Long) : SessionScoped

    data class ToolInputStarted(override val sessionId: String, val messageId: String, val callId: String, val name: String, val at: Long) : SessionScoped
    data class ToolInputDelta(override val sessionId: String, val messageId: String, val callId: String, val delta: String) : SessionScoped
    data class ToolInputEnded(override val sessionId: String, val messageId: String, val callId: String, val text: String) : SessionScoped
    data class ToolCalled(override val sessionId: String, val messageId: String, val callId: String, val input: String) : SessionScoped
    data class ToolProgress(override val sessionId: String, val messageId: String, val callId: String, val metadata: Map<String, String>) : SessionScoped
    data class ToolSucceeded(
        override val sessionId: String,
        val messageId: String,
        val callId: String,
        val output: String,
        val metadata: Map<String, String>,
        val at: Long,
        val diffs: List<FileDiff> = emptyList(),
    ) : SessionScoped
    data class ToolFailed(override val sessionId: String, val messageId: String, val callId: String, val error: String, val at: Long) : SessionScoped

    data class InboxEnqueued(override val sessionId: String, val inboxId: String, val text: String, val at: Long) : SessionScoped
    data class InboxDelivered(override val sessionId: String, val inboxId: String) : SessionScoped
    data class InboxCancelled(override val sessionId: String, val inboxId: String) : SessionScoped
    data class InboxDeliveryChanged(override val sessionId: String, val inboxId: String, val delivery: PromptDelivery) : SessionScoped

    data class UsageUpdated(override val sessionId: String, val cost: Double, val tokens: TokenUsage) : SessionScoped

    data class PermissionChanged(override val sessionId: String) : SessionScoped

    /** A form was created, answered or cancelled. */
    data class FormsChanged(override val sessionId: String) : SessionScoped

    /**
     * A revert was staged ([messageId] set), cleared, or committed ([committed]: the hidden
     * messages are now deleted on the server).
     */
    data class RevertChanged(
        override val sessionId: String,
        val messageId: String?,
        val committed: Boolean = false,
    ) : SessionScoped

    /** Events that change session transcript structure without a dedicated live reducer. */
    data class TranscriptChanged(override val sessionId: String, val type: String) : SessionScoped
}
