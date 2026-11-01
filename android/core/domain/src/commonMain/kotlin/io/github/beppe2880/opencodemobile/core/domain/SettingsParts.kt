package io.github.beppe2880.opencodemobile.core.domain

/** A group of settings with its own live preview in Settings, in the order Settings lists them. */
enum class SettingsArea {
    Appearance,
    Chat,
    LiveActivity,
    Composer,
    Sessions,
    Terminal,
    Notifications,
    Updates,
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

    /** When a picked agent or model reaches the server. */
    SelectionTiming(SettingsArea.Composer),

    /** The model of the new chat sheet. */
    NewChat(SettingsArea.Composer),
    SessionRows(SettingsArea.Sessions),
    SwipeChats(SettingsArea.Sessions),

    /** The session list beside the chat on wide screens. */
    SplitView(SettingsArea.Sessions),
    KeyBar(SettingsArea.Terminal),
    TerminalKeyboard(SettingsArea.Terminal),
    TerminalFullscreen(SettingsArea.Terminal),

    /** Session notifications as a whole. */
    NotificationShade(SettingsArea.Notifications),
    PermissionNotification(SettingsArea.Notifications),
    QuestionNotification(SettingsArea.Notifications),
    FinishedNotification(SettingsArea.Notifications),
    UpdateBanner(SettingsArea.Updates),
    ;

    companion object {
        fun of(area: SettingsArea): List<SettingsPart> = entries.filter { it.area == area }
    }
}
