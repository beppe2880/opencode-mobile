package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.PromptFileAttachment
import io.github.beppe2880.opencodemobile.api.models.PromptFileSource
import io.github.beppe2880.opencodemobile.api.models.SessionMessageAssistant
import io.github.beppe2880.opencodemobile.api.models.SessionMessageAssistantContentInner
import io.github.beppe2880.opencodemobile.api.models.SessionMessageAssistantTool
import io.github.beppe2880.opencodemobile.api.models.SessionMessageAssistantToolState
import io.github.beppe2880.opencodemobile.api.models.SessionMessageCompaction
import io.github.beppe2880.opencodemobile.api.models.SessionMessageIdle
import io.github.beppe2880.opencodemobile.api.models.SessionMessageInfo
import io.github.beppe2880.opencodemobile.api.models.SessionMessageShell
import io.github.beppe2880.opencodemobile.core.domain.ImageAttachment
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.NoticeKind
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import kotlinx.serialization.json.JsonNull
import java.net.URI

/**
 * Maps one stored message to its transcript entry, or null for entries the transcript hides
 * (system and synthetic context, successful idle markers).
 *
 * Notice texts carry the raw subject (agent id, model key, directory, skill name, summary or
 * error message); the UI composes localized wording around them.
 */
fun SessionMessageInfo.toDomain(): Message? = when (this) {
    is SessionMessageInfo.SessionMessageUserValue -> with(value) {
        Message.User(
            id = id.orEmpty(),
            createdAt = time.created.toLong(),
            text = text.orEmpty(),
            attachments = files.orEmpty().size,
            images = files.orEmpty().filter { it.mime.startsWith("image/") }.map { file ->
                // Stored copies have the payload emptied (see MessageCodec.slim).
                ImageAttachment(file.name, file.mime, file.data.takeIf { it.isNotEmpty() }?.let { "data:${file.mime};base64,$it" })
            },
            files = files.orEmpty().filterNot { it.mime.startsWith("image/") }.mapNotNull { it.serverPath() },
        )
    }
    is SessionMessageInfo.SessionMessageAssistantValue -> value.toDomain()
    is SessionMessageInfo.SessionMessageShellValue -> value.toDomain()
    is SessionMessageInfo.SessionMessageAgentSelectedValue -> with(value) {
        notice(id, time.created, NoticeKind.AgentSwitched, agent.orEmpty())
    }
    is SessionMessageInfo.SessionMessageModelSelectedValue -> with(value) {
        notice(id, time.created, NoticeKind.ModelSwitched, model?.toDomain()?.key.orEmpty())
    }
    is SessionMessageInfo.SessionMessageLocationSwitchedValue -> with(value) {
        notice(id, time.created, NoticeKind.LocationSwitched, location?.directory.orEmpty())
    }
    is SessionMessageInfo.SessionMessageSkillValue -> with(value) {
        notice(id, time.created, NoticeKind.Skill, name ?: skill.orEmpty())
    }
    is SessionMessageInfo.SessionMessageCompactionValue -> when (val compaction = value) {
        is SessionMessageCompaction.RunningWrapper -> with(compaction.value) { notice(id, time.created, NoticeKind.Compacting, "") }
        is SessionMessageCompaction.CompletedWrapper -> with(compaction.value) {
            notice(id, time.created, NoticeKind.Compaction, summary.orEmpty())
        }
        is SessionMessageCompaction.FailedWrapper -> with(compaction.value) {
            notice(id, time.created, NoticeKind.Error, error.describe())
        }
    }
    is SessionMessageInfo.SessionMessageIdleValue -> with(value) {
        Message.Idle(
            id = id.orEmpty(),
            createdAt = time.created.toLong(),
            outcome = when (outcome) {
                SessionMessageIdle.Outcome.SUCCEEDED -> SessionOutcome.Succeeded
                SessionMessageIdle.Outcome.FAILED -> SessionOutcome.Failed
                SessionMessageIdle.Outcome.INTERRUPTED -> SessionOutcome.Interrupted
            },
            error = error?.describe(),
        )
    }
    is SessionMessageInfo.SessionMessageSyntheticValue -> with(value) {
        Message.Synthetic(id = id.orEmpty(), createdAt = time.created.toLong())
    }
    is SessionMessageInfo.SessionMessageSystemValue -> null
}

