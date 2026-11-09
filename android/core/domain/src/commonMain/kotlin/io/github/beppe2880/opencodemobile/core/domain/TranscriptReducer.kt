package io.github.beppe2880.opencodemobile.core.domain

/**
 * Applies live stream events to an ordered transcript.
 *
 * The server stays the source of truth: callers reload messages after a step ends
 * or the stream reconnects. This reducer only bridges the gap so text, reasoning and
 * tool calls appear while they are generated.
 */
object TranscriptReducer {

    fun reduce(messages: List<Message>, event: ServerEvent.SessionScoped): List<Message> = when (event) {
        is ServerEvent.StepStarted -> upsertAssistant(messages, event.messageId) {
            it ?: Message.Assistant(
                id = event.messageId,
                createdAt = event.startedAt,
                agent = event.agent,
                model = event.model,
                parts = emptyList(),
            )
        }

        is ServerEvent.StepEnded -> updateAssistant(messages, event.messageId) {
            it.copy(
                completedAt = event.at,
                finish = event.finish,
                cost = event.cost,
                tokens = event.tokens,
                parts = it.parts.map(::settle),
            )
        }

        is ServerEvent.StepStreamed -> updateAssistant(messages, event.messageId) { it.copy(streamedAt = event.at) }

        is ServerEvent.StepFailed -> updateAssistant(messages, event.messageId) {
            it.copy(error = event.error, parts = it.parts.map(::settle))
        }

        is ServerEvent.TextStarted -> updatePart(messages, event.messageId, "text-${event.ordinal}") {
            it ?: Part.Text(event.ordinal, "", streaming = true)
        }

        is ServerEvent.TextDelta -> updatePart(messages, event.messageId, "text-${event.ordinal}") {
            val current = it as? Part.Text ?: Part.Text(event.ordinal, "", streaming = true)
            current.copy(text = current.text + event.delta, streaming = true)
        }

        is ServerEvent.TextEnded -> updatePart(messages, event.messageId, "text-${event.ordinal}") {
            Part.Text(event.ordinal, event.text, streaming = false)
        }

        is ServerEvent.ReasoningStarted -> updatePart(messages, event.messageId, "reasoning-${event.ordinal}") {
            it ?: Part.Reasoning(event.ordinal, "", streaming = true, startedAt = event.at)
        }

        is ServerEvent.ReasoningDelta -> updatePart(messages, event.messageId, "reasoning-${event.ordinal}") {
            val current = it as? Part.Reasoning ?: Part.Reasoning(event.ordinal, "", streaming = true)
            current.copy(text = current.text + event.delta, streaming = true)
        }

        is ServerEvent.ReasoningEnded -> updatePart(messages, event.messageId, "reasoning-${event.ordinal}") {
            val current = it as? Part.Reasoning
            Part.Reasoning(
                ordinal = event.ordinal,
                text = event.text.ifEmpty { current?.text.orEmpty() },
                streaming = false,
                startedAt = current?.startedAt,
                completedAt = event.at,
            )
        }

        is ServerEvent.ToolInputStarted -> updatePart(messages, event.messageId, "tool-${event.callId}") {
            it ?: Part.Tool(event.callId, event.name, ToolStatus.Streaming, input = "", startedAt = event.at)
        }

        is ServerEvent.ToolInputDelta -> updateTool(messages, event.messageId, event.callId) {
            it.copy(input = it.input + event.delta)
        }

        is ServerEvent.ToolInputEnded -> updateTool(messages, event.messageId, event.callId) {
            it.copy(input = event.text)
        }

        is ServerEvent.ToolCalled -> updateTool(messages, event.messageId, event.callId) {
            it.copy(input = event.input, status = ToolStatus.Running)
        }

        is ServerEvent.ToolProgress -> updateTool(messages, event.messageId, event.callId) {
            it.copy(metadata = it.metadata + event.metadata)
        }

        is ServerEvent.ToolSucceeded -> updateTool(messages, event.messageId, event.callId) {
            it.copy(
                status = ToolStatus.Completed,
                output = event.output,
                metadata = it.metadata + event.metadata,
                completedAt = event.at,
                diffs = event.diffs.ifEmpty { it.diffs },
                files = event.files.ifEmpty { it.files },
            )
        }

        is ServerEvent.ToolFailed -> updateTool(messages, event.messageId, event.callId) {
            it.copy(status = ToolStatus.Error, error = event.error, completedAt = event.at)
        }

        is ServerEvent.InboxEnqueued ->
            if (messages.any { it.id == event.inboxId }) messages
            else messages + Message.User(event.inboxId, event.at, event.text, pending = true)

        is ServerEvent.InboxDelivered -> messages.map {
            if (it is Message.User && it.id == event.inboxId) it.copy(pending = false) else it
        }

        is ServerEvent.InboxCancelled -> messages.filterNot { it is Message.User && it.id == event.inboxId && it.pending }

        is ServerEvent.ExecutionEnded -> {
            val id = event.idleId
            if (id == null || messages.any { it.id == id }) messages
            else messages + Message.Idle(id, event.at, event.outcome, event.error)
        }

        else -> messages
    }

    /**
     * Merges a freshly loaded transcript with local state, keeping queued prompts and any
     * live assistant message that the server has not persisted yet.
     */
    fun merge(server: List<Message>, local: List<Message>): List<Message> {
        val serverIds = server.mapTo(HashSet()) { it.id }
        val pending = local.filter { it.id !in serverIds && ((it is Message.User && it.pending) || (it is Message.Assistant && it.isStreaming)) }
        return server + pending
    }

    private fun settle(part: Part): Part = when (part) {
        is Part.Text -> part.copy(streaming = false)
        is Part.Reasoning -> part.copy(streaming = false)
        is Part.Tool -> part
    }

    private fun upsertAssistant(
        messages: List<Message>,
        id: String,
        transform: (Message.Assistant?) -> Message.Assistant,
    ): List<Message> {
        val index = messages.indexOfFirst { it.id == id }
        if (index < 0) return messages + transform(null)
        val existing = messages[index] as? Message.Assistant ?: return messages
        return messages.toMutableList().also { it[index] = transform(existing) }
    }

    private fun updateAssistant(
        messages: List<Message>,
        id: String,
        transform: (Message.Assistant) -> Message.Assistant,
    ): List<Message> {
        val index = messages.indexOfFirst { it.id == id }
        val existing = messages.getOrNull(index) as? Message.Assistant ?: return messages
        return messages.toMutableList().also { it[index] = transform(existing) }
    }

    private fun updatePart(
        messages: List<Message>,
        messageId: String,
        key: String,
        transform: (Part?) -> Part,
    ): List<Message> = updateAssistant(messages, messageId) { message ->
        val index = message.parts.indexOfFirst { it.key == key }
        val parts = if (index < 0) {
            message.parts + transform(null)
        } else {
            message.parts.toMutableList().also { it[index] = transform(it[index]) }
        }
        message.copy(parts = parts)
    }

    private fun updateTool(
        messages: List<Message>,
        messageId: String,
        callId: String,
        transform: (Part.Tool) -> Part.Tool,
    ): List<Message> = updatePart(messages, messageId, "tool-$callId") { part ->
        val tool = part as? Part.Tool ?: Part.Tool(callId, "tool", ToolStatus.Running, input = "")
        transform(tool)
    }
}
