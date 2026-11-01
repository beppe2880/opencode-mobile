package io.github.beppe2880.opencodemobile.core.ui.text

import androidx.compose.runtime.Composable
import io.github.beppe2880.opencodemobile.core.domain.ChatElement
import io.github.beppe2880.opencodemobile.core.ui.resources.Res
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_chat_part_activity
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_chat_part_composer
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_chat_part_footer
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_chat_part_header
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_chat_part_messages
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_chat_part_pending
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_chat_part_thoughts
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_chat_part_tools
import org.jetbrains.compose.resources.stringResource

/** The name of a part of the chat, as the chat preview and its options in Settings call it. */
@Composable
fun ChatElement.label(): String = stringResource(
    when (this) {
        ChatElement.Header -> Res.string.oc_chat_part_header
        ChatElement.Messages -> Res.string.oc_chat_part_messages
        ChatElement.Thoughts -> Res.string.oc_chat_part_thoughts
        ChatElement.Tools -> Res.string.oc_chat_part_tools
        ChatElement.Activity -> Res.string.oc_chat_part_activity
        ChatElement.Footer -> Res.string.oc_chat_part_footer
        ChatElement.PendingCard -> Res.string.oc_chat_part_pending
        ChatElement.Composer -> Res.string.oc_chat_part_composer
    },
)