private fun notice(id: String?, created: Double, kind: NoticeKind, text: String) =
    Message.Notice(id = id.orEmpty(), createdAt = created.toLong(), kind = kind, text = text)

private fun SessionMessageAssistant.toDomain(): Message.Assistant {
    val inProgress = time.completed == null && error == null
    var textOrdinal = 0
    var reasoningOrdinal = 0
    val parts = content.map { item ->
        when (item) {
            is SessionMessageAssistantContentInner.TextWrapper ->
                Part.Text(ordinal = textOrdinal++, text = item.value.text, streaming = inProgress)
            is SessionMessageAssistantContentInner.ReasoningWrapper -> with(item.value) {
                Part.Reasoning(
                    ordinal = reasoningOrdinal++,
                    text = text.orEmpty(),
                    streaming = inProgress && time?.completed == null,
                    startedAt = time?.created?.toLong(),
                    completedAt = time?.completed?.toLong(),
                )
            }
            is SessionMessageAssistantContentInner.ToolWrapper -> item.value.toDomain()
        }
    }
    return Message.Assistant(
        id = id.orEmpty(),
        createdAt = time.created.toLong(),
        agent = agent.orEmpty(),
        model = model?.toDomain() ?: ModelRef(id = "", providerId = ""),
        parts = parts,
        completedAt = time.completed?.toLong(),
        streamedAt = time.streamed?.toLong(),
        finish = finish?.value,
        error = error?.describe(),
        cost = cost ?: 0.0,
        tokens = tokens?.toDomain() ?: TokenUsage(),
    )
}

private fun SessionMessageAssistantTool.toDomain(): Part.Tool {
    val base = Part.Tool(
        callId = id.orEmpty(),
        name = name.orEmpty(),
        status = ToolStatus.Streaming,
        input = "",
        startedAt = time.created.toLong(),
        completedAt = time.completed?.toLong(),
    )
    return when (val state = state) {
        is SessionMessageAssistantToolState.StreamingWrapper -> base.copy(input = state.value.input)
        is SessionMessageAssistantToolState.RunningWrapper -> with(state.value) {
            base.copy(status = ToolStatus.Running, input = input.toString(), metadata = metadata.toStringMap())
        }
        is SessionMessageAssistantToolState.CompletedWrapper -> with(state.value) {
            base.copy(
                status = ToolStatus.Completed,
                input = input.toString(),
                output = content.joinedText(),
                metadata = metadata?.toStringMap().orEmpty(),
                diffs = toolDiffs(base.name, input, metadata),
            )
        }
        is SessionMessageAssistantToolState.ErrorWrapper -> with(state.value) {
            base.copy(
                status = ToolStatus.Error,
                input = input.toString(),
                output = content?.joinedText(),
                error = error.describe(),
                metadata = metadata?.toStringMap().orEmpty(),
            )
        }
    }
}

private fun SessionMessageShell.toDomain(): Message.Shell = Message.Shell(
    id = id.orEmpty(),
    createdAt = time.created.toLong(),
    command = command.orEmpty(),
    status = status.value,
    output = output?.output.orEmpty(),
    exit = exit?.takeUnless { it is JsonNull }?.displayText(),
)

/** Absolute path of a file attached by reference (`file://` URI); null for uploaded content. */
internal fun PromptFileAttachment.serverPath(): String? {
    val uri = (source as? PromptFileSource.PromptFileSourceAnyOf1Value)?.value?.uri ?: return null
    if (!uri.startsWith("file://")) return null
    // This app sends paths unencoded; other clients may percent-encode them.
    return runCatching { URI(uri).path }.getOrNull()?.takeIf { it.isNotEmpty() } ?: uri.removePrefix("file://")
}
