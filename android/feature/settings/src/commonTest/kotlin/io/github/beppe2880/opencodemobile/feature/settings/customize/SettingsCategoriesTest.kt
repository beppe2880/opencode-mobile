package io.github.beppe2880.opencodemobile.feature.settings.customize

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.ChatSwipe
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.SttProvider
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.core.domain.UnfoldedLayout
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.ui.platform.ScreenCapabilities
import io.github.beppe2880.opencodemobile.core.ui.text.UiText
import io.github.beppe2880.opencodemobile.core.ui.theme.OpenCodeThemes
import io.github.beppe2880.opencodemobile.feature.settings.SettingsState
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_demo_server
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_list_width
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_no_key
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_off
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_split
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_swipe
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_swipe_drawer
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_default
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_dark
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unfolded_layout_panel
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_mode_cloud
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_mode_system
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsCategoriesTest {

    @Test
    fun everyOptionIsInExactlyOneCategory() {
        val listed = SettingsArea.entries.flatMap { it.options }
        assertEquals(SettingOption.entries.toSet(), listed.toSet())
        assertEquals(listed.size, listed.toSet().size, "an option is listed twice: ${listed.groupBy { it }.filterValues { it.size > 1 }.keys}")
    }

    /** Both styles open an option in the same category: the Preview style at a part of it. */
    @Test
    fun thePreviewOpensEveryOptionInItsOwnCategory() {
        SettingOption.entries.forEach { option ->
            assertEquals(option.area, option.part.area, "$option")
            assertTrue(option in option.part.options, "$option")
        }
    }

    @Test
    fun theCategoriesTheIssueAskedFor() {
        assertEquals(listOf(SettingOption.Theme, SettingOption.OpenCodeTheme, SettingOption.TextSize), SettingsArea.Appearance.options)
        // The list beside the chat moved from Appearance to its own category, in three groups.
        assertEquals(LARGE_SCREEN_OPTIONS, SettingsArea.LargeScreens.options)
        assertEquals(
            listOf(
                listOf(SettingOption.SplitView, SettingOption.UnfoldedLayout, SettingOption.ListWidth),
                listOf(SettingOption.UnfoldOpenChat, SettingOption.FoldOpenChat),
                listOf(SettingOption.ListHandle, SettingOption.BackShowsList, SettingOption.ListSwipe),
            ),
            SettingsArea.LargeScreens.groups.map { it.options },
        )
        assertEquals(SettingsArea.Chat, SettingOption.SwipeChats.area)
        assertEquals(SettingsArea.Chat, SettingOption.QuestionsCollapsed.area)
        // Right after the tool row options of the transcript group.
        val transcript = SettingOption.ToolMediaSize.group.options
        assertEquals(SettingsArea.Chat, SettingOption.ToolMediaSize.area)
        assertEquals(transcript.indexOf(SettingOption.FullCommands) + 1, transcript.indexOf(SettingOption.ToolMediaSize))
        assertEquals(0, SettingOption.ToolMediaSize.depthIn(SettingOption.ToolMediaSize.group))
        assertEquals(SettingsArea.LiveActivity, SettingOption.OutputLines.area)
        assertEquals(
            listOf(SettingOption.AgentOutline, SettingOption.SelectionApply, SettingOption.CloseModelPickerOnSelect, SettingOption.NewChatModel),
            SettingsArea.Composer.options,
        )
        assertEquals(VOICE_OPTIONS, SettingsArea.VoiceInput.options)
        assertEquals(listOf(SettingOption.RunningInList, SettingOption.ConfirmStop), SettingsArea.Sessions.options)
        assertEquals(listOf(SettingOption.UpdateNotice), SettingsArea.Updates.options)
        assertTrue(SettingsArea.Servers.groups.isEmpty() && SettingsArea.About.groups.isEmpty())
    }

    @Test
    fun dependentOptionsFollowTheirParentAndNestUnderIt() {
        SettingsArea.entries.flatMap { it.groups }.forEach { group ->
            group.options.forEach { option ->
                val parent = option.parent ?: return@forEach
                if (parent in group.options) assertTrue(group.options.indexOf(parent) < group.options.indexOf(option), "$option before $parent")
            }
        }
        assertEquals(2, SettingOption.LiveInterval.depthIn(SettingOption.LiveInterval.group))
        assertEquals(1, SettingOption.UnfoldedLayout.depthIn(SettingOption.UnfoldedLayout.group))
        assertEquals(2, SettingOption.ListWidth.depthIn(SettingOption.ListWidth.group))
        assertEquals(0, SettingOption.UnfoldOpenChat.depthIn(SettingOption.UnfoldOpenChat.group))
        assertEquals(0, SettingOption.ListHandle.depthIn(SettingOption.ListHandle.group))
        assertEquals(1, SettingOption.BackShowsList.depthIn(SettingOption.BackShowsList.group))
        assertEquals(1, SettingOption.HeaderStyle.depthIn(SettingOption.HeaderStyle.group))
        // The cloud provider has a group of its own under the engine choice.
        assertEquals(0, SettingOption.VoiceProvider.depthIn(SettingOption.VoiceProvider.group))
        assertEquals(0, SettingOption.FollowRunning.depthIn(SettingOption.FollowRunning.group))
    }

    @Test
    fun pagesShowOnlyWhatApplies() {
        val phone = visibleGroups(SettingsArea.VoiceInput, AppSettings(), notificationsOn = true, ScreenCapabilities.Phone)
        assertEquals(listOf(listOf(SettingOption.Dictation, SettingOption.VoiceMode)), phone.map { it.options })
        val cloud = visibleGroups(SettingsArea.VoiceInput, AppSettings(voiceEngine = VoiceEngine.Cloud), notificationsOn = true, ScreenCapabilities.Phone)
        assertEquals(VOICE_OPTIONS, cloud.flatMap { it.options })
        val off = visibleGroups(
            SettingsArea.VoiceInput,
            AppSettings(dictation = false, voiceEngine = VoiceEngine.Cloud),
            notificationsOn = true,
            ScreenCapabilities.Phone,
        )
        assertEquals(listOf(SettingOption.Dictation), off.flatMap { it.options })

        // Every large screen option out of the box on a foldable, folded or not: they can be set before unfolding.
        assertEquals(LARGE_SCREEN_OPTIONS, largeScreenOptions(AppSettings(), ScreenCapabilities.Foldable))
        // Without the list beside the chat only its switch is left.
        assertEquals(listOf(SettingOption.SplitView), largeScreenOptions(AppSettings(splitView = false), ScreenCapabilities.Foldable))
        // The chat list panel keeps only the layout choice and the swipe.
        assertEquals(
            listOf(SettingOption.SplitView, SettingOption.UnfoldedLayout, SettingOption.ListSwipe),
            largeScreenOptions(AppSettings(unfoldedLayout = UnfoldedLayout.ListPanel), ScreenCapabilities.Foldable),
        )

        val folder = visibleGroups(SettingsArea.Chat, AppSettings(chatHeaderLine = ChatHeaderLine.Folder), true, ScreenCapabilities.Phone)
            .flatMap { it.options }
        assertTrue(SettingOption.HeaderLine in folder && SettingOption.HeaderStyle !in folder)
        assertTrue(visibleGroups(SettingsArea.Servers, AppSettings(), true, ScreenCapabilities.Foldable).isEmpty())
    }

    @Test
    fun foldingAndUnfoldingOnlyOnFoldables() {
        val foldable = visibleGroups(SettingsArea.LargeScreens, AppSettings(), true, ScreenCapabilities.Foldable)
        assertEquals(3, foldable.size)
        assertTrue(FOLDING_OPTIONS.all { it.availableOn(ScreenCapabilities.Foldable) })
        // A tablet keeps the layout and the divider, without the group about folding.
        val tablet = visibleGroups(SettingsArea.LargeScreens, AppSettings(), true, ScreenCapabilities.Tablet)
        assertEquals(
            listOf(
                listOf(SettingOption.SplitView, SettingOption.UnfoldedLayout, SettingOption.ListWidth),
                listOf(SettingOption.ListHandle, SettingOption.BackShowsList, SettingOption.ListSwipe),
            ),
            tablet.map { it.options },
        )
        assertEquals(foldable.map { it.title } - SettingOption.UnfoldOpenChat.group.title, tablet.map { it.title })
        // A phone has none of them.
        assertTrue(largeScreenOptions(AppSettings(), ScreenCapabilities.Phone).isEmpty())
        assertTrue(LARGE_SCREEN_OPTIONS.none { it.availableOn(ScreenCapabilities.Phone) })
        // The other options do not depend on the screen.
        assertTrue(SettingOption.entries.filter { it !in LARGE_SCREEN_OPTIONS }.all { it.availableOn(ScreenCapabilities.Phone) == it.supported })
    }

    private fun largeScreenOptions(settings: AppSettings, screen: ScreenCapabilities) =
        visibleGroups(SettingsArea.LargeScreens, settings, true, screen).flatMap { it.options }

    @Test
    fun aHiddenOptionIsPointedAtThroughTheOptionThatBringsItBack() {
        val defaults = AppSettings()
        assertEquals(SettingOption.VoiceMode, SettingOption.VoiceKey.anchor(defaults, true))
        assertEquals(SettingOption.Dictation, SettingOption.VoiceKey.anchor(AppSettings(dictation = false), true))
        assertEquals(SettingOption.VoiceKey, SettingOption.VoiceKey.anchor(AppSettings(voiceEngine = VoiceEngine.Cloud), true))
        assertEquals(SettingOption.HeaderLine, SettingOption.HeaderOpens.anchor(AppSettings(chatHeaderLine = ChatHeaderLine.Folder), true))
        assertEquals(SettingOption.FollowRunning, SettingOption.LiveInterval.anchor(AppSettings(followRunningTools = false), true))
        assertEquals(SettingOption.Notifications, SettingOption.NotifyFinished.anchor(defaults, notificationsOn = false))
        // The width of the list beside the chat comes back by choosing it over the panel.
        assertEquals(SettingOption.UnfoldedLayout, SettingOption.ListWidth.anchor(AppSettings(unfoldedLayout = UnfoldedLayout.ListPanel), true))
        assertEquals(SettingOption.SplitView, SettingOption.FoldOpenChat.anchor(AppSettings(splitView = false), true))
    }

    @Test
    fun platformsListOnlyTheirCategories() {
        assertEquals(SettingsArea.entries, visibleAreas(notifications = true, ScreenCapabilities.Foldable, sharing = true))
        val ios = visibleAreas(notifications = false, ScreenCapabilities.Phone, sharing = false)
        assertFalse(SettingsArea.Sharing in ios || SettingsArea.Notifications in ios || SettingsArea.LargeScreens in ios)
        assertEquals(SettingsArea.Servers, ios.first())
        assertEquals(SettingsArea.About, ios.last())
    }

    @Test
    fun largeScreensOnlyWhereTheAppCanGetAWideWindow() {
        assertFalse(SettingsArea.LargeScreens in visibleAreas(notifications = true, ScreenCapabilities.Phone, sharing = true))
        assertTrue(SettingsArea.LargeScreens in visibleAreas(notifications = true, ScreenCapabilities.Tablet, sharing = true))
        // A foldable lists it while folded too: the options can be set before unfolding.
        assertTrue(SettingsArea.LargeScreens in visibleAreas(notifications = true, ScreenCapabilities.Foldable, sharing = true))
        // An iPad.
        assertTrue(SettingsArea.LargeScreens in visibleAreas(notifications = false, ScreenCapabilities.Tablet, sharing = false))
    }

    @Test
    fun voiceSummaryNamesTheEngineProviderAndModel() {
        assertEquals(listOf(UiText.Resource(Res.string.settings_summary_off)), summaryOf(SettingsArea.VoiceInput, AppSettings(dictation = false)))
        assertEquals(listOf(UiText.Resource(Res.string.settings_voice_mode_system)), summaryOf(SettingsArea.VoiceInput, AppSettings()))
        val cloud = AppSettings(voiceEngine = VoiceEngine.Cloud, sttProvider = SttProvider.OpenRouter)
        assertEquals(
            listOf(
                UiText.Resource(Res.string.settings_voice_mode_cloud),
                UiText.Raw("OpenRouter"),
                UiText.Raw("openai/whisper-large-v3-turbo"),
                UiText.Resource(Res.string.settings_summary_no_key),
            ),
            summaryOf(SettingsArea.VoiceInput, cloud),
        )
        // With a key saved there is nothing missing to say.
        assertEquals(3, summary(SettingsArea.VoiceInput, SettingsState(settings = cloud, sttKeys = setOf(SttProvider.OpenRouter))).size)
    }

    @Test
    fun otherSummariesShowCurrentValues() {
        assertEquals(
            listOf(UiText.Resource(Res.string.settings_theme_dark), UiText.Resource(Res.string.settings_text_default)),
            summaryOf(SettingsArea.Appearance, AppSettings(theme = ThemeMode.Dark)),
        )
        // The OpenCode theme by its name, after the light or dark mode; none says nothing.
        val theme = OpenCodeThemes.all.first()
        assertEquals(
            listOf(UiText.Resource(Res.string.settings_theme_dark), UiText.Raw(theme.displayName), UiText.Resource(Res.string.settings_text_default)),
            summaryOf(SettingsArea.Appearance, AppSettings(theme = ThemeMode.Dark, openCodeTheme = theme.id)),
        )
        assertEquals(2, summaryOf(SettingsArea.Appearance, AppSettings(openCodeTheme = "retired")).size)
        val off = listOf(UiText.Resource(Res.string.settings_summary_off))
        assertEquals(off, summaryOf(SettingsArea.LiveActivity, AppSettings(followRunningTools = false)))
        val everything = AppSettings(liveThinking = true, liveShell = true, liveEdits = true, liveReads = true, liveOther = true)
        assertEquals(5, summaryOf(SettingsArea.LiveActivity, everything).size)
        assertEquals(off, summaryOf(SettingsArea.Sharing, AppSettings(showInShareMenu = false)))
        // Nothing on: the row falls back to what the category covers.
        assertTrue(summaryOf(SettingsArea.Sessions, AppSettings(showRunningWorkInList = false, confirmStopCommand = false)).isEmpty())

        val servers = listOf(server("a", "Laptop"), server("b", "Desk"))
        val active = summary(SettingsArea.Servers, SettingsState(servers = servers, activeServerId = "b"))
        assertEquals(listOf(UiText.Raw("Desk"), UiText.Raw("Laptop")), active)
        val demo = summary(SettingsArea.Servers, SettingsState(servers = servers), demo = true)
        assertEquals(listOf(UiText.Resource(Res.string.settings_demo_server)), demo)
    }

    @Test
    fun chatSummaryNamesTheSwipeChoice() {
        val swipe = UiText.Resource(Res.string.settings_summary_swipe)
        val drawer = UiText.Resource(Res.string.settings_summary_swipe_drawer)
        val switching = summaryOf(SettingsArea.Chat, AppSettings(chatSwipe = ChatSwipe.SwitchChats))
        assertTrue(swipe in switching && drawer !in switching)
        val list = summaryOf(SettingsArea.Chat, AppSettings())
        assertTrue(drawer in list && swipe !in list)
        val off = summaryOf(SettingsArea.Chat, AppSettings(chatSwipe = ChatSwipe.Off))
        assertTrue(swipe !in off && drawer !in off)
    }

    @Test
    fun largeScreensSummaryNamesTheLayoutAndTheListWidth() {
        assertEquals(
            listOf(UiText.Resource(Res.string.settings_summary_split), UiText.Resource(Res.string.settings_summary_list_width, listOf(40))),
            summaryOf(SettingsArea.LargeScreens, AppSettings()),
        )
        assertEquals(
            UiText.Resource(Res.string.settings_summary_list_width, listOf(35)),
            summaryOf(SettingsArea.LargeScreens, AppSettings(listPaneFraction = 0.35f)).last(),
        )
        assertEquals(
            listOf(UiText.Resource(Res.string.settings_unfolded_layout_panel)),
            summaryOf(SettingsArea.LargeScreens, AppSettings(unfoldedLayout = UnfoldedLayout.ListPanel)),
        )
        assertEquals(listOf(UiText.Resource(Res.string.settings_summary_off)), summaryOf(SettingsArea.LargeScreens, AppSettings(splitView = false)))
    }

    private fun summaryOf(area: SettingsArea, settings: AppSettings) = summary(area, SettingsState(settings = settings))

    private fun server(id: String, name: String) =
        ServerProfile(id = id, name = name, baseUrl = "http://localhost:4096", secret = "", credential = CredentialKind.Password, addedAt = 0)
}
