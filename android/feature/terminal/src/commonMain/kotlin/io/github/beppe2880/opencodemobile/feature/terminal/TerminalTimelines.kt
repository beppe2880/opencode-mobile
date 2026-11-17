package io.github.beppe2880.opencodemobile.feature.terminal

/**
 * The sizes each running shell's output was drawn at, kept while the app runs. Leaving the
 * terminal drops its screen, and reopening it replays the server's output from the start:
 * without these sizes it could only be parsed at one size, which garbles what was drawn at
 * another (the first prompt of a new shell is drawn for the server's initial 80×24).
 */
class TerminalTimelines {
    private val timelines = mutableMapOf<String, List<TerminalSize>>()

    operator fun get(terminalId: String): List<TerminalSize>? = timelines[terminalId]

    operator fun set(terminalId: String, timeline: List<TerminalSize>) {
        timelines[terminalId] = timeline
    }

    fun remove(terminalId: String) {
        timelines.remove(terminalId)
    }
}
