package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.AgentMode
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatElement
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_chat_part_customize
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.text.label
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

/**
 * A chat made of sample data and drawn by the real chat components with [settings], for the
 * customizer in Settings. Nothing in it reacts like the chat: tapping a part (or activating it
 * with a screen reader) calls [onCustomize] with it. [marks] outlines every part that can be
 * tapped; [selected] highlights the one whose options are open.
 */
@Composable
fun ChatPreview(
    settings: AppSettings,
    onCustomize: (ChatElement) -> Unit,
    modifier: Modifier = Modifier,
    marks: Boolean = true,
    selected: ChatElement? = null,
) {
    val state = remember(settings) { previewChatState(settings) }
    val cache = rememberMarkdownCache()
    val ignore: (ChatEvent) -> Unit = {}
    val part: @Composable (ChatElement, Modifier, @Composable () -> Unit) -> Unit = { element, partModifier, content ->
        Customizable(element, marks, selected == element, onCustomize, settings, partModifier, content)
    }
    val header: @Composable () -> Unit = {
        part(ChatElement.Header, Modifier.padding(OcSpacing.xxs)) {
            ChatTopBar(
                state = state,
                showBack = true,
                onBack = {},
                onContext = {},
                onChanges = {},
                onHeaderChanges = {},
                onTerminal = {},
                onMcps = {},
                onRunning = {},
                applyStatusBarInset = false,
            )
        }
    }
    val transcript: @Composable () -> Unit = {
        TranscriptLocals(state) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = OcSpacing.md),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                state.items.forEach { item ->
                    val element = item.element()
                    val row: @Composable () -> Unit = {
                        TranscriptItem(item, state, ignore, onCopy = {}, cache = cache, modifier = Modifier.fillMaxWidth().padding(OcSpacing.xs))
                    }
                    if (element == null) Box(Modifier.padding(horizontal = OcSpacing.md)) {
                        row()
                    } else part(element, Modifier.padding(horizontal = OcSpacing.md), row)
                }
            }
        }
    }
    val bottom: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = OcSpacing.xs, end = OcSpacing.xs, bottom = OcSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            state.permissions.firstOrNull()?.let { request ->
                part(ChatElement.PendingCard, Modifier) {
                    PermissionCard(
                        request = request,
                        pendingCount = state.permissions.size,
                        replying = false,
                        collapsed = state.isPermissionCollapsed(request.id),
                        onCollapse = {},
                        onReply = {},
                        // Room between the card's own border and the outline of the part.
                        modifier = Modifier.padding(OcSpacing.sm),
                    )
                }
            }
            part(ChatElement.Composer, Modifier) {
                Composer(
                    state = state,
                    onEvent = ignore,
                    onAttachPhoto = {},
                    onAttachFile = {},
                    modifier = Modifier.padding(OcSpacing.sm),
                    voice = if (state.dictation) VoiceInput(listening = false, level = 0f) else null,
                )
            }
        }
    }
    BoxWithConstraints(modifier.background(OcTheme.colors.bgBase).testTag("chat_preview")) {
        // Short windows (landscape phones) scroll the whole chat: the header and the composer
        // pinned like in the chat would leave no room for the messages.
        if (maxHeight >= PINNED_MIN_HEIGHT) {
            Column(Modifier.fillMaxSize()) {
                header()
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { transcript() }
                bottom()
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                header()
                transcript()
                bottom()
            }
        }
    }
}

private val PINNED_MIN_HEIGHT = 480.dp

/** The part of the chat a transcript row belongs to; null for rows no setting changes. */
internal fun ChatItem.element(): ChatElement? = when (this) {
    is ChatItem.User, is ChatItem.Text -> ChatElement.Messages
    is ChatItem.Reasoning, is ChatItem.Thoughts -> ChatElement.Thoughts
    is ChatItem.Tools -> ChatElement.Tools
    is ChatItem.Activity -> ChatElement.Activity
    is ChatItem.Footer -> ChatElement.Footer
    is ChatItem.Error, is ChatItem.Notice, is ChatItem.Shell, is ChatItem.Working -> null
}

/** The sample chat of the customizer, shown as [settings] would show a real one. */
internal fun previewChatState(settings: AppSettings): ChatState {
    val directory = "/home/user/code/shop"
    val models = SampleData.models
    return ChatState(
        sessionId = "preview",
        title = "Fix the failing cart tests",
        directory = directory,
        agent = "build",
        model = SampleData.haiku,
        models = models,
        agents = SampleData.agents.filter { !it.hidden && it.mode != AgentMode.Subagent },
        items = ChatItemsBuilder.build(
            SampleData.customizeTranscript,
            running = false,
            showReasoning = settings.showReasoning,
            modelLabel = models::labelFor,
            groupActivity = settings.groupActivity,
            groupThoughts = settings.groupThoughts,
        ),
        loading = false,
        synced = true,
        hasOlder = false,
        permissions = listOf(SampleData.permission),
        vcs = VcsLine(headerFolder(settings.chatHeaderLine, directory), "fix/coupon-rounding", files = 2)
            .takeIf { settings.chatHeaderLine != ChatHeaderLine.Folder },
    ).withSettings(settings)
}

/**
 * [content] as one button named after [element]: taps never reach the chat controls inside.
 * With [marks] a dashed outline and an edit badge show it can be tapped; [settings] only tells
 * when the part may have changed size.
 */
@Composable
private fun Customizable(
    element: ChatElement,
    marks: Boolean,
    selected: Boolean,
    onCustomize: (ChatElement) -> Unit,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = OcTheme.colors
    val label = element.label()
    val action = stringResource(UiRes.string.oc_chat_part_customize)
    val outline = if (selected) c.textAccent else c.textAccent.copy(alpha = 0.45f)
    // The options may take part of the screen: the part they change scrolls into view, also
    // after a change that resized it.
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(selected, settings) { if (selected) bringIntoView.bringIntoView() }
    Box(
        modifier = modifier
            .bringIntoViewRequester(bringIntoView)
            .testTag("chat_preview_${element.name.lowercase()}")
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                this.selected = selected
                onClick(label = action) {
                    onCustomize(element)
                    true
                }
            }
            .drawWithContent {
                drawContent()
                if (!marks && !selected) return@drawWithContent
                val width = (if (selected) 2.dp else 1.dp).toPx()
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(OcRadius.lg.toPx()),
                    topLeft = Offset(width / 2, width / 2),
                    size = Size(size.width - width, size.height - width),
                    style = Stroke(
                        width = width,
                        pathEffect = if (selected) null else PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            },
    ) {
        content()
        // On top of the content, so the chat controls under it never get the tap.
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(OcRadius.lg))
                .clickable { onCustomize(element) },
        )
        if (marks || selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(18.dp)
                    .background(if (selected) c.bgAccent else c.layer02, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = if (selected) c.onAccent else c.textAccent, modifier = Modifier.size(11.dp))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ChatPreviewPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        ChatPreview(settings = AppSettings(), onCustomize = {}, selected = ChatElement.Composer)
    }
}
