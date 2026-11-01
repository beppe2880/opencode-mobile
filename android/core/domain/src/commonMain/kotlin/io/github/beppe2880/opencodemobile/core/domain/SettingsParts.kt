package io.github.beppe2880.opencodemobile.core.domain

/** How Settings opens: the classic list, or the groups of settings with their live previews. */
enum class SettingsStyle { List, Preview }

/**
 * A category of Settings, in the order Settings lists them: a page of the List style and, when it
 * has [SettingsPart]s, a group with its own live preview in the Preview style. Only kept in
 * memory, never saved, so entries can be added or reordered freely.
 */
enum class SettingsArea {
    /** Saved servers and usage statistics; no options of its own. */
    Servers,
    Appearance,
    Chat,
    LiveActivity,

    /** The message box: agent outline, agent and model changes, the model of new chats. */
    Composer,
    VoiceInput,
    Sessions,

    /** Sharing from other apps to OpenCode (Android only). */
    Sharing,
    Notifications,
    Terminal,
    Updates,

    /** Versions and resetting every setting; no options of its own. */
    About,
}

/**
 * A part of an [area]'s preview that can be tapped: the preview reports which one, and Settings
 * offers the options of that part.
 */
enum class SettingsPart(val area: SettingsArea) {
    /** The colors of the whole app. */
    Theme(SettingsArea.Appearance),

    /** Prompts and answers: their text size. */
    TextSize(SettingsArea.Appearance),

    /** The title bar and the line under the title (folder, branch, changed files). */
    Header(SettingsArea.Chat),
    Messages(SettingsArea.Chat),
    Thoughts(SettingsArea.Chat),
    Tools(SettingsArea.Chat),

    /** Thinking and tool rows grouped into one summary row. */
    Activity(SettingsArea.Chat),

    /** The line under an answer: agent, model, time and cost. */
    Footer(SettingsArea.Chat),

    /** Permission requests and questions above the composer. */
    PendingCards(SettingsArea.Chat),

    /** Subagents and commands running in the background, under the chat title. */
    RunningLine(SettingsArea.LiveActivity),

    /** The step the agent is working on, kept open while it runs. */
    LiveStep(SettingsArea.LiveActivity),
    LiveThinking(SettingsArea.LiveActivity),
    LiveShell(SettingsArea.LiveActivity),
    LiveEdits(SettingsArea.LiveActivity),
    LiveReads(SettingsArea.LiveActivity),
    LiveOther(SettingsArea.LiveActivity),

    /** The prompt field with the agent and model chips and the microphone. */
    ComposerBox(SettingsArea.Composer),

    /** The microphone button: the phone's speech recognition or a cloud provider. */
    VoiceInput(SettingsArea.VoiceInput),

    /** When a picked agent or model reaches the server. */
    SelectionTiming(SettingsArea.Composer),

    /** The model of the new chat sheet. */
    NewChat(SettingsArea.Composer),
    SessionRows(SettingsArea.Sessions),
    SwipeChats(SettingsArea.Chat),

    /** The session list beside the chat on wide screens. */
    SplitView(SettingsArea.Appearance),

    /** OpenCode in the share menu of other apps, and what sharing opens. */
    ShareTarget(SettingsArea.Sharing),
    KeyBar(SettingsArea.Terminal),
    TerminalKeyboard(SettingsArea.Terminal),
    TerminalFullscreen(SettingsArea.Terminal),

    /** Session notifications as a whole. */
    NotificationShade(SettingsArea.Notifications),
    PermissionNotification(SettingsArea.Notifications),
    QuestionNotification(SettingsArea.Notifications),
    FinishedNotification(SettingsArea.Notifications),
    UpdateNotice(SettingsArea.Updates),
    ;

    companion object {
        fun of(area: SettingsArea): List<SettingsPart> = entries.filter { it.area == area }
    }
}
