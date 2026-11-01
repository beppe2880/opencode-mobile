package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.AgentMode
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.SelectionApply
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.domain.VoiceRecordingStyle
import io.github.beppe2880.opencodemobile.core.domain.sttModel
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcCustomizable
import io.github.beppe2880.opencodemobile.core.ui.components.PartPreview
import io.github.beppe2880.opencodemobile.core.ui.markdown.MarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.text.formatDuration
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_folds
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_kept_open
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_not_followed
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_refresh
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_selection_next
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_selection_now
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_stop_asks
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_stop_direct
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_voice_cloud
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_voice_hold
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_voice_off
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_preview_voice_system
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.TimeSource

// Previews of the chat for Settings: sample data drawn by the real chat components with the
// settings of [PartPreview]. Nothing in them reacts like the chat: the parts only report taps.

/** The chat area: header, prompt, thinking and tools, answer, footer, the pending cards and the composer. */
@Composable
fun ChatPreview(preview: PartPreview, modifier: Modifier = Modifier) {
    val state = remember(preview.settings, preview.running) { previewChatState(preview.settings, preview.running) }
    val cache = rememberMarkdownCache()
    Column(modifier.fillMaxWidth().testTag("chat_preview")) {
        OcCustomizable(SettingsPart.Header, preview, Modifier.padding(OcSpacing.sm)) {
            Column {
                PreviewTopBar(state)
                if (preview.running) RunningLine(state.work, state.headerStyle, onClick = {})
            }
        }
        Transcript(state, preview, cache, liveGroupKey = state.liveGroupKey())
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = OcSpacing.sm, end = OcSpacing.sm, bottom = OcSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            OcCustomizable(SettingsPart.PendingCards, preview) {
                Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    val request = state.permissions.first()
                    PermissionCard(
                        request = request,
                        pendingCount = 1,
                        replying = false,
                        collapsed = state.isPermissionCollapsed(request.id),
                        onCollapse = {},
                        onReply = {},
                    )
                    val form = state.forms.first()
                    FormCard(
                        form = form,
                        answers = emptyMap(),
                        pendingCount = 1,
                        submitting = false,
                        collapsed = state.isFormCollapsed(form.id),
                        onCollapse = {},
                        onAnswer = { _, _ -> },
                        onSubmit = {},
                        onDismiss = {},
                    )
                }
            }
            OcCustomizable(SettingsPart.ComposerBox, preview) { PreviewComposer(state) }
        }
    }
}

