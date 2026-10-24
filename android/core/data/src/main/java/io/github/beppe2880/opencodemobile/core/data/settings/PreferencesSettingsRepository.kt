package io.github.beppe2880.opencodemobile.core.data.settings

import android.content.SharedPreferences
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.LIVE_INTERVAL_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.OUTPUT_LINE_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.TEXT_SCALE_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Non-sensitive UI preferences. Unknown stored values fall back to [AppSettings] defaults. */
internal class PreferencesSettingsRepository(private val prefs: SharedPreferences) : SettingsRepository {

    private val _settings = MutableStateFlow(read())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override suspend fun update(transform: (AppSettings) -> AppSettings) = synchronized(this) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit()
            .putString(KEY_THEME, next.theme.name)
            .putBoolean(KEY_SHOW_REASONING, next.showReasoning)
            .putBoolean(KEY_EXPAND_TOOLS, next.expandToolsByDefault)
            .putBoolean(KEY_FOLLOW_RUNNING, next.followRunningTools)
            .putBoolean(KEY_LIVE_THINKING, next.liveThinking)
            .putBoolean(KEY_LIVE_SHELL, next.liveShell)
            .putBoolean(KEY_LIVE_EDITS, next.liveEdits)
            .putBoolean(KEY_LIVE_READS, next.liveReads)
            .putBoolean(KEY_LIVE_OTHER, next.liveOther)
            .putBoolean(KEY_FULL_COMMANDS, next.showFullCommands)
            .putBoolean(KEY_TOOL_DETAILS, next.showToolDetails)
            .putInt(KEY_OUTPUT_LINES, next.outputPreviewLines)
            .putLong(KEY_LIVE_INTERVAL, next.liveOutputIntervalMs)
            .putBoolean(KEY_KEEP_LIVE_OPEN, next.keepLiveStepsOpen)
            .putFloat(KEY_TEXT_SCALE, next.chatTextScale)
            .putBoolean(KEY_SHOW_COST, next.showCost)
            .putBoolean(KEY_SWIPE_CHATS, next.swipeBetweenChats)
            .putBoolean(KEY_AGENT_OUTLINE, next.agentColorOutline)
            .putBoolean(KEY_DICTATION, next.dictation)
            .putBoolean(KEY_GROUP_ACTIVITY, next.groupActivity)
            .putBoolean(KEY_NOTIFICATIONS, next.notifications)
            .putBoolean(KEY_NOTIFY_PERMISSIONS, next.notifyPermissions)
            .putBoolean(KEY_NOTIFY_QUESTIONS, next.notifyQuestions)
            .putBoolean(KEY_NOTIFY_FINISHED, next.notifyFinished)
            .putBoolean(KEY_NOTIFICATIONS_INTRO, next.notificationsIntroShown)
            .putString(KEY_TERMINAL_KEYS, next.terminalKeys?.let { Json.encodeToString(KeyRows, it) })
            .apply()
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: defaults.theme,
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
            swipeBetweenChats = prefs.getBoolean(KEY_SWIPE_CHATS, defaults.swipeBetweenChats),
            agentColorOutline = prefs.getBoolean(KEY_AGENT_OUTLINE, defaults.agentColorOutline),
            dictation = prefs.getBoolean(KEY_DICTATION, defaults.dictation),
            groupActivity = prefs.getBoolean(KEY_GROUP_ACTIVITY, defaults.groupActivity),
            notifications = prefs.getBoolean(KEY_NOTIFICATIONS, defaults.notifications),
            notifyPermissions = prefs.getBoolean(KEY_NOTIFY_PERMISSIONS, defaults.notifyPermissions),
            notifyQuestions = prefs.getBoolean(KEY_NOTIFY_QUESTIONS, defaults.notifyQuestions),
            notifyFinished = prefs.getBoolean(KEY_NOTIFY_FINISHED, defaults.notifyFinished),
            notificationsIntroShown = prefs.getBoolean(KEY_NOTIFICATIONS_INTRO, defaults.notificationsIntroShown),
            terminalKeys = prefs.getString(KEY_TERMINAL_KEYS, null)?.let { stored ->
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
        val KeyRows = ListSerializer(ListSerializer(String.serializer()))
    }
}
