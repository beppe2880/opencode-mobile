package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatMenuStateTest {
    @Test
    fun creationStaysAvailableWhileRunningButHistoryActionsDoNot() {
        val state = ChatState(directory = "/scratch", loading = false)
        assertTrue(state.menuReady)
        assertTrue(state.historyReady)
        assertTrue(state.copy(running = true).menuReady)
        assertFalse(state.copy(running = true).historyReady)
        assertFalse(state.copy(compacting = true).historyReady)
        assertFalse(state.copy(reverting = true).historyReady)
        assertFalse(state.copy(sending = true).historyReady)
    }

    @Test
    fun unknownDisconnectedOrAlreadyCreatingSessionsDisableMenuCommands() {
        val state = ChatState(directory = "/scratch", loading = false)
        assertFalse(ChatState().menuReady)
        assertFalse(state.copy(directory = "").menuReady)
        assertFalse(state.copy(connection = ConnectionState.Disconnected).menuReady)
        assertFalse(state.copy(creatingSession = true).menuReady)
        assertFalse(state.copy(forking = true).menuReady)
    }
}
