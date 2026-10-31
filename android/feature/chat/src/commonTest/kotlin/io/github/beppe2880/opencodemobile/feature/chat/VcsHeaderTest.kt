package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class VcsHeaderTest {
    private fun strings(files: Int, branch: String? = "main") = VcsStrings(
        detached = "Detached HEAD",
        detachedShort = "detached",
        count = "$files",
        filesShort = "$files files",
        filesFull = "$files files changed",
        editsShort = "$files agent edits",
        editsFull = "$files files edited by the agent",
        folderDescription = "Folder app",
        branchDescription = branch?.let { "Branch $it" } ?: "Detached HEAD",
    )

    private fun VcsText.plain() = (listOfNotNull(folder) + rest).joinToString(" · ") { it.text }

    @Test
    fun `each style writes the same line with icons or short or full words`() {
        val line = VcsLine("app", "main", 4)
        assertEquals(
            VcsText(VcsPart(VcsIcon.Folder, "app"), listOf(VcsPart(VcsIcon.Branch, "main"), VcsPart(VcsIcon.Changes, "4"))),
            line.text(ChatHeaderStyle.Icons, strings(4)),
        )
        assertEquals("app · main · 4 files", line.text(ChatHeaderStyle.Short, strings(4)).plain())
        assertEquals("app · main · 4 files changed", line.text(ChatHeaderStyle.Full, strings(4)).plain())
        // Text styles have no icons.
        assertEquals(null, line.text(ChatHeaderStyle.Short, strings(4)).folder?.icon)
    }

    @Test
    fun `without the folder or changed files only the branch remains`() {
        val text = VcsLine(null, "main", 0).text(ChatHeaderStyle.Icons, strings(0))
        assertEquals(VcsText(null, listOf(VcsPart(VcsIcon.Branch, "main"))), text)
    }

    @Test
    fun `a detached HEAD is named in every style`() {
        val line = VcsLine(null, null, 0)
        assertEquals(VcsPart(VcsIcon.Branch, "detached"), line.text(ChatHeaderStyle.Icons, strings(0, null)).rest.single())
        assertEquals("detached", line.text(ChatHeaderStyle.Short, strings(0, null)).plain())
        assertEquals("Detached HEAD", line.text(ChatHeaderStyle.Full, strings(0, null)).plain())
        assertEquals("Detached HEAD", line.description(strings(0, null)))
    }

    @Test
    fun `outside a repository only the edits of the agent are shown with a pencil`() {
        val line = VcsLine("app", null, 2, agentEdits = true)
        assertEquals(
            VcsText(VcsPart(VcsIcon.Folder, "app"), listOf(VcsPart(VcsIcon.Edits, "2"))),
            line.text(ChatHeaderStyle.Icons, strings(2)),
        )
        assertEquals("app · 2 agent edits", line.text(ChatHeaderStyle.Short, strings(2)).plain())
        assertEquals("app · 2 files edited by the agent", line.text(ChatHeaderStyle.Full, strings(2)).plain())
        assertEquals("Folder app, 2 files edited by the agent", line.description(strings(2)))
    }

    @Test
    fun `screen readers hear one full sentence whatever the style`() {
        assertEquals("Folder app, Branch main, 4 files changed", VcsLine("app", "main", 4).description(strings(4)))
        assertEquals("Branch main", VcsLine(null, "main", 0).description(strings(0)))
    }

    @Test
    fun `the folder name is the last segment of the directory`() {
        assertEquals("opencode-mobile", folderName("/home/me/opencode-mobile/"))
        assertEquals("project", folderName("C:\\work\\project"))
        assertEquals("/", folderName("/"))
    }
}
