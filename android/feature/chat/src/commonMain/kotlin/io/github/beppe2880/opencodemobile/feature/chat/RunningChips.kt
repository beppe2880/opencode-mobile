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
 * What the running chips opened: the list of [group] (a popover above the composer), or one
 * [item] of it in a sheet. [endedAt] (server clock) is set once [item] no longer runs, so its
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

/**
 * The panel after a refresh to [work] at [serverNow]: the open item takes its latest data or,
 * once gone, is marked ended (once); an open list closes when nothing of its kind runs anymore.
 */
internal fun RunningPanel.follow(work: RunningWork, serverNow: Long): RunningPanel? {
    val current = work.entries(group)
    val open = item ?: return takeIf { current.isNotEmpty() }
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
