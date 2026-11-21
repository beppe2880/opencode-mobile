package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextField
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.markdown.OcMarkdown
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_close
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_copy
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_retry
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_btw_ask
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_btw_empty
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_btw_failed
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_btw_loading
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_btw_question
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_btw_ready
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_btw_title
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

/** Only a completion arriving on this visible screen may open it, never a replay on return. */
@Composable
internal fun BtwAutoOpen(state: ChatState, allowed: Boolean, onEvent: (ChatEvent) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var wasLoading by remember(state.sessionId) { mutableStateOf(false) }
    LaunchedEffect(state.sessionId, state.btw?.loading) {
        val loading = state.btw?.loading == true
        if (wasLoading && !loading && allowed && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            !state.modelSheetOpen && state.changes == null && state.mcps == null && !state.workOpen &&
            state.runningPanel == null && state.preview == null
        ) {
            onEvent(ChatEvent.BtwOpen(true))
        }
        wasLoading = loading
    }
}

@Composable
internal fun BtwStatus(state: ChatState, onEvent: (ChatEvent) -> Unit) {
    val btw = state.btw ?: return
    val label = when {
        btw.loading -> Res.string.chat_btw_loading
        btw.error != null -> Res.string.chat_btw_failed
        btw.answer != null -> Res.string.chat_btw_ready
        else -> Res.string.chat_btw_title
    }
    OcChip(
        text = "/btw · " + stringResource(label),
        onClick = { onEvent(ChatEvent.BtwOpen(true)) },
        modifier = Modifier.testTag("chat_btw_status").semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
internal fun BtwPanel(state: BtwState, onEvent: (ChatEvent) -> Unit, onCopy: (String) -> Unit) {
    val dismiss = { onEvent(ChatEvent.BtwOpen(false)) }
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val wide = with(density) { size.width.toDp() >= 600.dp }
    val maxHeight = with(density) { size.height.toDp() * 0.85f }
    if (wide) {
        Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                color = OcTheme.colors.bgBase,
                shape = RoundedCornerShape(OcRadius.sheet),
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().testTag("chat_btw_panel").exposeTestTags(),
            ) {
                BtwContent(state, onEvent, onCopy, Modifier.heightIn(max = maxHeight))
            }
        }
    } else {
        OcBottomSheet(onDismiss = dismiss, testTag = "chat_btw_panel") {
            BtwContent(state, onEvent, onCopy, Modifier.heightIn(max = maxHeight).windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun BtwContent(state: BtwState, onEvent: (ChatEvent) -> Unit, onCopy: (String) -> Unit, modifier: Modifier) {
    val c = OcTheme.colors
    val cache = rememberMarkdownCache()
    Column(
        modifier = modifier.imePadding().padding(OcSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Text(stringResource(Res.string.chat_btw_title) + " · /btw", style = MaterialTheme.typography.titleMedium, color = c.textBase)
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            if (state.answer == null && !state.loading) {
                OcTextField(
                    value = state.question,
                    onValueChange = { onEvent(ChatEvent.BtwQuestion(it)) },
                    label = stringResource(Res.string.chat_btw_question),
                    singleLine = false,
                    maxLines = 5,
                    modifier = Modifier.testTag("chat_btw_question"),
                )
            } else {
                SelectionContainer { Text(state.question, style = MaterialTheme.typography.bodyMedium, color = c.textMuted) }
            }
            if (state.loading) Text(stringResource(Res.string.chat_btw_loading), color = c.textMuted)
            state.error?.let { Text(it.resolve(), color = c.danger.fg, modifier = Modifier.testTag("chat_btw_error")) }
            state.answer?.let { answer ->
                SelectionContainer {
                    OcMarkdown("btw", answer.ifBlank { stringResource(Res.string.chat_btw_empty) }, cache, Modifier.testTag("chat_btw_answer"))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            if (state.answer != null) {
                OcButton(
                    text = stringResource(UiRes.string.oc_copy),
                    onClick = { onCopy(state.answer) },
                    modifier = Modifier.weight(1f).testTag("chat_btw_copy"),
                )
            } else {
                OcButton(
                    text = stringResource(if (state.error != null) UiRes.string.oc_retry else Res.string.chat_btw_ask),
                    onClick = { onEvent(ChatEvent.AskBtw) },
                    enabled = state.question.isNotBlank(),
                    loading = state.loading,
                    modifier = Modifier.weight(1f).testTag("chat_btw_submit"),
                )
            }
            OcButton(
                text = stringResource(UiRes.string.oc_close),
                onClick = { onEvent(ChatEvent.BtwOpen(false)) },
                variant = OcButtonVariant.Secondary,
                modifier = Modifier.weight(1f).testTag("chat_btw_close"),
            )
        }
    }
}
