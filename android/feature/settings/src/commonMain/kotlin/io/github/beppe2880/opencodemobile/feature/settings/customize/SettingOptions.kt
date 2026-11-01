package io.github.beppe2880.opencodemobile.feature.settings.customize

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart

/**
 * One control of Settings. [fields] are the [AppSettings] properties it changes (a test checks
 * that every property has one); [requires] is the switch that must be on for it to apply.
 */
enum class SettingOption(val fields: List<String>, val requires: SettingOption? = null) {
    Theme(listOf("theme")),
    TextSize(listOf("chatTextScale")),
    HeaderLine(listOf("chatHeaderLine")),
    HeaderStyle(listOf("chatHeaderStyle")),
    HeaderOpens(listOf("headerOpens")),
    ShowReasoning(listOf("showReasoning")),
    GroupThoughts(listOf("groupThoughts")),
    GroupActivity(listOf("groupActivity")),
    ExpandTools(listOf("expandToolsByDefault")),
    ToolDetails(listOf("showToolDetails")),
    FullCommands(listOf("showFullCommands")),
    OutputLines(listOf("outputPreviewLines")),
    ShowCost(listOf("showCost")),
    PermissionsCollapsed(listOf("permissionsCollapsed")),
    QuestionsCollapsed(listOf("questionsCollapsed")),
    ConfirmStop(listOf("confirmStopCommand")),
    FollowRunning(listOf("followRunningTools")),
    KeepLiveOpen(listOf("keepLiveStepsOpen"), requires = FollowRunning),
    LiveThinking(listOf("liveThinking"), requires = FollowRunning),
    LiveShell(listOf("liveShell"), requires = FollowRunning),
    LiveInterval(listOf("liveOutputIntervalMs"), requires = LiveShell),
    LiveEdits(listOf("liveEdits"), requires = FollowRunning),
    LiveReads(listOf("liveReads"), requires = FollowRunning),
    LiveOther(listOf("liveOther"), requires = FollowRunning),
    AgentOutline(listOf("agentColorOutline")),
    Dictation(listOf("dictation")),
    SelectionApply(listOf("selectionApply")),
    NewChatModel(listOf("newChatModel")),
    RunningInList(listOf("showRunningWorkInList")),
    SwipeChats(listOf("swipeBetweenChats")),
    SplitView(listOf("splitView")),
    ListWidth(listOf("listPaneFraction"), requires = SplitView),
    ListHandle(listOf("listPaneHandle"), requires = SplitView),
    BackShowsList(listOf("backShowsList"), requires = ListHandle),
    TerminalKeys(listOf("terminalKeys")),
    TerminalKeyboard(listOf("terminalOpenKeyboard")),
    TerminalFullscreen(listOf("terminalFullscreenByDefault")),
    Notifications(listOf("notifications")),
    NotifyPermissions(listOf("notifyPermissions"), requires = Notifications),
    NotifyQuestions(listOf("notifyQuestions"), requires = Notifications),
    NotifyFinished(listOf("notifyFinished"), requires = Notifications),
    CheckUpdates(listOf("checkForUpdates")),
    ;

    /** The part whose options include this one first: where a search result opens. */
    val part: SettingsPart get() = SettingsPart.entries.first { this in it.options }
    val area: SettingsArea get() = part.area
}

/**
 * Properties no control changes directly: the one-time notifications explanation, and the edit
 * marks of the previews, switched from the toolbar of every area.
 */
internal val UNLISTED_FIELDS = setOf("notificationsIntroShown", "showEditMarks")

