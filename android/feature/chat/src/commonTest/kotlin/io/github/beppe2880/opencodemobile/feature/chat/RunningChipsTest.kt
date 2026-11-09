package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.RunningAgent
import io.github.beppe2880.opencodemobile.core.domain.RunningCommand
import io.github.beppe2880.opencodemobile.core.domain.RunningWork
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunningChipsTest {

    private fun command(id: String, startedAt: Long, line: String = "npm test") = RunningCommand(id, line, "/repo", startedAt, "s1")

    private fun agent(id: String, title: String, agent: String? = "explore", startedAt: Long? = 1_000) = RunningAgent(
        Session(id, "s1", "p", title, "/repo", agent, null, 0.0, TokenUsage(), null, 0, 0, null),
        startedAt,
    )

    private val work = RunningWork(
        agents = listOf(agent("a1", "Find the tests")),
        commands = listOf(command("late", 5_000, "watch"), command("early", 1_000)),
    )

    @Test
    fun `one chip per running kind with its count and shells first`() {
        assertEquals(listOf(RunningChip(RunningGroup.Commands, 2), RunningChip(RunningGroup.Agents, 1)), runningChips(work, enabled = true))
        assertEquals(listOf(RunningChip(RunningGroup.Agents, 1)), runningChips(work.copy(commands = emptyList()), enabled = true))
        assertTrue(runningChips(RunningWork(), enabled = true).isEmpty())
    }

    @Test
    fun `no chips with the setting off`() {
        assertTrue(runningChips(work, enabled = false).isEmpty())
        assertTrue(AppSettings().runningChips)
        val off = ChatState().withSettings(AppSettings(runningChips = false))
        assertFalse(off.runningChips)
        assertTrue(runningChips(work, off.runningChips).isEmpty())
    }

    @Test
    fun `turning the setting off closes what the chips opened`() {
        val open = ChatState(runningPanel = RunningPanel(RunningGroup.Commands))
        assertNull(open.withSettings(AppSettings(runningChips = false)).runningPanel)
        assertEquals(RunningPanel(RunningGroup.Commands), open.withSettings(AppSettings()).runningPanel)
    }

    @Test
    fun `entries list shells oldest first and subagents with their task and agent`() {
        assertEquals(listOf("early", "late"), work.entries(RunningGroup.Commands).map { it.id })
        assertEquals(RunningEntry("early", RunningGroup.Commands, "npm test", startedAt = 1_000), work.entries(RunningGroup.Commands).first())
        assertEquals(RunningEntry("a1", RunningGroup.Agents, "Find the tests", "explore", 1_000), work.entries(RunningGroup.Agents).single())
    }

    @Test
    fun `elapsed time reads like a stopwatch`() {
        assertEquals("0:00", formatElapsedClock(0))
        assertEquals("0:07", formatElapsedClock(7_900))
        assertEquals("5:55", formatElapsedClock(355_000))
        assertEquals("59:59", formatElapsedClock(3_599_000))
        assertEquals("1:02:03", formatElapsedClock(3_723_000))
        assertEquals("0:00", formatElapsedClock(-5_000))
    }

    @Test
    fun `elapsed runs on the server clock and stops when the item ended`() {
        val entry = RunningEntry("x", RunningGroup.Commands, "npm test", startedAt = 100_000)
        assertEquals(15_000L, runningElapsedMs(entry, endedAt = null, deviceNow = 98_000, offsetMs = 17_000))
        assertEquals(372_000L, runningElapsedMs(entry, endedAt = 472_000, deviceNow = 999_999, offsetMs = 0))
        assertNull(runningElapsedMs(entry.copy(startedAt = null), endedAt = null, deviceNow = 0, offsetMs = 0))
    }

    @Test
    fun `an open item follows its data and is marked ended once when gone`() {
        val panel = RunningPanel(RunningGroup.Agents, RunningEntry("a1", RunningGroup.Agents, "", startedAt = null))
        val followed = panel.follow(work, serverNow = 9_000)!!
        assertEquals("Find the tests", followed.item?.label)
        assertNull(followed.endedAt)

        val ended = followed.follow(RunningWork(), serverNow = 10_000)!!
        assertEquals(10_000L, ended.endedAt)
        assertEquals("Find the tests", ended.item?.label)
        assertEquals(10_000L, ended.follow(RunningWork(), serverNow = 20_000)?.endedAt)
    }

    @Test
    fun `an open list closes when nothing of its kind runs`() {
        val list = RunningPanel(RunningGroup.Commands)
        assertEquals(list, list.follow(work, serverNow = 0))
        assertNull(list.follow(work.copy(commands = emptyList()), serverNow = 0))
    }
}
