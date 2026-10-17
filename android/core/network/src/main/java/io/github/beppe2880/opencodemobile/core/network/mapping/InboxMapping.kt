package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.SessionInboxDelivery
import io.github.beppe2880.opencodemobile.api.models.SessionInboxInfo
import io.github.beppe2880.opencodemobile.core.domain.PromptDelivery
import io.github.beppe2880.opencodemobile.core.domain.QueuedPrompt

fun SessionInboxDelivery.toDomain(): PromptDelivery = when (this) {
    SessionInboxDelivery.STEER -> PromptDelivery.Steer
    SessionInboxDelivery.QUEUE -> PromptDelivery.Queue
}

/** The user's prompts; compactions, moves and synthetic items the server queued itself are skipped. */
fun SessionInboxInfo.toQueuedPrompt(): QueuedPrompt? {
    val user = (this as? SessionInboxInfo.UserWrapper)?.value ?: return null
    return QueuedPrompt(
        id = user.id ?: return null,
        text = user.payload.text,
        delivery = user.delivery.toDomain(),
        createdAt = user.time.created.toLong(),
    )
}
