package io.github.beppe2880.opencodemobile.feature.terminal

import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebSurfaceTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `recreated page restores dimensions at their original output positions`() = runTest(main.testDispatcher) {
        val surface = WebSurface({}, { _, _ -> }, {})
        surface.write("old prompt")
        surface.onPageResize(50, 24)
        runCurrent()
        surface.write("redrawn prompt")
        surface.onPageResize(50, 24)
        runCurrent()
        surface.onPageResize(100, 12)
        runCurrent()
        val calls = mutableListOf<Pair<String, List<String>>>()
        surface.attach(object : WebTerminalPage {
            override fun call(function: String, vararg arguments: String) {
                calls += function to arguments.toList()
            }
        })
        assertEquals("restore" to listOf("\"old promptredrawn prompt\"", "[[10,50,24], [24,100,12]]"), calls.first { it.first != "keyboardVisible" })
        surface.close()
    }

    @Test
    fun `trimming history retains the geometry at the new beginning`() = runTest(main.testDispatcher) {
        val surface = WebSurface({}, { _, _ -> }, {})
        surface.write("a".repeat(300_000) + "\n")
        surface.onPageResize(80, 24)
        runCurrent()
        surface.write("a".repeat(300_000) + "\n")
        surface.onPageResize(50, 24)
        runCurrent()
        surface.write("b".repeat(800_000) + "\n")
        surface.write("tail")
        surface.onPageResize(100, 12)
        runCurrent()
        var geometry: String? = null
        surface.attach(object : WebTerminalPage {
            override fun call(function: String, vararg arguments: String) {
                if (function == "restore") geometry = arguments[1]
            }
        })
        assertEquals("[[0,50,24], [800005,100,12]]", geometry)
        surface.close()
    }

    @Test
    fun `output of an already running shell is replayed at the fitted size`() = runTest(main.testDispatcher) {
        val surface = WebSurface({}, { _, _ -> }, {})
        surface.fitBeforeReplay()
        val first = mutableListOf<Pair<String, List<String>>>()
        surface.attach(object : WebTerminalPage {
            override fun call(function: String, vararg arguments: String) {
                first += function to arguments.toList()
            }
        })
        assertEquals("fitBeforeReplay" to emptyList<String>(), first.first { it.first != "keyboardVisible" })
        surface.write("prompt drawn for the phone")
        surface.onPageResize(50, 24)
        runCurrent()
        // A recreated page restores that size before the whole replay, not after it.
        val calls = mutableListOf<Pair<String, List<String>>>()
        surface.attach(object : WebTerminalPage {
            override fun call(function: String, vararg arguments: String) {
                calls += function to arguments.toList()
            }
        })
        assertEquals("restore" to listOf("\"prompt drawn for the phone\"", "[[0,50,24]]"), calls.first { it.first != "keyboardVisible" })
        surface.close()
    }

    @Test
    fun `reset discards dimensions belonging to the old shell`() = runTest(main.testDispatcher) {
        val surface = WebSurface({}, { _, _ -> }, {})
        surface.write("old")
        surface.onPageResize(50, 24)
        runCurrent()
        surface.reset()
        val calls = mutableListOf<String>()
        surface.attach(object : WebTerminalPage {
            override fun call(function: String, vararg arguments: String) {
                calls += function
            }
        })
        assertEquals(listOf("keyboardVisible", "setModifiers", "setLive", "focus"), calls)
        surface.close()
    }
}
