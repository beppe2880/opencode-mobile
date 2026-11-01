package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.ui.Modifier
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.ui.components.EditMode
import io.github.beppe2880.opencodemobile.core.ui.components.editable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EditModeTest {

    @Test
    fun outsideTheLiveDemoEditableChangesNothing() {
        val modifier = Modifier
        assertSame(modifier, modifier.editable(SettingsPart.Header, null))
        // Use mode in the live demo: the screens work as usual too.
        assertSame(modifier, modifier.editable(SettingsPart.SessionRows, EditMode(active = false)))
    }

    @Test
    fun inEditModeTheElementIsMarked() {
        assertNotSame<Modifier>(Modifier, Modifier.editable(SettingsPart.ComposerBox, EditMode(active = true)))
    }

    @Test
    fun everyMarkedPartOpensOptions() {
        assertTrue(EDIT_MODE_PARTS.isNotEmpty())
        EDIT_MODE_PARTS.forEach { assertTrue(it.options.isNotEmpty(), "$it has no options") }
        assertEquals(EDIT_MODE_PARTS.size, EDIT_MODE_PARTS.toSet().size)
    }

    @Test
    fun theElementsTheOwnerNamedOpenTheirOptions() {
        assertTrue(SettingOption.SelectionApply in SettingsPart.SelectionTiming.options, "agent chip")
        assertTrue(SettingOption.HeaderStyle in SettingsPart.Header.options, "header line")
        assertTrue(SettingOption.GroupThoughts in SettingsPart.Thoughts.options, "thoughts row")
        assertTrue(SettingOption.FullCommands in SettingsPart.Tools.options, "tool row")
        assertTrue(SettingOption.QuestionsCollapsed in SettingsPart.PendingCards.options, "question card")
        assertTrue(SettingOption.RunningInList in SettingsPart.SessionRows.options, "session row")
        assertTrue(SettingOption.TerminalKeys in SettingsPart.KeyBar.options, "key bar")
    }

    /** A new option fails here until it is reachable in edit mode or knowingly left to the list and the previews. */
    @Test
    fun everyOptionWithAnElementOnTheRealScreensIsReachableInEditMode() {
        val reachable = EDIT_MODE_PARTS.flatMap { it.options }.toSet()
        val withoutElement = setOf(
            // The whole app, not one element.
            SettingOption.Theme,
            SettingOption.SwipeChats,
            // Only while an agent works.
            SettingOption.FollowRunning,
            SettingOption.KeepLiveOpen,
            SettingOption.LiveThinking,
            SettingOption.LiveShell,
            SettingOption.LiveInterval,
            SettingOption.LiveEdits,
            SettingOption.LiveReads,
            SettingOption.LiveOther,
            // Only outside the app.
            SettingOption.Notifications,
            SettingOption.NotifyPermissions,
            SettingOption.NotifyQuestions,
            SettingOption.NotifyFinished,
        )
        assertEquals(SettingOption.entries.toSet() - withoutElement, reachable)
    }
}
