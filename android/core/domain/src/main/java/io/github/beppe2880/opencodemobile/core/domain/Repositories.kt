package io.github.beppe2880.opencodemobile.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contracts between presentation and data. Implementations live in `:core:data`;
 * features depend only on these interfaces and the domain models.
 *
 * Failures are reported as [OpenCodeError] inside [Result] for one-shot calls.
 */

sealed class OpenCodeError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** Credentials rejected (HTTP 401). */
    class Unauthorized(message: String = "Authentication failed") : OpenCodeError(message)

    /** Server could not be reached (DNS, refused, timeout, TLS). */
    class Unreachable(message: String, cause: Throwable? = null) : OpenCodeError(message, cause)

    /** Pairing code expired, already used or invalid. */
    class PairingFailed(message: String) : OpenCodeError(message)

    class NotFound(message: String) : OpenCodeError(message)

    /** Any other HTTP or decoding failure. [tag] is the server `_tag` when present. */
    class Server(val status: Int, val tag: String?, message: String) : OpenCodeError(message)
}

/** [Unauthorized]: the server rejected the saved credentials; no retry until the server is paired again. */
enum class ConnectionState { Disconnected, Connecting, Connected, Reconnecting, Unauthorized }

interface ServerRepository {
    /** Saved servers, most recently added first. */
    val servers: StateFlow<List<ServerProfile>>

    /** The server the app is currently using, or null when the user must connect. */
    val activeServer: StateFlow<ServerProfile?>

    /** False until the saved servers have been read from storage; [activeServer] is null until then. */
    val loaded: StateFlow<Boolean> get() = AlwaysLoaded

    /** Checks reachability and credentials without saving anything. */
    suspend fun probe(baseUrl: String, password: String?): Result<ServerInfo>

    /** Redeems a pairing code for a session token and saves + activates the server. */
    suspend fun pair(baseUrl: String, code: String, name: String? = null): Result<ServerProfile>

    /** Validates the password and saves + activates the server. */
    suspend fun addWithPassword(baseUrl: String, password: String, name: String? = null): Result<ServerProfile>

    suspend fun activate(serverId: String)
    suspend fun rename(serverId: String, name: String)
    suspend fun remove(serverId: String)

    /** Parses a scanned QR code, pasted pairing link or typed host; null when it is not a server address. */
    fun parseConnectTarget(raw: String): ConnectTarget? = null
}

private val AlwaysLoaded: StateFlow<Boolean> = MutableStateFlow(true)

/** Live `/api/event` stream for the active server, shared by all screens. */
interface EventRepository {
    val connection: StateFlow<ConnectionState>

    /** Typed events; reconnects automatically while collected. Emits [ServerEvent.Connected] after each (re)connect. */
    val events: Flow<ServerEvent>

    /**
     * While enabled, [events] stays connected with no screen visible: a foreground service is
     * watching running sessions to notify the user. Normally the stream pauses in the background.
     */
    fun keepAliveInBackground(enabled: Boolean) {}
}

interface SessionRepository {
    /** Root sessions (no parent), newest first. [search] filters by title on the server. */
    suspend fun list(search: String? = null, directory: String? = null, limit: Int = 100): Result<List<Session>>

    /** IDs of sessions currently executing. */
    suspend fun active(): Result<Set<String>>

    suspend fun get(sessionId: String): Result<Session>
    suspend fun create(directory: String, agent: String? = null, model: ModelRef? = null): Result<Session>
    suspend fun rename(sessionId: String, title: String): Result<Unit>
    suspend fun delete(sessionId: String): Result<Unit>

    /**
     * Copies the session into a new root session ("… (fork #n)"): the history before
     * [beforeMessageId], or all of it when null. The original is left untouched.
     */
    suspend fun fork(sessionId: String, beforeMessageId: String? = null): Result<Session>

    suspend fun projects(): Result<List<Project>>

    /**
     * The sessions of saved server [serverId] last loaded by [list] without a search for
     * [directory], kept on the device so they show before the server answers; null when never loaded.
     */
    suspend fun cachedList(serverId: String, directory: String? = null): List<Session>? = null

    /** The [projects] last loaded from saved server [serverId]; null when never loaded. */
    suspend fun cachedProjects(serverId: String): List<Project>? = null

    /** The session as last seen in [list] or [get] in this process, to show before loading. */
    fun cached(sessionId: String): Session? = null

    /** Session IDs in the order the session list last showed them; swiping in a chat follows it. */
    fun browseOrder(): List<String> = emptyList()

    fun setBrowseOrder(sessionIds: List<String>) {}
}

/** Unsent composer text per session, kept on the device so it survives switching chats. */
interface DraftRepository {
    fun load(sessionId: String): String

    /** Blank [text] removes the draft. */
    fun save(sessionId: String, text: String)
}

enum class PromptDelivery { Steer, Queue }

/** A prompt waiting in the session inbox: [Queue]d ones run after the current work ends. */
data class QueuedPrompt(val id: String, val text: String, val delivery: PromptDelivery, val createdAt: Long)

/** Range of a session's file changes: its newest turn, or every turn from the first prompt. */
enum class ChangesScope { LastTurn, Session }

interface ChatRepository {
    /**
     * Transcript, oldest first. System instructions are omitted; [Message.Idle] and
     * [Message.Synthetic] are kept as turn boundaries and are not rendered.
     * Syncs the newest part of the local copy: only messages newer than what is already stored
     * are downloaded, and a session never opened starts from its newest page.
     */
    suspend fun messages(sessionId: String): Result<List<Message>>

    /** The locally stored transcript, shown before [messages] completes; empty when none. */
    suspend fun cachedMessages(sessionId: String): List<Message> = emptyList()

