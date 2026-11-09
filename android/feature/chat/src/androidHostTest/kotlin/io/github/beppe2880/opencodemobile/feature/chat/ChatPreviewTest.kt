package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderStyle
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPreviewTest {

    private fun elements(settings: AppSettings) = previewChatState(settings, running = false).items.mapNotNull { it.part() }

    @Test
    fun `default settings group thinking and tools into one activity row`() {
        assertEquals(
            listOf(SettingsPart.Messages, SettingsPart.Activity, SettingsPart.Messages, SettingsPart.Footer),
            elements(AppSettings()),
        )
    }

    @Test
    fun `without grouping activity the thoughts and tools rows show`() {
        assertEquals(
            listOf(SettingsPart.Messages, SettingsPart.Thoughts, SettingsPart.Tools, SettingsPart.Messages, SettingsPart.Footer),
            elements(AppSettings(groupActivity = false)),
        )
        // Two thoughts in a row: one row when grouped, two otherwise.
        assertEquals(2, elements(AppSettings(groupActivity = false, groupThoughts = false)).count { it == SettingsPart.Thoughts })
        assertFalse(SettingsPart.Thoughts in elements(AppSettings(groupActivity = false, showReasoning = false)))
    }

    @Test
    fun `the display settings reach the preview like the chat`() {
        val state = previewChatState(
            running = false,
            settings = AppSettings(
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
    fun `a running chat keeps its newest group open and shows the work in the background`() {
        val state = previewChatState(AppSettings(), running = true)
        assertTrue(state.running)
        assertEquals(state.items.last { it !is ChatItem.Working }.key, state.liveGroupKey())
        assertFalse(state.work.isEmpty)
        assertNull(previewChatState(AppSettings(followRunningTools = false), running = true).liveGroupKey())
        assertNull(previewChatState(AppSettings(), running = false).liveGroupKey())
    }

    @Test
    fun `the question card sits next to the permission card and follows its setting`() {
        val state = previewChatState(AppSettings(questionsCollapsed = true), running = false)
        assertTrue(state.isFormCollapsed(state.forms.single().id))
        assertFalse(state.isPermissionCollapsed(state.permissions.single().id))
    }

    @Test
    fun `the header line follows its setting`() {
        assertEquals("shop", previewChatState(AppSettings(), running = false).vcs?.folder)
        assertNull(previewChatState(AppSettings(chatHeaderLine = ChatHeaderLine.BranchFiles), running = false).vcs?.folder)
        assertNull(previewChatState(AppSettings(chatHeaderLine = ChatHeaderLine.Folder), running = false).vcs)
    }
}
