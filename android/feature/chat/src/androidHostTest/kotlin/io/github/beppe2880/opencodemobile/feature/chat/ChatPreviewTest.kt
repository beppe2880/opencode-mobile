package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatElement
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPreviewTest {

    private fun elements(settings: AppSettings) = previewChatState(settings).items.mapNotNull { it.element() }

    @Test
    fun `default settings group thinking and tools into one activity row`() {
        assertEquals(
            listOf(ChatElement.Messages, ChatElement.Activity, ChatElement.Messages, ChatElement.Footer),
            elements(AppSettings()),
        )
    }

    @Test
    fun `without grouping activity the thoughts and tools rows show`() {
        assertEquals(
            listOf(ChatElement.Messages, ChatElement.Thoughts, ChatElement.Tools, ChatElement.Messages, ChatElement.Footer),
            elements(AppSettings(groupActivity = false)),
        )
        // Two thoughts in a row: one row when grouped, two otherwise.
        assertEquals(2, elements(AppSettings(groupActivity = false, groupThoughts = false)).count { it == ChatElement.Thoughts })
        assertFalse(ChatElement.Thoughts in elements(AppSettings(groupActivity = false, showReasoning = false)))
    }

    @Test
    fun `the display settings reach the preview like the chat`() {
        val state = previewChatState(
            AppSettings(
                showCost = false,
                agentColorOutline = false,
                chatTextScale = 1.3f,
                permissionsCollapsed = true,
                expandToolsByDefault = true,
                outputPreviewLines = 6,
                chatHeaderStyle = ChatHeaderStyle.Full,
            ),
        )
        assertFalse(state.showCost)
        assertFalse(state.agentColorOutline)
        assertEquals(1.3f, state.chatTextScale)
        assertTrue(state.isPermissionCollapsed(state.permissions.single().id))
        assertTrue(state.expandToolsByDefault)
        assertEquals(6, state.outputPreviewLines)
        assertEquals(ChatHeaderStyle.Full, state.headerStyle)
    }

    @Test
    fun `the header line follows its setting`() {
        assertEquals("shop", previewChatState(AppSettings()).vcs?.folder)
        assertNull(previewChatState(AppSettings(chatHeaderLine = ChatHeaderLine.BranchFiles)).vcs?.folder)
        assertNull(previewChatState(AppSettings(chatHeaderLine = ChatHeaderLine.Folder)).vcs)
    }
}
