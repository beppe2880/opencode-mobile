package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.Command
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.FileDiff
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import io.github.beppe2880.opencodemobile.core.domain.PromptDelivery
import io.github.beppe2880.opencodemobile.core.domain.QueuedPrompt
import io.github.beppe2880.opencodemobile.core.ui.text.UiText

@Immutable
data class ChatState(
    val sessionId: String = "",
    val title: String = "",
    val directory: String = "",
    val agent: String? = null,
    val model: ModelRef? = null,
    val items: List<ChatItem> = emptyList(),
    val loading: Boolean = true,
    val error: UiText? = null,
    val running: Boolean = false,
    val connection: ConnectionState = ConnectionState.Connected,
    val draft: String = "",
    val delivery: PromptDelivery = PromptDelivery.Steer,
    val sending: Boolean = false,
    val stopping: Boolean = false,
    val permissions: List<PermissionRequest> = emptyList(),
    val replyingPermission: String? = null,
    val agents: List<Agent> = emptyList(),
    val models: List<ModelInfo> = emptyList(),
    val recentModels: List<ModelRef> = emptyList(),
    val modelSheetOpen: Boolean = false,
    /** Keys whose expansion differs from the default (see [isExpanded]). */
    val toggled: Set<String> = emptySet(),
    val expandToolsByDefault: Boolean = false,
    /** Which running work stays open (settings); null when following is off. */
    val live: LiveFollow? = LiveFollow(),
    /** An opened tool group shows each tool's output (setting). */
    val showToolDetails: Boolean = true,
    /** Tool rows wrap long commands and paths (setting). */
    val showFullCommands: Boolean = false,
    val showCost: Boolean = true,
    val agentColorOutline: Boolean = true,
    /** The microphone button of the composer (setting). */
    val dictation: Boolean = true,
    /** Uploaded files and picked `@` mentions for the next prompt. */
    val attachments: List<PromptAttachment> = emptyList(),
    /** `@` file or `/` command completions for the end of the draft. */
    val suggestions: List<Suggestion> = emptyList(),
    val commands: List<Command> = emptyList(),
    /** Pending questions; the first one is shown with [formAnswers]. */
    val forms: List<Form> = emptyList(),
    val formAnswers: Map<String, FormAnswer> = emptyMap(),
    val submittingForm: Boolean = false,
    /** Messages hidden by a staged revert; 0 when none. */
    val revertedCount: Int = 0,
    val reverting: Boolean = false,
    val forking: Boolean = false,
    /** Blocking work the user can move to the background; the hint shows once it runs long. */
    val backgroundable: BackgroundableWork? = null,
    /** Context the next step will use; null before the first reply or right after a compaction. */
    val context: ContextUsage? = null,
    val compacting: Boolean = false,
    /** Neighbors in the session list, reached by swiping right (previous) or left (next). */
    val previousSession: SessionPeek? = null,
    val nextSession: SessionPeek? = null,
    val swipeEnabled: Boolean = true,
    /** The server transcript has been loaded at least once (cached content may show before). */
    val synced: Boolean = false,
    /** Older history may exist above the oldest loaded message. */
    val hasOlder: Boolean = true,
    val loadingOlder: Boolean = false,
    /** The last older page failed; the top of the transcript offers to retry. */
    val olderFailed: Boolean = false,
    /** Full-screen image open over the chat. */
    val preview: ImagePreview? = null,
    /** Where the user left this chat last time; null opens it at the bottom. */
    val savedScroll: ScrollMemory.Anchor? = null,
    /** Prompts queued to run after the current work, oldest first; shown above the composer. */
    val queued: List<QueuedPrompt> = emptyList(),
    /** The sheet of files the session changed; null while closed. */
    val changes: ChangesPanel? = null,
) {
    val uploads: List<PromptAttachment> get() = attachments.filter { it.mention == null }

    /** The draft is a `!command` for the session's shell. */
    val isShell: Boolean get() = Completions.shell(draft) != null
    val canSend: Boolean get() = when {
        sending || reverting -> false
        isShell -> !Completions.shell(draft).isNullOrEmpty()
        else -> draft.isNotBlank() || uploads.isNotEmpty()
    }
    val modelLabel: ModelLabel? get() = model?.let(models::labelFor)

    fun isExpanded(key: String, defaultExpanded: Boolean = false): Boolean = (key in toggled) != defaultExpanded
}

data class SessionPeek(val id: String, val title: String)

/** Changed files of [scope]; [files] is null until first loaded, and kept while it refreshes. */
data class ChangesPanel(
    val scope: ChangesScope,
    val files: List<FileDiff>? = null,
    val loading: Boolean = true,
    val error: UiText? = null,
)

