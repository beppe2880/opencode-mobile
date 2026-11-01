package io.github.beppe2880.opencodemobile.feature.settings.customize

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatElement
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatOptionsTest {

    @Test
    fun everyPartHasOptionsAndEveryOptionHasAPart() {
        ChatElement.entries.forEach { assertTrue(it.options.isNotEmpty(), "$it has no options") }
        assertEquals(ChatOption.entries.toSet(), ChatElement.entries.flatMap { it.options }.toSet())
    }

    @Test
    fun eachPartOffersOnlyItsOwnOptions() {
        assertEquals(listOf(ChatOption.HeaderLine, ChatOption.HeaderStyle, ChatOption.HeaderOpens), ChatElement.Header.options)
        assertEquals(listOf(ChatOption.TextSize), ChatElement.Messages.options)
        assertEquals(listOf(ChatOption.ShowReasoning, ChatOption.GroupThoughts, ChatOption.GroupActivity), ChatElement.Thoughts.options)
        assertEquals(
            listOf(ChatOption.ExpandTools, ChatOption.ToolDetails, ChatOption.FullCommands, ChatOption.OutputLines, ChatOption.GroupActivity),
            ChatElement.Tools.options,
        )
        assertEquals(listOf(ChatOption.ShowCost), ChatElement.Footer.options)
        assertEquals(listOf(ChatOption.PermissionsCollapsed, ChatOption.QuestionsCollapsed), ChatElement.PendingCard.options)
        assertEquals(listOf(ChatOption.AgentOutline, ChatOption.Dictation), ChatElement.Composer.options)
    }

    @Test
    fun groupedActivityOffersTheThinkingAndToolOptions() {
        val activity = ChatElement.Activity.options.toSet()
        assertEquals(ChatOption.GroupActivity, ChatElement.Activity.options.first())
        assertTrue(activity.containsAll(ChatElement.Thoughts.options))
        assertTrue(activity.containsAll(ChatElement.Tools.options))
    }

    @Test
    fun aFolderAloneHasNoStyleAndNoTarget() {
        val folder = AppSettings(chatHeaderLine = ChatHeaderLine.Folder)
        assertEquals(listOf(ChatOption.HeaderLine), ChatElement.Header.optionsFor(folder))
        assertEquals(ChatElement.Header.options, ChatElement.Header.optionsFor(AppSettings(chatHeaderLine = ChatHeaderLine.BranchFiles)))
    }
}
