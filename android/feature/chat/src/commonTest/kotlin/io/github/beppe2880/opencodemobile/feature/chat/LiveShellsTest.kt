package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.RunningCommand
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/** Values from the 2.2.1 recording in core:testing (`shell_running_*`). */
class LiveShellsTest {
    private val session = "ses_e67beca81ffe2jOx0y5gIe6nFA"
    private val command = "for i in \$(seq 1 30); do echo line \$i; sleep 1; done"
    private val input = """{"command":"for i in ${'$'}(seq 1 30); do echo line ${'$'}i; sleep 1; done"}"""

    private fun tool(callId: String, startedAt: Long, metadata: Map<String, String> = emptyMap(), name: String = "shell") =
        Part.Tool(callId, name, ToolStatus.Running, input, metadata = metadata, startedAt = startedAt)

    private fun shell(id: String, startedAt: Long, sessionId: String? = session, line: String = command) =
        RunningCommand(id, line, "/home/user/project", startedAt, sessionId)

    @Test
    fun `a stored running tool finds its shell among the session shells`() {
        val running = listOf(tool("toolu_011VK49E5rAcJEGeH7tHgxkQ", 1793556134529))
        val shells = listOf(shell("sh_19846bb5a0016Hcm0TX7E0r087", 1793556134746))

        assertEquals(
            mapOf("toolu_011VK49E5rAcJEGeH7tHgxkQ" to "sh_19846bb5a0016Hcm0TX7E0r087"),
            LiveShells.match(session, running, shells),
        )
    }

    @Test
    fun `the shell id in the tool metadata wins as older servers and the live stream send it`() {
        val running = listOf(tool("c1", 1, metadata = mapOf("shellID" to "sh_meta")))

        assertEquals(mapOf("c1" to "sh_meta"), LiveShells.match(session, running, listOf(shell("sh_list", 2))))
        assertEquals(mapOf("c1" to "sh_meta"), LiveShells.match(session, running, emptyList()))
    }

    @Test
    fun `a shell found earlier is kept when a reload drops the metadata`() {
        val running = listOf(tool("c1", 1))

        assertEquals(mapOf("c1" to "sh_known"), LiveShells.match(session, running, emptyList(), known = mapOf("c1" to "sh_known")))
    }

    @Test
    fun `repeated commands pair with their shells in start order`() {
        val running = listOf(tool("late", 20), tool("early", 10))
        val shells = listOf(shell("sh_b", 21), shell("sh_a", 11))

        assertEquals(mapOf("early" to "sh_a", "late" to "sh_b"), LiveShells.match(session, running, shells))
    }

    @Test
    fun `a shell already followed is not given to another call`() {
        val running = listOf(tool("c1", 10, metadata = mapOf("shellID" to "sh_a")), tool("c2", 20))
        val shells = listOf(shell("sh_a", 11), shell("sh_b", 21))

        assertEquals(mapOf("c1" to "sh_a", "c2" to "sh_b"), LiveShells.match(session, running, shells))
    }

    @Test
    fun `shells of other sessions or other commands and non shell tools are never matched`() {
        val running = listOf(tool("c1", 1), tool("c2", 1, name = "read"))
        val shells = listOf(
            shell("sh_other_session", 2, sessionId = "ses_other"),
            shell("sh_unowned", 2, sessionId = null),
            shell("sh_other_command", 2, line = "ls"),
        )

        assertEquals(emptyMap(), LiveShells.match(session, running, shells))
    }
}
