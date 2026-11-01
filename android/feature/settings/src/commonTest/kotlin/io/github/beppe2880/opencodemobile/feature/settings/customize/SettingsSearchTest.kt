package io.github.beppe2880.opencodemobile.feature.settings.customize

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

    private fun search(query: String) = searchOptions(query, { titles[it] ?: it.name }, { descriptions[it].orEmpty() })

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
    fun descriptionsFindOptionsByTheirAreaAndPart() {
        assertEquals(listOf(SettingOption.ShowCost), search("footer"))
    }
}