/** Theme and chat text size: the title bar and the three themes, then a prompt and its answer. */
@Composable
fun AppearancePreview(preview: PartPreview, modifier: Modifier = Modifier) {
    val state = remember(preview.settings) { previewChatState(preview.settings, running = false) }
    val cache = rememberMarkdownCache()
    Column(modifier.fillMaxWidth().padding(vertical = OcSpacing.xs).testTag("appearance_preview")) {
        OcCustomizable(SettingsPart.Theme, preview, Modifier.padding(horizontal = OcSpacing.sm)) {
            Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                PreviewTopBar(state)
                ThemeSwatches(preview.settings.theme)
            }
        }
        OcCustomizable(SettingsPart.TextSize, preview, Modifier.padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm)) {
            TranscriptLocals(state) {
                Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    state.items.filter { it is ChatItem.User || it is ChatItem.Text }.forEach { item ->
                        TranscriptItem(item, state, onEvent = {}, onCopy = {}, cache = cache, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/**
 * The agent at work: what runs in the background, then one running step per kind of work the
 * chat can follow, open or folded as the settings say. A running command prints a line at the
 * output refresh interval.
 */
@Composable
fun LiveActivityPreview(preview: PartPreview, modifier: Modifier = Modifier) {
    val s = preview.settings
    val cache = rememberMarkdownCache()
    val base = remember(s) { ChatState(directory = SHOP, models = SampleData.models, loading = false).withSettings(s) }
    var lines by remember { mutableIntStateOf(3) }
    val ticking = !preview.still && s.followRunningTools && s.liveShell
    LaunchedEffect(ticking, s.liveOutputIntervalMs) {
        while (ticking) {
            delay(s.liveOutputIntervalMs)
            lines = if (lines >= 12) 3 else lines + 1
        }
    }
    Column(modifier.fillMaxWidth().padding(vertical = OcSpacing.xs).testTag("live_preview")) {
        OcCustomizable(SettingsPart.RunningLine, preview, Modifier.padding(horizontal = OcSpacing.sm)) {
            Column {
                RunningLine(SampleData.runningWork, base.headerStyle, onClick = {})
                Caption(if (s.confirmStopCommand) Res.string.chat_preview_stop_asks else Res.string.chat_preview_stop_direct)
            }
        }
        val kept = s.followRunningTools && s.keepLiveStepsOpen
        LiveStep(SettingsPart.LiveStep, SampleData.customizeTranscript, running = false, base, preview, cache, keptOpen = kept) {
            Caption(
                when {
                    !s.followRunningTools -> Res.string.chat_preview_not_followed
                    kept -> Res.string.chat_preview_kept_open
                    else -> Res.string.chat_preview_folds
                },
            )
        }
        LiveStep(SettingsPart.LiveThinking, SampleData.liveStep(SampleData.LiveKind.Thinking), running = true, base, preview, cache)
        LiveStep(SettingsPart.LiveShell, SampleData.liveStep(SampleData.LiveKind.Shell, lines), running = true, base, preview, cache) {
            if (ticking) Caption(Res.string.chat_preview_refresh, formatDuration(s.liveOutputIntervalMs))
        }
        LiveStep(SettingsPart.LiveEdits, SampleData.liveStep(SampleData.LiveKind.Edit), running = true, base, preview, cache)
        LiveStep(SettingsPart.LiveReads, SampleData.liveStep(SampleData.LiveKind.Read), running = true, base, preview, cache)
        LiveStep(SettingsPart.LiveOther, SampleData.liveStep(SampleData.LiveKind.Other), running = true, base, preview, cache)
    }
}

/** The composer with the agent outline, and when a picked agent or model applies. */
@Composable
fun ComposerPreview(preview: PartPreview, modifier: Modifier = Modifier) {
    val state = remember(preview.settings) { previewChatState(preview.settings, running = false) }
    val c = OcTheme.colors
    Column(modifier.fillMaxWidth().padding(OcSpacing.sm).testTag("composer_preview"), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        OcCustomizable(SettingsPart.ComposerBox, preview) { PreviewComposer(state) }
        OcCustomizable(SettingsPart.SelectionTiming, preview) {
            Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    OcChip(text = "Plan", onClick = {}, dotColor = c.agents.forAgent("plan"), selected = true)
                    OcChip(text = "Claude Sonnet 5", onClick = {}, secondaryText = "Anthropic", selected = true)
                }
                Caption(
                    if (preview.settings.selectionApply == SelectionApply.NextMessage) Res.string.chat_preview_selection_next
                    else Res.string.chat_preview_selection_now,
                )
            }
        }
    }
}

/** The composer with its microphone, and what turns the voice into text. */
@Composable
fun VoiceInputPreview(preview: PartPreview, modifier: Modifier = Modifier) {
    val state = remember(preview.settings) { previewChatState(preview.settings, running = false) }
    Column(modifier.fillMaxWidth().padding(OcSpacing.sm).testTag("voice_preview")) {
        OcCustomizable(SettingsPart.VoiceInput, preview) {
            Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                PreviewComposer(state)
                VoicePreview(preview.settings)
            }
        }
    }
}

private const val SHOP = "/home/user/code/shop"

@Composable
private fun PreviewTopBar(state: ChatState) {
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

@Composable
private fun PreviewComposer(state: ChatState) {
    Composer(
        state = state,
        onEvent = {},
        onAttachPhoto = {},
        onAttachFile = {},
        voice = if (state.dictation) VoiceInput(listening = false, level = 0f) else null,
    )
}

/** What the microphone does: off, the phone's recognizer, or a sample cloud recording with where it goes. */
@Composable
private fun VoicePreview(settings: AppSettings) {
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
        when {
            !settings.dictation -> Caption(Res.string.chat_preview_voice_off)
            settings.voiceEngine == VoiceEngine.System -> Caption(Res.string.chat_preview_voice_system)
            else -> {
                when (settings.voiceRecordingStyle) {
                    VoiceRecordingStyle.Waveform -> {
                        // A sample voice keeps the waveform moving.
                        val start = remember { TimeSource.Monotonic.markNow() }
                        RecorderBar(
                            elapsedMs = 7_000,
                            level = { previewLevel(start.elapsedNow().inWholeMilliseconds) },
                            reducedMotion = rememberReducedMotion(),
                            onCancel = {},
                            onConfirm = {},
                        )
                    }
                    VoiceRecordingStyle.Compact -> VoiceStrip(CloudVoicePhase.Recording(elapsedMs = 7_000), settings.sttProvider.label, onVoice = {})
                }
                Caption(Res.string.chat_preview_voice_cloud, settings.sttProvider.label, settings.sttModel())
                if (settings.voiceHoldToRecord) Caption(Res.string.chat_preview_voice_hold)
            }
        }
    }
}

/** The transcript rows, each in the part it belongs to. */
@Composable
private fun Transcript(state: ChatState, preview: PartPreview, cache: MarkdownCache, liveGroupKey: String?) {
    TranscriptLocals(state) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = OcSpacing.sm, vertical = OcSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            state.items.forEach { item ->
                val row: @Composable () -> Unit = {
                    TranscriptItem(item, state, onEvent = {}, onCopy = {}, cache = cache, modifier = Modifier.fillMaxWidth(), liveGroupKey = liveGroupKey)
                }
                val part = item.part()
                if (part == null) Box(Modifier.padding(horizontal = OcSpacing.sm)) { row() } else OcCustomizable(part, preview, content = row)
            }
        }
    }
}

