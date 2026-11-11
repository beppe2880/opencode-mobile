package io.github.beppe2880.opencodemobile.navigation

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatSwipe
import io.github.beppe2880.opencodemobile.core.domain.FoldChat
import io.github.beppe2880.opencodemobile.core.domain.UnfoldChat
import io.github.beppe2880.opencodemobile.core.domain.UnfoldedLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplitBackStackTest {

    private val chat = ChatDestination("ses_a")
    private val terminal = TerminalDestination("/repo")

    @Test
    fun fullScreenChatAboveTheListMovesBesideItWhenTheWindowWidens() {
        assertEquals(SplitStep(selectChat = "ses_a"), splitStep(split = true, widened = true, waiting = false, top = chat, below = SessionsDestination))
    }

    @Test
    fun chatPushedAboveTheListInAWideWindowStaysFullScreen() {
        // A subagent opened from the chat beside the list: Back returns to its parent.
        assertEquals(SplitStep(), splitStep(split = true, widened = false, waiting = false, top = chat, below = SessionsDestination))
    }

    @Test
    fun narrowWindowLeavesTheStackAloneAndStopsWaiting() {
        assertEquals(SplitStep(), splitStep(split = false, widened = false, waiting = true, top = chat, below = SessionsDestination))
    }

    @Test
    fun screenAboveTheChatIsKeptAndTheChatMovesOnceItCloses() {
        assertEquals(SplitStep(waiting = true), splitStep(split = true, widened = true, waiting = false, top = terminal, below = chat))
        assertEquals(SplitStep(selectChat = "ses_a"), splitStep(split = true, widened = false, waiting = true, top = chat, below = SessionsDestination))
    }

    @Test
    fun subagentAboveItsParentWaitsForTheParent() {
        val sub = ChatDestination("ses_sub")
        assertEquals(SplitStep(waiting = true), splitStep(split = true, widened = true, waiting = false, top = sub, below = chat))
    }

    @Test
    fun returningToTheListOrConnectStopsWaiting() {
        assertEquals(SplitStep(), splitStep(split = true, widened = false, waiting = true, top = SessionsDestination, below = null))
        assertEquals(SplitStep(), splitStep(split = true, widened = true, waiting = false, top = ConnectDestination(), below = null))
    }

    @Test
    fun keepingTheChatFullScreenOnUnfoldLeavesTheStackAlone() {
        val full = UnfoldChat.FullScreen
        assertEquals(SplitStep(), splitStep(split = true, widened = true, waiting = false, top = chat, below = SessionsDestination, unfold = full))
        // Nor does it wait for a chat under another screen.
        assertEquals(SplitStep(), splitStep(split = true, widened = true, waiting = false, top = terminal, below = chat, unfold = full))
        assertEquals(SplitStep(), splitStep(split = true, widened = false, waiting = true, top = chat, below = SessionsDestination, unfold = full))
        // Showing the list beside it is the default.
        assertEquals(
            SplitStep(selectChat = "ses_a"),
            splitStep(split = true, widened = true, waiting = false, top = chat, below = SessionsDestination, unfold = UnfoldChat.BesideList),
        )
    }

    @Test
    fun foldingKeepsTheChatFullScreenOrGoesBackToTheList() {
        val keep = AppSettings()
        assertEquals(FoldStep.OpenFullScreen, foldStep(wasSplit = true, split = false, chatOpen = true, settings = keep))
        val list = AppSettings(foldOpenChat = FoldChat.ShowList)
        assertEquals(FoldStep.CloseChat, foldStep(wasSplit = true, split = false, chatOpen = true, settings = list))
        // Nothing open, still split, or never split: nothing to do.
        assertEquals(FoldStep.None, foldStep(wasSplit = true, split = false, chatOpen = false, settings = keep))
        assertEquals(FoldStep.None, foldStep(wasSplit = true, split = true, chatOpen = true, settings = list))
        assertEquals(FoldStep.None, foldStep(wasSplit = false, split = false, chatOpen = true, settings = keep))
    }

    @Test
    fun turningTheListBesideTheChatOffInSettingsIsNotFolding() {
        assertEquals(FoldStep.None, foldStep(wasSplit = true, split = false, chatOpen = true, settings = AppSettings(splitView = false)))
        val panel = AppSettings(unfoldedLayout = UnfoldedLayout.ListPanel, foldOpenChat = FoldChat.ShowList)
        assertEquals(FoldStep.None, foldStep(wasSplit = true, split = false, chatOpen = true, settings = panel))
    }

    @Test
    fun theChatListPanelUsesThePhoneLayoutOnWideWindows() {
        assertTrue(AppSettings().showsSplit(wide = true))
        assertFalse(AppSettings().showsSplit(wide = false))
        assertFalse(AppSettings(unfoldedLayout = UnfoldedLayout.ListPanel).showsSplit(wide = true))
        assertFalse(AppSettings(splitView = false).showsSplit(wide = true))
    }

    @Test
    fun theListSwipeWorksOnWideWindowsWhateverTheChatSwipe() {
        for (swipe in ChatSwipe.entries) {
            assertTrue(AppSettings(chatSwipe = swipe).swipeShowsList(wide = true), "$swipe")
            assertTrue(AppSettings(chatSwipe = swipe, unfoldedLayout = UnfoldedLayout.ListPanel).swipeShowsList(wide = true), "$swipe")
        }
        assertFalse(AppSettings(listSwipe = false).swipeShowsList(wide = true))
        // Phones and the phone layout keep the chat swipe choice.
        assertFalse(AppSettings().swipeShowsList(wide = false))
        assertFalse(AppSettings(splitView = false).swipeShowsList(wide = true))
    }
}
