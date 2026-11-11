package io.github.beppe2880.opencodemobile.feature.settings.customize

import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.ui.platform.ScreenCapabilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsSearchTest {

    private val titles = mapOf(
        SettingOption.ShowCost to "Show cost",
        SettingOption.Theme to "Theme",
        SettingOption.LiveShell to "Running commands",
        SettingOption.TerminalKeyboard to "Open the keyboard",
    )
    private val descriptions = mapOf(
        SettingOption.ShowCost to "Cost of each turn · Answer footer · Chat",
        SettingOption.LiveShell to "Show their output as it arrives · Live activity",
        SettingOption.TerminalKeyboard to "Shows the keyboard with the terminal · Terminal",
    )

    private fun search(query: String) = searchOptions(query, { titles[it] ?: it.name }, {
        descriptions[it].orEmpty() + " " + it.searchKeywords.joinToString(" ")
    })

    @Test
    fun blankQueriesFindNothing() {
        assertTrue(search("").isEmpty())
        assertTrue(search("   ").isEmpty())
    }

    @Test
    fun titlesMatchIgnoringCaseAndComeFirst() {
        assertEquals(SettingOption.ShowCost, search("COST").first())
        // "keyboard" is in a title and in a description: the title match leads.
        assertEquals(listOf(SettingOption.TerminalKeyboard), search("keyboard").take(1))
    }

    @Test
    fun everyWordMustMatchSomewhere() {
        assertEquals(listOf(SettingOption.LiveShell), search("output live"))
        assertTrue(search("output terminal").isEmpty())
    }

    @Test
    fun categoriesAreFoundByNameOrDescription() {
        val names = mapOf(SettingsArea.Servers to "Servers", SettingsArea.About to "About and reset", SettingsArea.VoiceInput to "Voice input")
        val descriptions = mapOf(SettingsArea.Servers to "Saved servers, pairing and usage statistics", SettingsArea.About to "Versions, reset all settings")
        fun areas(query: String) = searchAreas(query, names.keys.toList(), { names.getValue(it) }, { descriptions[it].orEmpty() })
        assertEquals(listOf(SettingsArea.Servers), areas("usage"))
        assertEquals(listOf(SettingsArea.About), areas("RESET"))
        assertEquals(listOf(SettingsArea.VoiceInput), areas("voice"))
        assertTrue(areas(" ").isEmpty())
        assertTrue(areas("voice usage").isEmpty())
    }

    @Test
    fun descriptionsFindOptionsByTheirAreaAndPart() {
        assertEquals(listOf(SettingOption.ShowCost), search("footer"))
    }

    @Test
    fun foldablesFindTheLargeScreenOptions() {
        assertEquals(
            listOf(SettingOption.SplitView, SettingOption.UnfoldedLayout, SettingOption.UnfoldOpenChat, SettingOption.FoldOpenChat),
            search("foldable"),
        )
        // Both swipes, the chat's and the one of the list beside it.
        assertEquals(listOf(SettingOption.SwipeChats, SettingOption.ListSwipe), search("swipe sidebar"))
    }

    @Test
    fun onlyDevicesWithTheOptionsFindThem() {
        fun findable(query: String, screen: ScreenCapabilities): List<SettingOption> {
            val areas = visibleAreas(notifications = true, screen, sharing = true)
            return search(query).filter { it.findableIn(areas, screen) }
        }
        assertEquals(search("foldable"), findable("foldable", ScreenCapabilities.Foldable))
        // A tablet does not fold.
        assertEquals(listOf(SettingOption.SplitView, SettingOption.UnfoldedLayout), findable("foldable", ScreenCapabilities.Tablet))
        // A phone has no wide window: only the chat's own swipe is left.
        assertTrue(findable("foldable", ScreenCapabilities.Phone).isEmpty())
        assertEquals(listOf(SettingOption.SwipeChats), findable("swipe sidebar", ScreenCapabilities.Phone))
    }

    @Test
    fun reasoningFindsTheModelPickerPreference() {
        assertEquals(listOf(SettingOption.CloseModelPickerOnSelect), search("model reasoning"))
    }
}
