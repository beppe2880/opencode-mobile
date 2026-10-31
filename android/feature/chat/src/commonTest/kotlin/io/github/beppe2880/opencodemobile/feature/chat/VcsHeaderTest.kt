package io.github.beppe2880.opencodemobile.feature.chat

import kotlin.test.Test
import kotlin.test.assertEquals

class VcsHeaderTest {
    @Test
    fun `the folder is kept apart from the rest so it can shrink first`() {
        assertEquals(VcsText("repo", "main · 2 files"), VcsLine("repo", "main", 2).text("Detached HEAD", "2 files"))
        assertEquals(VcsText(null, "main"), VcsLine(null, "main", 0).text("Detached HEAD", null))
    }

    @Test
    fun `a repository without a branch is on a detached HEAD`() {
        assertEquals(VcsText(null, "Detached HEAD"), VcsLine(null, null, 0).text("Detached HEAD", null))
    }

    @Test
    fun `outside a repository only the edits of the agent are shown`() {
        assertEquals(VcsText("repo", "1 file edited"), VcsLine("repo", null, 1, agentEdits = true).text("Detached HEAD", "1 file edited"))
    }

    @Test
    fun `the folder name is the last segment of the directory`() {
        assertEquals("opencode-mobile", folderName("/home/me/opencode-mobile/"))
        assertEquals("project", folderName("C:\\work\\project"))
        assertEquals("/", folderName("/"))
    }
}
