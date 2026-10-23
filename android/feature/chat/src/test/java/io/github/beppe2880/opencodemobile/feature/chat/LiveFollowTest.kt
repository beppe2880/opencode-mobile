package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveFollowTest {
    private fun tool(name: String) = Part.Tool("c", name, ToolStatus.Completed, input = "{}")

    @Test
    fun `each kind of tool follows its own setting`() {
        val live = LiveFollow(shell = true, edits = false, reads = true, other = false)

        assertTrue(live.covers(tool("bash")))
        assertFalse(live.covers(tool("write")))
        assertFalse(live.covers(tool("apply_patch")))
        assertTrue(live.covers(tool("grep")))
        assertFalse(live.covers(tool("webfetch")))
        assertFalse(live.covers(tool("github_create_issue")))
    }

    @Test
    fun `settings map to what is followed`() {
        assertNull(LiveFollow.of(AppSettings(followRunningTools = false)))
        assertEquals(
            LiveFollow(thinking = false, shell = true, edits = true, reads = true, other = false),
            LiveFollow.of(AppSettings(liveThinking = false, liveReads = true)),
        )
    }
}
