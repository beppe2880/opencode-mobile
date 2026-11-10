package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.RunningWork

/**
 * One item the running chips list: a shell command ([RunningGroup.Commands]) or a subagent
 * ([RunningGroup.Agents]). [label] is the command line or the subagent's task; [agent] the
 * subagent's agent; [startedAt] is on the server clock, null when unknown.
 */
data class RunningEntry(
    val id: String,
    val group: RunningGroup,
    val label: String,
    val agent: String? = null,
    val startedAt: Long? = null,
)

/** A chip above the composer: how many items of [group] run. */
data class RunningChip(val group: RunningGroup, val count: Int)

/**
 * What the running chips opened: the list of everything that runs (a popover above the
 * composer), [group] (the chip tapped) first, or one [item] of [group] in a sheet. [endedAt] (server clock) is set once [item] no longer runs, so its
 * sheet shows how long it ran instead of disappearing.
 */
data class RunningPanel(
    val group: RunningGroup,
    val item: RunningEntry? = null,
    val endedAt: Long? = null,
)

/** The items of [group], oldest first; subagents keep the server's order. */
internal fun RunningWork.entries(group: RunningGroup): List<RunningEntry> = when (group) {
    RunningGroup.Commands -> commands.sortedBy { it.startedAt }.map { RunningEntry(it.id, group, it.command, startedAt = it.startedAt) }
    RunningGroup.Agents -> agents.map { RunningEntry(it.session.id, group, it.session.title, it.session.agent, it.startedAt) }
}

internal fun RunningWork.count(group: RunningGroup): Int = when (group) {
    RunningGroup.Commands -> commands.size
    RunningGroup.Agents -> agents.size
}

/** One chip per kind that runs, shells first; none when the setting is off. */
internal fun runningChips(work: RunningWork, enabled: Boolean): List<RunningChip> =
    if (!enabled) {
        emptyList()
    } else {
        listOf(RunningGroup.Commands, RunningGroup.Agents).mapNotNull { group ->
            work.count(group).takeIf { it > 0 }?.let { RunningChip(group, it) }
        }
    }

/** The running line under the header, unless the chips above the composer already show what runs. */
internal val ChatState.showsRunningLine: Boolean
    get() = !work.isEmpty && runningChips(work, runningChips).isEmpty()

/** The kinds the list shows, [first] (the chip tapped) first; only those that run. */
internal fun RunningWork.listGroups(first: RunningGroup): List<RunningGroup> =
    (listOf(first) + RunningGroup.entries).distinct().filter { count(it) > 0 }

/** A Stop in the footer of the running list. */
internal sealed interface RunningStop {
    /** The only item that runs: its own Stop, with its confirmation. */
    data class One(val entry: RunningEntry) : RunningStop

    /** Everything that runs: the subagents and this session's shells. */
    data object All : RunningStop

    /** Every item of [group]; [count] picks "Stop agent" or "Stop agents". */
    data class Group(val group: RunningGroup, val count: Int) : RunningStop
}

/**
 * The Stops of the running list: one item alone gets its own Stop; one kind only, its group's;
 * both kinds, Stop all and each group's.
 */
internal fun runningStops(work: RunningWork): List<RunningStop> {
    val agents = work.count(RunningGroup.Agents)
    val shells = work.count(RunningGroup.Commands)
    return when {
        agents + shells == 0 -> emptyList()
        agents + shells == 1 -> listOf(RunningStop.One((work.entries(RunningGroup.Agents) + work.entries(RunningGroup.Commands)).single()))
        agents > 0 && shells > 0 -> listOf(
            RunningStop.All,
            RunningStop.Group(RunningGroup.Agents, agents),
            RunningStop.Group(RunningGroup.Commands, shells),
        )
        agents > 0 -> listOf(RunningStop.Group(RunningGroup.Agents, agents))
        else -> listOf(RunningStop.Group(RunningGroup.Commands, shells))
    }
}

/** What pressing [this] sends; each asks for confirmation as its counterpart elsewhere does. */
internal val RunningStop.event: ChatEvent
    get() = when (this) {
        is RunningStop.One -> when (entry.group) {
            RunningGroup.Commands -> ChatEvent.StopCommand(entry.id)
            RunningGroup.Agents -> ChatEvent.StopAgent(entry.id)
        }
        RunningStop.All -> ChatEvent.StopAll(StopAllTarget.Everything)
        is RunningStop.Group -> ChatEvent.StopAll(group)
    }

/** Everything [this] would stop is already stopping. */
internal fun RunningStop.stopping(state: ChatState): Boolean {
    val shells = state.work.commands.all { it.id in state.stoppingCommands }
    val agents = state.work.agents.all { it.session.id in state.stoppingAgents }
    return when (this) {
        is RunningStop.One -> when (entry.group) {
            RunningGroup.Commands -> entry.id in state.stoppingCommands
            RunningGroup.Agents -> entry.id in state.stoppingAgents
        }
        RunningStop.All -> shells && agents
        is RunningStop.Group -> when (group) {
            RunningGroup.Commands -> shells
            RunningGroup.Agents -> agents
        }
    }
}

/**
 * The panel after a refresh to [work] at [serverNow]: the open item takes its latest data or,
 * once gone, is marked ended (once); an open list closes when nothing runs anymore.
 */
internal fun RunningPanel.follow(work: RunningWork, serverNow: Long): RunningPanel? {
    val open = item ?: return takeIf { !work.isEmpty }
    val current = work.entries(group)
    val found = current.firstOrNull { it.id == open.id }
    return when {
        found != null -> copy(item = found, endedAt = null)
        endedAt == null -> copy(endedAt = serverNow)
        else -> this
    }
}

/** A stopwatch: `0:07`, `5:55`, `1:02:03`. */
internal fun formatElapsedClock(millis: Long): String {
    val seconds = millis.coerceAtLeast(0) / 1_000
    val h = seconds / 3_600
    val m = (seconds % 3_600) / 60
    val s = seconds % 60
    val ss = s.toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$ss" else "$m:$ss"
}

/** How long [entry] ran: until [endedAt] when it ended, else until now ([deviceNow] moved onto the server clock by [offsetMs]). */
internal fun runningElapsedMs(entry: RunningEntry, endedAt: Long?, deviceNow: Long, offsetMs: Long): Long? {
    val start = entry.startedAt ?: return null
    return if (endedAt != null) (endedAt - start).coerceAtLeast(0) else elapsedMs(start, deviceNow, offsetMs)
}
