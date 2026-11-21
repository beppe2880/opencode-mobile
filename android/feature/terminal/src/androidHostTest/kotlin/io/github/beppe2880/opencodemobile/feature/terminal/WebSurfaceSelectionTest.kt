package io.github.beppe2880.opencodemobile.feature.terminal

import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WebSurfaceSelectionTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `selection commands target the page without sending shell input`() {
        val sent = mutableListOf<ByteArray>()
        val surface = WebSurface(send = { sent += it }, onResize = { _, _ -> }, onModifiersUsed = {})
        val calls = mutableListOf<String>()
        val page = object : WebTerminalPage {
            override fun call(function: String, vararg arguments: String) { calls += function }
        }
        surface.attach(page)
        calls.clear()
        surface.toggleSelectionMode()
        surface.selectAll()
        surface.copySelection()
        surface.clearSelection()
        assertEquals(listOf("toggleSelectionMode", "selectAll", "copySelection", "clearSelection"), calls)
        assertTrue(sent.isEmpty())
        surface.detach(page)
        surface.copySelection()
        assertEquals(4, calls.size)
        surface.close()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `selection availability follows the page and resets on detach`() = runTest {
        val surface = WebSurface(send = {}, onResize = { _, _ -> }, onModifiersUsed = {})
        val page = object : WebTerminalPage {
            override fun call(function: String, vararg arguments: String) = Unit
        }
        assertEquals(false, surface.selectionAvailable.value)
        surface.attach(page)
        surface.onPageSelectionPicking(true)
        advanceUntilIdle()
        assertEquals(true, surface.selectionPicking.value)
        surface.onPageSelectionAvailable(true)
        advanceUntilIdle()
        assertEquals(true, surface.selectionAvailable.value)
        surface.onPageSelectionAvailable(false)
        advanceUntilIdle()
        assertEquals(false, surface.selectionAvailable.value)
        surface.onPageSelectionAvailable(true)
        advanceUntilIdle()
        surface.detach(page)
        assertEquals(false, surface.selectionAvailable.value)
        assertEquals(false, surface.selectionPicking.value)
        surface.close()
    }

    @Test
    fun `translated handle labels are escaped and replayed after recreation`() {
        val surface = WebSurface(send = {}, onResize = { _, _ -> }, onModifiersUsed = {})
        val labels = mutableListOf<List<String>>()
        val page = object : WebTerminalPage {
            override fun call(function: String, vararg arguments: String) {
                if (function == "selectionLabels") labels += arguments.toList()
            }
        }
        surface.setSelectionLabels("Start \"handle\"", "End", "Row %1\$d, column %2\$d")
        surface.attach(page)
        assertEquals(listOf("\"Start \\\"handle\\\"\"", "\"End\"", "\"Row %1\$d, column %2\$d\""), labels.single())
        surface.detach(page)
        surface.attach(page)
        assertEquals(2, labels.size)
        surface.close()
    }
}