/** One step of the agent ([messages]) as the chat shows it, without the prompt, in [part]. */
@Composable
private fun LiveStep(
    part: SettingsPart,
    messages: List<Message>,
    running: Boolean,
    base: ChatState,
    preview: PartPreview,
    cache: MarkdownCache,
    keptOpen: Boolean = false,
    caption: @Composable () -> Unit = {},
) {
    val s = preview.settings
    val state = base.copy(
        running = running,
        items = ChatItemsBuilder.build(messages, running, s.showReasoning, SampleData.models::labelFor, s.groupActivity, s.groupThoughts)
            .filter { it is ChatItem.Tools || it is ChatItem.Activity || it is ChatItem.Reasoning || it is ChatItem.Thoughts },
    )
    val live = state.liveGroupKey()
    OcCustomizable(part, preview, Modifier.padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs)) {
        TranscriptLocals(state) {
            Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                state.items.forEach { item ->
                    TranscriptItem(
                        item,
                        state,
                        onEvent = {},
                        onCopy = {},
                        cache = cache,
                        modifier = Modifier.fillMaxWidth(),
                        liveGroupKey = live,
                        keptOpen = { keptOpen },
                    )
                }
                caption()
            }
        }
    }
}

@Composable
private fun Caption(text: StringResource, vararg args: Any) {
    Text(
        stringResource(text, *args),
        style = MaterialTheme.typography.bodySmall,
        color = OcTheme.colors.textMuted,
        modifier = Modifier.padding(horizontal = OcSpacing.gutter, vertical = OcSpacing.xxs),
    )
}

/** The three themes, the chosen one outlined; each tile is drawn in its own colors. */
@Composable
private fun ThemeSwatches(theme: ThemeMode) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = OcSpacing.gutter, end = OcSpacing.gutter, bottom = OcSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Swatch(ThemeMode.System, Icons.Outlined.SettingsBrightness, theme, Modifier.weight(1f))
        Swatch(ThemeMode.Light, Icons.Outlined.LightMode, theme, Modifier.weight(1f))
        Swatch(ThemeMode.Dark, Icons.Outlined.DarkMode, theme, Modifier.weight(1f))
    }
}

