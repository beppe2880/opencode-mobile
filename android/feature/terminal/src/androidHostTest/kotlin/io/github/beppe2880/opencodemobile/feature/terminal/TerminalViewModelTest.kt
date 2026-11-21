package io.github.beppe2880.opencodemobile.feature.terminal

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.TerminalInfo
import io.github.beppe2880.opencodemobile.core.domain.TerminalLink
import io.github.beppe2880.opencodemobile.core.domain.TerminalRepository
import io.github.beppe2880.opencodemobile.core.domain.TerminalUpdate
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `terminal output pauses while the terminal is hidden and resumes when shown`() = runTest(main.testDispatcher) {
        val terminals = FakeTerminals()
        val viewModel = TerminalViewModel("/project", terminals, FakeSettings())
        val page = RecordingPage()
        viewModel.surface.attach(page)

        runCurrent()
        viewModel.setTerminalVisible(false)
        runCurrent()
        viewModel.setTerminalVisible(true)
        advanceUntilIdle()

        // WebSurface writes to the page as JavaScript literals, so each chunk arrives quoted.
        assertEquals(listOf("\"hello\"", "\"again\""), page.writes)
        assertEquals(TerminalLink.Connected, viewModel.state.value.link)
        assertEquals(1, terminals.openCalls)
    }

    @Test
    fun `server exit followed by pause and resume does not open a new shell`() = runTest(main.testDispatcher) {
        val terminals = FakeTerminals(exits = true)
        val viewModel = TerminalViewModel("/project", terminals, FakeSettings())
        runCurrent()
        assertEquals(TerminalLink.Exited, viewModel.state.value.link)
        viewModel.setTerminalVisible(false)
        runCurrent()
        viewModel.setTerminalVisible(true)
        runCurrent()
        assertEquals(1, terminals.openCalls)
        assertEquals(TerminalLink.Exited, viewModel.state.value.link)
    }

    @Test
    fun `new terminal does not resize to the old shell dimensions before replay`() = runTest(main.testDispatcher) {
        val terminals = FakeTerminals()
        val viewModel = TerminalViewModel("/project", terminals, FakeSettings())
        runCurrent()
        viewModel.surface.onPageResize(50, 12)
        advanceUntilIdle()
        assertEquals(listOf(Triple("t1", 50, 12)), terminals.resizes)

        viewModel.onEvent(TerminalEvent.Restart)
        advanceUntilIdle()
        assertEquals(2, terminals.openCalls)
        assertEquals(listOf(Triple("t1", 50, 12)), terminals.resizes)

        // The new page signals its fitted size only after parsing the replay.
        viewModel.surface.onPageResize(50, 12)
        advanceUntilIdle()
        assertEquals(listOf(Triple("t1", 50, 12), Triple("t2", 50, 12)), terminals.resizes)
    }

    @Test
    fun `the first size reaches a new shell at once and later ones are debounced`() = runTest(main.testDispatcher) {
        val terminals = FakeTerminals()
        val viewModel = TerminalViewModel("/project", terminals, FakeSettings())
        runCurrent()
        viewModel.surface.onPageResize(50, 12)
        runCurrent()
        assertEquals(listOf(Triple("t1", 50, 12)), terminals.resizes)
        viewModel.surface.onPageResize(50, 20)
        runCurrent()
        assertEquals(1, terminals.resizes.size)
        advanceUntilIdle()
        assertEquals(Triple("t1", 50, 20), terminals.resizes.last())
    }

    @Test
    fun `an already running shell is replayed at the page size`() = runTest(main.testDispatcher) {
        val page = RecordingPage()
        val viewModel = TerminalViewModel("/project", FakeTerminals(running = true), FakeSettings())
        viewModel.surface.attach(page)
        runCurrent()
        assertEquals(1, page.calls.count { it == "fitBeforeReplay" })

        val fresh = RecordingPage()
        val created = TerminalViewModel("/project", FakeTerminals(), FakeSettings())
        created.surface.attach(fresh)
        runCurrent()
        assertEquals(0, fresh.calls.count { it == "fitBeforeReplay" })
    }

    @Test
    fun `reopening a running shell replays it at the sizes of the earlier visit`() = runTest(main.testDispatcher) {
        val timelines = TerminalTimelines()
        val first = TerminalViewModel("/project", FakeTerminals(), FakeSettings(), timelines)
        runCurrent()
        first.surface.onPageResize(49, 19)
        advanceUntilIdle()
        assertEquals(listOf(TerminalSize(5, 49, 19)), timelines["t1"])

        val page = RecordingPage()
        val reopened = TerminalViewModel("/project", FakeTerminals(running = true), FakeSettings(), timelines)
        reopened.surface.attach(page)
        runCurrent()
        assertEquals(0, page.calls.count { it == "fitBeforeReplay" })
        assertEquals(listOf("restore"), page.calls.filter { it == "restore" || it == "write" }.take(1))
    }

    @Test
    fun `a new shell forgets the sizes of the old one`() = runTest(main.testDispatcher) {
        val timelines = TerminalTimelines()
        val viewModel = TerminalViewModel("/project", FakeTerminals(), FakeSettings(), timelines)
        runCurrent()
        viewModel.surface.onPageResize(49, 19)
        advanceUntilIdle()
        viewModel.onEvent(TerminalEvent.Restart)
        advanceUntilIdle()
        assertEquals(null, timelines["t1"])
    }

    private class FakeSettings : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            settings.value = transform(settings.value)
        }
    }

    private class FakeTerminals(private val exits: Boolean = false, private val running: Boolean = false) : TerminalRepository {
        var openCalls = 0
        val resizes = mutableListOf<Triple<String, Int, Int>>()
        override suspend fun open(directory: String): Result<TerminalInfo> {
            openCalls++
            return Result.success(TerminalInfo("t$openCalls", "bash", running = true, created = !running))
        }

        override suspend fun resize(directory: String, terminalId: String, columns: Int, rows: Int): Result<Unit> {
            resizes += Triple(terminalId, columns, rows)
            return Result.success(Unit)
        }

        override suspend fun close(directory: String, terminalId: String) = Result.success(Unit)

        override fun attach(
            directory: String,
            terminalId: String,
            input: Flow<ByteArray>,
            visible: Flow<Boolean>,
        ) = flow {
            visible.first { it }
            emit(TerminalUpdate.Link(TerminalLink.Connected))
            emit(TerminalUpdate.Output("hello"))
            if (exits) {
                emit(TerminalUpdate.Link(TerminalLink.Exited))
                return@flow
            }
            visible.first { !it }
            visible.first { it }
            emit(TerminalUpdate.Output("again"))
        }
    }

    private class RecordingPage : WebTerminalPage {
        val writes = mutableListOf<String>()
        val calls = mutableListOf<String>()

        override fun call(function: String, vararg arguments: String) {
            calls += function
            if (function == "write") writes += arguments.first()
        }
    }
}