/** Every option of this part of a preview, applying or not. */
val SettingsPart.options: List<SettingOption>
    get() = when (this) {
        SettingsPart.Theme -> listOf(SettingOption.Theme)
        SettingsPart.TextSize -> listOf(SettingOption.TextSize)
        SettingsPart.Header -> listOf(SettingOption.HeaderLine, SettingOption.HeaderStyle, SettingOption.HeaderOpens)
        SettingsPart.Messages -> listOf(SettingOption.TextSize)
        SettingsPart.Thoughts -> listOf(SettingOption.ShowReasoning, SettingOption.GroupThoughts, SettingOption.GroupActivity)
        SettingsPart.Tools -> listOf(
            SettingOption.ExpandTools,
            SettingOption.ToolDetails,
            SettingOption.FullCommands,
            SettingOption.OutputLines,
            SettingOption.GroupActivity,
        )
        SettingsPart.Activity -> listOf(
            SettingOption.GroupActivity,
            SettingOption.ShowReasoning,
            SettingOption.GroupThoughts,
            SettingOption.ExpandTools,
            SettingOption.ToolDetails,
            SettingOption.FullCommands,
            SettingOption.OutputLines,
        )
        SettingsPart.Footer -> listOf(SettingOption.ShowCost)
        SettingsPart.PendingCards -> listOf(SettingOption.PermissionsCollapsed, SettingOption.QuestionsCollapsed)
        SettingsPart.RunningLine -> listOf(SettingOption.ConfirmStop)
        SettingsPart.LiveStep -> listOf(SettingOption.FollowRunning, SettingOption.KeepLiveOpen)
        SettingsPart.LiveThinking -> listOf(SettingOption.LiveThinking)
        SettingsPart.LiveShell -> listOf(SettingOption.LiveShell, SettingOption.LiveInterval)
        SettingsPart.LiveEdits -> listOf(SettingOption.LiveEdits)
        SettingsPart.LiveReads -> listOf(SettingOption.LiveReads)
        SettingsPart.LiveOther -> listOf(SettingOption.LiveOther)
        SettingsPart.ComposerBox -> listOf(SettingOption.AgentOutline, SettingOption.Dictation)
        SettingsPart.SelectionTiming -> listOf(SettingOption.SelectionApply)
        SettingsPart.NewChat -> listOf(SettingOption.NewChatModel)
        SettingsPart.SessionRows -> listOf(SettingOption.RunningInList)
        SettingsPart.SwipeChats -> listOf(SettingOption.SwipeChats)
        SettingsPart.SplitView -> listOf(SettingOption.SplitView, SettingOption.ListWidth, SettingOption.ListHandle, SettingOption.BackShowsList)
        SettingsPart.KeyBar -> listOf(SettingOption.TerminalKeys)
        SettingsPart.TerminalKeyboard -> listOf(SettingOption.TerminalKeyboard)
        SettingsPart.TerminalFullscreen -> listOf(SettingOption.TerminalFullscreen)
        SettingsPart.NotificationShade -> listOf(SettingOption.Notifications)
        SettingsPart.PermissionNotification -> listOf(SettingOption.NotifyPermissions)
        SettingsPart.QuestionNotification -> listOf(SettingOption.NotifyQuestions)
        SettingsPart.FinishedNotification -> listOf(SettingOption.NotifyFinished)
        SettingsPart.UpdateBanner -> listOf(SettingOption.CheckUpdates)
    }

/**
 * Whether the option changes anything with [settings]: a folder path alone has no style and is
 * not a line to tap, and the kinds of a switched off feature are hidden. [notificationsOn] is the
 * notification switch together with the system permission.
 */
fun SettingOption.appliesTo(settings: AppSettings, notificationsOn: Boolean = settings.notifications): Boolean = when (this) {
    SettingOption.HeaderStyle, SettingOption.HeaderOpens -> settings.chatHeaderLine != ChatHeaderLine.Folder
    SettingOption.KeepLiveOpen, SettingOption.LiveThinking, SettingOption.LiveShell, SettingOption.LiveEdits,
    SettingOption.LiveReads, SettingOption.LiveOther,
    -> settings.followRunningTools
    SettingOption.LiveInterval -> settings.followRunningTools && settings.liveShell
    SettingOption.ListWidth, SettingOption.ListHandle -> settings.splitView
    SettingOption.BackShowsList -> settings.splitView && settings.listPaneHandle
    SettingOption.NotifyPermissions, SettingOption.NotifyQuestions, SettingOption.NotifyFinished -> notificationsOn
    else -> true
}

/**
 * Whether the preview shows this part with [settings]: grouped thinking and tools make one row,
 * and hidden reasoning has no row. Its options stay reachable to bring it back.
 */
fun SettingsPart.shownWith(settings: AppSettings): Boolean = when (this) {
    SettingsPart.Thoughts -> settings.showReasoning && !settings.groupActivity
    SettingsPart.Tools -> !settings.groupActivity
    SettingsPart.Activity -> settings.groupActivity
    else -> true
}
