package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.ImageAttachment
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.NoticeKind
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus

/**
 * A running shell command or subagent in the step still in progress: the work the server
 * can move to the background. Mirrors the TUI hint "Press ctrl+b to move running work to
 * the background", shown [HINT_DELAY_MS] after the tool started.
 */
data class BackgroundableWork(val key: String, val startedAt: Long?) {
    companion object {
        const val HINT_DELAY_MS = 3_000L
        private val NAMES = setOf("shell", "bash", "subagent", "task")

        fun find(messages: List<Message>): BackgroundableWork? {
            val step = messages.lastOrNull { it is Message.Assistant && it.isStreaming } as? Message.Assistant
                ?: return null
            val tool = step.parts.filterIsInstance<Part.Tool>()
                .firstOrNull { it.status == ToolStatus.Running && it.name in NAMES } ?: return null
            return BackgroundableWork("${step.id}:${tool.callId}", tool.startedAt)
        }
    }
}

/** Flattened, render-ready transcript rows with stable keys. */
@Immutable
sealed interface ChatItem {
    val key: String

    data class User(
        override val key: String,
        val text: String,
        val pending: Boolean,
        val attachments: Int,
        val images: List<ImageAttachment> = emptyList(),
        val files: List<String> = emptyList(),
    ) : ChatItem
    data class Text(override val key: String, val text: String, val streaming: Boolean) : ChatItem
    data class Reasoning(override val key: String, val text: String, val streaming: Boolean, val durationMs: Long?) : ChatItem
    data class Tools(override val key: String, val tools: List<Part.Tool>) : ChatItem {
        val running: Boolean get() = tools.any { it.status == ToolStatus.Running || it.status == ToolStatus.Streaming }
        val failed: Int get() = tools.count { it.status == ToolStatus.Error }
    }
    data class Error(override val key: String, val text: String) : ChatItem
    data class Notice(override val key: String, val kind: NoticeKind, val text: String) : ChatItem
    data class Shell(override val key: String, val command: String, val output: String, val status: String, val exit: String?) : ChatItem
    data class Footer(
        override val key: String,
        val agent: String,
        val model: String,
        val provider: String,
        val durationMs: Long?,
        /** Output + reasoning tokens per second of streaming, null until every step has streamed. */
        val tokensPerSecond: Double?,
        val cost: Double,
        val copyText: String,
        val interrupted: Boolean,
        /**
         * Tokens the turn added: input, output, reasoning and cache writes over every step.
         * Cache reads are left out: each step re-reads the whole context, so counting them
         * would add the context size once per step (millions in a long turn). This is the
         * TUI's per-step "new" count (`total - cache.read`).
         */
        val totalTokens: Long = 0,
    ) : ChatItem
    data class Working(override val key: String = "working") : ChatItem

    /**
     * Thinking and tool rows that follow each other with nothing else in between, shown as
     * one summary row. [entries] are [Reasoning] and [Tools] in transcript order. The key is
     * the first entry's, so the row keeps its place (and expansion) while the group grows.
     */
    data class Activity(override val key: String, val entries: List<ChatItem>) : ChatItem {
        val tools: List<Part.Tool> get() = entries.filterIsInstance<Tools>().flatMap { it.tools }
        val thoughts: List<Reasoning> get() = entries.filterIsInstance<Reasoning>()
        val thinkingMs: Long get() = thoughts.sumOf { it.durationMs ?: 0L }
        val running: Boolean
            get() = entries.any { (it is Tools && it.running) || (it is Reasoning && it.streaming) }
        val failed: Int get() = tools.count { it.status == ToolStatus.Error }
    }
}

/** Display name of a model and of its provider. */
data class ModelLabel(val name: String, val provider: String)

/**
 * Builds [ChatItem]s from a transcript. Consecutive tool calls inside one turn are
 * grouped (also across steps) until text, reasoning or an error interrupts them.
 *
 * Turns follow the OpenCode TUI: a turn is one server execution, from the first user or
 * synthetic message after the previous [Message.Idle] up to the next one, so prompts steered
 * or queued while the agent runs share the turn and its footer. Sessions without any idle
 * marker (older servers) start a new turn at every prompt.
 */
object ChatItemsBuilder {

    fun build(
        messages: List<Message>,
        running: Boolean,
        showReasoning: Boolean,
        modelLabel: (ModelRef) -> ModelLabel,
        groupActivity: Boolean = false,
    ): List<ChatItem> {
        val items = buildRows(messages, running, showReasoning, modelLabel)
        return if (groupActivity) groupActivity(items) else items
    }

    /** Merges each run of two or more consecutive [ChatItem.Reasoning] / [ChatItem.Tools] rows. */
    internal fun groupActivity(items: List<ChatItem>): List<ChatItem> {
        val out = ArrayList<ChatItem>(items.size)
        var run = ArrayList<ChatItem>()
        fun flush() {
            when (run.size) {
                0 -> Unit
                1 -> out += run.single()
                else -> out += ChatItem.Activity(run.first().key, run)
            }
            run = ArrayList()
        }
        for (item in items) {
            if (item is ChatItem.Reasoning || item is ChatItem.Tools) run += item else {
                flush()
                out += item
            }
        }
        flush()
        return out
    }

