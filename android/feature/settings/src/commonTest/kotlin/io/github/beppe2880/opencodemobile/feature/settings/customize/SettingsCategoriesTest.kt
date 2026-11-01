package io.github.beppe2880.opencodemobile.feature.settings.customize

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.SttProvider
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.ui.text.UiText
import io.github.beppe2880.opencodemobile.feature.settings.SettingsState
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_demo_server
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_no_key
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_off
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_summary_split
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_text_default
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_dark
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
        assertEquals(listOf(SettingOption.Theme, SettingOption.TextSize, SettingOption.SplitView), SettingsArea.Appearance.options.take(3))
        assertEquals(SettingsArea.Chat, SettingOption.SwipeChats.area)
        assertEquals(SettingsArea.Chat, SettingOption.QuestionsCollapsed.area)
        assertEquals(SettingsArea.LiveActivity, SettingOption.OutputLines.area)
        assertEquals(listOf(SettingOption.AgentOutline, SettingOption.SelectionApply, SettingOption.NewChatModel), SettingsArea.Composer.options)
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
        assertEquals(1, SettingOption.ListWidth.depthIn(SettingOption.ListWidth.group))
        assertEquals(2, SettingOption.BackShowsList.depthIn(SettingOption.BackShowsList.group))
        assertEquals(1, SettingOption.HeaderStyle.depthIn(SettingOption.HeaderStyle.group))
        // The cloud provider has a group of its own under the engine choice.
        assertEquals(0, SettingOption.VoiceProvider.depthIn(SettingOption.VoiceProvider.group))
        assertEquals(0, SettingOption.FollowRunning.depthIn(SettingOption.FollowRunning.group))
    }

    @Test
    fun pagesShowOnlyWhatApplies() {
        val phone = visibleGroups(SettingsArea.VoiceInput, AppSettings(), notificationsOn = true, wide = false)
        assertEquals(listOf(listOf(SettingOption.Dictation, SettingOption.VoiceMode)), phone.map { it.options })
        val cloud = visibleGroups(SettingsArea.VoiceInput, AppSettings(voiceEngine = VoiceEngine.Cloud), notificationsOn = true, wide = false)
        assertEquals(VOICE_OPTIONS, cloud.flatMap { it.options })
        val off = visibleGroups(SettingsArea.VoiceInput, AppSettings(dictation = false, voiceEngine = VoiceEngine.Cloud), notificationsOn = true, wide = false)
        assertEquals(listOf(SettingOption.Dictation), off.flatMap { it.options })

        // The list beside the chat only on wide windows.
        val narrow = visibleGroups(SettingsArea.Appearance, AppSettings(), true, wide = false)
        assertEquals(listOf(SettingOption.Theme, SettingOption.TextSize), narrow.flatMap { it.options })
        assertTrue(SettingOption.BackShowsList in visibleGroups(SettingsArea.Appearance, AppSettings(), true, wide = true).flatMap { it.options })
        assertFalse(SettingOption.ListWidth in visibleGroups(SettingsArea.Appearance, AppSettings(splitView = false), true, wide = true).flatMap { it.options })

        val folder = visibleGroups(SettingsArea.Chat, AppSettings(chatHeaderLine = ChatHeaderLine.Folder), true, wide = false).flatMap { it.options }
        assertTrue(SettingOption.HeaderLine in folder && SettingOption.HeaderStyle !in folder)
        assertTrue(visibleGroups(SettingsArea.Servers, AppSettings(), true, wide = true).isEmpty())
    }

    @Test
    fun aHiddenOptionIsPointedAtThroughTheOptionThatBringsItBack() {
        val defaults = AppSettings()
        assertEquals(SettingOption.VoiceMode, SettingOption.VoiceKey.anchor(defaults, true))
        assertEquals(SettingOption.Dictation, SettingOption.VoiceKey.anchor(AppSettings(dictation = false), true))
        assertEquals(SettingOption.VoiceKey, SettingOption.VoiceKey.anchor(AppSettings(voiceEngine = VoiceEngine.Cloud), true))
        assertEquals(SettingOption.HeaderLine, SettingOption.HeaderOpens.anchor(AppSettings(chatHeaderLine = ChatHeaderLine.Folder), true))
        assertEquals(SettingOption.FollowRunning, SettingOption.LiveInterval.anchor(AppSettings(followRunningTools = false), true))
        assertEquals(SettingOption.Notifications, SettingOption.NotifyFinished.anchor(defaults, notificationsOn = false))
    }

    @Test
    fun platformsListOnlyTheirCategories() {
        assertEquals(SettingsArea.entries, visibleAreas(notifications = true, sharing = true))
        val ios = visibleAreas(notifications = false, sharing = false)
        assertFalse(SettingsArea.Sharing in ios || SettingsArea.Notifications in ios)
        assertEquals(SettingsArea.Servers, ios.first())
        assertEquals(SettingsArea.About, ios.last())
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
        assertEquals(3, summary(SettingsArea.VoiceInput, SettingsState(settings = cloud, sttKeys = setOf(SttProvider.OpenRouter)), wide = false).size)
    }

    @Test
    fun otherSummariesShowCurrentValues() {
        assertEquals(
            listOf(UiText.Resource(Res.string.settings_theme_dark), UiText.Resource(Res.string.settings_text_default)),
            summaryOf(SettingsArea.Appearance, AppSettings(theme = ThemeMode.Dark)),
        )
        val wide = summary(SettingsArea.Appearance, SettingsState(), wide = true)
        assertEquals(UiText.Resource(Res.string.settings_summary_split), wide.last())
        val off = listOf(UiText.Resource(Res.string.settings_summary_off))
        assertEquals(off, summaryOf(SettingsArea.LiveActivity, AppSettings(followRunningTools = false)))
        val everything = AppSettings(liveThinking = true, liveShell = true, liveEdits = true, liveReads = true, liveOther = true)
        assertEquals(5, summaryOf(SettingsArea.LiveActivity, everything).size)
        assertEquals(off, summaryOf(SettingsArea.Sharing, AppSettings(showInShareMenu = false)))
        // Nothing on: the row falls back to what the category covers.
        assertTrue(summaryOf(SettingsArea.Sessions, AppSettings(showRunningWorkInList = false, confirmStopCommand = false)).isEmpty())

        val servers = listOf(server("a", "Laptop"), server("b", "Desk"))
        val active = summary(SettingsArea.Servers, SettingsState(servers = servers, activeServerId = "b"), wide = false)
        assertEquals(listOf(UiText.Raw("Desk"), UiText.Raw("Laptop")), active)
        val demo = summary(SettingsArea.Servers, SettingsState(servers = servers), wide = false, demo = true)
        assertEquals(listOf(UiText.Resource(Res.string.settings_demo_server)), demo)
    }

    private fun summaryOf(area: SettingsArea, settings: AppSettings) = summary(area, SettingsState(settings = settings), wide = false)

    private fun server(id: String, name: String) =
        ServerProfile(id = id, name = name, baseUrl = "http://localhost:4096", secret = "", credential = CredentialKind.Password, addedAt = 0)
}
