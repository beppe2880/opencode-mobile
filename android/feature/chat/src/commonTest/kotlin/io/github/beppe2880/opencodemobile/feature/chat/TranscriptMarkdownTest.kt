package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.ImageAttachment
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranscriptMarkdownTest {
    @Test
    fun exportsTextAndCollapsesToolsWithoutImagesOrReasoning() {
        val messages = listOf(
            Message.User(
                "u", 0, "A **question**",
                images = listOf(ImageAttachment("private.png", "image/png", "data:image/png;base64,secret")),
            ),
            Message.Assistant(
                "a", 1, "build", ModelRef("cheap", "test"),
                listOf(
                    Part.Text(0, "An answer"),
                    Part.Reasoning(1, "private reasoning"),
                    Part.Tool("t", "<shell>", ToolStatus.Completed, "{}", "```code```"),
                ),
            ),
            Message.Synthetic("s", 2),
        )
        val markdown = transcriptMarkdown("Title", messages)
        assertTrue(markdown.startsWith("# Title\n"))
        assertTrue(markdown.contains("A **question**"))
        assertTrue(markdown.contains("An answer"))
        assertTrue(markdown.contains("<details>\n<summary>&lt;shell&gt;</summary>"))
        assertTrue(markdown.contains("````\n```code```\n````"))
        assertFalse(markdown.contains("private reasoning"))
        assertFalse(markdown.contains("base64"))
        assertFalse(markdown.contains("private.png"))
    }

    @Test
    fun filenamesAreBoundedAndCannotEscapeTheExportDirectory() {
        assertEquals("OpenCode.md", markdownFileName("..."))
        assertFalse(markdownFileName("../../a/b\\c").contains('/'))
        assertTrue(markdownFileName("a".repeat(200)).length <= 83)
        assertEquals("Conversazione italiana.md", markdownFileName("Conversazione italiana"))
    }

    @Test
    fun historyActionsDisableWhileBusyOrDisconnected() {
        val idle = ChatState(directory = "/scratch", loading = false)
        assertTrue(idle.historyReady)
        assertFalse(idle.copy(running = true).historyReady)
        assertFalse(idle.copy(compacting = true).historyReady)
        assertFalse(idle.copy(reverting = true).historyReady)
        assertFalse(idle.copy(connection = io.github.beppe2880.opencodemobile.core.domain.ConnectionState.Disconnected).menuReady)
        assertFalse(idle.copy(creatingSession = true).menuReady)
    }
}