    private fun buildRows(
        messages: List<Message>,
        running: Boolean,
        showReasoning: Boolean,
        modelLabel: (ModelRef) -> ModelLabel,
    ): List<ChatItem> {
        val out = ArrayList<ChatItem>(messages.size * 2)
        var tools = ArrayList<Part.Tool>()
        // Call ids are only unique per message (some providers restart at call_0 every step).
        var toolsKey = ""
        var turn = ArrayList<Message.Assistant>()
        var turnStart: Long? = null
        // Position right after the turn's last assistant item; the footer goes there even when
        // notices or prompts follow before the turn closes.
        var turnEnd = 0
        val legacyTurns = messages.none { it is Message.Idle }

        fun addTurnItem(item: ChatItem) {
            out += item
            turnEnd = out.size
        }

        fun flushTools() {
            if (tools.isNotEmpty()) {
                addTurnItem(ChatItem.Tools(toolsKey, tools))
                tools = ArrayList()
            }
        }

        fun closeTurn(isLast: Boolean, idle: Message.Idle? = null) {
            flushTools()
            val last = turn.lastOrNull()
            if (last != null && !(isLast && running) && !last.isStreaming) {
                val end = last.completedAt
                val start = turnStart ?: turn.first().createdAt
                val label = modelLabel(last.model)
                out.add(
                    turnEnd,
                    ChatItem.Footer(
                        key = "footer-${last.id}",
                        agent = last.agent,
                        model = label.name,
                        provider = label.provider,
                        durationMs = end?.let { (it - start).coerceAtLeast(0) },
                        tokensPerSecond = tokensPerSecond(turn),
                        cost = turn.sumOf { it.cost },
                        copyText = turn.flatMap { it.parts }.filterIsInstance<Part.Text>().joinToString("\n\n") { it.text.trim() }.trim(),
                        interrupted = last.error != null || idle?.outcome == SessionOutcome.Interrupted,
                        totalTokens = turn.sumOf { it.tokens.total - it.tokens.cacheRead },
                    ),
                )
            }
            turn = ArrayList()
            turnStart = null
        }

        /** Handles a prompt; [render] adds its row. A prompt opening a new turn also anchors [turnEnd]. */
        fun startTurn(at: Long, render: () -> Unit = {}) {
            if (legacyTurns) closeTurn(isLast = false) else flushTools()
            val opensTurn = legacyTurns || turnStart == null
            if (opensTurn) turnStart = at
            render()
            if (opensTurn) turnEnd = out.size
        }

        messages.forEachIndexed { index, message ->
            when (message) {
                is Message.User -> startTurn(message.createdAt) {
                    out += ChatItem.User(message.id, message.text, message.pending, message.attachments, message.images, message.files)
                }
                is Message.Synthetic -> startTurn(message.createdAt)
                is Message.Idle -> {
                    closeTurn(isLast = false, idle = message)
                    message.error?.let { out += ChatItem.Notice(message.id, NoticeKind.Error, it) }
                    turnEnd = out.size
                }
                is Message.Assistant -> {
                    turn += message
                    message.parts.forEach { part ->
                        when (part) {
                            is Part.Tool -> {
                                if (tools.isEmpty()) toolsKey = "tools-${message.id}/${part.callId}"
                                tools += part
                            }
                            is Part.Text -> if (part.text.isNotBlank() || part.streaming) {
                                flushTools()
                                addTurnItem(ChatItem.Text("${message.id}/${part.key}", part.text, part.streaming))
                            }
                            is Part.Reasoning -> if (showReasoning && (part.text.isNotBlank() || part.streaming)) {
                                flushTools()
                                val duration = if (part.startedAt != null && part.completedAt != null) part.completedAt!! - part.startedAt!! else null
                                addTurnItem(ChatItem.Reasoning("${message.id}/${part.key}", part.text.trim(), part.streaming, duration))
                            }
                        }
                    }
                    message.error?.let {
                        flushTools()
                        addTurnItem(ChatItem.Error("${message.id}/error", it))
                    }
                }
                is Message.Notice -> {
                    flushTools()
                    out += ChatItem.Notice(message.id, message.kind, message.text)
                }
                is Message.Shell -> {
                    flushTools()
                    out += ChatItem.Shell(message.id, message.command, message.output, message.status, message.exit)
                }
            }
            if (index == messages.lastIndex) closeTurn(isLast = true)
        }
        if (running) {
            val last = out.lastOrNull()
            val activelyStreaming = (last is ChatItem.Text && last.streaming) || (last is ChatItem.Reasoning && last.streaming) ||
                (last is ChatItem.Tools && last.running)
            if (!activelyStreaming) out += ChatItem.Working()
        }
        return out
    }

    /**
     * Same metric as the OpenCode TUI: output + reasoning tokens over the summed streaming time
     * (`streamed - created`) of the turn's provider steps, so tool execution after streaming and
     * gaps between steps are excluded. Unknown until every step has finished streaming.
     */
    internal fun tokensPerSecond(turn: List<Message.Assistant>): Double? {
        if (turn.isEmpty()) return null
        var tokens = 0L
        var millis = 0L
        for (step in turn) {
            val streamed = step.streamedAt ?: return null
            tokens += step.tokens.output + step.tokens.reasoning
            millis += (streamed - step.createdAt).coerceAtLeast(0)
        }
        if (tokens <= 0 || millis <= 0) return null
        return tokens * 1_000.0 / millis
    }
}
