package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.OpenCodeEvent
import io.github.beppe2880.opencodemobile.api.models.SessionInboxItem
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome

/** Maps a stream event to the subset the app reacts to; returns null for everything else. */
fun OpenCodeEvent.toDomain(): ServerEvent? = when (this) {
    is OpenCodeEvent.ServerconnectedWrapper -> ServerEvent.Connected

    is OpenCodeEvent.SessioncreatedWrapper -> ServerEvent.SessionChanged(value.data.sessionID)
    is OpenCodeEvent.SessionrenamedWrapper -> ServerEvent.SessionChanged(value.data.sessionID)
    is OpenCodeEvent.SessionmovedWrapper -> ServerEvent.SessionChanged(value.data.sessionID)
    is OpenCodeEvent.SessionmetadataupdatedWrapper -> ServerEvent.SessionChanged(value.data.sessionID)
    is OpenCodeEvent.SessiondeletedWrapper -> ServerEvent.SessionChanged(value.data.sessionID, deleted = true)

    is OpenCodeEvent.SessionexecutionstartedWrapper -> ServerEvent.ExecutionStarted(value.data.sessionID)
    is OpenCodeEvent.SessionexecutionsucceededWrapper -> with(value) {
        ServerEvent.ExecutionEnded(data.sessionID, SessionOutcome.Succeeded, idleId(id), created.toLong())
    }
    is OpenCodeEvent.SessionexecutionfailedWrapper -> with(value) {
        ServerEvent.ExecutionEnded(data.sessionID, SessionOutcome.Failed, idleId(id), created.toLong(), data.error.describe())
    }
    is OpenCodeEvent.SessionexecutioninterruptedWrapper -> with(value) {
        ServerEvent.ExecutionEnded(data.sessionID, SessionOutcome.Interrupted, idleId(id), created.toLong())
    }

    is OpenCodeEvent.SessionstepstartedWrapper -> with(value.data) {
        ServerEvent.StepStarted(sessionID, assistantMessageID, agent.orEmpty(), model.toDomain(), started)
    }
    is OpenCodeEvent.SessionstependedWrapper -> with(value.data) {
        ServerEvent.StepEnded(sessionID, assistantMessageID, finish.value, cost, tokens.toDomain(), value.created.toLong())
    }
    is OpenCodeEvent.SessionstepstreamedWrapper ->
        ServerEvent.StepStreamed(value.data.sessionID, value.data.assistantMessageID, value.created.toLong())
    is OpenCodeEvent.SessionstepfailedWrapper -> with(value.data) {
        ServerEvent.StepFailed(sessionID, assistantMessageID, error.describe())
    }

    is OpenCodeEvent.SessiontextstartedWrapper -> with(value.data) {
        ServerEvent.TextStarted(sessionID, assistantMessageID, ordinal.toInt())
    }
    is OpenCodeEvent.SessiontextdeltaWrapper -> with(value.data) {
        ServerEvent.TextDelta(sessionID, assistantMessageID, ordinal.toInt(), delta)
    }
    is OpenCodeEvent.SessiontextendedWrapper -> with(value.data) {
        ServerEvent.TextEnded(sessionID, assistantMessageID, ordinal.toInt(), text)
    }

    is OpenCodeEvent.SessionreasoningstartedWrapper -> with(value.data) {
        ServerEvent.ReasoningStarted(sessionID, assistantMessageID, ordinal.toInt(), value.created.toLong())
    }
    is OpenCodeEvent.SessionreasoningdeltaWrapper -> with(value.data) {
        ServerEvent.ReasoningDelta(sessionID, assistantMessageID, ordinal.toInt(), delta)
    }
    is OpenCodeEvent.SessionreasoningendedWrapper -> with(value.data) {
        ServerEvent.ReasoningEnded(sessionID, assistantMessageID, ordinal.toInt(), text, value.created.toLong())
    }

    is OpenCodeEvent.SessiontoolinputstartedWrapper -> with(value.data) {
        ServerEvent.ToolInputStarted(sessionID, assistantMessageID, id, name, value.created.toLong())
    }
    is OpenCodeEvent.SessiontoolinputdeltaWrapper -> with(value.data) {
        ServerEvent.ToolInputDelta(sessionID, assistantMessageID, id, delta)
    }
    is OpenCodeEvent.SessiontoolinputendedWrapper -> with(value.data) {
        ServerEvent.ToolInputEnded(sessionID, assistantMessageID, id, text)
    }
    is OpenCodeEvent.SessiontoolcalledWrapper -> with(value.data) {
        ServerEvent.ToolCalled(sessionID, assistantMessageID, id, input.toString())
    }
    is OpenCodeEvent.SessiontoolprogressWrapper -> with(value.data) {
        ServerEvent.ToolProgress(sessionID, assistantMessageID, id, metadata.toStringMap())
    }
    is OpenCodeEvent.SessiontoolsuccessWrapper -> with(value.data) {
        ServerEvent.ToolSucceeded(
            sessionID, assistantMessageID, id,
            output = content.joinedText(),
            metadata = metadata?.toStringMap().orEmpty(),
            at = value.created.toLong(),
            diffs = metadata.fileDiffs(),
        )
    }
    is OpenCodeEvent.SessiontoolfailedWrapper -> with(value.data) {
        ServerEvent.ToolFailed(sessionID, assistantMessageID, id, error.describe(), value.created.toLong())
    }

    is OpenCodeEvent.SessioninboxenqueuedWrapper -> with(value.data) {
        (item as? SessionInboxItem.SessionInboxItemAnyOfValue)?.let { user ->
            ServerEvent.InboxEnqueued(sessionID, inboxID, user.value.payload.text, value.created.toLong())
        }
    }
    is OpenCodeEvent.SessioninboxdeliveredWrapper -> ServerEvent.InboxDelivered(value.data.sessionID, value.data.inboxID)
    is OpenCodeEvent.SessioninboxcancelledWrapper -> ServerEvent.InboxCancelled(value.data.sessionID, value.data.inboxID)
    is OpenCodeEvent.SessioninboxdeliverychangedWrapper -> with(value.data) {
        ServerEvent.InboxDeliveryChanged(sessionID, inboxID, delivery.toDomain())
    }

    is OpenCodeEvent.SessionusageupdatedWrapper -> with(value.data) {
        ServerEvent.UsageUpdated(sessionID, cost, tokens.toDomain())
    }

    is OpenCodeEvent.PermissionaskedWrapper -> ServerEvent.PermissionChanged(value.data.sessionID)
    is OpenCodeEvent.PermissionrepliedWrapper -> ServerEvent.PermissionChanged(value.data.sessionID)

    is OpenCodeEvent.SessionagentselectedWrapper -> transcriptChanged(value.data.sessionID, "session.agent.selected")
    is OpenCodeEvent.SessionmodelselectedWrapper -> transcriptChanged(value.data.sessionID, "session.model.selected")
    is OpenCodeEvent.SessionshellstartedWrapper -> transcriptChanged(value.data.sessionID, "session.shell.started")
    is OpenCodeEvent.SessionshellendedWrapper -> transcriptChanged(value.data.sessionID, "session.shell.ended")
    is OpenCodeEvent.SessioncompactionstartedWrapper -> transcriptChanged(value.data.sessionID, "session.compaction.started")
    is OpenCodeEvent.SessioncompactionendedWrapper -> transcriptChanged(value.data.sessionID, "session.compaction.ended")
    is OpenCodeEvent.SessioncompactionfailedWrapper -> transcriptChanged(value.data.sessionID, "session.compaction.failed")
    is OpenCodeEvent.SessionskillactivatedWrapper -> transcriptChanged(value.data.sessionID, "session.skill.activated")
    is OpenCodeEvent.SessionretryscheduledWrapper -> transcriptChanged(value.data.sessionID, "session.retry.scheduled")
    is OpenCodeEvent.SessionrevertstagedWrapper -> ServerEvent.RevertChanged(value.data.sessionID, value.data.revert.messageID)
    is OpenCodeEvent.SessionrevertclearedWrapper -> ServerEvent.RevertChanged(value.data.sessionID, null)
    is OpenCodeEvent.SessionrevertcommittedWrapper -> ServerEvent.RevertChanged(value.data.sessionID, null, committed = true)

    is OpenCodeEvent.FormcreatedWrapper -> ServerEvent.FormsChanged(value.data.form.sessionID)
    is OpenCodeEvent.FormrepliedWrapper -> ServerEvent.FormsChanged(value.data.sessionID)
    is OpenCodeEvent.FormcancelledWrapper -> ServerEvent.FormsChanged(value.data.sessionID)

    else -> null
}

private fun transcriptChanged(sessionId: String, type: String) = ServerEvent.TranscriptChanged(sessionId, type)

/** The idle message recorded for an execution end reuses the event id with the `msg_` prefix. */
private fun idleId(eventId: String): String = "msg_" + eventId.removePrefix("evt_")