    /**
     * The page of history right before the stored transcript, oldest first, or null when the
     * history is complete. Pages beyond the on-device cache cap are returned but not stored.
     * Call repeatedly to fill in older messages.
     */
    suspend fun olderMessages(sessionId: String): Result<List<Message>?> = Result.success(null)

    /** The images attached to one prompt as `data:` URIs, fetched from the server. */
    suspend fun messageImages(sessionId: String, messageId: String): Result<List<String>> = Result.success(emptyList())

    /** Stores the last [lastMessages] of sessions not cached yet so they open instantly. Never fails. */
    suspend fun prefetch(sessionIds: List<String>, lastMessages: Int = 2) {}

    suspend fun prompt(
        sessionId: String,
        text: String,
        delivery: PromptDelivery = PromptDelivery.Steer,
        attachments: List<PromptAttachment> = emptyList(),
    ): Result<Unit>

    /** Runs the server slash command [name] with [arguments] (the text after the name). */
    suspend fun command(
        sessionId: String,
        name: String,
        arguments: String,
        delivery: PromptDelivery = PromptDelivery.Steer,
        attachments: List<PromptAttachment> = emptyList(),
    ): Result<Unit> = Result.success(Unit)

    /**
     * Runs [command] in the session's shell, like `!` in the TUI; the output joins the
     * transcript. Long commands keep running on the server after this returns.
     */
    suspend fun shell(sessionId: String, command: String): Result<Unit> = Result.success(Unit)

    /** Prompts sent but not delivered yet, oldest first. */
    suspend fun inbox(sessionId: String): Result<List<QueuedPrompt>> = Result.success(emptyList())

    /** Removes a prompt that has not been delivered; a no-op once it was. */
    suspend fun cancelQueued(sessionId: String, inboxId: String): Result<Unit> = Result.success(Unit)

    /** Delivers a queued prompt at the next step instead of after the current work. */
    suspend fun steerQueued(sessionId: String, inboxId: String): Result<Unit> = Result.success(Unit)

    suspend fun interrupt(sessionId: String): Result<Unit>

    /**
     * Moves running shell commands and subagents to the background, so the agent can go
     * on while they finish (the TUI's ctrl+b). A no-op when nothing is running.
     */
    suspend fun background(sessionId: String): Result<Unit> = Result.success(Unit)

    /** Summarizes the history to free context; runs at the next step boundary if busy. */
    suspend fun compact(sessionId: String): Result<Unit> = Result.success(Unit)

    /** Waits until the session stops executing (after [interrupt]). */
    suspend fun awaitIdle(sessionId: String): Result<Unit> = Result.success(Unit)

    suspend fun switchAgent(sessionId: String, agent: String): Result<Unit>
    suspend fun switchModel(sessionId: String, model: ModelRef): Result<Unit>

    suspend fun permissions(sessionId: String): Result<List<PermissionRequest>>
    suspend fun replyPermission(sessionId: String, requestId: String, decision: PermissionDecision, message: String? = null): Result<Unit>

    /** Pending questions of the session. */
    suspend fun forms(sessionId: String): Result<List<Form>> = Result.success(emptyList())
    suspend fun replyForm(sessionId: String, formId: String, answers: Map<String, FormAnswer>): Result<Unit> = Result.success(Unit)
    suspend fun cancelForm(sessionId: String, formId: String): Result<Unit> = Result.success(Unit)

    /** Hides [messageId] and everything after it and restores the files changed since. */
    suspend fun revert(sessionId: String, messageId: String): Result<Unit> = Result.success(Unit)

    /** Brings back what a staged revert hid. */
    suspend fun clearRevert(sessionId: String): Result<Unit> = Result.success(Unit)

    /** Makes a staged revert permanent (done before sending a new prompt). */
    suspend fun commitRevert(sessionId: String): Result<Unit> = Result.success(Unit)

    /** Files the session changed in [scope], with unified diffs; empty before the first prompt. */
    suspend fun changes(sessionId: String, scope: ChangesScope): Result<List<FileDiff>> = Result.success(emptyList())
}

interface CatalogRepository {
    /** Agents available in [directory]; hidden and subagent-only agents are included and flagged. */
    suspend fun agents(directory: String?): Result<List<Agent>>

    /** Enabled models, newest release first. */
    suspend fun models(directory: String?): Result<List<ModelInfo>>

    /** Slash commands available in [directory]. */
    suspend fun commands(directory: String?): Result<List<Command>> = Result.success(emptyList())

    /** Project files matching [query], as paths relative to [directory]. */
    suspend fun findFiles(directory: String, query: String): Result<List<String>> = Result.success(emptyList())
}

enum class ThemeMode { System, Light, Dark }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.System,
    val showReasoning: Boolean = true,
    val expandToolsByDefault: Boolean = false,
    /** Cost of each turn in the turn footer. */
    val showCost: Boolean = true,
    /** Horizontal swipes in a chat open the previous or next session of the list. */
    val swipeBetweenChats: Boolean = true,
    /** The composer outline takes the selected agent's color. */
    val agentColorOutline: Boolean = true,
    /** Consecutive thinking and tool rows collapse into one summary row. */
    val groupActivity: Boolean = true,
    /**
     * While sessions run, keep the connection in the background and notify when one needs an
     * answer or finishes. Only works once the notification permission is granted.
     */
    val notifications: Boolean = true,
    /** With [notifications] on: permission requests, with Allow and Reject actions. */
    val notifyPermissions: Boolean = true,
    /** With [notifications] on: questions the agent asks. */
    val notifyQuestions: Boolean = true,
    /** With [notifications] on: sessions that finish, fail or are interrupted. */
    val notifyFinished: Boolean = true,
    /** The one-time explanation of notifications, shown once a server is connected, was answered. */
    val notificationsIntroShown: Boolean = false,
)

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