/** An image shown full screen; [dataUri] is null while it is fetched or when that [failed]. */
data class ImagePreview(val name: String?, val dataUri: String?, val failed: Boolean = false)

/** A completion offered above the composer. */
sealed interface Suggestion {
    val key: String

    data class File(val path: String) : Suggestion {
        override val key: String get() = "file:$path"
    }

    data class Slash(val name: String, val description: UiText?) : Suggestion {
        override val key: String get() = "cmd:$name"
    }
}

/** Catalog names for [ref]; raw IDs when the model is not in the catalog (e.g. disabled). */
fun List<ModelInfo>.labelFor(ref: ModelRef): ModelLabel =
    firstOrNull { it.ref.key == ref.key }?.let { ModelLabel(it.name, it.providerName) }
        ?: ModelLabel(ref.id, ref.providerId)

sealed interface ChatEvent {
    data object Retry : ChatEvent
    data class DraftChanged(val text: String) : ChatEvent
    data class DeliveryChanged(val delivery: PromptDelivery) : ChatEvent
    data object Send : ChatEvent
    data object Stop : ChatEvent

    /** Moves the running shell command or subagent to the background (TUI ctrl+b). */
    data object MoveToBackground : ChatEvent

    /** Summarizes the history to free context (`/compact`). */
    data object Compact : ChatEvent

    /** Opens a subagent's child session. */
    data class OpenSession(val sessionId: String) : ChatEvent

    /** Opens a file of the server read-only; [path] is absolute or relative to the session folder. */
    data class OpenFile(val path: String) : ChatEvent

    /** Replaces this chat with a neighbor in the session list (swipe). */
    data class SwitchSession(val sessionId: String) : ChatEvent

    /** The user scrolled near the oldest loaded message. */
    data object LoadOlder : ChatEvent

    /** The transcript scrolled; null once back at the bottom. */
    data class ScrollMoved(val anchor: ScrollMemory.Anchor?) : ChatEvent
    data class Toggle(val key: String) : ChatEvent
    data class SelectAgent(val agent: String) : ChatEvent
    data class ModelSheet(val open: Boolean) : ChatEvent
    data class SelectModel(val model: ModelRef) : ChatEvent

    /** Reasoning level of the current model; null is its default. */
    data class SelectVariant(val variant: String?) : ChatEvent
    data class ReplyPermission(val requestId: String, val decision: PermissionDecision) : ChatEvent
    data object Back : ChatEvent

    /** Files read from the system pickers, ready to send. */
    data class AddAttachments(val attachments: List<PromptAttachment>) : ChatEvent
    data class RemoveAttachment(val uri: String) : ChatEvent

    /** Opens an attachment of the composer full screen. */
    data class PreviewAttachment(val uri: String) : ChatEvent

    /** Opens the [index]th image of a sent prompt full screen. */
    data class OpenImage(val messageId: String, val index: Int) : ChatEvent
    data object ClosePreview : ChatEvent

    /** A picked file could not be read or is too large. */
    data object AttachmentFailed : ChatEvent
    data class PickSuggestion(val suggestion: Suggestion) : ChatEvent

    data class FormAnswerChanged(val key: String, val answer: FormAnswer?) : ChatEvent
    data object SubmitForm : ChatEvent
    data object DismissForm : ChatEvent

    /** Undo [messageId] and everything after it; its text returns to the composer. */
    data class Revert(val messageId: String) : ChatEvent
    data object RestoreRevert : ChatEvent

    /** Copies the history before [messageId] to a new session and opens it with that prompt as draft. */
    data class Fork(val messageId: String) : ChatEvent

    data object OpenChanges : ChatEvent
    data object OpenTerminal : ChatEvent
    data object CloseChanges : ChatEvent

    /** Shows another range in the open changes sheet, or reloads the current one. */
    data class LoadChanges(val scope: ChangesScope) : ChatEvent

    /** Delivers a queued prompt at the next step instead of after the current work. */
    data class SteerQueued(val id: String) : ChatEvent

    /** Takes a queued prompt back into the composer. */
    data class EditQueued(val id: String) : ChatEvent
    data class RemoveQueued(val id: String) : ChatEvent
}

sealed interface ChatEffect {
    data class ShowMessage(val message: UiText) : ChatEffect
    data object NavigateBack : ChatEffect
    data object PromptSent : ChatEffect
    data class OpenSession(val sessionId: String) : ChatEffect
    data class SwitchSession(val sessionId: String) : ChatEffect
    data class OpenTerminal(val directory: String) : ChatEffect
    data class OpenFile(val directory: String, val path: String) : ChatEffect
}
