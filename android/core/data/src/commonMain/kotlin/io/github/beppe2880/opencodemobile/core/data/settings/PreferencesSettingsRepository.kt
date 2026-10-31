package io.github.beppe2880.opencodemobile.core.data.settings

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.HEADER_OPENS_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.LIVE_INTERVAL_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.OUTPUT_LINE_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.TEXT_SCALE_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Non-sensitive UI preferences. Unknown stored values fall back to [AppSettings] defaults. */
internal class PreferencesSettingsRepository(private val prefs: Settings) : SettingsRepository {

    private val mutex = Mutex()

    private val _settings = MutableStateFlow(read())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override suspend fun update(transform: (AppSettings) -> AppSettings) = mutex.withLock {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.putString(KEY_THEME, next.theme.name)
        prefs.putBoolean(KEY_SHOW_REASONING, next.showReasoning)
        prefs.putBoolean(KEY_EXPAND_TOOLS, next.expandToolsByDefault)
        prefs.putBoolean(KEY_FOLLOW_RUNNING, next.followRunningTools)
        prefs.putBoolean(KEY_LIVE_THINKING, next.liveThinking)
        prefs.putBoolean(KEY_LIVE_SHELL, next.liveShell)
        prefs.putBoolean(KEY_LIVE_EDITS, next.liveEdits)
        prefs.putBoolean(KEY_LIVE_READS, next.liveReads)
        prefs.putBoolean(KEY_LIVE_OTHER, next.liveOther)
        prefs.putBoolean(KEY_FULL_COMMANDS, next.showFullCommands)
        prefs.putBoolean(KEY_TOOL_DETAILS, next.showToolDetails)
        prefs.putInt(KEY_OUTPUT_LINES, next.outputPreviewLines)
        prefs.putLong(KEY_LIVE_INTERVAL, next.liveOutputIntervalMs)
        prefs.putBoolean(KEY_KEEP_LIVE_OPEN, next.keepLiveStepsOpen)
        prefs.putFloat(KEY_TEXT_SCALE, next.chatTextScale)
        prefs.putBoolean(KEY_SHOW_COST, next.showCost)
        prefs.putString(KEY_HEADER_LINE, next.chatHeaderLine.name)
        prefs.putString(KEY_HEADER_OPENS, next.headerOpens.name)
        prefs.putBoolean(KEY_SWIPE_CHATS, next.swipeBetweenChats)
        prefs.putBoolean(KEY_AGENT_OUTLINE, next.agentColorOutline)
        prefs.putBoolean(KEY_DICTATION, next.dictation)
        prefs.putBoolean(KEY_GROUP_ACTIVITY, next.groupActivity)
        prefs.putBoolean(KEY_NOTIFICATIONS, next.notifications)
        prefs.putBoolean(KEY_NOTIFY_PERMISSIONS, next.notifyPermissions)
        prefs.putBoolean(KEY_NOTIFY_QUESTIONS, next.notifyQuestions)
        prefs.putBoolean(KEY_NOTIFY_FINISHED, next.notifyFinished)
        prefs.putBoolean(KEY_NOTIFICATIONS_INTRO, next.notificationsIntroShown)
        val keys = next.terminalKeys
        if (keys == null) prefs.remove(KEY_TERMINAL_KEYS) else prefs.putString(KEY_TERMINAL_KEYS, Json.encodeToString(KeyRows, keys))
        prefs.putBoolean(KEY_TERMINAL_KEYBOARD, next.terminalOpenKeyboard)
        prefs.putBoolean(KEY_TERMINAL_FULLSCREEN, next.terminalFullscreenByDefault)
        prefs.putBoolean(KEY_SPLIT_VIEW, next.splitView)
        prefs.putBoolean(KEY_LIST_PANE_HANDLE, next.listPaneHandle)
        val fraction = next.listPaneFraction
        if (fraction == null) prefs.remove(KEY_LIST_PANE_FRACTION) else prefs.putFloat(KEY_LIST_PANE_FRACTION, fraction)
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = ThemeMode.entries.firstOrNull { it.name == prefs.getStringOrNull(KEY_THEME) } ?: defaults.theme,
            showReasoning = prefs.getBoolean(KEY_SHOW_REASONING, defaults.showReasoning),
            expandToolsByDefault = prefs.getBoolean(KEY_EXPAND_TOOLS, defaults.expandToolsByDefault),
            followRunningTools = prefs.getBoolean(KEY_FOLLOW_RUNNING, defaults.followRunningTools),
            liveThinking = prefs.getBoolean(KEY_LIVE_THINKING, defaults.liveThinking),
            liveShell = prefs.getBoolean(KEY_LIVE_SHELL, defaults.liveShell),
            liveEdits = prefs.getBoolean(KEY_LIVE_EDITS, defaults.liveEdits),
            liveReads = prefs.getBoolean(KEY_LIVE_READS, defaults.liveReads),
            liveOther = prefs.getBoolean(KEY_LIVE_OTHER, defaults.liveOther),
            showFullCommands = prefs.getBoolean(KEY_FULL_COMMANDS, defaults.showFullCommands),
            showToolDetails = prefs.getBoolean(KEY_TOOL_DETAILS, defaults.showToolDetails),
            outputPreviewLines = prefs.getInt(KEY_OUTPUT_LINES, defaults.outputPreviewLines)
                .takeIf { it in OUTPUT_LINE_CHOICES } ?: defaults.outputPreviewLines,
            liveOutputIntervalMs = prefs.getLong(KEY_LIVE_INTERVAL, defaults.liveOutputIntervalMs)
                .takeIf { it in LIVE_INTERVAL_CHOICES } ?: defaults.liveOutputIntervalMs,
            keepLiveStepsOpen = prefs.getBoolean(KEY_KEEP_LIVE_OPEN, defaults.keepLiveStepsOpen),
            chatTextScale = prefs.getFloat(KEY_TEXT_SCALE, defaults.chatTextScale)
                .takeIf { it in TEXT_SCALE_CHOICES } ?: defaults.chatTextScale,
            showCost = prefs.getBoolean(KEY_SHOW_COST, defaults.showCost),
            chatHeaderLine = ChatHeaderLine.entries.firstOrNull { it.name == prefs.getStringOrNull(KEY_HEADER_LINE) } ?: defaults.chatHeaderLine,
            headerOpens = HEADER_OPENS_CHOICES.firstOrNull { it.name == prefs.getStringOrNull(KEY_HEADER_OPENS) } ?: defaults.headerOpens,
            swipeBetweenChats = prefs.getBoolean(KEY_SWIPE_CHATS, defaults.swipeBetweenChats),
            agentColorOutline = prefs.getBoolean(KEY_AGENT_OUTLINE, defaults.agentColorOutline),
            dictation = prefs.getBoolean(KEY_DICTATION, defaults.dictation),
            groupActivity = prefs.getBoolean(KEY_GROUP_ACTIVITY, defaults.groupActivity),
            notifications = prefs.getBoolean(KEY_NOTIFICATIONS, defaults.notifications),
            notifyPermissions = prefs.getBoolean(KEY_NOTIFY_PERMISSIONS, defaults.notifyPermissions),
            notifyQuestions = prefs.getBoolean(KEY_NOTIFY_QUESTIONS, defaults.notifyQuestions),
            notifyFinished = prefs.getBoolean(KEY_NOTIFY_FINISHED, defaults.notifyFinished),
            notificationsIntroShown = prefs.getBoolean(KEY_NOTIFICATIONS_INTRO, defaults.notificationsIntroShown),
            terminalOpenKeyboard = prefs.getBoolean(KEY_TERMINAL_KEYBOARD, defaults.terminalOpenKeyboard),
            terminalFullscreenByDefault = prefs.getBoolean(KEY_TERMINAL_FULLSCREEN, defaults.terminalFullscreenByDefault),
            splitView = prefs.getBoolean(KEY_SPLIT_VIEW, defaults.splitView),
            listPaneHandle = prefs.getBoolean(KEY_LIST_PANE_HANDLE, defaults.listPaneHandle),
            listPaneFraction = prefs.getFloatOrNull(KEY_LIST_PANE_FRACTION)?.takeIf { it in 0.05f..0.95f },
            terminalKeys = prefs.getStringOrNull(KEY_TERMINAL_KEYS)?.let { stored ->
                try {
                    Json.decodeFromString(KeyRows, stored)
                } catch (_: IllegalArgumentException) {
                    null
                }
            },
        )
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_SHOW_REASONING = "show_reasoning"
        const val KEY_EXPAND_TOOLS = "expand_tools_by_default"
        const val KEY_FOLLOW_RUNNING = "follow_running_tools"
        const val KEY_LIVE_THINKING = "live_thinking"
        const val KEY_LIVE_SHELL = "live_shell"
        const val KEY_LIVE_EDITS = "live_edits"
        const val KEY_LIVE_READS = "live_reads"
        const val KEY_LIVE_OTHER = "live_other"
        const val KEY_FULL_COMMANDS = "show_full_commands"
        const val KEY_TOOL_DETAILS = "show_tool_details"
        const val KEY_OUTPUT_LINES = "output_preview_lines"
        const val KEY_LIVE_INTERVAL = "live_output_interval_ms"
        const val KEY_KEEP_LIVE_OPEN = "keep_live_steps_open"
        const val KEY_TEXT_SCALE = "chat_text_scale"
        const val KEY_SHOW_COST = "show_cost"
        const val KEY_HEADER_LINE = "chat_header_line"
        const val KEY_HEADER_OPENS = "chat_header_opens"
        const val KEY_SWIPE_CHATS = "swipe_between_chats"
        const val KEY_AGENT_OUTLINE = "agent_color_outline"
        const val KEY_DICTATION = "dictation"
        const val KEY_GROUP_ACTIVITY = "group_activity"
        const val KEY_NOTIFICATIONS = "notifications"
        const val KEY_NOTIFY_PERMISSIONS = "notify_permissions"
        const val KEY_NOTIFY_QUESTIONS = "notify_questions"
        const val KEY_NOTIFY_FINISHED = "notify_finished"
        const val KEY_NOTIFICATIONS_INTRO = "notifications_intro_shown"
        const val KEY_TERMINAL_KEYS = "terminal_keys"
        const val KEY_TERMINAL_KEYBOARD = "terminal_open_keyboard"
        const val KEY_TERMINAL_FULLSCREEN = "terminal_fullscreen"
        const val KEY_SPLIT_VIEW = "split_view"
        const val KEY_LIST_PANE_FRACTION = "list_pane_fraction"
        const val KEY_LIST_PANE_HANDLE = "list_pane_handle"
        val KeyRows = ListSerializer(ListSerializer(String.serializer()))
    }
}