@Composable
private fun Swatch(mode: ThemeMode, icon: ImageVector, selected: ThemeMode, modifier: Modifier) {
    val accent = OcTheme.colors.textAccent
    val shape = RoundedCornerShape(OcRadius.md)
    val tile: @Composable (Boolean, Modifier) -> Unit = { dark, tileModifier ->
        OcTheme(darkTheme = dark) {
            val c = OcTheme.colors
            Column(tileModifier.fillMaxHeight().background(c.bgBase).padding(OcSpacing.xs), verticalArrangement = Arrangement.spacedBy(OcSpacing.xxs)) {
                Box(Modifier.fillMaxWidth(0.7f).height(4.dp).background(c.textBase, RoundedCornerShape(2.dp)))
                Box(Modifier.align(Alignment.End).fillMaxWidth(0.5f).height(8.dp).background(c.userBubbleBg, RoundedCornerShape(2.dp)))
                Box(Modifier.fillMaxWidth(0.9f).height(4.dp).background(c.textMuted, RoundedCornerShape(2.dp)))
            }
        }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(OcSpacing.xxs)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .border(if (mode == selected) 2.dp else 1.dp, if (mode == selected) accent else OcTheme.colors.borderStrong, shape)
                .padding(3.dp)
                .clip(RoundedCornerShape(OcRadius.sm)),
        ) {
            when (mode) {
                ThemeMode.System -> {
                    tile(false, Modifier.weight(1f))
                    tile(true, Modifier.weight(1f))
                }
                ThemeMode.Light -> tile(false, Modifier.weight(1f))
                ThemeMode.Dark -> tile(true, Modifier.weight(1f))
            }
        }
        Icon(icon, contentDescription = null, tint = if (mode == selected) accent else OcTheme.colors.iconMuted, modifier = Modifier.size(16.dp))
    }
}

/** The part of the chat a transcript row belongs to; null for rows no setting changes. */
internal fun ChatItem.part(): SettingsPart? = when (this) {
    is ChatItem.User, is ChatItem.Text -> SettingsPart.Messages
    is ChatItem.Reasoning, is ChatItem.Thoughts -> SettingsPart.Thoughts
    is ChatItem.Tools -> SettingsPart.Tools
    is ChatItem.Activity -> SettingsPart.Activity
    is ChatItem.Footer -> SettingsPart.Footer
    is ChatItem.Error, is ChatItem.Notice, is ChatItem.Shell, is ChatItem.Working -> null
}

/** The group the agent is working on, open between one tool and the next, like in the chat. */
internal fun ChatState.liveGroupKey(): String? =
    if (!running || live == null) null
    else items.lastOrNull { it !is ChatItem.Working }?.takeIf { it is ChatItem.Tools || it is ChatItem.Activity }?.key

/** The sample chat of the previews, shown as [settings] would show a real one, finished or [running]. */
internal fun previewChatState(settings: AppSettings, running: Boolean): ChatState {
    val models = SampleData.models
    return ChatState(
        sessionId = "preview",
        title = "Fix the failing cart tests",
        directory = SHOP,
        agent = "build",
        model = SampleData.haiku,
        models = models,
        agents = SampleData.agents.filter { !it.hidden && it.mode != AgentMode.Subagent },
        items = ChatItemsBuilder.build(
            if (running) SampleData.customizeRunning else SampleData.customizeTranscript,
            running = running,
            showReasoning = settings.showReasoning,
            modelLabel = models::labelFor,
            groupActivity = settings.groupActivity,
            groupThoughts = settings.groupThoughts,
        ),
        loading = false,
        synced = true,
        hasOlder = false,
        running = running,
        permissions = listOf(SampleData.permission),
        forms = listOf(SampleData.question),
        work = if (running) SampleData.runningWork else ChatState().work,
        vcs = VcsLine(headerFolder(settings.chatHeaderLine, SHOP), "fix/coupon-rounding", files = 2)
            .takeIf { settings.chatHeaderLine != ChatHeaderLine.Folder },
    ).withSettings(settings)
}

@PreviewLightDark
@Composable
private fun ChatPreviewPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        ChatPreview(PartPreview(AppSettings(), selected = SettingsPart.ComposerBox))
    }
}
