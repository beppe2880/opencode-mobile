package io.github.beppe2880.opencodemobile.feature.settings.customize

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.ui.platform.shareTargetSupported

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
    VoiceMode(listOf("voiceEngine"), requires = Dictation),
    VoiceProvider(listOf("sttProvider", "sttBaseUrl"), requires = VoiceMode),
    VoiceModel(listOf("sttModels"), requires = VoiceMode),

    /** The provider's API key, kept in secure storage rather than in the settings. */
    VoiceKey(emptyList(), requires = VoiceMode),
    VoiceLanguage(listOf("sttLanguageHint"), requires = VoiceMode),
    SelectionApply(listOf("selectionApply")),
    NewChatModel(listOf("newChatModel")),
    ShareMenu(listOf("showInShareMenu")),
    ShareOpens(listOf("shareOpens"), requires = ShareMenu),
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
    UpdateNotice(listOf("updateNotice")),
    ;

    /** The part whose options include this one first: where a search result opens. */
    val part: SettingsPart get() = SettingsPart.entries.first { this in it.options }
    val area: SettingsArea get() = part.area
}

/**
 * Properties no option changes: the one-time notifications explanation, and how Settings itself
 * looks (the edit marks and the preview size of the previews, the style picked at its top).
 */
internal val UNLISTED_FIELDS = setOf("notificationsIntroShown", "showEditMarks", "settingsStyle", "settingsPreviewFraction")

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
        SettingsPart.VoiceInput -> VOICE_OPTIONS
        SettingsPart.SelectionTiming -> listOf(SettingOption.SelectionApply, SettingOption.AgentOutline)
        // Sharing from other apps opens the new chat sheet.
        SettingsPart.NewChat -> listOf(SettingOption.NewChatModel, SettingOption.ShareMenu, SettingOption.ShareOpens)
        SettingsPart.SessionRows -> listOf(SettingOption.RunningInList)
        SettingsPart.SwipeChats -> listOf(SettingOption.SwipeChats)
        SettingsPart.SplitView -> listOf(SettingOption.SplitView, SettingOption.ListWidth, SettingOption.ListHandle, SettingOption.BackShowsList)
        SettingsPart.KeyBar -> listOf(SettingOption.TerminalKeys, SettingOption.TerminalKeyboard)
        SettingsPart.TerminalKeyboard -> listOf(SettingOption.TerminalKeyboard)
        SettingsPart.TerminalFullscreen -> listOf(SettingOption.TerminalFullscreen)
        SettingsPart.NotificationShade -> listOf(SettingOption.Notifications)
        SettingsPart.PermissionNotification -> listOf(SettingOption.NotifyPermissions)
        SettingsPart.QuestionNotification -> listOf(SettingOption.NotifyQuestions)
        SettingsPart.FinishedNotification -> listOf(SettingOption.NotifyFinished)
        SettingsPart.UpdateNotice -> listOf(SettingOption.UpdateNotice)
    }

/** The microphone and what turns the voice into text, in the order Settings lists them. */
val VOICE_OPTIONS: List<SettingOption> = listOf(
    SettingOption.Dictation,
    SettingOption.VoiceMode,
    SettingOption.VoiceProvider,
    SettingOption.VoiceModel,
    SettingOption.VoiceKey,
    SettingOption.VoiceLanguage,
)

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
    SettingOption.VoiceMode -> settings.dictation
    SettingOption.VoiceProvider, SettingOption.VoiceModel, SettingOption.VoiceKey, SettingOption.VoiceLanguage ->
        settings.dictation && settings.voiceEngine == VoiceEngine.Cloud
    SettingOption.ShareMenu -> supported
    SettingOption.ShareOpens -> supported && settings.showInShareMenu
    else -> true
}

/** Whether the option exists on this platform: nothing can be shared to the iOS app yet. */
val SettingOption.supported: Boolean
    get() = shareTargetSupported || (this != SettingOption.ShareMenu && this != SettingOption.ShareOpens)

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

/**
 * The parts the real screens mark for the live demo's edit mode (with `Modifier.editable`), and
 * so the options it reaches. The others have no element of their own on the real screens: the
 * theme and swiping concern whole screens, running steps and notifications only exist while an
 * agent works or the app is in the background. Their options stay in the list and the previews.
 */
val EDIT_MODE_PARTS: List<SettingsPart> = listOf(
    SettingsPart.Header,
    SettingsPart.RunningLine,
    SettingsPart.Messages,
    SettingsPart.Thoughts,
    SettingsPart.Tools,
    SettingsPart.Activity,
    SettingsPart.Footer,
    SettingsPart.PendingCards,
    SettingsPart.ComposerBox,
    SettingsPart.VoiceInput,
    SettingsPart.SelectionTiming,
    SettingsPart.SessionRows,
    SettingsPart.NewChat,
    SettingsPart.SplitView,
    SettingsPart.UpdateNotice,
    SettingsPart.KeyBar,
    SettingsPart.TerminalFullscreen,
)
