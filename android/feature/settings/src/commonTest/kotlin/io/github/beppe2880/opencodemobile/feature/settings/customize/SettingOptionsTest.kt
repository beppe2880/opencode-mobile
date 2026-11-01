package io.github.beppe2880.opencodemobile.feature.settings.customize

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.ui.platform.shareTargetSupported
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingOptionsTest {

    @Test
    fun everyAreaWithOptionsHasPartsAndEveryPartHasOptions() {
        // Servers and about have pages of their own but no options, so no preview.
        SettingsArea.entries.filter { it.options.isNotEmpty() }.forEach { assertTrue(SettingsPart.of(it).isNotEmpty(), "$it has no parts") }
        assertTrue(SettingsPart.of(SettingsArea.Servers).isEmpty() && SettingsPart.of(SettingsArea.About).isEmpty())
        SettingsPart.entries.forEach { assertTrue(it.options.isNotEmpty(), "$it has no options") }
    }

    @Test
    fun everyOptionIsInAPartAndOpensItsFirstOne() {
        assertEquals(SettingOption.entries.toSet(), SettingsPart.entries.flatMap { it.options }.toSet())
        assertEquals(SettingsPart.TextSize, SettingOption.TextSize.part)
        assertEquals(SettingsArea.Appearance, SettingOption.TextSize.area)
        assertEquals(SettingsPart.Thoughts, SettingOption.GroupActivity.part)
        assertEquals(SettingsArea.LiveActivity, SettingOption.LiveInterval.area)
        assertEquals(SettingsArea.Appearance, SettingOption.BackShowsList.area)
        assertEquals(SettingsPart.SplitView, SettingOption.BackShowsList.part)
        assertEquals(SettingsArea.Terminal, SettingOption.TerminalKeys.area)
    }

    @Test
    fun eachPartOffersOnlyItsOwnOptions() {
        assertEquals(listOf(SettingOption.HeaderLine, SettingOption.HeaderStyle, SettingOption.HeaderOpens), SettingsPart.Header.options)
        assertEquals(listOf(SettingOption.PermissionsCollapsed, SettingOption.QuestionsCollapsed), SettingsPart.PendingCards.options)
        assertEquals(listOf(SettingOption.AgentOutline, SettingOption.Dictation), SettingsPart.ComposerBox.options)
        assertEquals(SettingsPart.VoiceInput, SettingOption.Dictation.part)
        assertEquals(SettingsPart.VoiceInput, SettingOption.VoiceKey.part)
        assertEquals(listOf(SettingOption.LiveShell, SettingOption.LiveInterval, SettingOption.OutputLines), SettingsPart.LiveShell.options)
        assertEquals(listOf(SettingOption.UpdateNotice), SettingsPart.UpdateNotice.options)
        assertTrue(SettingsPart.Activity.options.containsAll(SettingsPart.Thoughts.options + SettingsPart.Tools.options))
    }

    @Test
    fun optionsApplyOnlyWhenTheirSwitchIsOn() {
        val defaults = AppSettings()
        // Out of the box every option of this platform has an effect, but the cloud voice ones (phone dictation is the default).
        val cloudVoice = VOICE_OPTIONS - SettingOption.Dictation - SettingOption.VoiceMode
        assertTrue(SettingOption.entries.filter { it.supported && it !in cloudVoice }.all { it.appliesTo(defaults) })
        assertTrue(cloudVoice.none { it.appliesTo(defaults) })
        assertTrue(cloudVoice.all { it.appliesTo(AppSettings(voiceEngine = VoiceEngine.Cloud)) })
        assertFalse(SettingOption.VoiceMode.appliesTo(AppSettings(dictation = false)))
        assertFalse(SettingOption.VoiceKey.appliesTo(AppSettings(dictation = false, voiceEngine = VoiceEngine.Cloud)))
        assertFalse(SettingOption.ShareOpens.appliesTo(AppSettings(showInShareMenu = false)))
        assertFalse(SettingOption.HeaderStyle.appliesTo(AppSettings(chatHeaderLine = ChatHeaderLine.Folder)))
        assertTrue(SettingOption.HeaderStyle.appliesTo(defaults))
        assertFalse(SettingOption.LiveThinking.appliesTo(AppSettings(followRunningTools = false)))
        assertFalse(SettingOption.LiveInterval.appliesTo(AppSettings(liveShell = false)))
        assertFalse(SettingOption.BackShowsList.appliesTo(AppSettings(listPaneHandle = false)))
        assertFalse(SettingOption.NotifyQuestions.appliesTo(defaults, notificationsOn = false))
        assertTrue(SettingOption.NotifyQuestions.appliesTo(defaults, notificationsOn = true))
    }

    @Test
    fun sharingHasItsOwnCategoryIsReachedFromItsSheetAndOnlyWhereAppsCanShare() {
        assertEquals(SettingsArea.Sharing, SettingOption.ShareMenu.area)
        assertEquals(SettingsPart.ShareTarget, SettingOption.ShareMenu.part)
        assertTrue(SettingOption.ShareOpens in SettingsPart.NewChat.options)
        assertEquals(SettingOption.ShareMenu, SettingOption.ShareOpens.requires)
        assertEquals(shareTargetSupported, SettingOption.ShareMenu.supported)
        assertEquals(shareTargetSupported, SettingOption.ShareMenu.appliesTo(AppSettings()))
        assertTrue(SettingOption.NewChatModel.supported)
    }

    @Test
    fun dependentOptionsNameTheSwitchThatTurnsThemOn() {
        SettingOption.entries.filter { it.requires != null }.forEach { option ->
            val parent = option.requires!!
            assertTrue(SettingOption.entries.indexOf(parent) < SettingOption.entries.indexOf(option), "$option before $parent")
        }
        assertEquals(SettingOption.LiveShell, SettingOption.LiveInterval.requires)
        assertEquals(SettingOption.FollowRunning, SettingOption.LiveShell.requires)
    }

    @Test
    fun groupingDecidesWhichThinkingAndToolRowsThePreviewShows() {
        val grouped = AppSettings()
        assertTrue(SettingsPart.Activity.shownWith(grouped))
        assertFalse(SettingsPart.Thoughts.shownWith(grouped))
        assertFalse(SettingsPart.Tools.shownWith(grouped))
        val apart = AppSettings(groupActivity = false)
        assertTrue(SettingsPart.Thoughts.shownWith(apart) && SettingsPart.Tools.shownWith(apart))
        assertFalse(SettingsPart.Thoughts.shownWith(apart.copy(showReasoning = false)))
        assertTrue(SettingsPart.entries.filterNot { it in setOf(SettingsPart.Thoughts, SettingsPart.Tools, SettingsPart.Activity) }.all { it.shownWith(apart) })
    }
}
