package io.github.beppe2880.opencodemobile.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
